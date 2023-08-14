/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.callback.listener

import com.ouyx.wificonnector.core.dispatcher.WifiRequestDispatcher
import com.ouyx.wificonnector.data.WifiConnectInfo
import kotlinx.coroutines.launch


/**
 * WiFi 连接断开信息监听器
 *
 * @author ouyx
 * @date 2023年08月11日 15时08分
 */
class WifiConnectionStatusListener {

    private val mainScope = WifiRequestDispatcher.getInstance().getMainScope()

    /**
     * 有WiFi设备连接
     */
    private var connected: ((wifiConnectInfo: WifiConnectInfo) -> Unit)? = null

    /**
     * 有WiFi设备断开
     */
    private var onDisConnected: (() -> Unit)? = null


    /**
     * 对外提供的Lambda
     */
    fun onConnected(connected: ((wifiConnectInfo: WifiConnectInfo) -> Unit)) {
        this.connected = connected
    }

    /**
     * 对外提供的Lambda
     */
    fun onDisConnected(disConnected: (() -> Unit)) {
        onDisConnected = disConnected
    }

    internal fun callOnConnected(wifiConnectInfo: WifiConnectInfo) {
        mainScope.launch { connected?.invoke(wifiConnectInfo) }
    }

    internal fun callOnDisConnected() {
        mainScope.launch { onDisConnected?.invoke() }
    }

}