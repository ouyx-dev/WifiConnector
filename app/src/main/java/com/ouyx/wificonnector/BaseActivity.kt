/*
 * Copyright (c) 2022-2032 ouyx
 * 不能修改和删除上面的版权声明
 * 此代码属于ouyx编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding


/**
 * Base Activity
 *
 * @author ouyx
 * @date 2023年07月11日 10时49分
 */
open class BaseActivity<VB : ViewBinding>(private val inflate: (LayoutInflater) -> VB) : AppCompatActivity() {
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    private var permissionAgree: (() -> Unit)? = null

    private var permissionDisAgree: ((refusePermissions: ArrayList<String>) -> Unit)? = null

    lateinit var viewBinding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = inflate(layoutInflater)
        setContentView(viewBinding.root)

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
                permissionDisAgree?.invoke(refusePermission)
            } else {
                permissionAgree?.invoke()
            }
        }
    }

    fun requestPermission(
        permissions: Array<String>,
        agree: (() -> Unit)? = null,
        disAgree: ((refusePermissions: ArrayList<String>) -> Unit)? = null,
    ) {
        this.permissionAgree = agree
        this.permissionDisAgree = disAgree

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