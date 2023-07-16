package com.ouyx.wificonnector

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.ouyx.wificonnector.databinding.ActivityMainBinding
import com.ouyx.wificonnector.core.request.WifiConnectRequest
import com.ouyx.wificonnector.data.ConnectFailType
import com.ouyx.wificonnector.launch.WifiConnector
import com.ouyx.wificonnector.util.WifiUtil

class MainActivity : BaseActivity() {
    lateinit var viewBinding: ActivityMainBinding


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        WifiConnector.getInstance().init(application)

        viewBinding.btnConnect.setOnClickListener {
            requestPermission(arrayOf(Manifest.permission.CHANGE_WIFI_STATE,Manifest.permission.ACCESS_FINE_LOCATION), agree = {
                WifiConnector.getInstance().startConnect(viewBinding.editSsid.text.toString(), viewBinding.editPassword.text.toString()) {
                    onConnectStart {
                        DefaultLogger.info(message = "onConnectStart>>>")
                        findViewById<TextView>(R.id.txt_log).text = "连接中..."
                    }
                    onConnectSuccess {
                        DefaultLogger.info(message = "onConnectSuccess\n $it")
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
                        DefaultLogger.info(message = "onConnectFail: $cause")
                        findViewById<TextView>(R.id.txt_log).text = "onConnectFail: $cause"
                    }
                }
            })

        }




        viewBinding.btnScan.setOnClickListener {
            requestPermission(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), agree = {
                DefaultLogger.info(message = " ACCESS_FINE_LOCATION 权限已获取")
                startScan()

            }, refuse = {
                DefaultLogger.error(message = "没有权限!")
            })
        }

        viewBinding.btnTest.setOnClickListener {
            val isOpen = WifiUtil.isLocationEnabled(application)
            DefaultLogger.info(message = "isOpen = $isOpen")
        }
    }

    private fun startScan() {
        WifiConnector.getInstance().startScan {
            onScanStart {
                DefaultLogger.info(message = "onScanStart")
            }
            onScanSuccess {
                DefaultLogger.info(message = "onScanSuccess: $it")
            }
            onScanFail {
                DefaultLogger.info(message = "onScanFail: $it")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WifiConnector.getInstance().release()
    }
}