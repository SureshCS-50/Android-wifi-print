package com.sureshkumar.PrintDemo.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.List;

/**
 * Created by Sureshkumar on 17-06-2015.
 */
public class WifiScanner extends BroadcastReceiver {

    private List<ScanResult> mScanResults;

    @Override
    public void onReceive(Context context, Intent intent) {
        // fetch list of available wifi nearby.
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        setScanResults(mWifiManager.getScanResults());
    }

    public List<ScanResult> getScanResults() {
        return mScanResults;
    }

    public void setScanResults(List<ScanResult> mScanResults) {
        this.mScanResults = mScanResults;
    }
}