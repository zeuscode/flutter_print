package com.missfresh.print_pda;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class BluetoothDevicesAdapter extends BaseAdapter {

    private ArrayList<BluetoothDevice> mDevices;
    private LayoutInflater mInflater;
    private String mConnectedDeviceAddress;

    public BluetoothDevicesAdapter(Context mContext, ArrayList<BluetoothDevice> mDevices) {
        this.mInflater = LayoutInflater.from(mContext);
        this.mDevices = null == mDevices ? new ArrayList<BluetoothDevice>() : mDevices;
        mConnectedDeviceAddress = PrintUtil.getDefaultBluethoothDeviceAddress(mContext);
    }

    public void setDevices(ArrayList<BluetoothDevice> mDevices){
        if (null == mDevices){
            mDevices = new ArrayList<>();
        }else {
            this.mDevices = mDevices;
        }
        this.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetChanged() {
        if (null != this.mDevices){
            this.mDevices = sortByDevices(this.mDevices);
        }
        super.notifyDataSetChanged();
    }

    private ArrayList<BluetoothDevice> sortByDevices(ArrayList<BluetoothDevice> mDevices) {
        if (1 == mDevices.size()){
            return mDevices;
        }
        ArrayList<BluetoothDevice> bondDevices = new ArrayList<>();
        ArrayList<BluetoothDevice> unBoundDevices = new ArrayList<>();
        for (BluetoothDevice device:mDevices){
            if (device.getBondState() == BluetoothDevice.BOND_BONDED){
                bondDevices.add(device);
            }else {
                unBoundDevices.add(device);
            }
        }

        mDevices.clear();
        mDevices.addAll(bondDevices);
        mDevices.addAll(unBoundDevices);
        bondDevices.clear();
        bondDevices = null;
        unBoundDevices.clear();
        unBoundDevices = null;
        return mDevices;
    }


    public void addDevices(ArrayList<BluetoothDevice> mDevices) {
        if (null == mDevices) {
            return;
        }
        for (BluetoothDevice bluetoothDevice : mDevices) {
            addDevices(bluetoothDevice);
        }
    }

    public void addDevices(BluetoothDevice mDevice) {
        if (null == mDevice) {
            return;
        }
        if (!this.mDevices.contains(mDevice)) {
            this.mDevices.add(mDevice);
            this.notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public BluetoothDevice getItem(int position) {
        return mDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    public void setConnectedDeviceAddress(String macAddress) {
        this.mConnectedDeviceAddress = macAddress;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (null != convertView){
            holder = (ViewHolder) convertView.getTag();
        }else{
            convertView = mInflater.inflate(R.layout.item_device,parent,false);
            holder = new ViewHolder();
            if (null != convertView) {
                convertView.setTag(holder);
            }
        }
        holder.name =  convertView.findViewById(R.id.tv_bt_name);
        holder.address =  convertView.findViewById(R.id.tv_bt_address);
        holder.bond = convertView.findViewById(R.id.tv_has_bond);

        BluetoothDevice bluetoothDevice  = mDevices.get(position);
        String deviceName = TextUtils.isEmpty(bluetoothDevice.getName()) ? "未知设备":bluetoothDevice.getName();
        holder.name.setText(deviceName);
        String deviceAddress = TextUtils.isEmpty(bluetoothDevice.getAddress())?"未知地址":bluetoothDevice.getAddress();
        holder.address.setText(deviceAddress);

        int paddingVertical = 8;
        int paddingHorizontal = 16;

        if (BluetoothDevice.BOND_BONDED == bluetoothDevice.getBondState()){
            if (deviceAddress.equals(mConnectedDeviceAddress)) {
                holder.bond.setText("已连接");
                holder.bond.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            } else {
                holder.bond.setText("已配对");
                holder.bond.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            }
        } else {
            holder.bond.setText("未配对");
            holder.bond.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
        }
        return convertView;
    }


    static class ViewHolder{
        TextView name;
        TextView address;
        TextView bond;
    }
}
