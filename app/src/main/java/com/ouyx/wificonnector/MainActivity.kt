package com.ouyx.wificonnector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.ouyx.wificonnector.data.ConnectFailType
import com.ouyx.wificonnector.data.WifiCipherType
import com.ouyx.wificonnector.databinding.ActivityMainBinding
import com.ouyx.wificonnector.launch.WifiConnector
import com.ouyx.wificonnector.util.WifiUtil


class MainActivity : BaseActivity() {
    lateinit var viewBinding: ActivityMainBinding


    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        WifiConnector.getInstance().init(application)

        viewBinding.btnConnect.setOnClickListener {
            requestPermission(arrayOf(Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION), agree = {
                WifiConnector.getInstance()
                    .startConnect(viewBinding.editSsid.text.toString(), viewBinding.editPassword.text.toString(), WifiCipherType.NO_PASS) {
                        onConnectStart {
                            DefaultLogger.debug(message = "onConnectStart>>>")
                            findViewById<TextView>(R.id.txt_log).text = "连接中..."
                        }
                        onConnectSuccess {
                            DefaultLogger.debug(message = "onConnectSuccess\n $it")
                            findViewById<TextView>(R.id.txt_log).text = "onConnectSuccess\n$it"
                        }
                        onConnectFail {
                            val cause = when (it) {
                                ConnectFailType.CancelByChoice -> "用户主动取消"
                                ConnectFailType.ConnectTimeout -> "超时"
                                ConnectFailType.ConnectingInProgress -> "正在连接中..."
                                ConnectFailType.PermissionNotEnough -> "权限不够"
                                is ConnectFailType.SSIDConnected -> "目标SSID 已连接[${it.wifiConnectInfo}]"
                                ConnectFailType.WifiNotEnable -> "WIFI未开启"
                            }
                            DefaultLogger.debug(message = "onConnectFail: $cause")
                            findViewById<TextView>(R.id.txt_log).text = "onConnectFail: $cause"
                        }
                    }
            })

        }


        viewBinding.btnScan.setOnClickListener {
            requestPermission(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), agree = {
                DefaultLogger.debug(message = " ACCESS_FINE_LOCATION 权限已获取")
                startScan()

            }, refuse = {
                DefaultLogger.error(message = "没有权限!")
            })
        }

        viewBinding.btnTest.setOnClickListener {
            requestPermission(
                arrayOf(
                    Manifest.permission.CHANGE_NETWORK_STATE,
                ), agree = {
                    connect2("ouyx", "123456789")
                }, refuse = {
                    DefaultLogger.info(message = "权限被拒绝 $it")
                })

        }
    }

    private fun startScan() {
        WifiConnector.getInstance().startScan {
            onScanStart {
                DefaultLogger.debug(message = "onScanStart")
            }
            onScanSuccess {
                DefaultLogger.debug(message = "onScanSuccess: $it")
            }
            onScanFail {
                DefaultLogger.debug(message = "onScanFail: $it")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WifiConnector.getInstance().release()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun connect2(ssid: String, pwd: String) {
        DefaultLogger.debug(message = "Build.VERSION.SDK_INT = ${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(pwd)
                .build()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build()
            val mConnectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkCallback = object : NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val connectInfo = wifiManager.connectionInfo
                    DefaultLogger.debug(
                        message = "onAvailable=${network.describeContents()} ,  connectInfo =${connectInfo.ssid}" +
                                "IP =${WifiUtil.intToInetAddress(connectInfo.ipAddress)?.hostAddress}"
                    )
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    DefaultLogger.debug(message = "onUnavailable !")
                }
            }
            // 连接wifi
            mConnectivityManager.requestNetwork(request, mNetworkCallback)
        }

    }

}