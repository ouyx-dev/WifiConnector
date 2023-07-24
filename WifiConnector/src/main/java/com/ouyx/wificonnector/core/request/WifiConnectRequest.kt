/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
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
 *  用于 targetSdk 28 及以下的
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
    private var mPwd: String? = null


    /**
     * 当前正在连接WIFI 的密码类型
     */
    private lateinit var mCipherType: WifiCipherType

    /**
     *  当前连接 对应系统已存在的配置
     */
    private var mExistingConfiguration: WifiConfiguration? = null


    companion object {
        @Volatile
        private var INSTANCE: WifiConnectRequest? = null
        fun get(): WifiConnectRequest =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WifiConnectRequest().also { INSTANCE = it }
            }
    }


    private val mWifiConnectBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val action = intent.action
                if (action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
                    val networkState =
                        (intent.getParcelableExtra<Parcelable>(WifiManager.EXTRA_NETWORK_INFO) as NetworkInfo).detailedState
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
    fun startConnect(
        ssid: String,
        pwd: String?,
        cipherType: WifiCipherType,
        timeoutInMillis: Long,
        connectCallback: WifiConnectCallback,
    ) {
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

        if (pwd != null && !WifiUtil.isTextAsciiEncodable(pwd)) {
            mConnectCallback?.callConnectFail(ConnectFailType.PasswordMustASCIIEncoded)
            return
        }

        if (ssid.trim().isEmpty()) {
            mConnectCallback?.callConnectFail(ConnectFailType.SsidInvalid)
            return
        }

        if (!WifiUtil.isWifiEnable(getWifiManager())) {
            mConnectCallback?.callConnectFail(ConnectFailType.WifiNotEnable)
            return
        }

        if (WifiUtil.getWifiInfo(getWifiManager()).ipAddress != 0 && WifiUtil.getWifiInfo(getWifiManager()).ssid.replace("\"", "") == ssid) {
            val wifiConnectedInfo = WifiConnectInfo().apply {
                name = ssid
                ip = WifiUtil.getIpAddress(getWifiManager())
                mac = WifiUtil.getMacAddress(getWifiManager())
                gateWay = WifiUtil.getGateway(getWifiManager())
            }
            mConnectCallback?.callConnectFail(ConnectFailType.SSIDConnected(wifiConnectedInfo))
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

        mConnectJob?.invokeOnCompletion { it ->
            isConnecting.set(false)
            when (it) {
                is TimeoutCancellationException -> {
                    DefaultLogger.debug(message = "超时而取消")
                    mConnectCallback?.callConnectFail(ConnectFailType.ConnectTimeout)

                    //连接已保存的配置失败时，需要删除配置防止下次连接继续失败
                    mExistingConfiguration?.let {
                        getWifiManager().removeNetwork(it.networkId)
                        getWifiManager().saveConfiguration()
                    }
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
     * 连接时主动取消连接任务,支持Android Q之前的设备
     *
     * Android Q或者Android Q后设备，不支持连接时取消任务
     *
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
        removeCallback()

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
        mExistingConfiguration = getConfigViaSSID(mTargetSSID)
        DefaultLogger.debug(message = "[$mTargetSSID] 在系统中对应的网络配置 :$mExistingConfiguration")

        //禁掉所有wifi
        for (c in getWifiManager().configuredNetworks) {
            getWifiManager().disableNetwork(c.networkId)
        }

        if (mExistingConfiguration != null) {
            DefaultLogger.debug(message = "[$mTargetSSID]是已存在配置, 尝试连接...")
            val enabled = getWifiManager().enableNetwork(mExistingConfiguration!!.networkId, true)
            DefaultLogger.debug(message = "[$mTargetSSID]enableNetwork返回值: $enabled")
        } else {

            val wifiConfig = createWifiCfg()
            DefaultLogger.debug(message = "根据[$mTargetSSID]创建新配置，尝试连接...")
            val netID = getWifiManager().addNetwork(wifiConfig)
            val enabled = getWifiManager().enableNetwork(netID, true)
            DefaultLogger.debug(message = "enableNetwork返回值 =$enabled")
        }
    }


    private fun handleNetState(state: DetailedState) {
        if (state == DetailedState.AUTHENTICATING) {
            DefaultLogger.debug(message = "认证中...")
        } else if (state == DetailedState.CONNECTING) {
            DefaultLogger.debug(message = "连接中...")
        } else if (state == DetailedState.DISCONNECTED) {
            DefaultLogger.debug(message = "已断开连接...")
        } else if (state == DetailedState.DISCONNECTING) {
            DefaultLogger.debug(message = "断开连接中...")
        } else if (state == DetailedState.FAILED) {
            DefaultLogger.debug(message = "连接失败...")
        } else if (state == DetailedState.SCANNING) {
            DefaultLogger.debug(message = "搜索中...")
        } else if (state == DetailedState.CONNECTED) {
            DefaultLogger.debug(message = "收到[${WifiUtil.getWifiInfo(getWifiManager()).ssid}]已连接的广播")
            if (isConnecting.get()) {
                val connectedSSID = WifiUtil.getConnectedSsid(getWifiManager())?.replace("\"", "")
                val wifiConnectedInfo = WifiConnectInfo().apply {
                    name = connectedSSID
                    ip = WifiUtil.getIpAddress(getWifiManager())
                    mac = WifiUtil.getMacAddress(getWifiManager())
                    gateWay = WifiUtil.getGateway(getWifiManager())
                }

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
                    if (isWepKeyHexadecimal(mPwd!!)) {
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

            WifiCipherType.WPA2, WifiCipherType.WPA3 -> {
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

}
