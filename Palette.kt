package com.bijoy.keyboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

/** শুধু সাদা ও কালো। ফোনে ডার্ক মোড থাকলেও অ্যাপ সবসময় সাদা থিমে চলবে */
class Palette {
    private fun c(s: String) = Color.parseColor(s)

    val bg = c("#F4F5F7")        // পেছনের হালকা সাদা-ধূসর
    val card = c("#FFFFFF")
    val stroke = c("#E2E5EA")    // সরু কিনারা
    val text = c("#0B0B0D")      // প্রায় কালো লেখা
    val sub = c("#5B606B")       // ধূসর ছোট লেখা
    val key = c("#EEF0F3")       // কী ও ইনপুট ঘরের হালকা ধূসর
    val primary = c("#0B0B0D")   // কালো (সুইচ, সক্রিয় বোতাম)
    val ok = c("#0B0B0D")
    val offTrack = c("#D6D9DF")
}

fun rounded(color: Int, radius: Float, strokeColor: Int = 0, strokeWidth: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
    }
    
