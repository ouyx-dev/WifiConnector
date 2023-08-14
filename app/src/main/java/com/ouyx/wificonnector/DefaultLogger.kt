package com.ouyx.wificonnector

import android.util.Log

 class DefaultLogger {
    private val defaultTag = "ouyx"
    private var isShowLog = true
    private var isShowStackTrace = true

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
        if (isShowStackTrace) {
            val threadName = Thread.currentThread().name
            sb.append("ThreadName=").append(threadName)

        }
        sb.append(" ] ")
        return sb.toString()
    }

}