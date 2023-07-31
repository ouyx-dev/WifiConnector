/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.launch

import com.ouyx.wificonnector.Constants.DEFAULT_CONNECT_TIMEOUT_MS_BEFORE_Q
import com.ouyx.wificonnector.Constants.DEFAULT_IS_DEBUG


/**
 * [WifiConnector] 构造参数
 *
 * @author ouyx
 * @date 2023年07月31日 10时21分
 */
class WiFiOptions private constructor(builder: Builder) {

    val isDebug = builder.isDebug

    /**
     * Android Q 之前连接的超时时间，默认为 [DEFAULT_CONNECT_TIMEOUT_MS_BEFORE_Q]
     */
    val connectTimeoutMsBeforeQ = builder.connectTimeOutMsBeforeQ

    companion object {

        @JvmStatic
        fun getDefaultWiFiOptions() = WiFiOptions(Builder())

    }

    class Builder {

        internal var isDebug = DEFAULT_IS_DEBUG

        internal var connectTimeOutMsBeforeQ = DEFAULT_CONNECT_TIMEOUT_MS_BEFORE_Q

        /**
         *  设置是否开始 debug
         */
        fun setDebug(isDebug: Boolean): Builder {
            this.isDebug = isDebug
            return this
        }

        /**
         * 设置Android Q 之前 WiFi连接 超时时间
         *
         * @param timeout : 超时时间，单位 ms
         */
        fun connectTimeoutMsBeforeQ(timeout: Long): Builder {
            this.connectTimeOutMsBeforeQ = timeout
            return this
        }


        fun build(): WiFiOptions = WiFiOptions(this)
    }
}