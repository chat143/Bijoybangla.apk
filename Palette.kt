package com.bijoy.keyboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

/** হালকা ও ডার্ক, দুই মোডের রঙ এক জায়গায় */
class Palette(val dark: Boolean) {
    private fun c(s: String) = Color.parseColor(s)

    val bg = c(if (dark) "#0B1020" else "#F1F5FF")
    val card = c(if (dark) "#151D33" else "#FFFFFF")
    val stroke = c(if (dark) "#25305A" else "#E3E9FA")
    val text = c(if (dark) "#EEF2FF" else "#0F1838")
    val sub = c(if (dark) "#9FACCB" else "#5A6685")
    val key = c(if (dark) "#1C2745" else "#EFF3FF")
    val primary = c("#3B6BFF")
    val ok = c("#16B36B")
    val offTrack = c(if (dark) "#2B3658" else "#D3DAEE")
    val heroA = c(if (dark) "#2347C9" else "#3B6BFF")
    val heroB = c(if (dark) "#5A39D1" else "#7A4DFF")
}

fun rounded(color: Int, radius: Float, strokeColor: Int = 0, strokeWidth: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
    }
