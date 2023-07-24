/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector.exceptions


/**
 * InitializationException
 *
 * @author ouyx
 * @date 2023年07月24日 16时59分
 */
class InitializationException(msg: String = "WifiConnector未初始化，请先调用WifiConnector.init()") : RuntimeException(msg)