/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.core.dispatcher

import android.os.Build
import com.ouyx.wificonnector.callback.WifiConnectCallback
import com.ouyx.wificonnector.callback.WifiScanCallback
import com.ouyx.wificonnector.callback.listener.WifiConnectionStatusListener
import com.ouyx.wificonnector.core.request.WifiConnectRequest
import com.ouyx.wificonnector.core.request.WifiConnectRequestQ
import com.ouyx.wificonnector.core.request.WifiScanRequest
import com.ouyx.wificonnector.core.listener.WifiStatusListener
import com.ouyx.wificonnector.data.WifiCipherType
import com.ouyx.wificonnector.launch.WifiConnector
import com.ouyx.wificonnector.util.DefaultLogger
import kotlinx.coroutines.*


/**
 *  请求 分发中心
 *
 * @author ouyx
 * @date 2023年07月10日 15时22分
 */
class WifiRequestDispatcher : IWiFiRequestDispatcher {
    private val mainScope = MainScope()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    internal fun getMainScope() = mainScope

    internal fun getIOScope() = ioScope


    companion object {
        @Volatile
        private var INSTANCE: WifiRequestDispatcher? = null
        fun getInstance(): WifiRequestDispatcher =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WifiRequestDispatcher().also { INSTANCE = it }
            }
    }

    /**
     * 开始连接WIFI
     *
     * Android Q 之前使用 WifiManager.enableNetwork 来连接 ，支持超时
     * Android Q 及之后 使用 requestNetwork 连接，因为系统会提供弹框辅助连接所以不支持超时
     */
    override fun connect(
        ssid: String,
        pwd: String?,
        cipherType: WifiCipherType,
        timeoutInMillis: Long?,
        connectCallback: WifiConnectCallback.() -> Unit,
    ) {
        val wifiConnectCallback = WifiConnectCallback()
        connectCallback.invoke(wifiConnectCallback)
        DefaultLogger.debug(message = "SDK版本 = ${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiConnectRequestQ.get().startConnect(ssid, pwd, cipherType, wifiConnectCallback)
        } else {
            val timeOut = timeoutInMillis ?: WifiConnector.get().getOptions().connectTimeoutMsBeforeQ
            DefaultLogger.info(message = "设置的超时时间 = $timeOut")
            WifiConnectRequest.get().startConnect(ssid, pwd, cipherType, timeOut, wifiConnectCallback)
        }
    }

    override fun startScan(scanCallback: WifiScanCallback.() -> Unit) {
        val wifiScanCallback = WifiScanCallback()
        scanCallback.invoke(wifiScanCallback)

        WifiScanRequest.getInstance().startScan(wifiScanCallback)
    }

    /**
     * 连接过程中，主动取消连接任务
     */
    override fun stopConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            DefaultLogger.error(message = "当前设备是Android 10 或者Android 10后设备，不支持连接时用户主动取消！")
        } else {
            WifiConnectRequest.get().stopConnect()
        }
    }

    /**
     * 监听 WiFi连接状态变化
     */
    override fun setWifiConnectionStatusListener(connectStatueCallback: WifiConnectionStatusListener.() -> Unit) {
        val wifiConnectionStatusListener = WifiConnectionStatusListener()
        wifiConnectionStatusListener.connectStatueCallback()

        WifiStatusListener.get().setWifiStatusListener(wifiConnectionStatusListener)
    }

    /**
     * 移除  WiFi连接状态变化 监听
     */
    override fun cancelWiFiConnectionStatusListener() {
        WifiStatusListener.get().cancelWifiStatusListener()
    }


    /**
     *  回收所有资源
     */
    override fun release() {
        WifiScanRequest.getInstance().release()

        WifiConnectRequestQ.get().release()

        WifiConnectRequest.get().release()

        WifiStatusListener.get().release()

        mainScope.cancel()
        ioScope.cancel()

        // mainScope  ioScope defaultScope  是 INSTANCE 创建时创建出来的，销毁的时候也要一起
        INSTANCE = null
    }


    /**
     * 解除所有 CallBack
     */
    override fun removeAllCallback() {
        WifiScanRequest.getInstance().removeCallback()

        WifiConnectRequestQ.get().removeCallback()

        WifiConnectRequest.get().removeCallback()

        WifiStatusListener.get().removeCallback()
    }

}