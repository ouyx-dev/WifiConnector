/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.core.dispatcher

import com.ouyx.wificonnector.callback.WifiConnectCallback
import com.ouyx.wificonnector.callback.WifiScanCallback
import com.ouyx.wificonnector.data.WifiCipherType


/**
 * 请求分发中心接口
 *
 * @author ouyx
 * @date 2023年07月10日 15时26分
 */
interface IRequestDispatcher {
    /**
     * 连接指定WIFI
     */
    fun connect(
        ssid: String,
        pwd: String?,
        cipherType: WifiCipherType = WifiCipherType.WEP,
        timeoutInMillis: Long,
        connectCallback: WifiConnectCallback.() -> Unit
    )

    /**
     *  扫描 获取WIFI 列表
     */
    fun startScan(scanCallback: WifiScanCallback.() -> Unit)

}