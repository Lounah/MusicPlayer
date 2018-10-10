package com.lounah.musicplayer.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.MotionEvent
import kotlin.math.abs


object ViewUtilities {

    fun dpToPx(dp: Int, context: Context): Int = Math.round(dp * (context.resources.displayMetrics.densityDpi / 160f))

    fun pxToDp(px: Int, context: Context) = Math.round(px * DisplayMetrics.DENSITY_DEFAULT.toDouble()) / context.resources.displayMetrics.xdpi

    fun spToPx(sp: Float, context: Context): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics).toInt()
    }

    fun isMotionEventInRect(targetRect: Rect, motionEvent: MotionEvent) = motionEvent.x >= targetRect.left - 20f
            && motionEvent.x <= targetRect.right + 20f
            && motionEvent.y >= targetRect.top - 20f
            && motionEvent.y <= targetRect.bottom + 20f

    fun isMotionEventInRect(targetRect: RectF, motionEvent: MotionEvent) = motionEvent.x >= targetRect.left - 20f
            && motionEvent.x <= targetRect.right + 20f
            && motionEvent.y >= targetRect.top - 20f
            && motionEvent.y <= targetRect.bottom + 20f

    fun drawOnClickShape(canvas: Canvas, targetRect: Rect, context: Context, paint: Paint) {
        canvas.drawCircle(targetRect.exactCenterX(), targetRect.exactCenterY(), abs(targetRect.right - targetRect.left) / 2f + dpToPx(8, context), paint)
    }

    fun isInLandscape(context: Context)
            = context.resources.configuration.orientation ==  Configuration.ORIENTATION_LANDSCAPE
}