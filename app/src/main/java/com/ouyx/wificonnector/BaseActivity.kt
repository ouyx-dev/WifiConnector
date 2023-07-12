/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


/**
 *
 *
 * @author ouyx
 * @date 2023年07月11日 10时49分
 */
open class BaseActivity : AppCompatActivity() {
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    private var permissionAgree: (() -> Unit)? = null

    private var permissionRefuse: ((refusePermissions: ArrayList<String>) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            val refusePermission: ArrayList<String> = ArrayList()
            it.keys.forEach { res ->
                if (it[res] == false) {
                    refusePermission.add(res)
                }
            }

            if (refusePermission.size > 0) {
                permissionRefuse?.invoke(refusePermission)
            } else {
                permissionAgree?.invoke()
            }
        }
    }

    fun requestPermission(
        permissions: Array<String>,
        agree: (() -> Unit)? = null,
        refuse: ((refusePermissions: ArrayList<String>) -> Unit)? = null
    ) {
        this.permissionAgree = agree
        this.permissionRefuse = refuse

        val notGrantedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) !=
                    PackageManager.PERMISSION_GRANTED
        }
        if (notGrantedPermissions.isEmpty()) {
            permissionAgree?.invoke()
            return
        }
        permissionLauncher?.launch(permissions)
    }
}