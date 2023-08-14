/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.core.listener

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.ouyx.wificonnector.callback.listener.WifiConnectionStatusListener
import com.ouyx.wificonnector.core.request.BaseRequest
import com.ouyx.wificonnector.data.WifiConnectInfo
import com.ouyx.wificonnector.launch.WifiConnector
import com.ouyx.wificonnector.util.DefaultLogger
import com.ouyx.wificonnector.util.WifiUtil
import java.util.concurrent.atomic.AtomicBoolean


/**
 * 监听WiFi连接状态变化
 *
 * @author ouyx
 * @date 2023年08月14日 11时06分
 */
class WifiStatusListener : BaseRequest() {

    private val mWifiManager = WifiConnector.get().mWifiManager

    private val mConnectivityManager = WifiConnector.get().mConnectivityManager

    private var mConnectionStatusListener: WifiConnectionStatusListener? = null

    private val isRegister = AtomicBoolean(false)

    companion object {
        @Volatile
        private var INSTANCE: WifiStatusListener? = null
        fun get(): WifiStatusListener =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WifiStatusListener().also { INSTANCE = it }
            }
    }


    private var networkCallback: ConnectivityManager.NetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val connectedSSID = WifiUtil.getConnectedSsid(mWifiManager)?.replace("\"", "")
            val wifiConnectedInfo = WifiConnectInfo().apply {
                name = connectedSSID
                ip = WifiUtil.getIpAddress(mWifiManager)
                gateWay = WifiUtil.getGateway(mWifiManager)
            }

            DefaultLogger.warning(message = "###有WiFi设备连接上: $wifiConnectedInfo")
            mConnectionStatusListener?.callOnConnected(wifiConnectedInfo)
        }

        override fun onLost(network: Network) {
            DefaultLogger.warning(message = "###有WiFi设备断开连接")
            mConnectionStatusListener?.callOnDisConnected()
        }
    }

    fun setWifiStatusListener(connectionStatusListener: WifiConnectionStatusListener) {
        if (isRegister.get()) {
            // 注册前 ,如果已经注册，需要先解除注册再注册
            cancelWifiStatusListener()
        }
        mConnectionStatusListener = connectionStatusListener

        val builder = NetworkRequest.Builder()
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

        // 注册网络回调
        mConnectivityManager.registerNetworkCallback(builder.build(), networkCallback)
        isRegister.set(true)
    }

    fun cancelWifiStatusListener() {
        if (!isRegister.get()) {
            DefaultLogger.warning(message = "已经取消 对WiFi状态变化的注册")
            return
        }
        mConnectivityManager.unregisterNetworkCallback(networkCallback)
        isRegister.set(false)
    }

    override fun removeCallback() {
        mConnectionStatusListener = null
    }

    override fun release() {
        mConnectionStatusListener = null
        cancelWifiStatusListener()
    }

}