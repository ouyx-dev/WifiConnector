package com.ouyx.wificonnector

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.ouyx.wificonnector.databinding.ActivityMainBinding
import com.ouyx.wificonnector.core.request.WifiConnectRequest
import com.ouyx.wificonnector.launch.WifiConnector

class MainActivity : AppCompatActivity() {
    lateinit var viewBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        WifiConnector.getInstance().init(application)

        viewBinding.btnConnect.setOnClickListener {
            WifiConnectRequest.getInstance().startConnect("ouyx", "123456") {
                onConnectStart {
                    MyLogger.i("onConnectStart")
                    findViewById<TextView>(R.id.txt_log).text = "onConnectStart"

                }
                onConnectSuccess {
                    MyLogger.i("onConnectSuccess")
                    findViewById<TextView>(R.id.txt_log).text = "onConnectSuccess $it"
                }
                onConnectFail {
                    MyLogger.i("onConnectFail $it")
                    findViewById<TextView>(R.id.txt_log).text = "onConnectFail $it"
                }
            }

        }


        viewBinding.btnCancelByChoice.setOnClickListener {
            WifiConnectRequest.getInstance().stopConnect()
        }

        viewBinding.btnScan.setOnClickListener {
            WifiConnector.getInstance().startScan {
                onScanStart { }
                onScanSuccess {

                }
                onScanFail {

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WifiConnectRequest.getInstance().removeAllCallBack()
    }
}