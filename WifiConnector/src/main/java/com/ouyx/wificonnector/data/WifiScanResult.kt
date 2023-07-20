/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.data


/**
 *  扫描后 WIFI列表
 *
 * @author ouyx
 * @date 2023年07月19日 16时23分
 */


data class WifiScanResult(val ssid: String, val level: WiFiStrength, val cipherType: WifiCipherType)