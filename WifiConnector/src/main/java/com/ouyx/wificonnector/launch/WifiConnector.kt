/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.launch

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import com.ouyx.wificonnector.callback.WifiConnectCallback
import com.ouyx.wificonnector.callback.WifiScanCallback
import com.ouyx.wificonnector.core.dispatcher.IWiFiRequestDispatcher
import com.ouyx.wificonnector.core.dispatcher.WifiRequestDispatcher
import com.ouyx.wificonnector.data.ConnectFailType
import com.ouyx.wificonnector.data.WifiCipherType
import com.ouyx.wificonnector.exceptions.InitializationException
import com.ouyx.wificonnector.util.DefaultLogger
import com.ouyx.wificonnector.Constants.DEFAULT_CONNECT_TIMEOUT_MS_BEFORE_Q
import com.ouyx.wificonnector.callback.listener.WifiConnectionStatusListener
import com.ouyx.wificonnector.data.WifiConnectInfo
import com.ouyx.wificonnector.util.WifiUtil

/**
 * Wifi连接 入口
 *
 * @author ouyx
 * @date 2023年07月10日 13时55分
 */
class WifiConnector private constructor() {

    lateinit var mApplication: Application

    lateinit var mWifiManager: WifiManager

    lateinit var mConnectivityManager: ConnectivityManager


    private lateinit var mDispatcher: IWiFiRequestDispatcher


    private lateinit var mWiFiOptions: WiFiOptions

    companion object {
        @Volatile
        private var INSTANCE: WifiConnector? = null
        fun get(): WifiConnector =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WifiConnector().also { INSTANCE = it }
            }
    }

    fun init(application: Application, options: WiFiOptions = WiFiOptions.getDefaultWiFiOptions()) {
        mApplication = application
        mWifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mConnectivityManager =
            application.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
        mDispatcher = WifiRequestDispatcher.getInstance()
        mWiFiOptions = options
        DefaultLogger.warning(message = "日志开启状态: ${mWiFiOptions.isDebug}")
        DefaultLogger.setDebug(mWiFiOptions.isDebug)

    }

    fun getOptions(): WiFiOptions = mWiFiOptions

    /**
     * WiFi连接 入口类
     *
     * @param ssid
     * @param pwd
     * @param cipherType 支持 WAP2 WAP3  WEP NO_PASS, 默认WPA2
     * @param timeoutInMillis Android Q 及 以后 不支持超时，因为有有系统弹框活系统页面辅助连接；
     * Android Q 之前 支持超时，默认 [DEFAULT_CONNECT_TIMEOUT_MS_BEFORE_Q]
     *
     * @param connectCallback 支持 start{} ; connectSuccess{wifiConnectInfo -> };
     *  connectFail{connectFailType -> } Lambda表达式
     *
     */
    fun connect(
        ssid: String,
        pwd: String?,
        cipherType: WifiCipherType = WifiCipherType.WPA2,
        timeoutInMillis: Long? = null,
        connectCallback: WifiConnectCallback.() -> Unit,
    ) {
        checkInitialization()

        mDispatcher.connect(ssid, pwd, cipherType, timeoutInMillis, connectCallback)
    }


    /**
     *  获取当前连接上的WiFi信息
     *
     * @return 当前连接的WiFi信息
     */
    fun getConnectedInfo(): WifiConnectInfo {
        checkInitialization()

        return WifiConnectInfo().apply {
            val connectedSSID = WifiUtil.getConnectedSsid(mWifiManager)?.replace("\"", "")
            name = connectedSSID
            ip = WifiUtil.getIpAddress(mWifiManager)
            gateWay = WifiUtil.getGateway(mWifiManager)
        }
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
        checkInitialization()

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
        checkInitialization()

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

    /**
     * 检验 是否初始化
     */
    private fun checkInitialization() {
        if (!::mWifiManager.isInitialized) {
            throw InitializationException()
        }
    }

    /**
     * 添加 网连接状态 监听
     */
    fun setWifiConnectionStatusListener(connectStatueCallback: WifiConnectionStatusListener.() -> Unit) {
        checkInitialization()

        mDispatcher.setWifiConnectionStatusListener(connectStatueCallback)
    }

    /**
     *  取消 网络状态 监听
     */
    fun cancelWifiConnectionStatusListener() {
        checkInitialization()

        mDispatcher.cancelWiFiConnectionStatusListener()
    }


}