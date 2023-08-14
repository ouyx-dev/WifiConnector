/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.core.request

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.ouyx.wificonnector.callback.WifiConnectCallback
import com.ouyx.wificonnector.data.ConnectFailType
import com.ouyx.wificonnector.data.WifiCipherType
import com.ouyx.wificonnector.data.WifiCipherType.*
import com.ouyx.wificonnector.data.WifiConnectInfo
import com.ouyx.wificonnector.util.DefaultLogger
import com.ouyx.wificonnector.util.WifiUtil
import java.util.concurrent.atomic.AtomicBoolean


/**
 *  API>=29 的连接Wifi
 *
 * @author ouyx
 * @date 2023年07月18日 13时59分
 */
class WifiConnectRequestQ private constructor() : BaseRequest() {

    private var mConnectCallback: WifiConnectCallback? = null


    /**
     * 当前正在连接WIFI 的 SSID
     */
    private lateinit var mTargetSSID: String


    /**
     * 当前正在连接WIFI 的密码
     */
    private var mPwd: String? = null


    /**
     * 当前正在连接WIFI 的密码类型
     */
    private lateinit var mCipherType: WifiCipherType


    private var mConnectivityManager: ConnectivityManager? = null

    /**
     * 是否注册NetworkCallback
     *
     * 对NetworkCallback取消注册 会断开 当前连接
     */
    private var isNetworkCallbackRegistered = AtomicBoolean(false)

    companion object {
        @Volatile
        private var INSTANCE: WifiConnectRequestQ? = null
        fun get(): WifiConnectRequestQ =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WifiConnectRequestQ().also { INSTANCE = it }
            }
    }


    private val mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val connectedSSID = WifiUtil.getConnectedSsid(getWifiManager())?.replace("\"", "")
            val wifiConnectedInfo = WifiConnectInfo().apply {
                name = connectedSSID
                ip = WifiUtil.getIpAddress(getWifiManager())
                gateWay = WifiUtil.getGateway(getWifiManager())
            }
            DefaultLogger.debug(message = "onAvailable:  connectInfo =${wifiConnectedInfo}")
            callConnectSuccess(wifiConnectedInfo)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            DefaultLogger.warning(message = "onUnavailable!")
            callConnectFail(ConnectFailType.ConnectUnavailable)
        }
    }


    /**
     *  开始连接WIFI
     *
     * @param ssid
     * @param pwd
     * @param cipherType
     *
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun startConnect(
        ssid: String,
        pwd: String?,
        cipherType: WifiCipherType,
        connectCallback: WifiConnectCallback,
    ) {
        mConnectCallback = connectCallback

        mTargetSSID = ssid
        mPwd = pwd
        mCipherType = cipherType

        if (!WifiUtil.isPermissionConnect(getApplication())) {
            DefaultLogger.warning(message = "connect 权限不够 ")
            callConnectFail(ConnectFailType.PermissionNotEnough)
            return
        }

        if (!isWifiEnable()) {
            DefaultLogger.warning(message = "wifi 未开启 ")
            callConnectFail(ConnectFailType.WifiNotEnable)
            return
        }

        if (ssid.trim().isEmpty()) {
            DefaultLogger.warning(message = " 输入的ssid 是空的 ")
            callConnectFail(ConnectFailType.SsidInvalid)
            return
        }

        if (cipherType != NO_PASS && pwd.isNullOrEmpty()) {
            DefaultLogger.warning(message = "加密模式下，密码不能为空 ")
            callConnectFail(ConnectFailType.EncryptionPasswordNotNull)
            return
        }

        if (pwd != null && !WifiUtil.isTextAsciiEncode(pwd)) {
            DefaultLogger.warning(message = " 密码必须是ASCII")
            callConnectFail(ConnectFailType.PasswordMustASCIIEncoded)
            return
        }

        val wifiInfo = WifiUtil.getWifiInfo(getWifiManager())
        if (wifiInfo.ssid.replace("\"", "") == mTargetSSID) {
            val wifiConnectedInfo = WifiConnectInfo().apply {
                name = ssid
                ip = WifiUtil.getIpAddress(getWifiManager())
                gateWay = WifiUtil.getGateway(getWifiManager())
            }
            DefaultLogger.warning(message = "待连接的WIFI 已连接")
            callConnectFail(ConnectFailType.SSIDConnected(wifiConnectedInfo))
            return
        }

        connect()
    }

    /**
     *
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connect() {
        DefaultLogger.debug(message = "开始连接wifi...")
        mConnectCallback?.callConnectStart()

        val builder = WifiNetworkSpecifier.Builder()
        when (mCipherType) {
            WEP, NO_PASS -> {
                builder.setSsid(mTargetSSID)
            }

            WPA2 -> {
                builder.setSsid(mTargetSSID).setWpa2Passphrase(mPwd!!)
            }

            WPA3 -> {
                builder.setSsid(mTargetSSID).setWpa3Passphrase(mPwd!!)
            }
        }
        val wifiNetworkSpecifier = builder.build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build()
        (this.getApplication().getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager).also {
            mConnectivityManager = it
        }

        unregisterNetwork()

        // 连接wifi
        mConnectivityManager?.requestNetwork(request, mNetworkCallback)
        isNetworkCallbackRegistered.set(true)
    }


    override fun removeCallback() {
        mConnectCallback = null
    }

    override fun release() {
        removeCallback()
        unregisterNetwork()
    }

    /**
     *  取消注册NetworkCallback，会断开连接
     *
     *  需和 requestNetwork 成对出现。 未requestNetwork先unregisterNetworkCallback会报错
     */
    private fun unregisterNetwork() {
        if (isNetworkCallbackRegistered.get()) {
            mConnectivityManager?.let {
                it.unregisterNetworkCallback(mNetworkCallback)
                isNetworkCallbackRegistered.set(false)
            }
        }
    }

    /**
     * WIFI 是否开启
     */
    private fun isWifiEnable() = getWifiManager().isWifiEnabled


    /**
     *  回调 连接成功
     */
    private fun callConnectSuccess(connectedInfo: WifiConnectInfo) {
        mConnectCallback?.callConnectSuccess(connectedInfo)
        removeCallback()
    }

    /**
     * 回调 连接失败
     */
    private fun callConnectFail(failType: ConnectFailType) {
        mConnectCallback?.callConnectFail(failType)
        removeCallback()
    }

}