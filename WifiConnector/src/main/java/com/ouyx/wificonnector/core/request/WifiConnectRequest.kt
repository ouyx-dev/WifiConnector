/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.core.request

import android.annotation.SuppressLint
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.text.TextUtils
import com.ouyx.wificonnector.callback.WifiConnectCallback
import com.ouyx.wificonnector.data.CancelReason
import com.ouyx.wificonnector.data.ConnectFailType
import com.ouyx.wificonnector.data.WifiCipherType
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
class WifiConnectRequest private constructor() : BaseRequest() {

    private var mConnectJob: Job? = null

    private var mConnectCallback: WifiConnectCallback? = null

    private var mConnectTime = 10 * 1000L

    private val isConnecting = AtomicBoolean(false)

    /**
     * 当前连接的WIFI 的配置文件是否存在
     */
    private var mConfigurationExisted = false

    /**
     * 当前正在连接WIFI 的 SSID
     */
    private lateinit var mSSID: String


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


    /**
     * 连接WIFI
     *
     * @param ssid
     * @param pwd
     * @param cipherType
     *
     */
    fun startConnect(ssid: String, pwd: String, cipherType: WifiCipherType, connectCallback: WifiConnectCallback) {
        mSSID = ssid
        mPwd = pwd
        mCipherType = cipherType

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
            mConnectCallback?.callConnectFail(ConnectFailType.SsidConnected)
            return
        }
        if (!isWifiEnable()) {
            mConnectCallback?.callConnectFail(ConnectFailType.WifiNotEnable)
            return
        }

        mConnectJob = ioScope.launch {
            withTimeout(mConnectTime) {
                isConnecting.set(true)
                mConnectCallback?.callConnectStart()
                connectWifi()

                delay(mConnectTime)
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
     *  连接 当前[mSSID]对应的wifi
     */
    @SuppressLint("MissingPermission")
    private fun connectWifi() {
        val existConfiguration = getExistWifiConfig(mSSID)
        DefaultLogger.info(message = "Wifi系统配置 中 Configuration = $existConfiguration")

        //禁掉所有wifi
        for (c in getWifiManager().configuredNetworks) {
            getWifiManager().disableNetwork(c.networkId)
            getWifiManager().removeNetwork(c.networkId)
        }
        val rst = getWifiManager().disconnect()
        DefaultLogger.info(message = "disconnect  结果 $rst")

        val isConfigureExisted = (existConfiguration != null)
        if (isConfigureExisted) {
            val netID = getWifiManager().addNetwork(existConfiguration)
            val result = getWifiManager().enableNetwork(netID, true)
            DefaultLogger.info(message = "使用已存在配置，enableNetwork 结果 =$result")

        } else {
            val wifiConfig = createWifiCfg()
            DefaultLogger.info(message = "新创建的WifiConfig= ${wifiConfig.describeContents()}")
            val netID = getWifiManager().addNetwork(wifiConfig)
            val rst = getWifiManager().enableNetwork(netID, true)
            DefaultLogger.info(message = "enableNetwork 结果 =$rst")
        }
    }


    /**
     *  获取系统配置中指定ssid的 网络配置
     *
     *  @param ssid
     */
    @SuppressLint("MissingPermission")
    private fun getExistWifiConfig(ssid: String): WifiConfiguration? {
        val existingConfigs = getWifiManager().configuredNetworks
        for (existingConfig in existingConfigs) {
            if (existingConfig.SSID == "\"" + ssid + "\"") {
                return existingConfig
            }
        }
        return null
    }


    /**
     * 获取当前正在 wifi 的信息
     *
     * 比如
     * SSID: "ouyx", Security type: 2, Supplicant state: COMPLETED, Wi-Fi standard: 4, RSSI: -22, Link speed: 192Mbps,
     * Tx Link speed: 192Mbps, Max Supported Tx Link speed: 144Mbps, Rx Link speed: 192Mbps, Max Supported Rx Link speed: 144Mbps,
     * Frequency: 2437MHz, Net ID: 8, Metered hint: true, score: 60, CarrierMerged: false, SubscriptionId: -1, IsPrimary: -1
     */
    private fun getWifiInfo(): WifiInfo = getWifiManager().connectionInfo


    /**
     * 根据当前 保存的[mCipherType] [mPwd] [mSSID] 创建wifi配置文件
     *
     */
    private fun createWifiCfg(): WifiConfiguration {
        val config = WifiConfiguration()
        config.allowedAuthAlgorithms.clear()
        config.allowedGroupCiphers.clear()
        config.allowedKeyManagement.clear()
        config.allowedPairwiseCiphers.clear()
        config.allowedProtocols.clear()
        config.SSID = "\"" + mSSID + "\""
        when (mCipherType) {
            WifiCipherType.WEP -> {
                if (!TextUtils.isEmpty(mPwd)) {
                    if (isHexWepKey(mPwd)) {
                        config.wepKeys[0] = mPwd
                    } else {
                        config.wepKeys[0] = "\"" + mPwd + "\""
                    }
                }
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                config.wepTxKeyIndex = 0
            }
            WifiCipherType.WPA -> {
                config.preSharedKey = "\"" + mPwd + "\""
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                config.status = WifiConfiguration.Status.ENABLED
            }
            WifiCipherType.NO_PASS -> {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }
        }
        return config
    }


    private fun isHexWepKey(wepKey: String): Boolean {
        val len = wepKey.length

        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false
        }
        return isHex(wepKey)
    }

    private fun isHex(key: String): Boolean {
        for (i in key.length - 1 downTo 0) {
            val c = key[i]
            if (!(c in '0'..'9' || c in 'A'..'F' || (c in 'a'..'f'))) {
                return false
            }
        }
        return true
    }

    /**
     * WIFI 是否开启
     */
    private fun isWifiEnable() = getWifiManager().isWifiEnabled


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
        mConnectJob?.cancel()
        mConnectCallback = null
        INSTANCE = null
    }


}