/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.data


/**
 * WIFI 连接完成后设备信息
 *
 * @author ouyx
 * @date 2023年07月06日 15时18分
 *
 * @param name: SSID
 * @param ip： 默认是IpV4
 * @param gateWay : 网关地址的字符串形式
 */
data class WifiConnectInfo(
    var name: String? = null,
    var ip: String? = null,
    var gateWay: String? = null
) {
    override fun toString(): String = "WifiConnectInfo{" +
            "name='" + name + '\'' +
            ", ip='" + ip + '\'' +
            ", gateWay='" + gateWay + '\'' +
            '}'

}