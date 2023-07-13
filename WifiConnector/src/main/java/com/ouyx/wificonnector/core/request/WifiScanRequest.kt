/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.core.request

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat
import com.ouyx.wificonnector.callback.WifiScanCallback
import com.ouyx.wificonnector.data.ScanFailType
import com.ouyx.wificonnector.util.WifiUtil
import java.util.concurrent.atomic.AtomicBoolean


/**
 * 管理 WIFI Scan
 *
 * @author ouyx
 * @date 2023年07月10日 16时17分
 */
class WifiScanRequest : BaseRequest() {

    private var mScanCallback: WifiScanCallback? = null
    private val isScanning = AtomicBoolean(false)

    companion object {
        @Volatile
        private var INSTANCE: WifiScanRequest? = null
        fun getInstance(): WifiScanRequest =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WifiScanRequest().also { INSTANCE = it }
            }
    }

    private val mWifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    scanSuccess()
                }
            }
        }
    }

    init {
        val intentFilter = IntentFilter().apply { addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) }
        getApplication().registerReceiver(mWifiScanReceiver, intentFilter)
    }


    fun startScan(scanCallback: WifiScanCallback) {
        mScanCallback = scanCallback

        if (isScanning.get()) {
            scanCallback.callScanFail(ScanFailType.ScanningInProgress)
            return
        }

        if (!WifiUtil.isPermissionScan(getApplication())) {
            scanCallback.callScanFail(ScanFailType.PermissionNotGranted)
            return
        }

        if (!WifiUtil.isLocationEnabled(getApplication())) {
            scanCallback.callScanFail(ScanFailType.LocationNotEnable)
            return
        }

        if (!getWifiManager().startScan()) {
            mScanCallback?.callScanFail(ScanFailType.StartScanError)
        } else {
            isScanning.set(true)
            mScanCallback?.callScanStart()
        }

    }

    /**
     *  移除 callback
     */
    override fun removeCallback() {
        mScanCallback = null

    }

    /**
     * 回收 所有资源
     */
    override fun release() {
        mScanCallback = null
        getApplication().unregisterReceiver(mWifiScanReceiver)
        INSTANCE = null
    }


    private fun scanSuccess() {
        isScanning.set(false)

        if (ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mScanCallback?.callScanSuccess(getWifiManager().scanResults)

    }

}