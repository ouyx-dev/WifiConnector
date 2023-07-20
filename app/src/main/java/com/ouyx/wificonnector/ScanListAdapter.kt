/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.ouyx.wificonnector

import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.ouyx.wificonnector.data.WifiScanResult


/**
 *  WiFi 扫描 列表Adapter
 *
 * @author ouyx
 * @date 2023年07月19日 17时33分
 */
class ScanListAdapter : BaseQuickAdapter<WifiScanResult, BaseViewHolder>(R.layout.item_scan) {
    override fun convert(holder: BaseViewHolder, item: WifiScanResult) {
        holder.getView<TextView>(R.id.tv_strength).text = item.level.desc
        holder.getView<TextView>(R.id.tv_ssid).text = item.ssid
        holder.getView<TextView>(R.id.tv_crpt).text=item.cipherType.name
    }
}