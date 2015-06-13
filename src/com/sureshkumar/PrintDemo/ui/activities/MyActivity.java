package com.sureshkumar.PrintDemo.ui.activities;

import android.app.Activity;
import android.content.*;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.print.PrintJob;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.sureshkumar.PrintDemo.Constants;
import com.sureshkumar.PrintDemo.R;
import com.sureshkumar.PrintDemo.Util;
import com.sureshkumar.PrintDemo.services.PrintCompleteService;
import com.sureshkumar.PrintDemo.services.PrintServicesAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyActivity extends Activity implements PrintCompleteService {

    private Button mBtnPrint;

    private WifiConfiguration mPrinterConfiguration;
    private WifiConfiguration mOldWifiConfiguration;
    private WifiManager mWifiManager;
    private List<ScanResult> mScanResults = new ArrayList<ScanResult>();
    private WifiScanner mWifiScanner;

    private PrintManager mPrintManager;
    private List<PrintJob> mPrintJobs;
    private PrintJob mCurrentPrintJob;

    private File pdfFile;
    private String externalStorageDirectory;

    private Handler mPrintStartHandler = new Handler();
    private Handler mPrintCompleteHandler = new Handler();
    private String connectionInfo;

    private boolean isMobileDataConnection = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        try {
            externalStorageDirectory = Environment.getExternalStorageDirectory().toString();
            File folder = new File(externalStorageDirectory, Constants.CONTROLLER_RX_PDF_FOLDER);
            pdfFile = new File(folder, "Print_testing.pdf"); // Todo.. pdf file hardcoded -> use downloaded rx file here.
        } catch (Exception e) {
            e.printStackTrace();
        }

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mWifiScanner = new WifiScanner();

        mBtnPrint = (Button) findViewById(R.id.btnPrint);

        mBtnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // check connectivity info -> mobile or wifi.
                connectionInfo = Util.connectionInfo(MyActivity.this);

                if (connectionInfo.equalsIgnoreCase(Constants.CONTROLLER_MOBILE)) {
                    // follow mobile flow.
                    isMobileDataConnection = true;

                    if (mWifiManager.isWifiEnabled() == false) {
                        Toast.makeText(getApplicationContext(), "Enabling WiFi..", Toast.LENGTH_LONG).show();
                        mWifiManager.setWifiEnabled(true);
                    }

                    mWifiManager.startScan();

                    printerConfiguration();

                } else if (connectionInfo.equalsIgnoreCase(Constants.CONTROLLER_WIFI)) {
                    // follow wifi flow..

                    // this will get current wifiInfo and store it in shared preference.
                    Util.storeCurrentWiFiConfiguration(MyActivity.this);

                    printerConfiguration();

                } else {
                    Toast.makeText(MyActivity.this, "Please connect to Internet", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // This will give list of wifi available nearby.
            registerReceiver(mWifiScanner, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            mWifiManager.startScan();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mWifiScanner);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printerConfiguration() {

        // check printer detail is available or not.
        mPrinterConfiguration = Util.getWifiConfiguration(MyActivity.this, Constants.CONTROLLER_PRINTER);

        if (mPrinterConfiguration == null) {
            // printer configuration is not available.
            // display list of wifi available in an activity

            // Todo.. wifi will be connected here..
            showWifiListActivity(Constants.REQUEST_CODE_PRINTER);

        } else {
            // get list of wifi available. if printer configuration available then connect it.
            // else.. show list of available wifi nearby.

            boolean isPrinterAvailable = false;

            // scans nearby wifi..
            mWifiManager.startScan();

            // Todo.. need to check this wifi in scan result list..
            for (int i = 0; i < mScanResults.size(); i++) {
                if (mPrinterConfiguration.SSID.equals("\"" + mScanResults.get(i).SSID + "\"")) {
                    isPrinterAvailable = true;
                    break;
                }
            }

            if (isPrinterAvailable) {
                // connect to printer wifi and show print settings dialog and continue with print flow.
                connectToWifi(mPrinterConfiguration);

                // Todo.. continue with print flow here..
                doPrint();

            } else {
                showWifiListActivity(Constants.REQUEST_CODE_PRINTER);
            }

        }
    }

    private void connectToWifi(WifiConfiguration mWifiConfiguration) {
        mWifiManager.enableNetwork(mWifiConfiguration.networkId, true);
    }

    private void showWifiListActivity(int requestCode) {
        Intent iWifi = new Intent(this, WifiListActivity.class);
        startActivityForResult(iWifi, requestCode);
    }

    @Override
    public void onMessage(int status) {

        // get List of PrintJob from PrintManager.
        mPrintJobs = mPrintManager.getPrintJobs();

        mPrintCompleteHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                mPrintCompleteHandler.postDelayed(this, 2000);

                if (mCurrentPrintJob.getInfo().getState() == PrintJobInfo.STATE_COMPLETED) {

                    // remove that PrintJob from PrintManager.
                    for(int i = 0; i < mPrintJobs.size(); i++){
                        if(mPrintJobs.get(i).getId() == mCurrentPrintJob.getId()){
                            mPrintJobs.remove(i);
                        }
                    }

                    // switching back to previous connection..
                    switchConnection();

                    // stops handler..
                    mPrintCompleteHandler.removeCallbacksAndMessages(null);
                } else if(mCurrentPrintJob.getInfo().getState() == PrintJobInfo.STATE_FAILED){
                    switchConnection();
                    Toast.makeText(MyActivity.this, "Print Failed!", Toast.LENGTH_LONG).show();
                    mPrintCompleteHandler.removeCallbacksAndMessages(null);
                } else if(mCurrentPrintJob.getInfo().getState() == PrintJobInfo.STATE_CANCELED){
                    switchConnection();
                    Toast.makeText(MyActivity.this, "Print Cancelled!", Toast.LENGTH_LONG).show();
                    mPrintCompleteHandler.removeCallbacksAndMessages(null);
                }

            }
        }, 2000);

    }

    public void switchConnection(){
        if (!isMobileDataConnection) {
            mOldWifiConfiguration = Util.getWifiConfiguration(MyActivity.this, Constants.CONTROLLER_WIFI);

            // get list of wifi available. if wifi configuration available then connect it.
            // else.. show list of available wifi nearby.
            boolean isWifiAvailable = false;

            // scans nearby wifi.
            mWifiManager.startScan();

            // Todo.. need to check this wifi in scan result list.
            for (int i = 0; i < mScanResults.size(); i++) {
                if (mOldWifiConfiguration.SSID.equals("\"" + mScanResults.get(i).SSID + "\"")) {
                    isWifiAvailable = true;
                    break;
                }
            }

            if (isWifiAvailable) {
                // connect to printer wifi and show print settings dialog and continue with print flow.
                connectToWifi(mOldWifiConfiguration);
            } else {
                showWifiListActivity(Constants.REQUEST_CODE_WIFI);
            }
        } else {
            mWifiManager.setWifiEnabled(false);
        }
    }

    public void printDocument(File pdfFile) {
        mPrintManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);

        String jobName = getString(R.string.app_name) + " Document";

        mCurrentPrintJob = mPrintManager.print(jobName, new PrintServicesAdapter(MyActivity.this, pdfFile), null);

    }

    public void doPrint() {
        // it is taking some time to connect to printer.. so i used handler.. and waiting for its status.
        mPrintStartHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Log.d("PrinterConnection Status",""+mPrinterConfiguration.status);

                mPrintStartHandler.postDelayed(this, 1000);

                if(mPrinterConfiguration.status == WifiConfiguration.Status.CURRENT) {
                    if (Util.computePDFPageCount(pdfFile) > 0) {
                        printDocument(pdfFile);
                    } else {
                        Toast.makeText(MyActivity.this, "Can't print, Page count is zero.", Toast.LENGTH_LONG).show();
                    }
                    mPrintStartHandler.removeCallbacksAndMessages(null);
                } else if(mPrinterConfiguration.status == WifiConfiguration.Status.DISABLED){
                    Toast.makeText(MyActivity.this, "Failed to connect to printer!.", Toast.LENGTH_LONG).show();
                    mPrintStartHandler.removeCallbacksAndMessages(null);
                }
            }
        }, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_PRINTER && resultCode == Constants.RESULT_CODE_PRINTER) {
            // get printer configuration stored locally.
            mPrinterConfiguration = Util.getWifiConfiguration(MyActivity.this, Constants.CONTROLLER_PRINTER);
            doPrint();
        }
    }

    public class WifiScanner extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // fetch list of available wifi nearby.
            mScanResults = mWifiManager.getScanResults();
            Log.e("scan result size", "" + mScanResults.size());
        }
    }

}