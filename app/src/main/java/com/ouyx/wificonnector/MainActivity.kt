package com.ouyx.wificonnector

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.ouyx.wificonnector.databinding.ActivityMainBinding
import com.ouyx.wificonnector.core.request.WifiConnectRequest
import com.ouyx.wificonnector.launch.WifiConnector
import com.ouyx.wificonnector.util.WifiUtil

class MainActivity : BaseActivity() {
    lateinit var viewBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        WifiConnector.getInstance().init(application)

        viewBinding.btnConnect.setOnClickListener {
            WifiConnector.getInstance().startConnect("ouyx", "123456") {
                onConnectStart {
                    DefaultLogger.info(message = "onConnectStart")
                    findViewById<TextView>(R.id.txt_log).text = "onConnectStart"
                }
                onConnectSuccess {
                    DefaultLogger.info(message = "onConnectSuccess")
                    findViewById<TextView>(R.id.txt_log).text = "onConnectSuccess $it"
                }
                onConnectFail {
                    DefaultLogger.info(message = "onConnectFail $it")
                    findViewById<TextView>(R.id.txt_log).text = "onConnectFail $it"
                }
            }
        }


        viewBinding.btnCancelByChoice.setOnClickListener {
            WifiConnectRequest.getInstance().stopConnect()
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