/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.launch

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import com.ouyx.wificonnector.callback.WifiConnectCallback
import com.ouyx.wificonnector.callback.WifiScanCallback
import com.ouyx.wificonnector.core.dispatcher.IWiFiRequestDispatcher
import com.ouyx.wificonnector.core.dispatcher.WifiRequestDispatcher
import com.ouyx.wificonnector.data.ConnectFailType
import com.ouyx.wificonnector.data.WifiCipherType
import com.ouyx.wificonnector.exceptions.InitializationException

/**
 * Wifi连接 入口
 *
 * @author ouyx
 * @date 2023年07月10日 13时55分
 */
class WifiConnector private constructor() {
    lateinit var mApplication: Application

    lateinit var mWifiManager: WifiManager

    private lateinit var mDispatcher: IWiFiRequestDispatcher

    companion object {
        @Volatile
        private var INSTANCE: WifiConnector? = null
        fun get(): WifiConnector =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WifiConnector().also { INSTANCE = it }
            }
    }

    fun init(application: Application) {
        mApplication = application
        mWifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mDispatcher = WifiRequestDispatcher.getInstance()
    }


    /**
     * WiFi连接 入口类
     *
     * @param ssid
     * @param pwd
     * @param cipherType 支持 WAP2 WAP3  WEP NO_PASS, 默认WPA2
     * @param timeoutInMillis Android Q 及 以后 不支持超时，因为有有系统弹框活系统页面辅助连接；
     * Android Q 之前 支持超时，默认5s
     *
     * @param connectCallback 支持 start{} ; connectSuccess{wifiConnectInfo -> };
     *  connectFail{connectFailType -> } Lambda表达式
     *
     */
    fun connect(
        ssid: String,
        pwd: String?,
        cipherType: WifiCipherType = WifiCipherType.WPA2,
        timeoutInMillis: Long = 5000,
        connectCallback: WifiConnectCallback.() -> Unit,
    ) {
        if (!::mWifiManager.isInitialized) {
            throw InitializationException()
        }
        mDispatcher.connect(ssid, pwd, cipherType, timeoutInMillis, connectCallback)
    }

    /**
     * 扫描获取附近WIFI设备
     *
     * @param scanCallback : 支持Lambda表达式 ，定义了三个函数 onScanStart、onScanSuccess和 onScanFail
     *  具体使用方式：
     *  WifiConnector.get().scan {
     *      onScanStart  {
     *       }
     *      onScanSuccess { scanResults, parsedScanResults ->
     *          // 扫描成功
     *      }
     *      onScanFail  { scanFailType ->
     *         // 扫描失败
     *      }
     *  }
     */
    fun scan(scanCallback: WifiScanCallback.() -> Unit) {
        if (!::mWifiManager.isInitialized) {
            throw InitializationException()
        }
        mDispatcher.startScan(scanCallback)
    }


    /**
     * 连接过程中，主动取消连接任务
     *
     * Android Q之前的设备,连接过程中调用[stopConnect], [WifiConnectCallback.connectFail]回调方法会触发，
     * 方法参数为[ConnectFailType.CancelByChoice]
     *
     * Android Q或者Android Q后设备，不支持连接时取消任务
     *
     */
    fun stopConnect() {
        mDispatcher.stopConnect()
    }

    /**
     * 移除所有回调
     */
    fun removeAllCallback() {
        mDispatcher.removeAllCallback()
    }


    /**
     * 回收所有资源
     */
    fun release() {
        mDispatcher.release()
    }

}