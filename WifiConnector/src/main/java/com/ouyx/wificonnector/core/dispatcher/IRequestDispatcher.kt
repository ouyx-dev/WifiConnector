/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.core.dispatcher

import com.ouyx.wificonnector.callback.WifiConnectCallback
import com.ouyx.wificonnector.callback.WifiScanCallback
import com.ouyx.wificonnector.core.request.WifiConnectRequest


/**
 * 请求分发中心接口
 *
 * @author ouyx
 * @date 2023年07月10日 15时26分
 */
interface IRequestDispatcher {
    fun startConnect(
        ssid: String,
        pwd: String,
        cipherType: WifiConnectRequest.WifiCipherType = WifiConnectRequest.WifiCipherType.WPA,
        connectCallback: WifiConnectCallback.() -> Unit
    )

    fun startScan(scanCallback: WifiScanCallback.() -> Unit)

}