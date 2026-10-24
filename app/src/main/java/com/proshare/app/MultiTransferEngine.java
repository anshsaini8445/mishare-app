package com.proshare.app;

import android.app.ActivityManager;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.text.format.Formatter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MultiTransferEngine {

    public static final int PORT = 9091;
    public static final int RADAR_PORT = 9092;
    public static final int WEB_PORT = 8080;

    private final ExecutorService pool = Executors.newFixedThreadPool(8);
    private volatile boolean active = true;
    private volatile boolean paused = false;
    private ServerSocket serverSocket;
    private ServerSocket webSocket;
    private DatagramSocket radarSocket;
    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;
    public static final List<String> chatMessages = new ArrayList<>();

    public interface Callback {
        void onRadarFound(String model, String ip);
        void onProgress(String name, int current, int total, int percent, double speedMbps);
        void onDone(int totalFiles, long totalBytes);
        void onError(String err);
    }

    public void pauseTransfer() { paused = true; }
    public void resumeTransfer() { paused = false; }
    public boolean isPaused() { return paused; }

    public static int getDynamicBufferSize(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(mi);
                long freeMb = mi.availMem / (1024 * 1024);
                if (freeMb > 1200) return 2097152; // 2 MB Chunk Buffer
                if (freeMb > 600) return 1048576;  // 1 MB Chunk Buffer
            }
        } catch (Exception ignored) {}
        return 524288;
    }

    // AUTO-ZIP ENGINE: Packs multiple small files into a single high-speed Zip bundle
    public static File createAutoZipBundle(Context ctx, List<AppSession.ShareItem> items) {
        try {
            File zipFile = new File(ctx.getCacheDir(), "HyperShare_Bundle_" + System.currentTimeMillis() + ".zip");
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
                byte[] buf = new byte[131072];
                for (AppSession.ShareItem it : items) {
                    File f = new File(it.path);
                    if (!f.exists()) continue;
                    zos.putNextEntry(new ZipEntry(it.name));
                    try (FileInputStream fis = new FileInputStream(f)) {
                        int r;
                        while ((r = fis.read(buf)) != -1) zos.write(buf, 0, r);
                    }
                    zos.closeEntry();
                }
            }
            return zipFile;
        } catch (Exception e) {
            return null;
        }
    }

    private void acquireHardwareLocks(Context ctx) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && wifiLock == null) {
                int lockMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        ? WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                        : WifiManager.WIFI_MODE_FULL_HIGH_PERF;
                wifiLock = wm.createWifiLock(lockMode, "HyperShare::TurboWifiLock");
                wifiLock.acquire();
            }
            PowerManager pm = (PowerManager) ctx.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (pm != null && wakeLock == null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HyperShare::TurboCpuLock");
                wakeLock.acquire(30 * 60 * 1000L);
            }
        } catch (Exception ignored) {}
    }

    public void stop() {
        active = false;
        paused = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        try { if (webSocket != null) webSocket.close(); } catch (Exception ignored) {}
        try { if (radarSocket != null) radarSocket.close(); } catch (Exception ignored) {}
        try { if (wifiLock != null && wifiLock.isHeld()) wifiLock.release(); } catch (Exception ignored) {}
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Exception ignored) {}
    }

    public void broadcastReceiverRadar(String myIp) {
        pool.execute(() -> {
            try {
                radarSocket = new DatagramSocket();
                radarSocket.setBroadcast(true);
                byte[] msg = ("HYPERSHARE|" + Build.MODEL + "|" + myIp).getBytes();
                while (active) {
                    DatagramPacket pkt = new DatagramPacket(msg, msg.length, InetAddress.getByName("255.255.255.255"), RADAR_PORT);
                    radarSocket.send(pkt);
                    Thread.sleep(900);
                }
            } catch (Exception ignored) {}
        });
    }

    // OFFLINE CHAT & CLIPBOARD TEXT BROADCAST OVER UDP
    public void sendOfflineMessage(String text) {
        pool.execute(() -> {
            try (DatagramSocket ds = new DatagramSocket()) {
                ds.setBroadcast(true);
                String payload = "HYPERCHAT|" + Build.MODEL + ": " + text;
                chatMessages.add("Me: " + text);
                byte[] data = payload.getBytes();
                ds.send(new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), RADAR_PORT));
            } catch (Exception ignored) {}
        });
    }

    public void scanRadar(Callback cb) {
        pool.execute(() -> {
            try (DatagramSocket ds = new DatagramSocket(RADAR_PORT)) {
                byte[] buf = new byte[512];
                while (active) {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    ds.receive(pkt);
                    String s = new String(pkt.getData(), 0, pkt.getLength());
                    if (s.startsWith("HYPERSHARE|")) {
                        String[] p = s.split("\\|");
                        if (p.length >= 3) cb.onRadarFound(p[1], p[2]);
                    } else if (s.startsWith("HYPERCHAT|")) {
                        chatMessages.add(s.replace("HYPERCHAT|", ""));
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    // 2MB CHUNKING + PAUSE/RESUME + AUTO-ZIP SENDER
    public void sendFiles(Context ctx, String targetIp, List<AppSession.ShareItem> rawList, Callback cb) {
        acquireHardwareLocks(ctx);
        final int dynBuffer = getDynamicBufferSize(ctx);

        pool.execute(() -> {
            List<AppSession.ShareItem> list = new ArrayList<>(rawList);
            if (AppSession.autoZipEnabled && list.size() > 1) {
                File z = createAutoZipBundle(ctx, list);
                if (z != null && z.exists()) {
                    list.clear();
                    list.add(new AppSession.ShareItem(z.getName(), z.getAbsolutePath(), z.length(), "FILES"));
                }
            }

            Socket soc = new Socket();
            try {
                soc.setTcpNoDelay(true);
                soc.setKeepAlive(true);
                soc.setTrafficClass(0x10 | 0x08);
                soc.setSendBufferSize(dynBuffer * 2);
                soc.connect(new InetSocketAddress(targetIp, PORT), 7000);

                DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(soc.getOutputStream(), dynBuffer));
                DataInputStream dis = new DataInputStream(soc.getInputStream());

                dos.writeInt(list.size());
                dos.flush();

                long totalBytes = 0;
                byte[] buf = new byte[dynBuffer];

                for (int i = 0; i < list.size(); i++) {
                    AppSession.ShareItem item = list.get(i);
                    File f = new File(item.path);
                    long len = f.length();

                    dos.writeUTF(item.name);
                    dos.writeLong(len);
                    dos.flush();

                    // Read resume offset from receiver (0 if new file, >0 if resuming broken transfer!)
                    long resumeOffset = dis.readLong();

                    try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
                        if (resumeOffset > 0 && resumeOffset < len) {
                            raf.seek(resumeOffset);
                        }
                        long sent = resumeOffset;
                        int r;
                        long t0 = System.currentTimeMillis();
                        while ((r = raf.read(buf)) != -1) {
                            while (paused && active) {
                                Thread.sleep(200);
                            }
                            dos.write(buf, 0, r);
                            sent += r;
                            totalBytes += r;
                            long dt = Math.max(1, System.currentTimeMillis() - t0);
                            double mbps = (((sent - resumeOffset) * 8.0) / 1048576.0) / (dt / 1000.0);
                            int pct = len > 0 ? (int) ((sent * 100) / len) : 100;
                            cb.onProgress(item.name, i + 1, list.size(), pct, mbps);
                        }
                    }
                    dos.flush();
                    AppSession.recordHistoryItem(ctx, item.name, item.path, len, "SENT");
                }
                dos.close();
                soc.close();
                AppSession.addStats(ctx, totalBytes, 0, list.size());
                HapticSoundEngine.popSuccess(ctx);
                cb.onDone(list.size(), totalBytes);
            } catch (Exception e) {
                cb.onError(e.getMessage());
            }
        });
    }

    // SMART STORAGE WARNING + AUTO-RESUME RECEIVER
    public void startReceiver(Context ctx, File dir, Callback cb) {
        acquireHardwareLocks(ctx);
        final int dynBuffer = getDynamicBufferSize(ctx);

        pool.execute(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                serverSocket.setReceiveBufferSize(dynBuffer * 2);
                while (active) {
                    Socket client = serverSocket.accept();
                    client.setTcpNoDelay(true);
                    client.setTrafficClass(0x10 | 0x08);
                    DataInputStream dis = new DataInputStream(new BufferedInputStream(client.getInputStream(), dynBuffer));
                    DataOutputStream dos = new DataOutputStream(client.getOutputStream());

                    int count = dis.readInt();
                    long totalBytes = 0;
                    byte[] buf = new byte[dynBuffer];
                    if (!dir.exists()) dir.mkdirs();

                    for (int i = 0; i < count; i++) {
                        String name = dis.readUTF();
                        long len = dis.readLong();

                        if (!AppSession.hasEnoughFreeSpace(len)) {
                            cb.onError("Storage Full! Need " + AppSession.formatSize(len));
                            dos.writeLong(0L);
                            dos.flush();
                            break;
                        }

                        File out = new File(dir, name);
                        long existingLen = (out.exists() && out.length() < len) ? out.length() : 0L;
                        dos.writeLong(existingLen);
                        dos.flush();

                        try (RandomAccessFile raf = new RandomAccessFile(out, "rw")) {
                            if (existingLen > 0) raf.seek(existingLen);
                            else raf.setLength(0);

                            long rec = existingLen;
                            int r;
                            long t0 = System.currentTimeMillis();
                            while (rec < len && (r = dis.read(buf, 0, (int) Math.min(buf.length, len - rec))) != -1) {
                                while (paused && active) {
                                    Thread.sleep(200);
                                }
                                raf.write(buf, 0, r);
                                rec += r;
                                totalBytes += r;
                                long dt = Math.max(1, System.currentTimeMillis() - t0);
                                double mbps = (((rec - existingLen) * 8.0) / 1048576.0) / (dt / 1000.0);
                                int pct = len > 0 ? (int) ((rec * 100) / len) : 100;
                                cb.onProgress(name, i + 1, count, pct, mbps);
                            }
                        }
                        AppSession.recordHistoryItem(ctx, name, out.getAbsolutePath(), len, "RECEIVED");
                    }
                    dis.close();
                    client.close();
                    AppSession.addStats(ctx, 0, totalBytes, count);
                    HapticSoundEngine.popSuccess(ctx);
                    cb.onDone(count, totalBytes);
                }
            } catch (Exception e) {
                if (active) cb.onError(e.getMessage());
            }
        });
    }

    public void startWebServer() {
        pool.execute(() -> {
            try {
                if (webSocket != null && !webSocket.isClosed()) return;
                webSocket = new ServerSocket(WEB_PORT);
                while (active) {
                    final Socket client = webSocket.accept();
                    pool.execute(() -> handleWebClient(client));
                }
            } catch (Exception ignored) {}
        });
    }

    private void handleWebClient(Socket client) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream out = client.getOutputStream();
            String requestLine = reader.readLine();

            if (requestLine != null && requestLine.startsWith("GET /download/")) {
                String[] parts = requestLine.split(" ");
                int idx = Integer.parseInt(parts[1].replace("/download/", "").trim());
                if (idx >= 0 && idx < AppSession.selectedQueue.size()) {
                    AppSession.ShareItem item = AppSession.selectedQueue.get(idx);
                    File file = new File(item.path);
                    if (file.exists()) {
                        String header = "HTTP/1.1 200 OK\r\n"
                                + "Content-Type: application/octet-stream\r\n"
                                + "Content-Length: " + file.length() + "\r\n"
                                + "Content-Disposition: attachment; filename=\"" + item.name + "\"\r\n\r\n";
                        out.write(header.getBytes());
                        try (FileInputStream fis = new FileInputStream(file)) {
                            byte[] buf = new byte[262144];
                            int r;
                            while ((r = fis.read(buf)) != -1) out.write(buf, 0, r);
                        }
                        out.flush();
                        client.close();
                        return;
                    }
                }
            }

            StringBuilder rows = new StringBuilder();
            if (AppSession.selectedQueue.isEmpty()) {
                rows.append("<p style='color:#FACC15;'>Select files in phone SEND tab to download here on TV / PC / Group Friends!</p>");
            } else {
                for (int i = 0; i < AppSession.selectedQueue.size(); i++) {
                    AppSession.ShareItem it = AppSession.selectedQueue.get(i);
                    rows.append("<div style='background:#151E32;border:1px solid #00F5D4;border-radius:16px;padding:16px;margin:12px auto;max-width:600px;display:flex;justify-content:space-between;align-items:center;'>")
                        .append("<div style='text-align:left;color:white;'><b>").append(it.name).append("</b><br><span style='color:#38BDF8;font-size:12px;'>").append(AppSession.formatSize(it.size)).append("</span></div>")
                        .append("<a href='/download/").append(i).append("' style='background:#00F5D4;color:#070B14;padding:10px 20px;border-radius:12px;text-decoration:none;font-weight:bold;'>⬇ DOWNLOAD</a>")
                        .append("</div>");
                }
            }

            String html = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n"
                    + "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                    + "<body style='background:#070B14;color:#00F5D4;font-family:sans-serif;text-align:center;padding:25px;'>"
                    + "<div style='border:2px solid #00F5D4;border-radius:24px;padding:25px;background:#11192E;max-width:700px;margin:auto;'>"
                    + "<h1>⚡ HyperShare Group & TV/PC Hub</h1>" + rows + "</div></body></html>";
            out.write(html.getBytes("UTF-8"));
            out.flush();
            client.close();
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("deprecation")
    public static String getLocalIp(Context ctx) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ip = wm.getConnectionInfo().getIpAddress();
            return ip == 0 ? "192.168.43.1" : Formatter.formatIpAddress(ip);
        } catch (Exception e) {
            return "192.168.43.1";
        }
    }
}
