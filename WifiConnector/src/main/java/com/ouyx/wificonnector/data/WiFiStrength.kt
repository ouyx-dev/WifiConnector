/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.data


/**
 * Wifi 强度类
 *
 * @author ouyx
 * @date 2023年07月19日 16时30分
 */
enum class WiFiStrength(val desc: String) {

    STRONG("强"),

    MODERATE("较强"),

    NORMAL("一般"),

    WEAK("弱")
}