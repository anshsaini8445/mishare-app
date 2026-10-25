package com.proshare.app;

import android.app.ActivityManager;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public interface Callback {
        void onRadarFound(String model, String ip, String ssid, String pass);
        void onProgress(String name, int current, int total, int percent, double speedMbps, long remainingBytes);
        void onDone(int totalFiles, long totalBytes);
        void onError(String err);
    }

    public void togglePause() {
        paused = !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    private void waitIfPaused() {
        while (paused && active) {
            try { Thread.sleep(150); } catch (Exception ignored) {}
        }
    }

    public static int getDynamicBufferSize(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(mi);
                long freeMb = mi.availMem / (1024 * 1024);
                if (freeMb > 1200) return 2097152; // 2 MB High-Speed Buffer
                if (freeMb > 600) return 1048576;  // 1 MB Buffer
                if (freeMb > 300) return 524288;   // 512 KB Buffer
            }
        } catch (Exception ignored) {}
        return 262144;
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

    // Broadcasts Receiver presence with real IP, SSID & Password for Zero-Click Radar Connect
    public void broadcastReceiverRadar(final String myIp, final String ssid, final String pass) {
        pool.execute(() -> {
            try {
                radarSocket = new DatagramSocket();
                radarSocket.setBroadcast(true);
                String safeSsid = (ssid == null || ssid.isEmpty()) ? "Hotspot" : ssid;
                String safePass = (pass == null || pass.isEmpty()) ? "none" : pass;
                byte[] msg = ("HYPERSHARE|" + Build.MODEL + "|" + myIp + "|" + safeSsid + "|" + safePass).getBytes();
                while (active) {
                    DatagramPacket pkt = new DatagramPacket(msg, msg.length, InetAddress.getByName("255.255.255.255"), RADAR_PORT);
                    radarSocket.send(pkt);
                    Thread.sleep(800);
                }
            } catch (Exception ignored) {}
        });
    }

    public void scanRadar(final Callback cb) {
        pool.execute(() -> {
            try (DatagramSocket ds = new DatagramSocket(null)) {
                ds.setReuseAddress(true);
                ds.bind(new InetSocketAddress(RADAR_PORT));
                byte[] buf = new byte[512];
                while (active) {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    ds.receive(pkt);
                    String s = new String(pkt.getData(), 0, pkt.getLength());
                    if (s.startsWith("HYPERSHARE|")) {
                        String[] p = s.split("\\|");
                        if (p.length >= 5) {
                            cb.onRadarFound(p[1], p[2], p[3], p[4]);
                        } else if (p.length >= 3) {
                            cb.onRadarFound(p[1], p[2], "Hotspot", "none");
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    // REAL HIGH-SPEED SENDER WITH TRUE BYTE-OFFSET RESUME
    public void sendFiles(final Context ctx, final String targetIp, final List<AppSession.ShareItem> list, final Callback cb) {
        acquireHardwareLocks(ctx);
        final int dynBuffer = getDynamicBufferSize(ctx);

        pool.execute(() -> {
            Socket soc = new Socket();
            try {
                soc.setTcpNoDelay(true);
                soc.setKeepAlive(true);
                soc.setTrafficClass(0x10 | 0x08);
                soc.setSendBufferSize(dynBuffer * 2);
                soc.connect(new InetSocketAddress(targetIp, PORT), 8000);

                DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(soc.getOutputStream(), dynBuffer));
                DataInputStream dis = new DataInputStream(new BufferedInputStream(soc.getInputStream()));

                dos.writeInt(list.size());
                dos.flush();

                long batchTotalBytes = 0;
                for (AppSession.ShareItem it : list) batchTotalBytes += it.size;
                long batchSentBytes = 0;

                byte[] buf = new byte[dynBuffer];
                long startTime = System.currentTimeMillis();

                for (int i = 0; i < list.size(); i++) {
                    AppSession.ShareItem item = list.get(i);
                    File f = new File(item.path);
                    long len = f.length();

                    dos.writeUTF(item.name);
                    dos.writeLong(len);
                    dos.flush();

                    // Read resume byte offset from Receiver (0 if new file, >0 if resuming broken transfer)
                    long resumeOffset = dis.readLong();
                    if (resumeOffset < 0 || resumeOffset >= len) resumeOffset = 0;
                    batchSentBytes += resumeOffset;

                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f), dynBuffer)) {
                        if (resumeOffset > 0) {
                            long skipped = 0;
                            while (skipped < resumeOffset) {
                                long s = bis.skip(resumeOffset - skipped);
                                if (s <= 0) break;
                                skipped += s;
                            }
                        }

                        long sent = resumeOffset;
                        int r;
                        while ((r = bis.read(buf)) != -1) {
                            waitIfPaused();
                            dos.write(buf, 0, r);
                            sent += r;
                            batchSentBytes += r;

                            long dt = Math.max(1, System.currentTimeMillis() - startTime);
                            double mbps = ((batchSentBytes * 8.0) / 1048576.0) / (dt / 1000.0);
                            int pct = len > 0 ? (int) ((sent * 100) / len) : 100;
                            long rem = Math.max(0, batchTotalBytes - batchSentBytes);
                            cb.onProgress(item.name, i + 1, list.size(), pct, mbps, rem);
                        }
                    }
                    dos.flush();
                    AppSession.historyVault.add(0, item);
                }

                dos.close();
                dis.close();
                soc.close();
                AppSession.addStats(ctx, batchSentBytes, 0, list.size());
                HapticSoundEngine.popSuccess(ctx);
                cb.onDone(list.size(), batchSentBytes);
            } catch (Exception e) {
                cb.onError("Connection Error: " + e.getMessage());
            }
        });
    }

    // REAL HIGH-SPEED RECEIVER WITH RANDOMACCESSFILE RESUME & GALLERY MEDIA SCANNER
    public void startReceiver(final Context ctx, final File dir, final Callback cb) {
        acquireHardwareLocks(ctx);
        final int dynBuffer = getDynamicBufferSize(ctx);

        pool.execute(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.setReceiveBufferSize(dynBuffer * 2);
                serverSocket.bind(new InetSocketAddress(PORT));

                while (active) {
                    Socket client = serverSocket.accept();
                    client.setTcpNoDelay(true);
                    client.setTrafficClass(0x10 | 0x08);

                    DataInputStream dis = new DataInputStream(new BufferedInputStream(client.getInputStream(), dynBuffer));
                    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));

                    int count = dis.readInt();
                    long totalBytes = 0;
                    byte[] buf = new byte[dynBuffer];
                    if (!dir.exists()) dir.mkdirs();

                    long startTime = System.currentTimeMillis();

                    for (int i = 0; i < count; i++) {
                        String name = dis.readUTF();
                        long len = dis.readLong();

                        if (!AppSession.hasEnoughFreeStorage(len)) {
                            dos.writeLong(0);
                            dos.flush();
                            cb.onError("Storage Full! Free some space to receive " + name);
                            break;
                        }

                        File out = new File(dir, name);
                        long existingLen = (out.exists() && out.length() < len) ? out.length() : 0;

                        // Tell Sender the exact byte position to resume from
                        dos.writeLong(existingLen);
                        dos.flush();

                        try (RandomAccessFile raf = new RandomAccessFile(out, "rw")) {
                            raf.seek(existingLen);
                            long rec = existingLen;
                            int r;
                            while (rec < len && (r = dis.read(buf, 0, (int) Math.min(buf.length, len - rec))) != -1) {
                                waitIfPaused();
                                raf.write(buf, 0, r);
                                rec += r;
                                totalBytes += r;

                                long dt = Math.max(1, System.currentTimeMillis() - startTime);
                                double mbps = ((totalBytes * 8.0) / 1048576.0) / (dt / 1000.0);
                                int pct = len > 0 ? (int) ((rec * 100) / len) : 100;
                                long rem = Math.max(0, len - rec);
                                cb.onProgress(name, i + 1, count, pct, mbps, rem);
                            }
                        }

                        // Immediately index received file into Android Gallery & Media Player
                        MediaScannerConnection.scanFile(ctx, new String[]{out.getAbsolutePath()}, null, null);
                        AppSession.historyVault.add(0, new AppSession.ShareItem(name, out.getAbsolutePath(), len, "FILES", AppSession.formatSize(len), ""));
                    }

                    dis.close();
                    dos.close();
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
                webSocket = new ServerSocket();
                webSocket.setReuseAddress(true);
                webSocket.bind(new InetSocketAddress(WEB_PORT));
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
                            while ((r = fis.read(buf)) != -1) {
                                out.write(buf, 0, r);
                            }
                        }
                        out.flush();
                        client.close();
                        return;
                    }
                }
            }

            StringBuilder rows = new StringBuilder();
            if (AppSession.selectedQueue.isEmpty()) {
                rows.append("<p style='color:#64748B;font-size:17px;'>No files selected yet. Go to Send on your phone and select files first.</p>");
            } else {
                for (int i = 0; i < AppSession.selectedQueue.size(); i++) {
                    AppSession.ShareItem it = AppSession.selectedQueue.get(i);
                    rows.append("<div style='background:#F8FAFC;border:1px solid #E2E8F0;border-radius:14px;padding:16px;margin:12px auto;max-width:560px;display:flex;justify-content:space-between;align-items:center;'>")
                        .append("<div style='text-align:left;color:#0F172A;'><b>").append(it.name).append("</b><br><span style='color:#64748B;font-size:13px;'>").append(AppSession.formatSize(it.size)).append("</span></div>")
                        .append("<a href='/download/").append(i).append("' style='background:#16CC3E;color:#FFFFFF;padding:10px 20px;border-radius:20px;text-decoration:none;font-weight:bold;'>Download</a>")
                        .append("</div>");
                }
            }

            String html = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n"
                    + "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                    + "<body style='background:#FFFFFF;color:#0F172A;font-family:sans-serif;text-align:center;padding:25px;'>"
                    + "<div style='border:1px solid #E2E8F0;border-radius:24px;padding:25px;max-width:650px;margin:auto;'>"
                    + "<h2 style='color:#1890FF;'>HyperShare Web Transfer</h2>"
                    + "<p style='color:#64748B;'>Connected Device: <b>" + Build.MODEL + "</b></p>"
                    + rows.toString()
                    + "</div></body></html>";
            out.write(html.getBytes("UTF-8"));
            out.flush();
            client.close();
        } catch (Exception ignored) {}
    }

    public static String getLocalIp(Context ctx) {
        return WifiDirectEngine.getRealDeviceIp(ctx);
    }
}
