package com.ouyx.wificonnector.util

import android.util.Log

object DefaultLogger {
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
        val separator = " & "
        val sb = StringBuilder("[")
        if (isShowStackTrace) {
            val threadName = Thread.currentThread().name
            val fileName = stackTraceElement.fileName
            val className = stackTraceElement.className
            val methodName = stackTraceElement.methodName
            val lineNumber = stackTraceElement.lineNumber
            sb.append("ThreadName=").append(threadName).append(separator)
            sb.append("FileName=").append(fileName).append(separator)
            sb.append("ClassName=").append(className).append(separator)
            sb.append("MethodName=").append(methodName).append(separator)
            sb.append("LineNumber=").append(lineNumber)
        }
        sb.append(" ] ")
        return sb.toString()
    }

}