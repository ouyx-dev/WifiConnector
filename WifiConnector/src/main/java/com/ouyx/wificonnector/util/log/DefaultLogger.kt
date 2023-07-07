package com.ouyx.wificonnector.util.log

import android.util.Log

internal class DefaultLogger(
    private val isShowLog: Boolean = true,
    private val isShowStackTrace: Boolean = true
) : ILogger {
    override fun isDebugModel(): Boolean {
        return true
    }

    override fun debug(tag: String, message: String) {
        if (isDebugModel() && isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.d(tag, message + getExtInfo(stackTraceElement))
        }
    }

    override fun info(tag: String, message: String) {
        if (isDebugModel() && isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.i(tag, message + getExtInfo(stackTraceElement))
        }
    }

    override fun warning(tag: String, message: String) {
        if (isDebugModel() && isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.w(tag, message + getExtInfo(stackTraceElement))
        }
    }

    override fun error(tag: String, message: String) {
        if (isDebugModel() && isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.e(tag, message + getExtInfo(stackTraceElement))
        }
    }

    override fun error(tag: String, message: String, e: Throwable?) {
        if (isDebugModel() && isShowLog) {
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