/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
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
import com.ouyx.wificonnector.data.WifiScanResult
import com.ouyx.wificonnector.util.DefaultLogger
import com.ouyx.wificonnector.util.WifiUtil
import java.util.concurrent.atomic.AtomicBoolean


/**
 * 管理 WiFi Scan
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
                } else {
                    DefaultLogger.warning(message = "收到 SCAN_RESULTS_AVAILABLE_ACTION 广播，ScanResult未更新")
                    scanFail(ScanFailType.ResultNotUpdated)
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
            DefaultLogger.warning(message = "扫描正在进行请稍后...")
            scanCallback.callScanFail(ScanFailType.ScanningInProgress)
            return
        }

        isScanning.set(true)
        mScanCallback?.callScanStart()

        if (!WifiUtil.isPermissionScan(getApplication())) {
            DefaultLogger.warning(message = "权限不够...")
            scanFail(ScanFailType.PermissionNotGranted)
            return
        }

        if (!WifiUtil.isLocationEnabled(getApplication())) {
            DefaultLogger.warning(message = "信息是否开启 无法开启扫描...")
            scanFail(ScanFailType.LocationNotEnable)
            return
        }

        // WifiManger.startScan 过时但是暂时没有给出替换方法
        if (!getWifiManager().startScan()) {
            DefaultLogger.warning(message = "WifiManager.startScan()启动失败,返回false")
            scanFail(ScanFailType.StartScanFail)
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
        if (ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            DefaultLogger.warning(message = "开启扫描后 权限不够...")
            scanFail(ScanFailType.PermissionNotGranted)
            return
        }
        val systemWiFiScanList = getWifiManager().scanResults
        val parsedScanResult: List<WifiScanResult> =
            systemWiFiScanList
                .filter { !it.SSID.isNullOrEmpty() }
                .distinctBy { it.SSID }
                .sortedByDescending { it.level }
                .map {
                    WifiScanResult(
                        ssid = it.SSID,
                        level = WifiUtil.analyzeSignalStrength(it.level),
                        cipherType = WifiUtil.analyzeWifiCipherType(it.capabilities)
                    )
                }
        mScanCallback?.callScanSuccess(getWifiManager().scanResults, parsedScanResult)

        removeCallback()
        isScanning.set(false)
    }

    private fun scanFail(scanFailType: ScanFailType) {
        mScanCallback?.callScanFail(scanFailType)

        //移除回调防止多次调用
        removeCallback()
        isScanning.set(false)
    }
}