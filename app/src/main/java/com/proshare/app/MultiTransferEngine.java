package com.proshare.app;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiTransferEngine {

    public static final int PORT = 9091;
    public static final int RADAR_PORT = 9092;
    public static final int WEB_PORT = 8080;
    private static final int BUFFER = 524288; // 512 KB Hardware Buffer

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private volatile boolean active = true;
    private ServerSocket serverSocket;
    private DatagramSocket radarSocket;

    public interface Callback {
        void onRadarFound(String model, String ip);
        void onProgress(String name, int current, int total, int percent, double speedMbps);
        void onDone(int totalFiles, long totalBytes);
        void onError(String err);
    }

    public void stop() {
        active = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        try { if (radarSocket != null) radarSocket.close(); } catch (Exception ignored) {}
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
                    Thread.sleep(1000);
                }
            } catch (Exception ignored) {}
        });
    }

    public void scanRadar(Callback cb) {
        pool.execute(() -> {
            try (DatagramSocket ds = new DatagramSocket(RADAR_PORT)) {
                byte[] buf = new byte[256];
                while (active) {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    ds.receive(pkt);
                    String s = new String(pkt.getData(), 0, pkt.getLength());
                    if (s.startsWith("HYPERSHARE|")) {
                        String[] p = s.split("\\|");
                        if (p.length >= 3) cb.onRadarFound(p[1], p[2]);
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public void sendFiles(Context ctx, String targetIp, List<AppSession.ShareItem> list, Callback cb) {
        pool.execute(() -> {
            Socket soc = new Socket();
            try {
                soc.setTcpNoDelay(true);
                soc.setKeepAlive(true);
                soc.setSendBufferSize(BUFFER * 2);
                soc.connect(new InetSocketAddress(targetIp, PORT), 6000);

                DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(soc.getOutputStream(), BUFFER));
                dos.writeInt(list.size());
                long totalBytes = 0;
                byte[] buf = new byte[BUFFER];

                for (int i = 0; i < list.size(); i++) {
                    AppSession.ShareItem item = list.get(i);
                    File f = new File(item.path);
                    long len = f.length();
                    dos.writeUTF(item.name);
                    dos.writeLong(len);

                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f), BUFFER)) {
                        long sent = 0;
                        int r;
                        long t0 = System.currentTimeMillis();
                        while ((r = bis.read(buf)) != -1) {
                            dos.write(buf, 0, r);
                            sent += r;
                            totalBytes += r;
                            long dt = Math.max(1, System.currentTimeMillis() - t0);
                            double mbps = ((sent * 8.0) / 1048576.0) / (dt / 1000.0);
                            int pct = len > 0 ? (int) ((sent * 100) / len) : 100;
                            cb.onProgress(item.name, i + 1, list.size(), pct, mbps);
                        }
                    }
                    dos.flush();
                }
                dos.close();
                soc.close();
                AppSession.addStats(ctx, totalBytes, 0, list.size());
                cb.onDone(list.size(), totalBytes);
            } catch (Exception e) {
                cb.onError(e.getMessage());
            }
        });
    }

    public void startReceiver(Context ctx, File dir, Callback cb) {
        pool.execute(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                serverSocket.setReceiveBufferSize(BUFFER * 2);
                while (active) {
                    Socket client = serverSocket.accept();
                    client.setTcpNoDelay(true);
                    DataInputStream dis = new DataInputStream(new BufferedInputStream(client.getInputStream(), BUFFER));
                    int count = dis.readInt();
                    long totalBytes = 0;
                    byte[] buf = new byte[BUFFER];
                    if (!dir.exists()) dir.mkdirs();

                    for (int i = 0; i < count; i++) {
                        String name = dis.readUTF();
                        long len = dis.readLong();
                        File out = new File(dir, name);
                        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(out), BUFFER)) {
                            long rec = 0;
                            int r;
                            long t0 = System.currentTimeMillis();
                            while (rec < len && (r = dis.read(buf, 0, (int) Math.min(buf.length, len - rec))) != -1) {
                                bos.write(buf, 0, r);
                                rec += r;
                                totalBytes += r;
                                long dt = Math.max(1, System.currentTimeMillis() - t0);
                                double mbps = ((rec * 8.0) / 1048576.0) / (dt / 1000.0);
                                int pct = len > 0 ? (int) ((rec * 100) / len) : 100;
                                cb.onProgress(name, i + 1, count, pct, mbps);
                            }
                            bos.flush();
                        }
                    }
                    dis.close();
                    client.close();
                    AppSession.addStats(ctx, 0, totalBytes, count);
                    cb.onDone(count, totalBytes);
                }
            } catch (Exception e) {
                if (active) cb.onError(e.getMessage());
            }
        });
    }

    public void startWebServer() {
        pool.execute(() -> {
            try (ServerSocket ws = new ServerSocket(WEB_PORT)) {
                while (active) {
                    Socket c = ws.accept();
                    OutputStream out = c.getOutputStream();
                    String html = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n"
                            + "<html><body style='background:#0B0F19;color:#00F5D4;font-family:sans-serif;text-align:center;padding:30px;'>"
                            + "<h2>HyperShare Pro - PC / iOS / Jio Web Share</h2>"
                            + "<p style='color:white;'>Connected Device: " + Build.MODEL + "</p>"
                            + "<p style='color:white;'>Files Queued: " + AppSession.selectedQueue.size() + "</p>"
                            + "</body></html>";
                    out.write(html.getBytes());
                    out.flush();
                    c.close();
                }
            } catch (Exception ignored) {}
        });
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
