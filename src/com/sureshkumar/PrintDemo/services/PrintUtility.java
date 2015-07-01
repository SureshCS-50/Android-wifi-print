package com.sureshkumar.PrintDemo.services;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.print.PrintJob;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.support.v4.app.Fragment;
import android.widget.Toast;
import com.sureshkumar.PrintDemo.Constants;
import com.sureshkumar.PrintDemo.ObservableSingleton;
import com.sureshkumar.PrintDemo.R;
import com.sureshkumar.PrintDemo.Util;
import com.sureshkumar.PrintDemo.observers.Observable;
import com.sureshkumar.PrintDemo.observers.Observer;
import com.sureshkumar.PrintDemo.ui.activities.WifiListActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sureshkumar on 17-06-2015.
 */
public class PrintUtility implements Observer {

    private static final int TIME_OUT = 10000;
    private static final int CONNECTION_TIME_OUT = 5000;

    private Activity mActivity;
    private Fragment mFragment = null;

    private WifiConfiguration mPrinterConfiguration;
    private WifiConfiguration mOldWifiConfiguration;
    private WifiManager mWifiManager;
    private WifiScanner mWifiScanner;
    private List<ScanResult> mScanResults = new ArrayList<ScanResult>();

    private PrintManager mPrintManager;
    private List<PrintJob> mPrintJobs;
    private PrintJob mCurrentPrintJob;

    private File pdfFile;
    private String externalStorageDirectory;

    private Handler mPrintStartHandler = new Handler();
    private Handler mPrintCompleteHandler = new Handler();
    private Handler mWifiConnectHandler = new Handler();
    private String connectionInfo;

    private boolean isMobileDataConnection = false;

    private PrintCompleteService mPrintCompleteService;

    //    Observer pattern
    private Observable mObservable;


    public PrintUtility(Activity mActivity, WifiManager mWifiManager, WifiScanner mWifiScanner) {
        this.mActivity = mActivity;
        this.mWifiManager = mWifiManager;
        this.mWifiScanner = mWifiScanner;
        mPrintCompleteService = (PrintCompleteService) mActivity;
        mObservable = ObservableSingleton.getInstance();
        mObservable.attach(this);
    }

    public PrintUtility(Activity mActivity, Fragment mFragment, WifiManager mWifiManager, WifiScanner mWifiScanner) {
        this.mActivity = mActivity;
        this.mFragment = mFragment;
        this.mWifiManager = mWifiManager;
        this.mWifiScanner = mWifiScanner;
        mPrintCompleteService = (PrintCompleteService) mFragment;
        mObservable = ObservableSingleton.getInstance();
        mObservable.attach(this);
    }

    // Todo.. assign ScanResult List to this mScanResult from onReceive() function.

    public void downloadAndPrint(String fileUrl, final String fileName) {

        new FileDownloader(mActivity, fileUrl, fileName) {
            @Override
            protected void onPostExecute(Boolean result) {

                if (!result) {
                    mObservable.notifyObserver(true);
                } else {

                    // print flow will come here.

                    try {
                        externalStorageDirectory = Environment.getExternalStorageDirectory().toString();
                        File folder = new File(externalStorageDirectory, Constants.CONTROLLER_PDF_FOLDER);
                        pdfFile = new File(folder, fileName);
                    } catch (Exception e) {
                        mObservable.notifyObserver(true);
                        e.printStackTrace();
                    }

                    print(pdfFile);

                }

            }
        }.execute("");
    }

    public void print(final File pdfFile) {

        this.pdfFile = pdfFile;

        // check connectivity info -> mobile or wifi.
        connectionInfo = Util.connectionInfo(mActivity);

        if (connectionInfo.equalsIgnoreCase(Constants.CONTROLLER_MOBILE)) {
            // follow mobile flow.
            isMobileDataConnection = true;

            if (mWifiManager.isWifiEnabled() == false) {
                mWifiManager.setWifiEnabled(true);
            }

            mWifiManager.startScan();
            setScanResults(mWifiScanner.getScanResults());

            printerConfiguration();

        } else if (connectionInfo.equalsIgnoreCase(Constants.CONTROLLER_WIFI)) {
            // follow wifi flow..

            // this will get current wifiInfo and store it in shared preference.
            Util.storeCurrentWiFiConfiguration(mActivity);

            printerConfiguration();

        } else {
            mObservable.notifyObserver(true);
        }

    }

    private void printerConfiguration() {

        // check printer detail is available or not.
        mPrinterConfiguration = Util.getWifiConfiguration(mActivity, Constants.CONTROLLER_PRINTER);

        if (mPrinterConfiguration == null) {
            // printer configuration is not available.
            // display list of wifi available in an activity

            showWifiListActivity(Constants.REQUEST_CODE_PRINTER);

        } else {
            // get list of wifi available. if printer configuration available then connect it.
            // else.. show list of available wifi nearby.

            boolean isPrinterAvailable = false;

            // scans nearby wifi..
            mWifiManager.startScan();
            setScanResults(mWifiScanner.getScanResults());


            // checks this wifi in scan result list..
            for (int i = 0; i < mScanResults.size(); i++) {
                if (mPrinterConfiguration.SSID.equals("\"" + mScanResults.get(i).SSID + "\"")) {
                    isPrinterAvailable = true;
                    break;
                }
            }

            if (isPrinterAvailable) {

                // connect to printer wifi and show print settings dialog and continue with print flow.
                connectToWifi(mPrinterConfiguration);

                // prints document.
                doPrint();

            } else {
                showWifiListActivity(Constants.REQUEST_CODE_PRINTER);
            }

        }
    }

