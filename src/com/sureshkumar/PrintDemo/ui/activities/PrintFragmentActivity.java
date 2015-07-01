package com.sureshkumar.PrintDemo.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.sureshkumar.PrintDemo.Constants;
import com.sureshkumar.PrintDemo.R;
import com.sureshkumar.PrintDemo.services.PrintFragmentCommunicator;
import com.sureshkumar.PrintDemo.ui.fragments.PrintFragment;

/**
 * Created by Sureshkumar on 01-07-2015.
 */
public class PrintFragmentActivity extends FragmentActivity {

    public PrintFragmentCommunicator mPrintFragmentCommunicator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_fragment);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame, new PrintFragment())
                .commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Constants.REQUEST_CODE_PRINTER && resultCode == Constants.RESULT_CODE_PRINTER) {
            if(mPrintFragmentCommunicator!=null){
                mPrintFragmentCommunicator.respondOnPrinterSelect();
            }
        } else if (requestCode == Constants.REQUEST_CODE_WIFI && resultCode == Constants.RESULT_CODE_PRINTER) {
            if(mPrintFragmentCommunicator!=null){
                mPrintFragmentCommunicator.respondOnPrintComplete();
            }
        } else if (requestCode == Constants.REQUEST_CODE_PRINTER && resultCode == Constants.RESULT_CODE_PRINTER_CONNECT_FAILED) {
            if(mPrintFragmentCommunicator!=null){
                mPrintFragmentCommunicator.respondOnPrinterSelectCancelled();
            }
        }
    }
}