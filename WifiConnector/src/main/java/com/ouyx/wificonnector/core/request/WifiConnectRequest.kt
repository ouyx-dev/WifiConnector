/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("DEPRECATION")

package com.ouyx.wificonnector.core.request

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.NetworkInfo.DetailedState
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Parcelable
import android.text.TextUtils
import com.ouyx.wificonnector.callback.WifiConnectCallback
import com.ouyx.wificonnector.data.CancelReason
import com.ouyx.wificonnector.data.ConnectFailType
import com.ouyx.wificonnector.data.WifiCipherType
import com.ouyx.wificonnector.data.WifiConnectInfo
import com.ouyx.wificonnector.util.DefaultLogger
import com.ouyx.wificonnector.util.WifiUtil
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 *  WIFI 连接请求
 *
 * @author ouyx
 * @date 2023年07月06日 14时15分
 */
@Suppress("DEPRECATION")
class WifiConnectRequest private constructor() : BaseRequest() {

    private var mConnectJob: Job? = null

    private var mConnectCallback: WifiConnectCallback? = null

    private var mTimeoutInMillis = 5 * 1000L

    private val isConnecting = AtomicBoolean(false)

    /**
     * 当前正在连接WIFI 的 SSID
     */
    private lateinit var mTargetSSID: String


    /**
     * 当前正在连接WIFI 的密码
     */
    private lateinit var mPwd: String


    /**
     * 当前正在连接WIFI 的密码类型
     */
    private lateinit var mCipherType: WifiCipherType


