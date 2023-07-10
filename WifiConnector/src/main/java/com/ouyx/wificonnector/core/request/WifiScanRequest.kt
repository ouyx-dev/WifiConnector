/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.core.request

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import com.ouyx.wificonnector.callback.WifiScanCallback
import com.ouyx.wificonnector.util.DefaultLogger


/**
 * 管理 WIFI Scan
 *
 * @author ouyx
 * @date 2023年07月10日 16时17分
 */
class WifiScanRequest : BaseRequest() {

    private var mScanCallback: WifiScanCallback? = null

    companion object {
        @Volatile
        private var INSTANCE: WifiScanRequest? = null
        fun getInstance(): WifiScanRequest =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WifiScanRequest().also { INSTANCE = it }
            }
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    scanSuccess()
                } else {
                    scanFailure()
                }
            }

        }
    }


    init {
        val intentFilter = IntentFilter().apply { addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) }
        getApplication().registerReceiver(wifiScanReceiver, intentFilter)
    }


    fun startScan(scanCallback: WifiScanCallback) {
        mScanCallback = scanCallback

        getWifiManager().startScan()
    }

    override fun removeCallback() {
        TODO("Not yet implemented")
    }

    override fun release() {
        TODO("Not yet implemented")
    }


    private fun scanFailure() {
        DefaultLogger.debug(message = "scan fail!")
    }

    private fun scanSuccess() {
        DefaultLogger.debug(message = "scan success!")
    }
}