/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.data


/**
 * 连接失败后原因
 *
 * @author ouyx
 * @date 2023年07月06日 15时21分
 */
sealed class ConnectFailType {

    /**
     * 超时未连接
     */
    object ConnectTimeout : ConnectFailType()

    /**
     * 权限不够
     */
    object PermissionNotEnough : ConnectFailType()

    /**
     * 主动取消
     */
    object CancelByChoice : ConnectFailType()


    /**
     * 正在 连接
     */
    object ConnectingInProgress : ConnectFailType()


    /**
     * 指定连接的SSID 已经连接
     */
    class SSIDConnected(val wifiConnectInfo: WifiConnectInfo) : ConnectFailType()

    /**
     * WIFI  没有开启
     */
    object WifiNotEnable :ConnectFailType()



}