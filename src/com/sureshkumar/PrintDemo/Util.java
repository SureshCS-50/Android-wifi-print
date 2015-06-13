package com.sureshkumar.PrintDemo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.itextpdf.text.io.RandomAccessSourceFactory;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;

/**
 * Created by Sureshkumar on 10-06-2015.
 */
public class Util {

    public static String connectionInfo(Activity mActivity) {
        String result = "not connected";

        ConnectivityManager cm = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();

        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase(Constants.CONTROLLER_WIFI)) {
                if (ni.isConnected()) {
                    result = Constants.CONTROLLER_WIFI;
                    break;
                }
            } else if (ni.getTypeName().equalsIgnoreCase(Constants.CONTROLLER_MOBILE)) {
                if (ni.isConnected()) {
                    result = Constants.CONTROLLER_MOBILE;
                    break;
                }
            }
        }

        return result;
    }

    public static void saveWifiConfiguration(Activity mActivity, WifiConfiguration mWifiConfiguration) {
        Gson mGson = new Gson();
        Type mType = new TypeToken<WifiConfiguration>() {
        }.getType();
        String sJson = mGson.toJson(mWifiConfiguration, mType);
        SharedPreferences mSharedPrefs = mActivity.getSharedPreferences(Constants.DEMO_PREFERENCES, Context.MODE_PRIVATE);
        mSharedPrefs.edit().putString(Constants.CONTROLLER_WIFI_CONFIGURATION, sJson).commit();
    }

    public static void savePrinterConfiguration(Activity mActivity, WifiConfiguration mPrinterConfiguration) {
        Gson mGson = new Gson();
        Type mType = new TypeToken<WifiConfiguration>() {
        }.getType();
        String sJson = mGson.toJson(mPrinterConfiguration, mType);
        SharedPreferences mSharedPrefs = mActivity.getSharedPreferences(Constants.DEMO_PREFERENCES, Context.MODE_PRIVATE);
        mSharedPrefs.edit().putString(Constants.CONTROLLER_PRINTER_CONFIGURATION, sJson).commit();
    }

    public static WifiConfiguration getWifiConfiguration(Activity mActivity, String configurationType) {
        WifiConfiguration mWifiConfiguration = new WifiConfiguration();
        Gson mGson = new Gson();
        SharedPreferences mSharedPrefs = mActivity.getSharedPreferences(Constants.DEMO_PREFERENCES, Context.MODE_PRIVATE);
        Type mWifiConfigurationType = new TypeToken<WifiConfiguration>() {
        }.getType();
        String mWifiJson = "";
        if (configurationType.equalsIgnoreCase(Constants.CONTROLLER_WIFI)) {
            mWifiJson = mSharedPrefs.getString(Constants.CONTROLLER_WIFI_CONFIGURATION, "");
        } else if (configurationType.equalsIgnoreCase(Constants.CONTROLLER_PRINTER)) {
            mWifiJson = mSharedPrefs.getString(Constants.CONTROLLER_PRINTER_CONFIGURATION, "");
        }
        if (!mWifiJson.isEmpty()) {
            mWifiConfiguration = mGson.fromJson(mWifiJson, mWifiConfigurationType);
        } else {
            mWifiConfiguration = null;
        }
        return mWifiConfiguration;
    }

    public static void storeCurrentWiFiConfiguration(Activity mActivity) {
        try {
            WifiManager wifiManager = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                WifiConfiguration mWifiConfiguration = new WifiConfiguration();
                mWifiConfiguration.networkId = connectionInfo.getNetworkId();
                mWifiConfiguration.BSSID = connectionInfo.getBSSID();
                mWifiConfiguration.hiddenSSID = connectionInfo.getHiddenSSID();
                mWifiConfiguration.SSID = connectionInfo.getSSID();

                // store it for future use -> after print is complete you need to reconnect wifi to this network.
                saveWifiConfiguration(mActivity, mWifiConfiguration);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int computePDFPageCount(File file) {
        RandomAccessFile raf = null;
        int pages = 0;
        try {
            raf = new RandomAccessFile(file, "r");

            RandomAccessFileOrArray pdfFile = new RandomAccessFileOrArray(
                    new RandomAccessSourceFactory().createSource(raf));
            PdfReader reader = new PdfReader(pdfFile, new byte[0]);
            pages = reader.getNumberOfPages();
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pages;
    }
}