package com.notifier.client.ui

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.notifier.client.R

fun setBottomNavTabActive(indicator: View, icon: ImageView, label: TextView, active: Boolean) {
    val context = icon.context
    val colorRes = if (active) R.color.dash_emerald_bright else R.color.glass_text_secondary
    val color = ContextCompat.getColor(context, colorRes)

    indicator.visibility = if (active) View.VISIBLE else View.INVISIBLE
    icon.imageTintList = ColorStateList.valueOf(color)
    label.setTextColor(color)
}
