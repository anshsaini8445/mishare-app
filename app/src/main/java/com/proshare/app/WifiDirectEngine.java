package com.proshare.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;

public class WifiDirectEngine {

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    @SuppressLint("MissingPermission")
    public void initFastP2p(Context ctx) {
        try {
            manager = (WifiP2pManager) ctx.getSystemService(Context.WIFI_P2P_SERVICE);
            if (manager != null) {
                channel = manager.initialize(ctx, Looper.getMainLooper(), null);
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(int reason) {}
                });
            }
        } catch (Exception ignored) {}
    }

    @SuppressLint("MissingPermission")
    public void createFastGroup() {
        try {
            if (manager != null && channel != null) {
                manager.createGroup(channel, null);
            }
        } catch (Exception ignored) {}
    }

    public void stopP2p() {
        try {
            if (manager != null && channel != null) {
                manager.removeGroup(channel, null);
                manager.stopPeerDiscovery(channel, null);
            }
        } catch (Exception ignored) {}
    }
}
