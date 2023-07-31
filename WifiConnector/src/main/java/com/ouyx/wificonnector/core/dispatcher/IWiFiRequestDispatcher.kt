/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.core.dispatcher

import com.ouyx.wificonnector.callback.WifiConnectCallback
import com.ouyx.wificonnector.callback.WifiScanCallback
import com.ouyx.wificonnector.data.ConnectFailType
import com.ouyx.wificonnector.data.WifiCipherType


/**
 * 请求分发中心接口
 *
 * @author ouyx
 * @date 2023年07月10日 15时26分
 */
interface IWiFiRequestDispatcher {
    /**
     * 连接指定WIFI
     */
    fun connect(
        ssid: String,
        pwd: String?,
        cipherType: WifiCipherType = WifiCipherType.WEP,
        timeoutInMillis: Long?,
        connectCallback: WifiConnectCallback.() -> Unit
    )

    /**
     *  扫描 获取WIFI 列表
     */
    fun startScan(scanCallback: WifiScanCallback.() -> Unit)


    /**
     * 连接过程中，主动取消连接任务
     *
     * Android Q之前的设备,连接过程中主动取消连接任务, callConnectFail回调会触发，回调传入的参数为[ConnectFailType.CancelByChoice]
     * Android Q或者Android Q后设备，不支持连接时取消任务
     */
    fun stopConnect()


    /**
     * 移除所有回调
     */
    fun removeAllCallback()


    /**
     * 回收所有资源
     */
    fun release()



}