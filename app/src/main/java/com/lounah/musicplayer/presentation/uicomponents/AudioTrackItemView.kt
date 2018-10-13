package com.lounah.musicplayer.presentation.uicomponents

import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import com.lounah.musicplayer.R
import com.lounah.musicplayer.presentation.model.AudioTrack
import com.lounah.musicplayer.util.ViewUtilities
import android.util.TypedValue
import com.lounah.musicplayer.core.executor.ExecutorSupplier
import com.lounah.musicplayer.core.memcache.BitmapMemoryCache
import com.lounah.musicplayer.presentation.model.PlaybackState
import java.util.concurrent.Callable
import java.util.concurrent.Future


class AudioTrackItemView constructor(context: Context, attributeSet: AttributeSet?, defStyleRes: Int = 0)
    : View(context, attributeSet, defStyleRes) {

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    private val ALBUM_COVER_BITMAP_CACHE_KEY = "ALBUM_COVER_BITMAP_CACHE_KEY_MINI"

    var currentTrack: AudioTrack? = AudioTrack()
        set(newValue) {
            field = newValue
            ellipsizedTrackTitle = ""
            ellipsizedTrackBand = ""
            invalidate()
        }

    private val DEFAULT_TRACK_TITLE_SIZE = ViewUtilities.spToPx(15f, context)
    private val DEFAULT_TRACK_BAND_TEXT_SIZE = ViewUtilities.spToPx(13f, context)
    private val DEFAULT_DURATION_TEXT_SIZE = ViewUtilities.spToPx(13f, context)
    private val DEFAULT_ALBUM_COVER_SIZE = ViewUtilities.dpToPx(48, context)
    private val DEFAULT_MARGIN_16_DP = ViewUtilities.dpToPx(16, context)
    private val DEFAULT_PAUSE_LOGO_COLOR = Color.WHITE

    private val bitmapMemoryCache = BitmapMemoryCache.instance

    private val playIconDrawable = ContextCompat.getDrawable(context, R.drawable.media_item_ic_play)

    private lateinit var albumCoverBitmap: Bitmap

    private lateinit var albumCoverRect: RectF
    private lateinit var playIconRect: Rect

    private val titleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bandTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val durationTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val albumCoverPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pauseLogoPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val clipPath = Path()

    private var durationMeasuredWidth = 0f

    private val pauseAlbumCoverShadeMatrix = floatArrayOf(
            0f, -3f, -3f, 0f, -3f,
            0f, -3f, -3f, 0f, -2f,
            0f, -3f, -2.9f, 0f, -1f,
            0f, -0.6f, 0.3f, 1.1f, 0f
            )

    private val colorMatrix = ColorMatrix(pauseAlbumCoverShadeMatrix)
    private val colorMatrixFilter = ColorMatrixColorFilter(colorMatrix)

    private var trackTitleMeasuredWidth = 0f
    private var trackBandMeasuredWidth = 0f

    private var ellipsizedTrackTitle: CharSequence = ""
    private var ellipsizedTrackBand: CharSequence = ""

    init {

        initAlbumCover()

        titleTextPaint.textSize = DEFAULT_TRACK_TITLE_SIZE.toFloat()

        bandTextPaint.textSize = DEFAULT_TRACK_BAND_TEXT_SIZE.toFloat()
        bandTextPaint.color = Color.GRAY

        durationTextPaint.textSize = DEFAULT_DURATION_TEXT_SIZE.toFloat()
        durationTextPaint.color = ContextCompat.getColor(context, R.color.blue)

        pauseLogoPaint.color = DEFAULT_PAUSE_LOGO_COLOR
        pauseLogoPaint.strokeWidth = ViewUtilities.dpToPx(8, context).toFloat()

        val outValue = TypedValue()
        getContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)
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
            initRects()
            measureTextViews()
        }

        when (currentTrack?.playbackState) {
            PlaybackState.IS_BEING_PLAYED, PlaybackState.IS_PAUSED -> {
                albumCoverPaint.colorFilter = colorMatrixFilter
            }
            PlaybackState.IDLE -> {
                albumCoverPaint.colorFilter = null
            }
        }

        currentTrack?.let {

            // BAND
            if (ellipsizedTrackBand != "") {
                canvas.drawText(ellipsizedTrackBand, 0, ellipsizedTrackBand.length, DEFAULT_MARGIN_16_DP * 2f + DEFAULT_ALBUM_COVER_SIZE,
                        height / 2f + DEFAULT_TRACK_TITLE_SIZE, bandTextPaint)
            } else {
                canvas.drawText(it.band,
                        DEFAULT_MARGIN_16_DP * 2f + DEFAULT_ALBUM_COVER_SIZE,
                        height / 2f + DEFAULT_TRACK_TITLE_SIZE, bandTextPaint)
            }

            // TITLE
            if (ellipsizedTrackTitle != "") {
                canvas.drawText(ellipsizedTrackTitle, 0, ellipsizedTrackTitle.length, DEFAULT_MARGIN_16_DP * 2f + DEFAULT_ALBUM_COVER_SIZE,
                        height / 2f - (DEFAULT_TRACK_TITLE_SIZE - DEFAULT_TRACK_BAND_TEXT_SIZE), titleTextPaint)
            } else {
                canvas.drawText(it.title,
                        DEFAULT_MARGIN_16_DP * 2f + DEFAULT_ALBUM_COVER_SIZE,
                        height / 2f - (DEFAULT_TRACK_TITLE_SIZE - DEFAULT_TRACK_BAND_TEXT_SIZE), titleTextPaint)
            }

            // DURATION
            canvas.drawText(parseSecondsToText(it.duration),
                    width - durationMeasuredWidth - DEFAULT_MARGIN_16_DP,
                    DEFAULT_MARGIN_16_DP * 4f, durationTextPaint)

            // ALBUM COVER
            if (::albumCoverBitmap.isInitialized) {
                canvas.save()
                clipPath.addRoundRect(albumCoverRect, ViewUtilities.dpToPx(6, context).toFloat(), ViewUtilities.dpToPx(6, context).toFloat(), Path.Direction.CW)
                canvas.clipPath(clipPath)
                canvas.drawBitmap(albumCoverBitmap, null, albumCoverRect, albumCoverPaint)
                canvas.restore()
            }


            if (currentTrack?.playbackState == PlaybackState.IS_BEING_PLAYED) {
                canvas.drawLine(DEFAULT_ALBUM_COVER_SIZE / 2F + DEFAULT_MARGIN_16_DP - ViewUtilities.dpToPx(6, context),
                        height - DEFAULT_MARGIN_16_DP * 2f,
                        DEFAULT_ALBUM_COVER_SIZE / 2F + DEFAULT_MARGIN_16_DP - ViewUtilities.dpToPx(6, context),
                        DEFAULT_MARGIN_16_DP * 2f, pauseLogoPaint)
                canvas.drawLine(DEFAULT_ALBUM_COVER_SIZE / 2F + DEFAULT_MARGIN_16_DP + ViewUtilities.dpToPx(6, context),
                        height - DEFAULT_MARGIN_16_DP * 2f,
                        DEFAULT_ALBUM_COVER_SIZE / 2F + DEFAULT_MARGIN_16_DP + ViewUtilities.dpToPx(6, context),
                        DEFAULT_MARGIN_16_DP * 2f, pauseLogoPaint)
            } else if (currentTrack?.playbackState == PlaybackState.IS_PAUSED) {
                playIconDrawable?.let {
                    it.bounds = playIconRect
                    it.draw(canvas)
                }
            }
        }
    }

    private fun initRects() {
        albumCoverRect = RectF(DEFAULT_MARGIN_16_DP.toFloat(),
                DEFAULT_MARGIN_16_DP.toFloat(),
                DEFAULT_MARGIN_16_DP.toFloat() + DEFAULT_ALBUM_COVER_SIZE,
                height - DEFAULT_MARGIN_16_DP.toFloat())

        playIconRect = Rect(DEFAULT_MARGIN_16_DP * 2, DEFAULT_MARGIN_16_DP * 2, DEFAULT_ALBUM_COVER_SIZE, height - DEFAULT_MARGIN_16_DP * 2)
    }

    private fun measureTextViews() {

        durationMeasuredWidth = durationTextPaint.measureText(parseSecondsToText(currentTrack?.duration!!))


        if (trackBandMeasuredWidth == 0f) {
            trackBandMeasuredWidth = bandTextPaint.measureText(currentTrack!!.band)

            val availableForBandLeftBorder = DEFAULT_MARGIN_16_DP + DEFAULT_ALBUM_COVER_SIZE + DEFAULT_MARGIN_16_DP
            val availableForBandRightBorder = width - DEFAULT_MARGIN_16_DP - durationMeasuredWidth - DEFAULT_MARGIN_16_DP

            val availableForBandWidth = availableForBandRightBorder - availableForBandLeftBorder

                ellipsizedTrackBand = TextUtils.ellipsize(currentTrack!!.band, bandTextPaint, availableForBandWidth, TextUtils.TruncateAt.END)

        }

        if (trackTitleMeasuredWidth == 0f) {
            trackTitleMeasuredWidth = titleTextPaint.measureText(currentTrack!!.title)

            val availableForTitleLeftBorder = DEFAULT_MARGIN_16_DP + DEFAULT_ALBUM_COVER_SIZE + DEFAULT_MARGIN_16_DP
            val availableForTitleRightBorder = width - DEFAULT_MARGIN_16_DP - durationMeasuredWidth - DEFAULT_MARGIN_16_DP

            val availableForTitleWidth = availableForTitleRightBorder - availableForTitleLeftBorder

                ellipsizedTrackTitle = TextUtils.ellipsize(currentTrack!!.title, titleTextPaint, availableForTitleWidth, TextUtils.TruncateAt.END)
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

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
         savedState.playbackState = when (currentTrack?.playbackState!!) {
                PlaybackState.IDLE -> 0
             PlaybackState.IS_PAUSED -> 1
             PlaybackState.IS_BEING_PLAYED -> 2
            }
            return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        this.currentTrack?.playbackState = when (state.playbackState) {
            0 -> PlaybackState.IDLE
            1 -> PlaybackState.IS_PAUSED
            2 -> PlaybackState.IS_BEING_PLAYED
            else -> PlaybackState.IDLE
        }

        super.onRestoreInstanceState(state.superState)
    }

    private fun initAlbumCover() {
        if (bitmapMemoryCache.getBitmapById(ALBUM_COVER_BITMAP_CACHE_KEY) == null) {
            val bitmapDecodeTask: Future<Bitmap>
                    = ExecutorSupplier.instance.backgroundThreadExecutor.submit(Callable<Bitmap> { getBitmapFromResources() })
            try {
                albumCoverBitmap = bitmapDecodeTask.get()
                bitmapMemoryCache.putBitmapInCache(ALBUM_COVER_BITMAP_CACHE_KEY, albumCoverBitmap)
            } catch (e: Exception) {

            }
        } else {
            albumCoverBitmap = bitmapMemoryCache.getBitmapById(ALBUM_COVER_BITMAP_CACHE_KEY)!!
        }
    }

    private fun getBitmapFromResources() = BitmapFactory.decodeResource(context.resources, R.drawable.albumcoverxx_mini)


    internal class SavedState : View.BaseSavedState {

        var playbackState: Int = 0 // 0 -- IDLE, 1 -- PAUSED, 2 -- PLAYING

        constructor(superState: Parcelable) : super(superState)

        private constructor(`in`: Parcel) : super(`in`) {
            this.playbackState = `in`.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(this.playbackState)
        }

        companion object {
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState> {
                    return arrayOf()
                }
            }
        }
    }
}