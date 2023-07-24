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


/**
 *  API>=29 的连接Wifi
 *
 * @author ouyx
 * @date 2023年07月18日 13时59分
 */
class WifiConnectRequestQ : BaseRequest() {

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


    private val mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val connectedSSID = WifiUtil.getConnectedSsid(getWifiManager())?.replace("\"", "")
            val wifiConnectedInfo = WifiConnectInfo().apply {
                name = connectedSSID
                ip = WifiUtil.getIpAddress(getWifiManager())
                mac = WifiUtil.getMacAddress(getWifiManager())
                gateWay = WifiUtil.getGateway(getWifiManager())
            }
            DefaultLogger.debug(message = "onAvailable=${network.describeContents()} ,  connectInfo =${wifiConnectedInfo}")

            mConnectCallback?.callConnectSuccess(wifiConnectedInfo)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            DefaultLogger.debug(message = "onUnavailable!")
            mConnectCallback?.callConnectFail(ConnectFailType.ConnectUnavailable)
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
            mConnectCallback?.callConnectFail(ConnectFailType.PermissionNotEnough)
            return
        }

        if (!isWifiEnable()) {
            mConnectCallback?.callConnectFail(ConnectFailType.WifiNotEnable)
            return
        }

        if(ssid.trim().isEmpty()){
            mConnectCallback?.callConnectFail(ConnectFailType.SsidInvalid)
            return
        }

        if (cipherType != NO_PASS && pwd.isNullOrEmpty()) {
            mConnectCallback?.callConnectFail(ConnectFailType.EncryptionPasswordNotNull)
            return
        }

        if (pwd != null && !WifiUtil.isTextAsciiEncodable(pwd)) {
            mConnectCallback?.callConnectFail(ConnectFailType.PasswordMustASCIIEncoded)
            return
        }

        val wifiInfo = WifiUtil.getWifiInfo(getWifiManager())
        if (wifiInfo.ipAddress != 0 && wifiInfo.ssid.replace("\"", "") == ssid) {
            val wifiConnectedInfo = WifiConnectInfo().apply {
                name = ssid
                ip = WifiUtil.getIpAddress(getWifiManager())
                mac = WifiUtil.getMacAddress(getWifiManager())
                gateWay = WifiUtil.getGateway(getWifiManager())
            }
            mConnectCallback?.callConnectFail(ConnectFailType.SSIDConnected(wifiConnectedInfo))
            return
        }

        connect()

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connect() {
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
        mConnectivityManager = getApplication().getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 连接wifi
        mConnectivityManager?.requestNetwork(request, mNetworkCallback)
    }


    override fun removeCallback() {
        mConnectCallback = null
    }

    override fun release() {
        removeCallback()

        mConnectivityManager?.unregisterNetworkCallback(mNetworkCallback)

    }

    /**
     * WIFI 是否开启
     */
    private fun isWifiEnable() = getWifiManager().isWifiEnabled

}