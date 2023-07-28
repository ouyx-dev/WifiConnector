package com.ouyx.wificonnector.util

import android.util.Log

internal object DefaultLogger {
    private const val defaultTag = "WifiConnector"
    private var isShowLog = true
    private var isShowStackTrace = true

    fun setDebug(isDebug: Boolean) {
        isShowLog = isDebug
    }

    fun debug(tag: String = defaultTag, message: String) {
        if (isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.d(tag, message + getExtInfo(stackTraceElement))
        }
    }

    fun info(tag: String = defaultTag, message: String) {
        if (isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.i(tag, message + getExtInfo(stackTraceElement))
        }
    }

    fun warning(tag: String = defaultTag, message: String) {
        if (isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.w(tag, message + getExtInfo(stackTraceElement))
        }
    }

    fun error(tag: String = defaultTag, message: String) {
        if (isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.e(tag, message + getExtInfo(stackTraceElement))
        }
    }

    fun error(tag: String = defaultTag, message: String, e: Throwable?) {
        if (isShowLog) {
            Log.e(tag, message, e)
        }
    }


    private fun getExtInfo(stackTraceElement: StackTraceElement): String {
        val sb = StringBuilder("\t[")
        if (isShowStackTrace) {
            val threadName = Thread.currentThread().name
            sb.append("ThreadName=").append(threadName)
        }
        sb.append(" ] ")
        return sb.toString()
    }

}