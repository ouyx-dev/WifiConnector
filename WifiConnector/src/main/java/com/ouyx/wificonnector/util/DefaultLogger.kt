package com.ouyx.wificonnector.util

import android.util.Log

internal object DefaultLogger {
    private const val defaultTag = "WifiConnector"
    private var isShowLog = true

    fun setDebug(isDebug: Boolean) {
        isShowLog = isDebug
    }

    fun debug(tag: String = defaultTag, message: String) {
        if (isShowLog) {
            Log.d(tag, getExtInfo() + message)
        }
    }

    fun info(tag: String = defaultTag, message: String) {
        if (isShowLog) {
            Log.i(tag, getExtInfo() + message)
        }
    }

    fun warning(tag: String = defaultTag, message: String) {
        if (isShowLog) {
            Log.w(tag, getExtInfo() + message)
        }
    }

    fun error(tag: String = defaultTag, message: String) {
        if (isShowLog) {
            Log.e(tag, getExtInfo() + message)
        }
    }


    private fun getExtInfo(): String {
        val sb = StringBuilder("\t[")
        val threadName = Thread.currentThread().name
        sb.append("ThreadName=").append(threadName)
        sb.append(" ]  ")
        return sb.toString()
    }

}