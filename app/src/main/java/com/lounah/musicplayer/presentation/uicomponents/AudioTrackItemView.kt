package com.lounah.musicplayer.presentation.uicomponents

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.lounah.musicplayer.R
import com.lounah.musicplayer.presentation.model.AudioTrack
import com.lounah.musicplayer.util.ViewUtilities
import kotlin.math.abs

class AudioTrackItemView constructor(context: Context, attributeSet: AttributeSet?, defStyleRes: Int = 0)
    : View(context, attributeSet, defStyleRes) {

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    var currentTrack: AudioTrack? = AudioTrack()
        set(newValue) {
            field = newValue
            invalidate()
        }

    private val DEFAULT_TRACK_TITLE_SIZE = ViewUtilities.spToPx(18f, context)
    private val DEFAULT_TRACK_BAND_TEXT_SIZE = ViewUtilities.spToPx(15f, context)
    private val DEFAULT_DURATION_TEXT_SIZE = ViewUtilities.spToPx(15f, context)
    private val DEFAULT_ALBUM_COVER_SIZE = ViewUtilities.dpToPx(48, context)
    private val DEFAULT_MARGIN_16_DP = ViewUtilities.dpToPx(16, context)
    private val DEFAULT_PLAYBACK_ANIMATION_POLE_COLOR = Color.WHITE


    private var albumCoverBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.albumcoverxx)

    private lateinit var albumCoverRect: Rect

    private val titleTextPaint = TextPaint()
    private val bandTextPaint = TextPaint()
    private val durationTextPaint = TextPaint()
    private val albumCoverPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val animationPolePaint = Paint()

    private val clipPath = Path()

    private var durationMeasuredWidth = 0f

    private val playbackAnimation = ValueAnimator.ofFloat(0f, 100f)
    private var playbackAnimationLastAnimatedValue = 0f

    private var firstPlaybackPoleTopY = 0f
        set(newValue) {
            field = newValue
            invalidate()
        }
    private var secondPlaybackPoleTopY = 0f
        set(newValue) {
            field = newValue
            invalidate()
        }
    private var thirdPlaybackPoleTopY = 0f
        set(newValue) {
            field = newValue
            invalidate()
        }

    init {

        titleTextPaint.textSize = DEFAULT_TRACK_TITLE_SIZE.toFloat()

        bandTextPaint.textSize = DEFAULT_TRACK_BAND_TEXT_SIZE.toFloat()
        bandTextPaint.color = Color.GRAY

        durationTextPaint.textSize = DEFAULT_DURATION_TEXT_SIZE.toFloat()
        durationTextPaint.color = ContextCompat.getColor(context, R.color.blue)

        animationPolePaint.color = DEFAULT_PLAYBACK_ANIMATION_POLE_COLOR
        animationPolePaint.strokeWidth = ViewUtilities.dpToPx(10, context).toFloat()

        playbackAnimation.duration = 100
        playbackAnimation.addUpdateListener {
            val animatedValue = it.animatedValue as Float
            val delta = abs(animatedValue - playbackAnimationLastAnimatedValue)
            playbackAnimationLastAnimatedValue = animatedValue

            firstPlaybackPoleTopY -= delta / 20
            secondPlaybackPoleTopY -= delta / 30
            thirdPlaybackPoleTopY -= delta / 40

        }
        playbackAnimation.repeatCount = -1

        playbackAnimation.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val desiredWidth = width

        val desiredHeight = DEFAULT_MARGIN_16_DP * 2 + DEFAULT_ALBUM_COVER_SIZE

        val measuredWidth = reconcileSize(desiredWidth, widthMeasureSpec)
        val measuredHeight = reconcileSize(desiredHeight, heightMeasureSpec)

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!::albumCoverRect.isInitialized) {
            albumCoverRect = Rect(DEFAULT_MARGIN_16_DP,
                    DEFAULT_MARGIN_16_DP,
                    DEFAULT_MARGIN_16_DP + DEFAULT_ALBUM_COVER_SIZE,
                    height - DEFAULT_MARGIN_16_DP)
            durationMeasuredWidth = durationTextPaint.measureText(parseSecondsToText(currentTrack?.duration!!))

            if (currentTrack?.isBeingPlayed!!) {
                albumCoverPaint.alpha = 100
                canvas.drawLine(DEFAULT_MARGIN_16_DP
                        + ViewUtilities.dpToPx(5, context).toFloat(),
                        height - DEFAULT_MARGIN_16_DP - ViewUtilities.dpToPx(5, context).toFloat(),
                        DEFAULT_MARGIN_16_DP + ViewUtilities.dpToPx(5, context).toFloat(), firstPlaybackPoleTopY,
                        animationPolePaint)
            }
        }

        currentTrack?.let {

            // TITLE
            canvas.drawText(it.title,
                    DEFAULT_MARGIN_16_DP * 2f + DEFAULT_ALBUM_COVER_SIZE,
                    DEFAULT_MARGIN_16_DP * 2f, titleTextPaint)

            // BAND
            canvas.drawText(it.band,
                    DEFAULT_MARGIN_16_DP * 2f + DEFAULT_ALBUM_COVER_SIZE,
                    DEFAULT_MARGIN_16_DP * 4f, bandTextPaint)

            // DURATION
            canvas.drawText(parseSecondsToText(it.duration),
                    width - durationMeasuredWidth - DEFAULT_MARGIN_16_DP,
                    DEFAULT_MARGIN_16_DP * 4f, durationTextPaint)

            // ALBUM COVER
            canvas.save()
            clipPath.addRoundRect(RectF(albumCoverRect), 12f, 12f, Path.Direction.CW)
            canvas.clipPath(clipPath)
            canvas.drawBitmap(albumCoverBitmap, null, albumCoverRect, albumCoverPaint)
            canvas.restore()
        }
    }

    private fun parseSecondsToText(sec: Int): String {
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun reconcileSize(contentSize: Int, measureSpec: Int): Int {
        val mode = View.MeasureSpec.getMode(measureSpec)
        val specSize = View.MeasureSpec.getSize(measureSpec)
        return when (mode) {
            View.MeasureSpec.EXACTLY -> specSize
            View.MeasureSpec.AT_MOST -> if (contentSize < specSize) {
                contentSize
            } else {
                specSize
            }
            View.MeasureSpec.UNSPECIFIED -> contentSize
            else -> contentSize
        }
    }
}