package com.lounah.musicplayer.util

import android.content.Context
import android.util.DisplayMetrics
import java.lang.Math.round
import android.util.TypedValue
import kotlin.math.round


class ViewUtilities private constructor() {

    companion object {
        fun dpToPx(dp: Int, context: Context) : Int = Math.round(dp * context.resources.displayMetrics.xdpi / 160f)

        fun pxToDp(px: Int, context: Context)
                = Math.round(px * DisplayMetrics.DENSITY_DEFAULT.toDouble()) / context.resources.displayMetrics.xdpi

        fun spToPx(sp: Float, context: Context): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics).toInt()
        }
    }
}