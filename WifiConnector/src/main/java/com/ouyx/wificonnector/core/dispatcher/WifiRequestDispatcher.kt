/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.core.dispatcher

import com.ouyx.wificonnector.callback.WifiConnectCallback
import com.ouyx.wificonnector.callback.WifiScanCallback
import com.ouyx.wificonnector.core.request.WifiConnectRequest
import com.ouyx.wificonnector.core.request.WifiScanRequest
import kotlinx.coroutines.*


/**
 *  请求 分发中心
 *
 * @author ouyx
 * @date 2023年07月10日 15时22分
 */
class WifiRequestDispatcher : IRequestDispatcher {
    private val mainScope = MainScope()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    internal fun getMainScope() = mainScope

    internal fun getIOScope() = ioScope

    internal fun getDefaultScope() = defaultScope


    companion object {
        @Volatile
        private var INSTANCE: WifiRequestDispatcher? = null
        fun getInstance(): WifiRequestDispatcher =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WifiRequestDispatcher().also { INSTANCE = it }
            }
    }

    override fun startConnect(
        ssid: String,
        pwd: String,
        cipherType: WifiConnectRequest.WifiCipherType,
        connectCallback: WifiConnectCallback.() -> Unit
    ) {
        val wifiConnectCallback = WifiConnectCallback()
        connectCallback.invoke(wifiConnectCallback)

        WifiConnectRequest.getInstance().startConnect(ssid, pwd, cipherType, wifiConnectCallback)
    }

    override fun startScan(scanCallback: WifiScanCallback.() -> Unit) {
        val wifiScanCallback = WifiScanCallback()
        scanCallback.invoke(wifiScanCallback)
        WifiScanRequest.getInstance().startScan(wifiScanCallback)
    }


    /**
     *  回收所有资源
     */
    fun release() {
        WifiScanRequest.getInstance().release()

        mainScope.cancel()
        ioScope.cancel()
        defaultScope.cancel()
        INSTANCE = null
    }

    /**
     * 解除所有 CallBack
     */
    fun removeAllCallBack() {
        WifiScanRequest.getInstance().removeCallback()
    }

}