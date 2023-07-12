/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.util

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build


/**
 * WifiManager 帮助类
 *
 * @author ouyx
 * @date 2023年07月10日 11时31分
 */
object WifiUtil {

    /**
     * 判断是否拥有[permission]权限
     * @return true = 拥有该权限
     */
    private fun isPermissionScan(application: Application?, permission: String): Boolean {
        return application?.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 判断是否拥有WIFI 扫描
     * 参考https://developer.android.com/guide/topics/connectivity/wifi-scan?hl=zh-cn
     * @return true = 拥有该权限
     */
    fun isPermissionScan(application: Application?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Android 10 or above
            return isPermissionScan(application, Manifest.permission.ACCESS_FINE_LOCATION)
                    && isPermissionScan(application, Manifest.permission.CHANGE_WIFI_STATE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            //Android 9.0 or above
            return (isPermissionScan(application, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    isPermissionScan(application, Manifest.permission.ACCESS_COARSE_LOCATION))
                    && isPermissionScan(application, Manifest.permission.CHANGE_WIFI_STATE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Android 8.0 or above
            return (isPermissionScan(application, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    isPermissionScan(application, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    isPermissionScan(application, Manifest.permission.CHANGE_WIFI_STATE))
        }
        return true
    }


    /**
     *  判断位置信息是否开启
     */
    fun isLocationEnabled(context: Application): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


}