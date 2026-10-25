package com.proshare.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class WifiDirectEngine {

    public interface GroupReadyListener {
        void onGroupReady(String ssid, String password, String groupOwnerIp, String bandLabel);
    }

    public interface PeerConnectListener {
        void onConnectedToReceiver(String receiverIp);
        void onStatusUpdate(String status);
    }

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ConnectivityManager.NetworkCallback networkCallback;

    // Automatically detects highest hardware band: 6GHz -> 5GHz -> 2.4GHz
    public static String getBestSupportedBandName(Context ctx) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm.is6GHzBandSupported()) {
                    return "6GHz Ultra Band";
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && wm.is5GHzBandSupported()) {
                    return "5GHz High-Speed Band";
                }
            }
        } catch (Exception ignored) {}
        return "2.4GHz Turbo Band";
    }

    @SuppressLint("MissingPermission")
    public void startReceiverGroup(final Context ctx, final GroupReadyListener listener) {
        try {
            final String bandLabel = getBestSupportedBandName(ctx);
            manager = (WifiP2pManager) ctx.getSystemService(Context.WIFI_P2P_SERVICE);
            if (manager == null) {
                listener.onGroupReady("Hotspot_" + Build.MODEL, "12345678", getRealDeviceIp(ctx), bandLabel);
                return;
            }
            channel = manager.initialize(ctx, Looper.getMainLooper(), null);

            // Clean any stale group first, then create fresh high-speed group
            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    createOptimalBandGroup(ctx, bandLabel, listener);
                }
                @Override
                public void onFailure(int reason) {
                    createOptimalBandGroup(ctx, bandLabel, listener);
                }
            });
        } catch (Exception e) {
            listener.onGroupReady("DIRECT-" + Build.MODEL, "hyper1234", getRealDeviceIp(ctx), "Auto Band");
        }
    }

    @SuppressLint("MissingPermission")
    private void createOptimalBandGroup(final Context ctx, final String bandLabel, final GroupReadyListener listener) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // GROUP_OWNER_BAND_AUTO automatically selects 6GHz -> 5GHz -> 2.4GHz highest supported band!
                WifiP2pConfig config = new WifiP2pConfig.Builder()
                        .setGroupOperatingBand(WifiP2pConfig.GROUP_OWNER_BAND_AUTO)
                        .build();
                manager.createGroup(channel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        pollRealGroupCredentials(ctx, bandLabel, listener, 0);
                    }
                    @Override
                    public void onFailure(int reason) {
                        manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                            @Override public void onSuccess() { pollRealGroupCredentials(ctx, bandLabel, listener, 0); }
                            @Override public void onFailure(int r) {
                                listener.onGroupReady("Hotspot-" + Build.MODEL, "Direct IP Mode", getRealDeviceIp(ctx), bandLabel);
                            }
                        });
                    }
                });
            } else {
                manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        pollRealGroupCredentials(ctx, bandLabel, listener, 0);
                    }
                    @Override
                    public void onFailure(int reason) {
                        listener.onGroupReady("Hotspot-" + Build.MODEL, "Direct IP Mode", getRealDeviceIp(ctx), bandLabel);
                    }
                });
            }
        } catch (Exception e) {
            listener.onGroupReady("Hotspot-" + Build.MODEL, "Direct IP Mode", getRealDeviceIp(ctx), bandLabel);
        }
    }

    @SuppressLint("MissingPermission")
    private void pollRealGroupCredentials(final Context ctx, final String bandLabel, final GroupReadyListener listener, final int attempt) {
        if (manager == null || channel == null) return;
        manager.requestGroupInfo(channel, group -> {
            if (group != null && group.getNetworkName() != null && group.getPassphrase() != null) {
                String ssid = group.getNetworkName();
                String pass = group.getPassphrase();
                // Standard Android Wi-Fi Direct Group Owner IP is 192.168.49.1
                String ownerIp = getP2pOrLocalIp(ctx);
                listener.onGroupReady(ssid, pass, ownerIp, bandLabel);
            } else if (attempt < 12) {
                handler.postDelayed(() -> pollRealGroupCredentials(ctx, bandLabel, listener, attempt + 1), 350);
            } else {
                listener.onGroupReady("Hotspot-" + Build.MODEL, "Same Wi-Fi / Hotspot", getRealDeviceIp(ctx), bandLabel);
            }
        });
    }

    // SENDER SIDE: Automatically joins Receiver's Wi-Fi Direct / Hotspot network when QR is scanned
    @SuppressWarnings("deprecation")
    public void autoConnectToReceiverWifi(final Context ctx, final String ssid, final String password, final String targetIp, final PeerConnectListener listener) {
        if (ssid == null || ssid.isEmpty() || ssid.startsWith("Hotspot-") || password.equals("Direct IP Mode")) {
            listener.onConnectedToReceiver(targetIp);
            return;
        }

        // If already on the same subnet, connect immediately without switching Wi-Fi
        String myIp = getRealDeviceIp(ctx);
        if (myIp.startsWith("192.168.49.") && targetIp.startsWith("192.168.49.")) {
            listener.onConnectedToReceiver(targetIp);
            return;
        }

        listener.onStatusUpdate("Connecting to " + ssid + " (" + getBestSupportedBandName(ctx) + ")...");

        try {
            final ConnectivityManager cm = (ConnectivityManager) ctx.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cm != null) {
                WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                        .setSsid(ssid)
                        .setWpa2Passphrase(password)
                        .build();
                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .setNetworkSpecifier(specifier)
                        .build();

                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        cm.bindProcessToNetwork(network);
                        handler.postDelayed(() -> listener.onConnectedToReceiver(targetIp), 400);
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        // Fallback: try direct socket if already reachable via Hotspot/LAN
                        handler.post(() -> listener.onConnectedToReceiver(targetIp));
                    }
                };
                cm.requestNetwork(request, networkCallback);
            } else {
                // Android 5.0 to Android 9.0 Silent Auto-Connect
                WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    if (!wm.isWifiEnabled()) wm.setWifiEnabled(true);
                    WifiConfiguration conf = new WifiConfiguration();
                    conf.SSID = "\"" + ssid + "\"";
                    conf.preSharedKey = "\"" + password + "\"";
                    int netId = wm.addNetwork(conf);
                    wm.disconnect();
                    wm.enableNetwork(netId, true);
                    wm.reconnect();
                }
                handler.postDelayed(() -> listener.onConnectedToReceiver(targetIp), 2500);
            }
        } catch (Exception e) {
            listener.onConnectedToReceiver(targetIp);
        }
    }

    @SuppressLint("MissingPermission")
    public void discoverNearbyPeers(Context ctx) {
        try {
            manager = (WifiP2pManager) ctx.getSystemService(Context.WIFI_P2P_SERVICE);
            if (manager != null) {
                channel = manager.initialize(ctx, Looper.getMainLooper(), null);
                manager.discoverPeers(channel, null);
            }
        } catch (Exception ignored) {}
    }

    public static String getP2pOrLocalIp(Context ctx) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                String name = intf.getName().toLowerCase();
                if (name.contains("p2p")) {
                    for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "192.168.49.1";
    }

    public static String getRealDeviceIp(Context ctx) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            String fallback = null;
            for (NetworkInterface intf : interfaces) {
                if (!intf.isUp()) continue;
                String name = intf.getName().toLowerCase();
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (name.contains("p2p") || ip.startsWith("192.168.49.")) return ip;
                        if (name.contains("ap") || name.contains("wlan") || name.contains("swlan")) {
                            fallback = ip;
                        }
                    }
                }
            }
            if (fallback != null) return fallback;
        } catch (Exception ignored) {}
        return "192.168.43.1";
    }

    public void stopP2p(Context ctx) {
        try {
            if (manager != null && channel != null) {
                manager.removeGroup(channel, null);
                manager.stopPeerDiscovery(channel, null);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && networkCallback != null) {
                ConnectivityManager cm = (ConnectivityManager) ctx.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    cm.bindProcessToNetwork(null);
                    cm.unregisterNetworkCallback(networkCallback);
                }
            }
        } catch (Exception ignored) {}
    }
}