    private void showWifiListActivity(int requestCode) {
        Intent iWifi = new Intent(mActivity, WifiListActivity.class);
        mActivity.startActivityForResult(iWifi, requestCode);
    }

    private void connectToWifi(WifiConfiguration mWifiConfiguration) {
        mWifiManager.enableNetwork(mWifiConfiguration.networkId, true);
    }

    public void doPrint() {

        try {
            // it is taking some time to connect to printer.. so i used handler.. and waiting for its status.
            mPrintStartHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    mPrintStartHandler.postDelayed(this, TIME_OUT);

                    if (mPrinterConfiguration.status == WifiConfiguration.Status.CURRENT) {
                        if (mWifiManager.getConnectionInfo().getSupplicantState() == SupplicantState.COMPLETED) {

                            if (Util.computePDFPageCount(pdfFile) > 0) {
                                printDocument(pdfFile);
                            } else {

                                AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);

                                alert.setMessage("Can't print, Page count is zero.");

                                alert.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        dialog.dismiss();
                                        switchConnection();
                                    }
                                });

                                alert.show();
                            }
                        }
                        mPrintStartHandler.removeCallbacksAndMessages(null);
                    } else {
                        Toast.makeText(mActivity, "Failed to connect to printer!.", Toast.LENGTH_LONG).show();
                        switchConnection();
                        mPrintStartHandler.removeCallbacksAndMessages(null);
                    }
                }
            }, TIME_OUT);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mActivity, "Failed to connect to printer!.", Toast.LENGTH_LONG).show();
            switchConnection();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void printDocument(File pdfFile) {

        mPrintManager = (PrintManager) mActivity.getSystemService(Context.PRINT_SERVICE);

        String jobName = mActivity.getResources().getString(R.string.app_name) + " Document";

        mCurrentPrintJob = mPrintManager.print(jobName, new PrintServicesAdapter(mActivity, mFragment, pdfFile), null);
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void completePrintJob() {
        mPrintJobs = mPrintManager.getPrintJobs();

        mPrintCompleteHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                mPrintCompleteHandler.postDelayed(this, CONNECTION_TIME_OUT);

                if (mCurrentPrintJob.getInfo().getState() == PrintJobInfo.STATE_COMPLETED) {

                    // remove that PrintJob from PrintManager.
                    for (int i = 0; i < mPrintJobs.size(); i++) {
                        if (mPrintJobs.get(i).getId() == mCurrentPrintJob.getId()) {
                            mPrintJobs.remove(i);
                        }
                    }

                    // switching back to previous connection..
                    switchConnection();

                    // stops handler..
                    mPrintCompleteHandler.removeCallbacksAndMessages(null);
                } else if (mCurrentPrintJob.getInfo().getState() == PrintJobInfo.STATE_FAILED) {
                    switchConnection();
                    Toast.makeText(mActivity, "Print Failed!", Toast.LENGTH_LONG).show();
                    mPrintCompleteHandler.removeCallbacksAndMessages(null);
                } else if (mCurrentPrintJob.getInfo().getState() == PrintJobInfo.STATE_CANCELED) {
                    switchConnection();
                    Toast.makeText(mActivity, "Print Cancelled!", Toast.LENGTH_LONG).show();
                    mPrintCompleteHandler.removeCallbacksAndMessages(null);
                }

            }
        }, CONNECTION_TIME_OUT);
    }

    public void switchConnection() {
        try {
            if (!isMobileDataConnection) {

                mOldWifiConfiguration = Util.getWifiConfiguration(mActivity, Constants.CONTROLLER_WIFI);

                // get list of wifi available. if wifi configuration available then connect it.
                // else.. show list of available wifi nearby.
                boolean isWifiAvailable = false;

                // scans nearby wifi.
                mWifiManager.startScan();
                setScanResults(mWifiScanner.getScanResults());

                // checks this wifi in scan result list.
                for (int i = 0; i < mScanResults.size(); i++) {
                    if (mOldWifiConfiguration.SSID.equals("\"" + mScanResults.get(i).SSID + "\"")) {
                        isWifiAvailable = true;
                        break;
                    }
                }

                if (isWifiAvailable) {

                    // connect to printer wifi and show print settings dialog and continue with print flow.
                    connectToWifi(mOldWifiConfiguration);

                    mWifiConnectHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mWifiConnectHandler.postDelayed(this, TIME_OUT);
                            if (mOldWifiConfiguration.status == WifiConfiguration.Status.CURRENT) {
                                if (mWifiManager.getConnectionInfo().getSupplicantState() == SupplicantState.COMPLETED) {

                                    try {
                                        mObservable.notifyObserver(true);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    mWifiConnectHandler.removeCallbacksAndMessages(null);
                                }
                            }
                        }
                    }, TIME_OUT);

                } else {
                    showWifiListActivity(Constants.REQUEST_CODE_WIFI);
                }
            } else {
                mWifiManager.setWifiEnabled(false);
                mObservable.notifyObserver(true);
            }
        } catch (Exception e) {
            mObservable.notifyObserver(true);
            e.printStackTrace();
        }
    }

    public void getPrinterConfigAndPrint() {
        mPrinterConfiguration = Util.getWifiConfiguration(mActivity, Constants.CONTROLLER_PRINTER);
        doPrint();
    }

    public void setScanResults(List<ScanResult> scanResults) {
        this.mScanResults = scanResults;
    }

    public void onPrintCancelled() {
        switchConnection();
    }

    @Override
    public void update() {
        mObservable.detach(this);
    }

    @Override
    public void updateObserver(boolean bool) {

    }

    @Override
    public void updateObserverProgress(int percentage) {

    }

}