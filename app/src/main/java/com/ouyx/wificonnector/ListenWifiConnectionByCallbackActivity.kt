/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import com.ouyx.wificonnector.data.ConnectFailType
import com.ouyx.wificonnector.data.WifiConnectInfo
import com.ouyx.wificonnector.databinding.ActivityListenCallbackBinding
import com.ouyx.wificonnector.launch.WifiConnector
import com.ouyx.wificonnector.util.WifiUtil


/**
 *
 *
 * @author ouyx
 * @date 2023年08月10日 15时30分
 */
class ListenWifiConnectionByCallbackActivity :
    BaseActivity<ActivityListenCallbackBinding>(ActivityListenCallbackBinding::inflate) {
    private val log: DefaultLogger = DefaultLogger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding.but.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermission(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), agree = {
                    startConnect()
                }, disAgree = {
                    log.info(message = "权限拒绝")
                })
            } else {
                requestPermission(
                    arrayOf(
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ), agree = {
                        startConnect()
                    }, disAgree = {
                        log.info(message = "权限拒绝")
                    })
            }
        }

        viewBinding.butRegister.setOnClickListener {
            registerNetworkCallback()
        }
    }

    private var networkCallback: ConnectivityManager.NetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectedSSID = WifiUtil.getConnectedSsid(wifiManager)?.replace("\"", "")
            val wifiConnectedInfo = WifiConnectInfo().apply {
                name = connectedSSID
                ip = WifiUtil.getIpAddress(wifiManager)
                gateWay = WifiUtil.getGateway(wifiManager)
            }
            log.debug(message = "###onAvailable: WiFi connected $wifiConnectedInfo")
        }
        override fun onLost(network: Network) {
            log.debug(message = "###onLost: WiFi disconnected")
        }
    }


    @SuppressLint("SetTextI18n")
    private fun startConnect() {
        WifiConnector.get()
            .connect("ouyx", "123456789") {
                onConnectStart {
                    log.debug(message = "onConnectStart>>>")
                }
                onConnectSuccess {
                    log.debug(message = "onConnectSuccess\n $it")
                }
                onConnectFail {
                    val cause: String = when (it) {
                        ConnectFailType.CancelByChoice -> "用户主动取消"
                        ConnectFailType.ConnectTimeout -> "超时"
                        ConnectFailType.ConnectingInProgress -> "正在连接中..."
                        ConnectFailType.PermissionNotEnough -> "权限不够"
                        is ConnectFailType.SSIDConnected -> "目标SSID 已连接[${it.wifiConnectInfo}]"
                        ConnectFailType.WifiNotEnable -> "WIFI未开启"
                        ConnectFailType.ConnectUnavailable -> "连接不可达"
                        ConnectFailType.EncryptionPasswordNotNull -> "加密时密码不能为空"
                        ConnectFailType.PasswordMustASCIIEncoded -> "秘密必须被ASCII编码"
                        ConnectFailType.SsidInvalid -> "SSID 无效"
                    }
                    log.debug(message = "onConnectFail: $cause")
                }
            }
    }


    /**
     *  注册网络 监听
     */
    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        // 创建网络请求
        val builder = NetworkRequest.Builder()
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

        // 注册网络回调
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
    }

    private fun unregisterNetworkCallback() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterNetworkCallback()
    }
}