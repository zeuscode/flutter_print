package com.missfresh.print_pda;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import java.lang.reflect.Method;

public class BluetoothDeviceListActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {

    public static final String CONNECTED_DEVICE = "connected_device";
    private TextView tv_title;
    private TextView tv_summary;
    private ListView devicesList;
    private BluetoothDevicesAdapter devicesAdapter;
    private BluetoothAdapter mBtAdapter;


    protected BroadcastReceiver mBtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent) {
                return;
            }
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                btStartDiscovery(intent);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                btFinishDiscovery(intent);
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                btStatusChanged(intent);
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                btFoundDevice(intent);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                btBondStatusChange(intent);
            } else if ("android.bluetooth.device.action.PAIRING_REQUEST".equals(action)) {
                btPairingRequest(intent);
            }
        }
    };

    private void btPairingRequest(Intent intent) {

    }

    private void btBondStatusChange(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDING://正在配对
                Log.d("BlueToothTestActivity", "正在配对......");
                break;
            case BluetoothDevice.BOND_BONDED://配对结束
                Log.d("BlueToothTestActivity", "正在配对");
                connectBlt(device);
                break;
            case BluetoothDevice.BOND_NONE://取消配对/未配对
                Log.d("BlueToothTestActivity", "取消配对");
            default:
                break;
        }
    }


    private void btFoundDevice(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (null != mBtAdapter &&  null != device) {
            devicesAdapter.addDevices(device);
        }
    }

    private void btStatusChanged(Intent intent) {
        if (mBtAdapter.getState()==BluetoothAdapter.STATE_OFF ){//蓝牙被关闭时强制打开
            mBtAdapter.enable();
        }
        if ( mBtAdapter.getState()==BluetoothAdapter.STATE_ON ){//蓝牙打开时搜索蓝牙
            searchDeviceOrOpenBluetooth();
        }

    }

    private void btFinishDiscovery(Intent intent) {
        tv_title.setText("搜索完成");
        tv_summary.setText("点击重新搜索");
    }

    private void btStartDiscovery(Intent intent) {
        tv_title.setText("正在搜索蓝牙设备…");
        tv_summary.setText("");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_devices);
        setResult(Activity.RESULT_CANCELED);
        initView();

    }

    private void initData() {
        if (!BluetoothUtil.isOpen(mBtAdapter)) {
            tv_title.setText("未连接蓝牙打印机");
            tv_summary.setText("系统蓝牙已关闭,点击开启");

        } else {
            if (!PrintUtil.isBondPrinter(this, mBtAdapter)) {
                tv_title.setText("未连接蓝牙打印机");
                tv_summary.setText("点击后搜索蓝牙打印机");
            } else {
                tv_title.setText(getPrinterName() + "已连接");
                String blueAddress = PrintUtil.getDefaultBluethoothDeviceAddress(this);
                if (TextUtils.isEmpty(blueAddress)) {
                    blueAddress = "点击后搜索蓝牙打印机";
                }
                tv_summary.setText(blueAddress);
            }
        }

    }

    private void searchDeviceOrOpenBluetooth() {
        if (BluetoothUtil.isOpen(mBtAdapter)) {
            BluetoothUtil.searchDevices(mBtAdapter);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        BluetoothUtil.registerBluetoothReceiver(mBtReceiver,this);

    }

    @Override
    protected void onStop() {
        super.onStop();
        BluetoothUtil.cancelDiscovery(mBtAdapter);
        BluetoothUtil.unregisterBluetoothReceiver(mBtReceiver,this);
    }

    private void initView() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        devicesList = findViewById(R.id.lv_devices);
        tv_title = findViewById(R.id.tv_title);
        tv_summary = findViewById(R.id.tv_summary);
        tv_summary.setOnClickListener(this);
        devicesAdapter = new BluetoothDevicesAdapter(this,null);
        devicesList.setAdapter(devicesAdapter);
        devicesList.setOnItemClickListener(this);
        initData();
        searchDeviceOrOpenBluetooth();
    }

    private String getPrinterName(){
        String dName = PrintUtil.getDefaultBluetoothDeviceName(this);
        if (TextUtils.isEmpty(dName)) {
            dName = "未知设备";
        }
        return dName;
    }

    private String getPrinterName(String dName) {
        if (TextUtils.isEmpty(dName)) {
            dName = "未知设备";
        }
        return dName;
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.tv_summary) {
            Toast.makeText(this,"已经点击",Toast.LENGTH_SHORT).show();
            searchDeviceOrOpenBluetooth();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        if (null == devicesAdapter) {
            return;
        }
        final BluetoothDevice bluetoothDevice = (BluetoothDevice) devicesAdapter.getItem(position);
        if (null == bluetoothDevice) {
            return;
        }
        showAlertDialog(bluetoothDevice);

    }

    private void showAlertDialog(final BluetoothDevice bluetoothDevice) {
        new AlertDialog.Builder(this)
                .setTitle("绑定" + getPrinterName(bluetoothDevice.getName()) + "?")
                .setMessage("点击确认绑定蓝牙设备")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            BluetoothUtil.cancelDiscovery(mBtAdapter);
                            if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                                connectBlt(bluetoothDevice);
                            } else {
                                Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                                createBondMethod.invoke(bluetoothDevice);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            PrintUtil.setDefaultBluetoothDeviceAddress(getApplicationContext(), "");
                            PrintUtil.setDefaultBluetoothDeviceName(getApplicationContext(), "");
                            Toast.makeText(BluetoothDeviceListActivity.this,"蓝牙绑定失败,请重试",Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .create()
                .show();
    }


    private void connectBlt(BluetoothDevice bluetoothDevice) {
        if (null != devicesAdapter) {
            devicesAdapter.setConnectedDeviceAddress(bluetoothDevice.getAddress());
        }
        initData();
        devicesAdapter.notifyDataSetChanged();
        PrintUtil.setDefaultBluetoothDeviceAddress(getApplicationContext(), bluetoothDevice.getAddress());
        PrintUtil.setDefaultBluetoothDeviceName(getApplicationContext(), bluetoothDevice.getName());
        Intent mIntent = new Intent();
        mIntent.putExtra(CONNECTED_DEVICE, bluetoothDevice.getAddress());
        setResult(Activity.RESULT_OK,mIntent);
    }
}
