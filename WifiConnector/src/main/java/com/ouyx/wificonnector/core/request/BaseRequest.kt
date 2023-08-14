/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.core.request

import com.ouyx.wificonnector.core.dispatcher.WifiRequestDispatcher
import com.ouyx.wificonnector.launch.WifiConnector


/**
 * 所有Request 基类
 *
 * @author ouyx
 * @date 2023年07月10日 15时10分
 */
abstract class BaseRequest {


    fun getApplication() = WifiConnector.get().mApplication

    fun getWifiManager() = WifiConnector.get().mWifiManager


    val ioScope = WifiRequestDispatcher.getInstance().getIOScope()

    val mainScope = WifiRequestDispatcher.getInstance().getMainScope()

    val defaultScope = WifiRequestDispatcher.getInstance().getMainScope()

    /**
     * 移除 Request 对上层的 回调
     */
    abstract fun removeCallback()


    /**
     *  销毁所有资源
     */
    abstract fun release()


}