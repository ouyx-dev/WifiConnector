/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.launch

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import com.ouyx.wificonnector.callback.WifiConnectCallback
import com.ouyx.wificonnector.callback.WifiScanCallback
import com.ouyx.wificonnector.core.dispatcher.WifiRequestDispatcher
import com.ouyx.wificonnector.core.request.WifiConnectRequest
import com.ouyx.wificonnector.util.DefaultLogger


/**
 * Wifi连接 入口
 *
 * @author ouyx
 * @date 2023年07月10日 13时55分
 */
class WifiConnector private constructor() {
    lateinit var mApplication: Application

    lateinit var mWifiManager: WifiManager

    lateinit var mDispatcher: WifiRequestDispatcher

    companion object {
        @Volatile
        private var INSTANCE: WifiConnector? = null
        fun getInstance(): WifiConnector =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WifiConnector().also { INSTANCE = it }
            }
    }

    fun init(application: Application) {
        mApplication = application
        mWifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mDispatcher = WifiRequestDispatcher.getInstance()
    }


    fun startConnect(
        ssid: String,
        pwd: String,
        cipherType: WifiConnectRequest.WifiCipherType = WifiConnectRequest.WifiCipherType.WPA,
        connectCallback: WifiConnectCallback.() -> Unit
    ) {
        if (!::mWifiManager.isInitialized) {
            DefaultLogger.error(message = "WifiConnector未初始化，请先调用WifiConnector.init()")
            return
        }
        mDispatcher.startConnect(ssid, pwd, cipherType, connectCallback)
    }

    fun startScan(scanCallback: WifiScanCallback.() -> Unit) {
        if (!::mWifiManager.isInitialized) {
            DefaultLogger.error(message = "WifiConnector未初始化，请先调用WifiConnector.init()")
            return
        }
        mDispatcher.startScan(scanCallback)
    }
}