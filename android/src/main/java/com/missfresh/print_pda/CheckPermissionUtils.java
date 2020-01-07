package com.missfresh.print_pda;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public final class CheckPermissionUtils {

    private static final  String[]  permissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
            Manifest.permission.ACCESS_COARSE_LOCATION,

    };
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 123;

    private CheckPermissionUtils(){}

    public static void initPermission(Activity activity){
        String[] permissions = CheckPermissionUtils.checkPermission(activity);
        if (permissions.length!=0){
            ActivityCompat.requestPermissions(activity,permissions,100);
        }

        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //判断是否需要向用户解释为什么需要申请该权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(activity, "自Android 6.0开始需要打开位置权限才可以搜索到Ble设备", Toast.LENGTH_SHORT).show();
            }
            //请求权限
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    private static String[] checkPermission(Context context){
        List<String> data = new ArrayList<>();
        for(String permission:permissions){
            int checkSelfPermission = ContextCompat.checkSelfPermission(context,permission);
            if (checkSelfPermission == PackageManager.PERMISSION_DENIED){
                data.add(permission);
            }
        }
        return data.toArray(new String[0]);
    }
}
