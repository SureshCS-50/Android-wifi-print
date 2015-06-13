package com.sureshkumar.PrintDemo.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.sureshkumar.PrintDemo.R;
import com.sureshkumar.PrintDemo.Util;
import com.sureshkumar.PrintDemo.adapter.WifiAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sureshkumar on 10-06-2015.
 */
public class WifiListActivity extends Activity implements View.OnClickListener {

    private ListView mListWifi;
    private Button mBtnScan;

    private WifiManager mWifiManager;
    private WifiAdapter adapter;
    private WifiListener mWifiListener;

    private List<ScanResult> mScanResults = new ArrayList<ScanResult>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_list);

        mBtnScan = (Button) findViewById(R.id.btnNext);
        mBtnScan.setOnClickListener(this);

        mListWifi = (ListView) findViewById(R.id.wifiList);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (mWifiManager.isWifiEnabled() == false) {
            Toast.makeText(getApplicationContext(), "wifi is disabled.. making it enabled", Toast.LENGTH_LONG).show();
            mWifiManager.setWifiEnabled(true);
        }

        mWifiListener = new WifiListener();

        adapter = new WifiAdapter(WifiListActivity.this, mScanResults);
        mListWifi.setAdapter(adapter);

        mListWifi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                connectToWifi(i);
            }
        });

    }

    @Override
    public void onClick(View view) {
        mWifiManager.startScan();
        Toast.makeText(this, "Scanning....", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            registerReceiver(mWifiListener, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            mWifiManager.startScan();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mWifiListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectToWifi(int position) {

        final ScanResult item = mScanResults.get(position);

        String Capabilities = item.capabilities;

        //Then you could add some code to check for a specific security type.
        if (Capabilities.contains("WPA")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(WifiListActivity.this);
            builder.setTitle("Password:");

            // Set up the input
            final EditText input = new EditText(WifiListActivity.this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String m_Text = input.getText().toString();
                    WifiConfiguration wifiConfiguration = new WifiConfiguration();
                    wifiConfiguration.SSID = "\"" + item.SSID + "\"";
                    wifiConfiguration.preSharedKey = "\"" + m_Text + "\"";
                    wifiConfiguration.hiddenSSID = true;
                    wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
                    wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA
                    wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                    wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                    int res = mWifiManager.addNetwork(wifiConfiguration);
                    Log.d("WifiPreference", "add Network returned " + res);
                    boolean b = mWifiManager.enableNetwork(res, true);
                    Log.d("WifiPreference", "enableNetwork returned " + b);

                    finishActivity(wifiConfiguration, res);

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();


        } else if (Capabilities.contains("WEP")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(WifiListActivity.this);
            builder.setTitle("Title");

            // Set up the input
            final EditText input = new EditText(WifiListActivity.this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String m_Text = input.getText().toString();
                    WifiConfiguration wifiConfiguration = new WifiConfiguration();
                    wifiConfiguration.SSID = "\"" + item.SSID + "\"";
                    wifiConfiguration.wepKeys[0] = "\"" + m_Text + "\"";
                    wifiConfiguration.wepTxKeyIndex = 0;
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    int res = mWifiManager.addNetwork(wifiConfiguration);
                    Log.d("WifiPreference", "add Network returned " + res);
                    boolean b = mWifiManager.enableNetwork(res, true);
                    Log.d("WifiPreference", "enableNetwork returned " + b);

                    finishActivity(wifiConfiguration, res);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();

        } else {

            WifiConfiguration wifiConfiguration = new WifiConfiguration();
            wifiConfiguration.SSID = "\"" + item.SSID + "\"";
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            int res = mWifiManager.addNetwork(wifiConfiguration);
            Log.d("WifiPreference", "add Network returned " + res);
            boolean b = mWifiManager.enableNetwork(res, true);
            Log.d("WifiPreference", "enableNetwork returned " + b);

            finishActivity(wifiConfiguration, res);
        }

    }

    private void finishActivity(WifiConfiguration mWifiConfiguration, int networkId) {
        // Todo.. call finish() here..
        // Don't know how to get class of device -> wifi or printer..
        // for now whatever user clicks assuming that.. It is a printer.. and saving that details in shared Prefs.

        mWifiConfiguration.networkId = networkId;

        Util.savePrinterConfiguration(WifiListActivity.this, mWifiConfiguration);
        Intent intent = new Intent();
        setResult(1001, intent);
        finish();
    }

    public class WifiListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mScanResults = mWifiManager.getScanResults();
            Log.e("scan result size ", "" + mScanResults.size());
            adapter.setElements(mScanResults);
        }
    }
}