package com.sureshkumar.PrintDemo.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.Button;
import android.widget.Toast;
import com.sureshkumar.PrintDemo.Constants;
import com.sureshkumar.PrintDemo.ObservableSingleton;
import com.sureshkumar.PrintDemo.R;
import com.sureshkumar.PrintDemo.Util;
import com.sureshkumar.PrintDemo.observers.Observable;
import com.sureshkumar.PrintDemo.observers.Observer;
import com.sureshkumar.PrintDemo.services.PrintCompleteService;
import com.sureshkumar.PrintDemo.services.PrintFragmentCommunicator;
import com.sureshkumar.PrintDemo.services.PrintUtility;
import com.sureshkumar.PrintDemo.services.WifiScanner;
import com.sureshkumar.PrintDemo.ui.activities.PrintFragmentActivity;

import java.io.File;

/**
 * Created by Sureshkumar on 01-07-2015.
 */
public class PrintFragment extends Fragment implements PrintCompleteService, Observer, PrintFragmentCommunicator {

    Dialog mPrintDialog;
    private Button mBtnPrint, mBtnDownloadAndPrint;
    private File pdfFile;
    private String externalStorageDirectory;
    private Activity mActivity;
    //    print variables..
    private PrintUtility mPrintUtility;
    private WifiScanner mWifiScanner;
    private WifiManager mWifiManager;
    //    Observable pattern..
    private Observable mObservable;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View mView = inflater.inflate(R.layout.fragemnt_print, container, false);

        try {
            externalStorageDirectory = Environment.getExternalStorageDirectory().toString();
            File folder = new File(externalStorageDirectory, Constants.CONTROLLER_PDF_FOLDER);
            pdfFile = new File(folder, "file name with extension");
        } catch (Exception e) {
            e.printStackTrace();
        }

        mObservable = ObservableSingleton.getInstance();

        mWifiManager = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
        mWifiScanner = new WifiScanner();
        mPrintUtility = new PrintUtility(mActivity, PrintFragment.this, mWifiManager, mWifiScanner);

        mBtnPrint = (Button) mView.findViewById(R.id.btnPrint);
        mBtnDownloadAndPrint = (Button) mView.findViewById(R.id.btnDownloadAndPrint);

        mBtnDownloadAndPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPrintDialog.show();
                mObservable.attach(PrintFragment.this);
                if (Util.hasConnection(mActivity)) {
                    mPrintUtility.downloadAndPrint("fileUrl", "fileName with extension");
                } else {
                    mObservable.notifyObserver(true);
                    Toast.makeText(mActivity, "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mObservable.attach(PrintFragment.this);
                mPrintUtility.print(pdfFile);
            }
        });

        initPrintDialog();

        return mView;
    }

    private void initPrintDialog() {
        mPrintDialog = new Dialog(mActivity);
        mPrintDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = mPrintDialog.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        mPrintDialog.setContentView(R.layout.dialog_progressbar);

        mPrintDialog.setCancelable(true);
        mPrintDialog.setCanceledOnTouchOutside(false);

        mPrintDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);

                    alert.setMessage("Do you want to cancel printing?");

                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            mPrintUtility.onPrintCancelled();

                        }
                    });

                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });

                    alert.show();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
        ((PrintFragmentActivity) activity).mPrintFragmentCommunicator = this;
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            // This will give list of wifi available nearby.
            mActivity.registerReceiver(mWifiScanner, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            mWifiManager.startScan();
            mPrintUtility.setScanResults(mWifiScanner.getScanResults());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            mActivity.unregisterReceiver(mWifiScanner);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
        try {
            if (mPrintDialog != null && mPrintDialog.isShowing()) {
                mPrintDialog.dismiss();
            }
            mObservable.detach(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateObserver(boolean bool) {
        try {
            mObservable.detach(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateObserverProgress(int percentage) {

    }

    @Override
    public void onMessage(int status) {
        mPrintUtility.completePrintJob();
    }

    @Override
    public void respondAfterWifiSwitch() {

    }

    @Override
    public void respondOnPrintComplete() {

    }

    @Override
    public void respondOnPrinterSelect() {
        try {
            if (!mPrintDialog.isShowing())
                mPrintDialog.show();

            mPrintUtility.getPrinterConfigAndPrint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void respondOnPrinterSelectCancelled() {
        if (!mPrintDialog.isShowing())
            mPrintDialog.show();

        mPrintUtility.onPrintCancelled();
    }
}