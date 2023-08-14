/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import com.ouyx.wificonnector.data.ConnectFailType
import com.ouyx.wificonnector.data.WifiConnectInfo
import com.ouyx.wificonnector.databinding.ActivityListenBroadcastBinding
import com.ouyx.wificonnector.launch.WifiConnector
import com.ouyx.wificonnector.util.WifiUtil


/**
 * 测试 WIFI 监听
 *
 * @author ouyx
 * @date 2023年08月10日 14时57分
 */
class ListenWifiConnectionByBroadcastActivity :
    BaseActivity<ActivityListenBroadcastBinding>(ActivityListenBroadcastBinding::inflate) {

    private val log = DefaultLogger()
    private val mWifiConnectBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
                    val networkState =
                        (intent.getParcelableExtra<Parcelable>(WifiManager.EXTRA_NETWORK_INFO) as NetworkInfo).detailedState
                    handleNetState(networkState)
                }
            }
        }
    }

    private fun handleNetState(state: NetworkInfo.DetailedState) {
        if (state == NetworkInfo.DetailedState.AUTHENTICATING) {
            log.debug(message = "认证中...")
        } else if (state == NetworkInfo.DetailedState.CONNECTING) {
            log.debug(message = "连接中...")
        } else if (state == NetworkInfo.DetailedState.DISCONNECTED) {
            log.debug(message = "已断开连接...")
        } else if (state == NetworkInfo.DetailedState.DISCONNECTING) {
            log.debug(message = "断开连接中...")
        } else if (state == NetworkInfo.DetailedState.FAILED) {
            log.debug(message = "连接失败...")
        } else if (state == NetworkInfo.DetailedState.SCANNING) {
            log.debug(message = "搜索中...")
        } else if (state == NetworkInfo.DetailedState.CONNECTED) {
            log.debug(message = "###收到 已连接的广播")
            val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager

            val connectedSSID = WifiUtil.getConnectedSsid(wifiManager)?.replace("\"", "")
            val wifiConnectedInfo = WifiConnectInfo().apply {
                name = connectedSSID
                ip = WifiUtil.getIpAddress(wifiManager)
                gateWay = WifiUtil.getGateway(wifiManager)
            }
            log.debug(message = "连接上 $wifiConnectedInfo")
        }
    }


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


    override fun onResume() {
        super.onResume()
        val mWifiConnectIntentFilter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)

        }
        application.registerReceiver(mWifiConnectBroadcastReceiver, mWifiConnectIntentFilter)

    }

    override fun onPause() {
        super.onPause()

        unregisterReceiver(mWifiConnectBroadcastReceiver)

    }


}