/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.data


/**
 * wifi 扫描 失败情况
 *
 * @author ouyx
 * @date 2023年07月10日 10时40分
 */
sealed class ScanFailType {

    /**
     * 缺乏权限
     */
    object PermissionNotGranted : ScanFailType()

}