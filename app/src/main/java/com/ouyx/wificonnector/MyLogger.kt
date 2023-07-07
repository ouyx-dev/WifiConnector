/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector

import android.util.Log


/**
 * log工具
 *
 * @author ouyx
 * @date 2023年07月06日 17时44分
 */
object MyLogger {

    private const val tag = "ouyx"
    private var isLogger = true
    private var isShowStackTrace = true

    fun d(msg: String?) {
        if (isLogger && msg != null) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.d(tag, msg + getExtInfo(stackTraceElement))
        }
    }

    fun i(msg: String?) {
        if (isLogger && msg != null) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.i(tag, msg + getExtInfo(stackTraceElement))
        }
    }

    fun e(msg: String?) {
        if (isLogger && msg != null) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.e(tag, msg + getExtInfo(stackTraceElement))
        }
    }

    fun w(msg: String?) {
        if (isLogger && msg != null) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.w(tag, msg + getExtInfo(stackTraceElement))
        }
    }


    private fun getExtInfo(stackTraceElement: StackTraceElement): String {
        val separator = " & "
        val sb = StringBuilder("\t[")
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