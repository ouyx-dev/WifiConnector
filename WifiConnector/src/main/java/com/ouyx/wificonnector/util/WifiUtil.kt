/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.ouyx.wificonnector.data.WiFiStrength
import com.ouyx.wificonnector.data.WifiCipherType
import java.net.InetAddress
import java.net.UnknownHostException


/**
 * WifiManager 帮助类
 *
 * @author ouyx
 * @date 2023年07月10日 11时31分
 */
object WifiUtil {

    /**
     * 判断是否拥有[permission]权限
     * @return true = 拥有该权限
     */
    private fun isPermission(application: Application?, permission: String): Boolean {
        return application?.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 判断是否有权限进行WIFI 扫描
     * 参考https://developer.android.com/guide/topics/connectivity/wifi-scan?hl=zh-cn
     * @return true = 拥有该权限
     */
    fun isPermissionScan(application: Application?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Android 10 or above
            return isPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
                    && isPermission(application, Manifest.permission.CHANGE_WIFI_STATE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            //Android 9.0 or above
            return (isPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    isPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION))
                    && isPermission(application, Manifest.permission.CHANGE_WIFI_STATE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Android 8.0 or above
            return (isPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    isPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    isPermission(application, Manifest.permission.CHANGE_WIFI_STATE))
        }
        return true
    }

    /**
     * 判断是否有权限 调用WIFI
     *
     * Android Q 及以上  有CHANGE_NETWORK_STATE 普通权限就可以连接，  有ACCESS_FINE_LOCATION权限能获取连接后WIFI 的ssid信息
     *
     *
     */
    fun isPermissionConnect(application: Application?): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isPermission(application, Manifest.permission.CHANGE_NETWORK_STATE) &&
                    isPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            isPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
                    && isPermission(application, Manifest.permission.ACCESS_WIFI_STATE)
                    && isPermission(application, Manifest.permission.CHANGE_WIFI_STATE)
        }
    }


    /**
     *  判断位置信息是否开启
     */
    fun isLocationEnabled(context: Application): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    /**
     *  判断String 是否 都是 Hex 字符
     */
    fun isHex(key: String): Boolean {
        for (i in key.length - 1 downTo 0) {
            val c = key[i]
            if (!(c in '0'..'9' || c in 'A'..'F' || (c in 'a'..'f'))) {
                return false
            }
        }
        return true
    }

    /**
     * Convert a IPv4 address from an integer to an InetAddress.
     *
     * @param hostAddress an int corresponding to the IPv4 address in network byte order
     */
    fun intToInetAddress(hostAddress: Int): InetAddress? {
        val addressBytes = byteArrayOf(
            (0xff and hostAddress).toByte(),
            (0xff and (hostAddress shr 8)).toByte(),
            (0xff and (hostAddress shr 16)).toByte(),
            (0xff and (hostAddress shr 24)).toByte()
        )
        return try {
            InetAddress.getByAddress(addressBytes)
        } catch (e: UnknownHostException) {
            throw AssertionError()
        }
    }

    /**
     * WIFI 是否开启
     */
    fun isWifiEnable(wifiManager: WifiManager) = wifiManager.isWifiEnabled

    /**
     * 获取当前已连接 wifi 的信息
     *
     * 比如
     * SSID: "ouyx", Security type: 2, Supplicant state: COMPLETED, Wi-Fi standard: 4, RSSI: -22, Link speed: 192Mbps,
     * Tx Link speed: 192Mbps, Max Supported Tx Link speed: 144Mbps, Rx Link speed: 192Mbps, Max Supported Rx Link speed: 144Mbps,
     * Frequency: 2437MHz, Net ID: 8, Metered hint: true, score: 60, CarrierMerged: false, SubscriptionId: -1, IsPrimary: -1
     */
    fun getWifiInfo(wifiManager: WifiManager): WifiInfo = wifiManager.connectionInfo


    /**
     * 获取ip地址
     */
    fun getIpAddress(wifiManager: WifiManager): String? =
        intToInetAddress(wifiManager.connectionInfo.ipAddress)?.hostAddress


    /**
     * 获取 mac 地址
     */
    fun getMacAddress(wifiManager: WifiManager) = wifiManager.connectionInfo.macAddress


    /**
     * 获取网关地址
     */
    fun getGateway(wifiManager: WifiManager): String? =
        intToInetAddress(wifiManager.dhcpInfo.gateway)?.hostAddress


    /**
     * 获取已连接WifiS设备的 SSID
     *
     * @return SSID
     */
    @SuppressLint("MissingPermission")
    fun getConnectedSsid(wifiManager: WifiManager): String? {
        val wifiInfo = wifiManager.connectionInfo
        var connectedWifiSSID = wifiInfo.ssid
        val networkId = wifiInfo.networkId
        val configuredNetworks = wifiManager.configuredNetworks
        for (wifiConfiguration in configuredNetworks) {
            if (wifiConfiguration.networkId == networkId) {
                connectedWifiSSID = wifiConfiguration.SSID
                break
            }
        }
        return connectedWifiSSID
    }


    /**
     *  根据  ScanResult.capabilities 判断 WifI 信号强弱
     *
     */
    fun analyzeSignalStrength(signalStrength: Int): WiFiStrength {
        return when {
            signalStrength >= -35 -> WiFiStrength.STRONG
            signalStrength >= -40 -> WiFiStrength.MODERATE
            signalStrength >= -70 -> WiFiStrength.NORMAL
            else -> WiFiStrength.WEAK
        }
    }


    /**
     *  获取加密方式
     *
     */
    fun analyzeWifiCipherType(capabilities: String?): WifiCipherType {
        if (capabilities == null || capabilities.isEmpty()) {
            return WifiCipherType.NO_PASS
        }
        return if (capabilities.contains("WPA3")) {
            WifiCipherType.WPA3
        } else if (capabilities.contains("WPA2")) {
            WifiCipherType.WPA2
        } else if (capabilities.contains("WEP")) {
            WifiCipherType.WEP
        } else {
            WifiCipherType.NO_PASS
        }
    }

    /**
     * 判断一个字符串是否可以进行ASCII编码
     */
    fun isAsciiEncodable(str: String): Boolean {
        return str.toCharArray().all { it.toInt() in 0..127 }
    }

}