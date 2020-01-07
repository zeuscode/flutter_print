package com.missfresh.print_pda;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android_print_sdk.PrinterType;
import com.android_print_sdk.bluetooth.BluetoothPrinter;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.app.Activity.RESULT_OK;

public class PrintPdaPlugin implements MethodCallHandler, FlutterPlugin, PluginRegistry.ActivityResultListener, EventChannel.StreamHandler, ActivityAware {

    private static final int REQUEST_CODE = 0x01;
    private static final String TAG = PrintPdaPlugin.class.getSimpleName();
    private Activity activity;
    private Application application;
    private Result result;
    private static final String UNSUPPORTEDDEVICE = "UnsupportedDevice";
    private static final String OPENFAILD = "open_failed";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothPrinter mPrinter;
    private Boolean is58mm = true;
    private EventChannel eventChannel;
    private MethodChannel methodChannel;
    private EventChannel.EventSink eventSink;
    private boolean isConnected;

    private BroadcastReceiver blueReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                if (mPrinter != null && device != null && isConnected) {
                    if (mPrinter.getMacAddress().equals(device.getAddress())) {
                        mPrinter.closeConnection();
                    }
                }
            }
        }
    };


    private Handler bluetoothHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            isConnected = false;
            switch (msg.what) {
                case BluetoothPrinter.Handler_Connect_Connecting:
                    eventSink.success("connecting");
                    break;
                case BluetoothPrinter.Handler_Connect_Success:
                    Toast.makeText(activity, "print machine connect success", Toast.LENGTH_SHORT).show();
                    eventSink.success("connected");
                    isConnected = true;
                    break;
                case BluetoothPrinter.Handler_Connect_Failed:
                    eventSink.success("failed");
                    break;
                case BluetoothPrinter.Handler_Connect_Closed:
                    eventSink.success("close");
                    break;
                case BluetoothPrinter.Handler_Message_Read:
                    if (msg != null) {
                        if (msg.obj != null) {
                            int states = (int) msg.obj;
                            eventSink.success("error:" + states);
                        } else {
                            eventSink.success("error:" + 996);
                        }
                    } else {
                        eventSink.error("UNAVAILABLE", "unavailable", null);
                    }
                    break;
            }
        }
    };

    public static void registerWith(Registrar registrar) {
        final PrintPdaPlugin instance = new PrintPdaPlugin();
        instance.onAttachedToEngine(registrar.messenger());
        registrar.addActivityResultListener(instance);

    }

    private void onAttachedToEngine(BinaryMessenger messenger) {
        eventChannel = new EventChannel(messenger, "plugins.flutter.io/missfresh.device_status");
        eventChannel.setStreamHandler(this);
        methodChannel = new MethodChannel(messenger, "plugins.flutter.io/missfresh.print");
        methodChannel.setMethodCallHandler(this);

    }

    private void requestPermission() {
        if (null != activity) {
            CheckPermissionUtils.initPermission(activity);
        }
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        this.result = result;
        switch (call.method) {
            case "init":
                initCallParams(call);
                if (checkBluetoothDevice()) {
                    result.success("initial success");
                }else{
                    result.error("996", "print machine initial failed", null);
                }
                break;
            case "open":
                showBluetoothDevicesView();
                break;
            case "print":
                if(checkBluetoothDevice()) {
                    if (null != mPrinter) {
                        Log.e(TAG, "打印信息：\n" + mPrinter.getMacAddress() + "\n" + mPrinter.isConnected());
                        try {

                            PrintUtil.print(mPrinter,call.argument("text").toString());
                        }catch (Exception e){
                            result.error("996",e.getLocalizedMessage(),e.getMessage());
                        }
                    } else {
                        result.error("996", "print machine initial failed", null);
                    }
                }
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void initCallParams(@NonNull MethodCall call) {
        if (call.hasArgument("is_58mm")) {
            is58mm = (Boolean) call.argument("is_58mm");
        }
    }

    private void showBluetoothDevicesView() {
        if (activity != null) {
            Intent intent = new Intent(activity, BluetoothDeviceListActivity.class);
            activity.startActivityForResult(intent, REQUEST_CODE);
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            return false;
        } else if (requestCode == REQUEST_CODE) {
            if (null != eventSink) {
                initPrintConnect();
            }
        }
        return false;
    }

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        this.application = (Application) binding.getApplicationContext();
        onAttachedToEngine(binding.getBinaryMessenger());
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
        activity = null;
        methodChannel.setMethodCallHandler(null);
        methodChannel = null;
        eventChannel.setStreamHandler(null);
        eventChannel = null;
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        eventSink = events;
    }

    @Override
    public void onCancel(Object arguments) {

    }

    public boolean checkBluetoothDevice() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            result.error(UNSUPPORTEDDEVICE, "不支持蓝牙设备", null);
            return false;
        }
        if (BluetoothUtil.isOpen(mBluetoothAdapter)) {
            if (TextUtils.isEmpty(getBondPrintDevice())) {
                showBluetoothDevicesView();
            } else if (null == mPrinter || false == mPrinter.isConnected()) {
                initPrintConnect();
            }
            return true;
        } else {
            BluetoothUtil.turnOnBluetooth();
            result.error(OPENFAILD, "蓝牙打开失败", null);
        }

        return false;
    }

    private void initPrintConnect() {
        removeFrontDevicePrintInstance();
        Log.e(TAG, "打印设备" + mBluetoothAdapter.getRemoteDevice(getBondPrintDevice()));
        mPrinter = new BluetoothPrinter(mBluetoothAdapter.getRemoteDevice(getBondPrintDevice()));
        mPrinter.setCurrentPrintType(is58mm?PrinterType.Printer_58:PrinterType.Printer_80);
        mPrinter.setEncoding("GBK");
        mPrinter.setHandler(bluetoothHandler);
        mPrinter.openConnection();
    }

    private void closePrint() {
        try {
            if (null != mPrinter) {
                mPrinter.closeConnection();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public String getBondPrintDevice() {
        return PrintUtil.getDefaultBluethoothDeviceAddress(this.application);
    }

    private void removeFrontDevicePrintInstance() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_CLASS_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        this.activity.registerReceiver(blueReceiver, filter);
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        activity = binding.getActivity();
        requestPermission();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {
        if (null != activity) {
            activity.unregisterReceiver(blueReceiver);
        }
        if (null != mPrinter) {
            closePrint();
        }

    }
}
