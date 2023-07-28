package com.ouyx.wificonnector

import android.Manifest
import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.ouyx.wificonnector.data.ConnectFailType
import com.ouyx.wificonnector.data.ScanFailType
import com.ouyx.wificonnector.data.WifiCipherType
import com.ouyx.wificonnector.databinding.ActivityMainBinding
import com.ouyx.wificonnector.databinding.ScanningBinding
import com.ouyx.wificonnector.databinding.ScanningFailBinding
import com.ouyx.wificonnector.databinding.ScanningSuccessBinding
import com.ouyx.wificonnector.launch.WifiConnector


class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private var mChipType = WifiCipherType.WPA2
    private var mListAdapter: ScanListAdapter = ScanListAdapter()

    private val log: DefaultLogger = DefaultLogger()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.recy.apply {
            adapter = mListAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }


        WifiConnector.get().init(application)

        viewBinding.radiosCipher.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                viewBinding.radioWep.id -> {
                    mChipType = WifiCipherType.WEP
                }

                viewBinding.radioWpa2.id -> {
                    mChipType = WifiCipherType.WPA2
                }

                viewBinding.radioWpa3.id -> {
                    mChipType = WifiCipherType.WPA3
                }

                viewBinding.radioNoPass.id -> {
                    mChipType = WifiCipherType.NO_PASS
                }
            }
        }
        viewBinding.btnConnect.setOnClickListener {
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

        viewBinding.btnScan.setOnClickListener {
            requestPermission(
                arrayOf(Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION),
                agree = {
                    startScan()
                },
                disAgree = {
                    log.error(message = "没有权限!")
                })
        }

        mListAdapter.setOnItemClickListener { _, _, position ->
            val wifiScanItem = mListAdapter.data[position]
            viewBinding.editSsid.setText(wifiScanItem.ssid)
            viewBinding.editPassword.setText("")
            when (wifiScanItem.cipherType) {
                WifiCipherType.WEP -> viewBinding.radioWep.isChecked = true
                WifiCipherType.WPA2 -> viewBinding.radioWpa2.isChecked = true
                WifiCipherType.WPA3 -> viewBinding.radioWpa3.isChecked = true
                WifiCipherType.NO_PASS -> viewBinding.radioNoPass.isChecked = true
            }
        }

        viewBinding.butTest.setOnClickListener {
            test()
        }


        test()
    }


    @SuppressLint("SetTextI18n")
    private fun startConnect() {
        WifiConnector.get()
            .connect(viewBinding.editSsid.text.toString(), viewBinding.editPassword.text.toString(), mChipType) {
                onConnectStart {
                    log.debug(message = "onConnectStart>>>")
                    viewBinding.txtLog.text = "连接中..."
                }
                onConnectSuccess {
                    log.debug(message = "onConnectSuccess\n $it")
                    viewBinding.txtLog.text = "onConnectSuccess\n$it"
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
                    viewBinding.txtLog.text = "onConnectFail: $cause"
                }
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun startScan() {
        mListAdapter.data.clear()
        mListAdapter.notifyDataSetChanged()

        WifiConnector.get().scan {
            onScanStart {
                mListAdapter.setHeaderView(getScanningView())

                log.debug(message = "onScanStart")
            }
            onScanSuccess { scanResults, parsedScanResults ->
                mListAdapter.setHeaderView(getSuccessView())

                log.debug(message = "onScanSuccess: $scanResults")
                parsedScanResults.forEach {
                    val ssid = it.ssid
                    val level = it.level
                    val capabilities = it.cipherType
                    log.info(message = "ssid = $ssid   level = $level   capabilities = $capabilities ")
                }
                mListAdapter.setList(parsedScanResults)
            }
            onScanFail {
                mListAdapter.data.clear()
                mListAdapter.notifyDataSetChanged()

                val errorMsg = when (it) {
                    ScanFailType.LocationNotEnable -> "位置信息未开启"
                    ScanFailType.PermissionNotGranted -> "需要ACCESS_FINE_LOCATION 和 CHANGE_WIFI_STATE 权限，参考https://developer.android.com/guide/topics/connectivity/wifi-scan?hl=zh-cn"
                    ScanFailType.ScanningInProgress -> "当前正在扫描，请稍后再试.."
                    ScanFailType.StartScanFail -> "由于短时间扫描过多，扫描请求可能遭到节流"
                    ScanFailType.ResultNotUpdated -> "WiFi扫描列表未更新"
                }
                mListAdapter.setHeaderView(getErrorView(errorMsg))

                log.debug(message = "onScanFail: $errorMsg")
            }
        }
    }


    private fun getScanningView(): View {
        val scanningBinding = ScanningBinding.inflate(layoutInflater)
        return scanningBinding.root
    }

    @SuppressLint("SetTextI18n")
    private fun getErrorView(msg: String): View {
        val scanningErrorBinding = ScanningFailBinding.inflate(layoutInflater)
        scanningErrorBinding.tvError.text = "扫描失败：$msg"
        return scanningErrorBinding.root
    }

    private fun getSuccessView(): View {
        val scanningSuccessBinding = ScanningSuccessBinding.inflate(layoutInflater)
        return scanningSuccessBinding.root
    }


    private fun test() {
        registerNetworkCallback()
    }


    private lateinit var networkCallback: NetworkCallback
    private fun registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

            // 创建网络请求
            val builder = NetworkRequest.Builder()
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

            // 创建网络回调
            networkCallback = object : NetworkCallback() {
                override fun onAvailable(network: Network) {
                    log.debug(message = "onAvailable: WiFi connected")
                    // WiFi已连接
                }

                override fun onLost(network: Network) {
                    log.debug(message = "onLost: WiFi disconnected")
                    // WiFi已断开连接
                }
            }

            // 注册网络回调
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
        }
    }

    private fun unregisterNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WifiConnector.get().release()
    }
}


