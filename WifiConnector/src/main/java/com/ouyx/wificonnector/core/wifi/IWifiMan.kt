package com.ouyx.wificonnector.core.wifi

import android.net.wifi.WifiInfo
import com.ouyx.wificonnector.data.WifiCipherType


/**
 * Wifi相关功能的抽象
 *
 * @author ouyx
 * @date 2023年07月13日 14时03分
 */
interface IWifiMan {
    fun connectWifi(ssid: String?, pwd: String?, cipherType: WifiCipherType): Boolean

    fun disconnectWifi(): Boolean

    fun getWifiInfo(): WifiInfo?

    fun scanWifiList(): Boolean
}