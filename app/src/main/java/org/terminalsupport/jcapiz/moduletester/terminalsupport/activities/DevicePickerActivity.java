package org.terminalsupport.jcapiz.moduletester.terminalsupport.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.terminalsupport.jcapiz.moduletester.R;
import org.terminalsupport.jcapiz.moduletester.terminalsupport.bluetooth.BluetoothManager;

/**
 * Created by jcapiz on 19/09/15.
 */
public class DevicePickerActivity extends Activity {

    private ArrayAdapter<String> arrAdapter;
    private BluetoothManager manager;
    private ListView devicesList;

    public void addDevice(String device){
        arrAdapter.add(device);
        arrAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_chooser);
        manager = new BluetoothManager(this);
        if( manager.getBluetoothAdapter() != null ) {
            manager.registerReceiver();
            manager.loadPairedDevices();
            manager.justTryToEnableBluetooth();
            devicesList = (ListView) findViewById(R.id.devices_list);
            arrAdapter = manager.getmArrayAdapter();
            devicesList.setAdapter(arrAdapter);
            devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String[] deviceData = ((TextView) view).getText().toString().split("\n");
                    String deviceAddr = deviceData[1];
                    Intent i = new Intent();
                    i.putExtra("device_addr", deviceAddr);
                    setResult(RESULT_OK, i);
                    finish();
                }
            });

        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        manager.unregisterReceiver();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case BluetoothManager.REQUEST_ENABLE_BT:
                    manager.registerReceiver();
                    manager.loadPairedDevices();
                    arrAdapter.notifyDataSetChanged();
                    break;
            }
        }
    }
}
