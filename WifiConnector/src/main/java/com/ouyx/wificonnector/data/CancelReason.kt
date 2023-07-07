/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
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