    companion object {
        @Volatile
        private var INSTANCE: WifiConnectRequest? = null
        fun getInstance(): WifiConnectRequest =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WifiConnectRequest().also { INSTANCE = it }
            }
    }


    private val mWifiConnectBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val action = intent.action
                if (action == WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION) {
                    val linkWifiResult = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 123)
                    if (linkWifiResult == WifiManager.ERROR_AUTHENTICATING) {
                        DefaultLogger.info(message = "密码错误")
                    }
                } else if (action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
                    val networkState = (intent.getParcelableExtra<Parcelable>(WifiManager.EXTRA_NETWORK_INFO) as NetworkInfo?)!!.detailedState
                    handleNetState(networkState)
                }
            }
        }
    }

    init {
        val mWifiConnectIntentFilter = IntentFilter().apply {
            addAction(WifiManager.ACTION_PICK_WIFI_NETWORK)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        getApplication().registerReceiver(mWifiConnectBroadcastReceiver, mWifiConnectIntentFilter)
    }


    /**
     *  开始连接WIFI
     *
     * @param ssid
     * @param pwd
     * @param cipherType
     *
     */
    fun startConnect(ssid: String, pwd: String, cipherType: WifiCipherType,timeoutInMillis:Long, connectCallback: WifiConnectCallback) {
        mTargetSSID = ssid
        mPwd = pwd
        mCipherType = cipherType
        mTimeoutInMillis = timeoutInMillis

        mConnectCallback = connectCallback
        if (!WifiUtil.isPermissionConnect(getApplication())) {
            mConnectCallback?.callConnectFail(ConnectFailType.PermissionNotEnough)
            return
        }
        if (isConnecting.get()) {
            mConnectCallback?.callConnectFail(ConnectFailType.ConnectingInProgress)
            return
        }
        if (getWifiInfo().ipAddress != 0 && getWifiInfo().ssid.replace("\"", "") == ssid) {
            val wifiConnectedInfo = WifiConnectInfo().apply {
                name = ssid
                ip = getIpAddress()
                mac = getMacAddress()
                gateWay = getGateway()
            }
            mConnectCallback?.callConnectFail(ConnectFailType.SSIDConnected(wifiConnectedInfo))
            return
        }
        if (!isWifiEnable()) {
            mConnectCallback?.callConnectFail(ConnectFailType.WifiNotEnable)
            return
        }

        mConnectJob = ioScope.launch {
            withTimeout(mTimeoutInMillis) {
                isConnecting.set(true)
                mConnectCallback?.callConnectStart()
                connectWifi()

                delay(mTimeoutInMillis)
            }
        }


        mConnectJob?.invokeOnCompletion {
            isConnecting.set(false)
            when (it) {
                is TimeoutCancellationException -> {
                    DefaultLogger.debug(message = "超时而取消")
                    mConnectCallback?.callConnectFail(ConnectFailType.ConnectTimeout)
                }
                is CancelReason -> {
                    when (it) {
                        CancelReason.CancelByChoice -> {
                            DefaultLogger.debug(message = "用户主动取消")
                            mConnectCallback?.callConnectFail(ConnectFailType.CancelByChoice)
                        }
                        CancelReason.CancelByError -> {
                            DefaultLogger.debug(message = "运行异常而取消")
                        }
                        is CancelReason.CancelBySuccess -> {
                            DefaultLogger.debug(message = "连接成功而取消任务：" + it.wifiConnectInfo.toString())
                            mConnectCallback?.callConnectSuccess(it.wifiConnectInfo)
                        }
                    }
                }
            }
        }
    }


    /**
     * 主动停止连接 WIFI
     */
    fun stopConnect() {
        mConnectJob?.cancel(CancelReason.CancelByChoice)
    }


    /**
     * 移除回调
     */
    override fun removeCallback() {
        mConnectCallback = null
    }

    /**
     *  回收所有资源
     */
    override fun release() {
        getApplication().unregisterReceiver(mWifiConnectBroadcastReceiver)
        mConnectJob?.cancel()
        mConnectCallback = null
        INSTANCE = null
    }

    /**
     *  连接 当前[mTargetSSID]对应的wifi
     */
    @SuppressLint("MissingPermission")
    private fun connectWifi() {
        val existingConfiguration = getConfigViaSSID(mTargetSSID)
        DefaultLogger.info(message = "[$mTargetSSID] 在系统中对应的网络配置 = $existingConfiguration")

        //禁掉所有wifi
        for (c in getWifiManager().configuredNetworks) {
            getWifiManager().disableNetwork(c.networkId)
        }

        if (existingConfiguration != null) {
            DefaultLogger.info(message = "[$mTargetSSID]是已存在配置, 尝试连接...")
            val enabled = getWifiManager().enableNetwork(existingConfiguration.networkId, true)
            DefaultLogger.info(message = "[$mTargetSSID] 设置网络配置 结果: $enabled")

        } else {

            val wifiConfig = createWifiCfg()
            DefaultLogger.info(message = "根据[$mTargetSSID]创建新配置，尝试连接...")
            val netID = getWifiManager().addNetwork(wifiConfig)
            val enabled = getWifiManager().enableNetwork(netID, true)
            DefaultLogger.info(message = "设置网络配置enable 结果 =$enabled")
        }
    }


    private fun handleNetState(state: DetailedState) {
        if (state == DetailedState.AUTHENTICATING) {
            DefaultLogger.info(message = "认证中...")
        } else if (state == DetailedState.CONNECTING) {
            DefaultLogger.info(message = "连接中...")
        } else if (state == DetailedState.DISCONNECTED) {
            DefaultLogger.info(message = "已断开连接...")
        } else if (state == DetailedState.DISCONNECTING) {
            DefaultLogger.info(message = "断开连接中...")
        } else if (state == DetailedState.FAILED) {
            DefaultLogger.info(message = "连接失败...")
        } else if (state == DetailedState.SCANNING) {
            DefaultLogger.info(message = "搜索中...")
        } else if (state == DetailedState.CONNECTED) {
            DefaultLogger.info(message = "收到[${getWifiInfo().ssid}]已连接的广播")
            if (isConnecting.get()) {
                val connectedSSID = getConnectedSsid()?.replace("\"","")
                val wifiConnectedInfo = WifiConnectInfo().apply {
                    name = connectedSSID
                    ip = getIpAddress()
                    mac = getMacAddress()
                    gateWay = getGateway()
                }
                DefaultLogger.info(message = "mTargetSSID =$mTargetSSID  connectedSSID =$connectedSSID")

                if (mTargetSSID == connectedSSID) {
                    mConnectJob?.cancel(CancelReason.CancelBySuccess(wifiConnectedInfo))
                }
            }
        }
    }


    /**
     *  获取系统配置中指定ssid的 网络配置
     *
     *  @param ssid
     */
    @SuppressLint("MissingPermission")
    private fun getConfigViaSSID(ssid: String): WifiConfiguration? {
        val existingConfigs = getWifiManager().configuredNetworks
        for (existingConfig in existingConfigs) {
            if (existingConfig.SSID == "\"" + ssid + "\"") {
                return existingConfig
            }
        }
        return null
    }


    /**
     * 获取当前已连接 wifi 的信息
     *
     * 比如
     * SSID: "ouyx", Security type: 2, Supplicant state: COMPLETED, Wi-Fi standard: 4, RSSI: -22, Link speed: 192Mbps,
     * Tx Link speed: 192Mbps, Max Supported Tx Link speed: 144Mbps, Rx Link speed: 192Mbps, Max Supported Rx Link speed: 144Mbps,
     * Frequency: 2437MHz, Net ID: 8, Metered hint: true, score: 60, CarrierMerged: false, SubscriptionId: -1, IsPrimary: -1
     */
    private fun getWifiInfo(): WifiInfo = getWifiManager().connectionInfo


    /**
     * 根据当前 保存的[mCipherType] [mPwd] [mTargetSSID] 创建wifi配置文件
     *
     */
    private fun createWifiCfg(): WifiConfiguration {
        val config = WifiConfiguration().also {
            it.allowedAuthAlgorithms.clear()
            it.allowedGroupCiphers.clear()
            it.allowedKeyManagement.clear()
            it.allowedPairwiseCiphers.clear()
            it.allowedProtocols.clear()
        }
        config.SSID = "\"" + mTargetSSID + "\""
        when (mCipherType) {
            WifiCipherType.WEP -> {
                if (!TextUtils.isEmpty(mPwd)) {
                    if (isWepKeyHexadecimal(mPwd)) {
                        config.wepKeys[0] = mPwd
                    } else {
                        config.wepKeys[0] = "\"" + mPwd + "\""
                    }
                }
                with(config) {
                    allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                    allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                }
                config.wepTxKeyIndex = 0
            }
            WifiCipherType.WPA -> {
                config.preSharedKey = "\"" + mPwd + "\""
                with(config) {
                    allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                    allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
                    allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                    allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                    allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                    allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                    allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                }
                config.status = WifiConfiguration.Status.ENABLED
            }
            WifiCipherType.NO_PASS -> {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }
        }
        return config
    }


    /**
     * 获取已连接WifiS设备的 SSID
     *
     * @return SSID
     */
    @SuppressLint("MissingPermission")
    private fun getConnectedSsid(): String? {
        val wifiInfo = getWifiManager().connectionInfo
        var connectedWifiSSID = wifiInfo.ssid
        val networkId = wifiInfo.networkId
        val configuredNetworks = getWifiManager().configuredNetworks
        for (wifiConfiguration in configuredNetworks) {
            if (wifiConfiguration.networkId == networkId) {
                connectedWifiSSID = wifiConfiguration.SSID
                break
            }
        }
        return connectedWifiSSID
    }

    /**
     * 获取ip地址
     */
    private fun getIpAddress(): String? =
        WifiUtil.intToInetAddress(getWifiManager().connectionInfo.ipAddress)?.hostAddress


    /**
     * 获取 mac 地址
     */
    private fun getMacAddress() = getWifiManager().connectionInfo.macAddress

    /**
     * 获取网关地址
     */
    private fun getGateway(): String? =
        WifiUtil.intToInetAddress(getWifiManager().dhcpInfo.gateway)?.hostAddress


    /**
     * 用于判断WEP密钥是否为十六进制
     *
     */
    private fun isWepKeyHexadecimal(wepKey: String): Boolean {
        val len = wepKey.length

        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false
        }
        return WifiUtil.isHex(wepKey)
    }


    /**
     * WIFI 是否开启
     */
    private fun isWifiEnable() = getWifiManager().isWifiEnabled



}
