/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.core.request

import com.ouyx.wificonnector.callback.WifiConnectCallback
import com.ouyx.wificonnector.data.CancelReason
import com.ouyx.wificonnector.data.ConnectFailType
import com.ouyx.wificonnector.util.DefaultLogger
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

    private var mConnectTime = 5000L

    private val isConnecting = AtomicBoolean(false)


    companion object {
        @Volatile
        private var INSTANCE: WifiConnectRequest? = null
        fun getInstance(): WifiConnectRequest =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WifiConnectRequest().also { INSTANCE = it }
            }
    }


    enum class WifiCipherType {
        WEP, WPA, NOPASS,
    }

    /**
     * 连接WIFI
     *
     * @param ssid
     * @param pwd
     * @param cipherType
     *
     */
    fun startConnect(ssid: String, pwd: String, cipherType: WifiCipherType = WifiCipherType.WPA, connectCallback: WifiConnectCallback.() -> Unit) {
        mConnectCallback = WifiConnectCallback()
        mConnectCallback?.connectCallback()

        if (isConnecting.get()) {
            mConnectCallback?.callConnectFail(ConnectFailType.ConnectingInProgress)
            return
        }

        mConnectJob = ioScope.launch {
            withTimeout(mConnectTime) {
                isConnecting.set(true)
                mConnectCallback?.callConnectStart()

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
     * 主动停止连接 WIFI
     */
    fun stopConnect() {
        mConnectJob?.cancel(CancelReason.CancelByChoice)
    }

    /**
     *  回收所有资源
     */
    fun closeAll() {

    }

    /**
     * 解除所有CallBack
     */
    fun removeAllCallBack() {
        mConnectCallback = null
    }

    override fun removeCallback() {
        TODO("Not yet implemented")
    }

    override fun release() {
        TODO("Not yet implemented")
    }


}