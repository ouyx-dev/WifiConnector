/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.data

import java.util.concurrent.CancellationException


/**
 *  Job 取消原因
 *
 * @author ouyx
 * @date 2023年07月07日 16时17分
 */
sealed class CancelReason : CancellationException() {
    /**
     * 主动取消Job
     */
    object CancelByChoice : CancelReason()

    /**
     * 因为异常而取消Job
     */
    object CancelByError : CancelReason()

    /**
     * 操作成功 而取消Job
     */
    class CancelBySuccess(val wifiConnectInfo: WifiConnectInfo) : CancelReason()
}