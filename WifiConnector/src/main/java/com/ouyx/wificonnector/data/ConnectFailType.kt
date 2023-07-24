/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
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
    object WifiNotEnable : ConnectFailType()


    /**
     *  无效参数： 加密模式 密码不能为空
     */
    object EncryptionPasswordNotNull : ConnectFailType()

    /**
     *  无效参数： 加密模式 密码不能为空
     */
    object SsidInvalid : ConnectFailType()

    /**
     *   密码格式不正确,可以进行ASCII编码
     */
    object PasswordMustASCIIEncoded : ConnectFailType()


    /**
     * 连接不可达
     */
    object ConnectUnavailable : ConnectFailType()


}