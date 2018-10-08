package com.lounah.musicplayer.presentation.uicomponents

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Parcelable
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.lounah.musicplayer.R
import com.lounah.musicplayer.presentation.model.AudioTrack
import com.lounah.musicplayer.util.ViewUtilities
import kotlin.math.abs
import android.os.Parcel
import android.util.Log


class BottomAudioView constructor(context: Context, attributeSet: AttributeSet?, defStyleRes: Int = 0)
    : View(context, attributeSet, defStyleRes) {

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    var currentTrack: AudioTrack = AudioTrack()
        set(newTrack) {
            field = newTrack
            trackTitleMeasuredWidth = 0f
            trackBandMeasuredWidth = 0f
            timeElapsedSinceTrackStartedToBePlayed = 0
            timeLineAnimationLastAnimatedValue = 0f
            currentPlaybackTimelineX = DEFAULT_MARGIN * 2f + ViewUtilities.dpToPx(12, context)
            if (::timelineAnimator.isInitialized) {
                timelineAnimator.duration = currentTrack.duration * 1000L
                timelineAnimator.start()
            }
            measureTrackInfoTextViews(width, height)
            invalidate()
        }

    var timeElapsedSinceTrackStartedToBePlayed: Int = 0
        set(newValue) {
            field = newValue
            invalidate()
        }

    var playbackState: Int = 1
        set(newValue) {
            field = newValue
            invalidate()
        }

    var onControlClickListener: OnControlButtonsClickListener? = null


    private val PLAYBACK_STATE_PAUSED = 1
    private val PLAYBACK_STATE_PLAYING = 2

    interface OnControlButtonsClickListener {
        fun onPauseClicked()

        fun onPlayClicked()

        fun onShuffleClicked()

        fun onMoreButtonClicked()

        fun onNextButtonClicked()

        fun onPreviousButtonClicked()

        fun onRepeatButtonClicked()

        fun onAddButtonClicked()

        fun onPlaybackTimeChanged(newPlaybackTimeSec: Int)

        fun onVisibilityChanged(newVisibilityState: Int)
    }

    private val COLLAPSED_BOTTOM_VIEW_HEIGHT = ViewUtilities.dpToPx(64, context)
    private val DEFAULT_MARGIN = ViewUtilities.dpToPx(8, context)
    private val EXPANDED_ALBUM_COVER_MARGIN = ViewUtilities.dpToPx(32, context)
    private val COLLAPSED_ALBUM_COVER_SIZE = ViewUtilities.dpToPx(48, context)
    private val EXPANDED_ALBUM_COVER_SIZE = ViewUtilities.dpToPx(356, context)
    private val DEFAULT_TRACK_TITLE_TEXT_SIZE = ViewUtilities.spToPx(18f, context)
    private val DEFAULT_TRACK_BAND_TEXT_SIZE = ViewUtilities.spToPx(21f, context)
    private val EXPANDED_TRACK_TITLE_TEXT_SIZE = ViewUtilities.spToPx(21f, context)
    private val EXPAND_ANIMATION_DURATION_MS = 200L
    private val COLLAPSE_ANIMATION_DURATION_MS = 200L
    private val DEFAULT_BACKGROUND_COLOR = Color.WHITE
    private val DEFAULT_TIMELINE_COLOR = ContextCompat.getColor(context, R.color.blueLight)
    private val FILLED_TIMELINE_COLOR = ContextCompat.getColor(context, R.color.blueDark)
    private val DEFAULT_TRACK_TIMELINE_CONTROL_VIEW_RADIUS = ViewUtilities.dpToPx(7, context)
    private val DEFAULT_TRACK_TIMELINE_TEXT_SIZE = ViewUtilities.spToPx(13f, context)
    private val DEFAULT_ON_CLICK_SHAPE_COLOR = ContextCompat.getColor(context, R.color.greyLight)

    private val collapsedPauseIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pause_28)
    private val collapsedNextIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_mini_player_next_28)
    private val collapsedPlayIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_play_48)
    private val expandedPauseIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pause_48)
    private val expandedPlayIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_play_48)
    private val expandedNextIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_skip_next_48)
    private val expandedPreviousIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_skip_previous_48)
    private val expandedShuffleDrawable = ContextCompat.getDrawable(context, R.drawable.ic_shuffle_24)
    private val expandedRepeatDrawable = ContextCompat.getDrawable(context, R.drawable.ic_repeat_24)
    private val expandedDotsDrawable = ContextCompat.getDrawable(context, R.drawable.ic_ic_more_24dp)
    private val expandedAddDrawable = ContextCompat.getDrawable(context, R.drawable.ic_add_outline_24)
    private var albumCoverBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.albumcoverxx)
    //  ?.apply { colorFilter = PorterDuffColorFilter(0x73BEF2,PorterDuff.Mode.MULTIPLY) }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val elevationPaint = Paint()
    private val trackTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val expandedTrackTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val trackBandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val controlButtonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackBaseTimelinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackFilledTimelinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPlaybackTimeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val trackPlaybackTimeCollapsedTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val onClickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val albumRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private lateinit var albumCoverRect: Rect
    private lateinit var bottomAudioViewRect: Rect

    private lateinit var collapsedPauseIconRect: Rect
    private lateinit var collapsedNextIconRect: Rect
    private lateinit var expandedPauseIconRect: Rect
    private lateinit var expandedPlayIconRect: Rect
    private lateinit var expandedNextIconRect: Rect
    private lateinit var expandedPreviousIconRect: Rect
    private lateinit var expandedShuffleIconRect: Rect
    private lateinit var expandedRepeatIconRect: Rect
    private lateinit var expandedDotsIconRect: Rect
    private lateinit var expandedAddIconRect: Rect

    private var trackTitleMeasuredWidth: Float = 0f
    private var trackBandMeasuredWidth: Float = 0f
    private var trackTitleLeftBorder: Float = 0f
    private var trackBandLeftBorder: Float = 0f

    private lateinit var ellipsizedTrackTitle: CharSequence
    private lateinit var ellipsizedTrackBand: CharSequence

    private val expandAnimator = ValueAnimator.ofFloat(0f, 100f)
    private val collapseAnimator = ValueAnimator.ofFloat(0f, 100f)
    private lateinit var timelineAnimator: ValueAnimator

    private var currentAlbumCoverCenterX = 0f
        set(newValue) {
            field = newValue
            invalidate()
        }
    private var currentAlbumCoverCenterY = 0f
        set(newValue) {
            field = newValue
            invalidate()
        }

    private var currentViewHeight: Float = 0f
        set(newValue) {
            field = newValue
            invalidate()
        }

    private var albumCoverSize: Int = COLLAPSED_ALBUM_COVER_SIZE
        set(newValue) {
            if (currentState == STATE_EXPANDING) {
                albumCoverRect.right = currentAlbumCoverCenterX.toInt() + COLLAPSED_ALBUM_COVER_SIZE / 2 + newValue
                albumCoverRect.left = currentAlbumCoverCenterX.toInt() - COLLAPSED_ALBUM_COVER_SIZE / 2 - newValue
                albumCoverRect.top = currentAlbumCoverCenterY.toInt() - COLLAPSED_ALBUM_COVER_SIZE / 2 - newValue
                albumCoverRect.bottom = currentAlbumCoverCenterY.toInt() + COLLAPSED_ALBUM_COVER_SIZE / 2 + newValue
                field = newValue
            } else {
                if (currentState == STATE_COLLAPSING) {
                    albumCoverRect.right = currentAlbumCoverCenterX.toInt() + EXPANDED_ALBUM_COVER_SIZE / 2 - newValue - DEFAULT_MARGIN * 4
                    albumCoverRect.left = currentAlbumCoverCenterX.toInt() - EXPANDED_ALBUM_COVER_SIZE / 2 + newValue + DEFAULT_MARGIN * 4
                    albumCoverRect.top = currentAlbumCoverCenterY.toInt() - EXPANDED_ALBUM_COVER_SIZE / 2 + newValue + DEFAULT_MARGIN * 4
                    albumCoverRect.bottom = currentAlbumCoverCenterY.toInt() + EXPANDED_ALBUM_COVER_SIZE / 2 - newValue - DEFAULT_MARGIN * 4
                    field = newValue
                }
            }
            invalidate()
        }

    private var currentPlaybackTimelineX: Float = DEFAULT_MARGIN * 2f + ViewUtilities.dpToPx(12, context)
        set(newValue) {
            field = newValue
            invalidate()
        }

    private var albumCoverCenterXDx = 0f
    private var albumCoverCenterYDy = 0f
    private var trackTitleTopY = 0f

    private var trackDurationTextViewMeasuredWidth = 0f

    private var rectsWereMeasured = false

    companion object {
        val STATE_COLLAPSED = 0
        val STATE_EXPANDING = 1
        val STATE_COLLAPSING = 2
        val STATE_EXPANDED = 3
    }

    var currentState = STATE_COLLAPSED
        set(newValue) {
            field = newValue
            onControlClickListener?.onVisibilityChanged(newValue)
        }
    private var isCollapsed = true

    private var collapsedNextButtonWasPressed = false
    private var collapsedPauseButtonWasPressed = false
    private var expandedPreviousButtonWasPressed = false
    private var expandedPlayButtonWasPressed = false
    private var expandedPauseButtonWasPressed = false
    private var expandedNextButtonWasPressed = false
    private var expandedRepeatButtonWasPressed = false
    private var expandedShuffleButtonWasPressed = false
    private var expandedDotsButtonWasPressed = false
    private var expandedAddButtonWasPressed = false
    private var albumCoverWasPressed = false

    private var timeLineAnimationLastAnimatedValue = 0f

    init {

        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        expandedAddDrawable?.let {
            it.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
        }

        expandedDotsDrawable?.let {
            it.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
        }

        expandedShuffleDrawable?.let {
            it.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
        }

        expandedRepeatDrawable?.let {
            it.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
        }

        expandedNextIconDrawable?.let {
            it.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
        }

        expandedPreviousIconDrawable?.let {
            it.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
        }

        collapsedPlayIconDrawable?.let {
            it.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
        }

        collapsedPlayIconDrawable
        controlButtonPaint.color = ContextCompat.getColor(context, R.color.blue)
        backgroundPaint.color = DEFAULT_BACKGROUND_COLOR
        elevationPaint.color = Color.BLACK
        trackTitlePaint.color = Color.BLACK
        trackTitlePaint.textSize = DEFAULT_TRACK_TITLE_TEXT_SIZE.toFloat()
        trackBandPaint.color = ContextCompat.getColor(context, R.color.blue)
        trackBandPaint.textSize = DEFAULT_TRACK_BAND_TEXT_SIZE.toFloat()
        expandedTrackTitlePaint.textSize = EXPANDED_TRACK_TITLE_TEXT_SIZE.toFloat()
        trackBaseTimelinePaint.color = DEFAULT_TIMELINE_COLOR
        trackBaseTimelinePaint.strokeWidth = ViewUtilities.dpToPx(3, context).toFloat()
        trackFilledTimelinePaint.color = FILLED_TIMELINE_COLOR
        trackFilledTimelinePaint.strokeWidth = ViewUtilities.dpToPx(3, context).toFloat()
        trackPlaybackTimeTextPaint.textSize = DEFAULT_TRACK_TIMELINE_TEXT_SIZE.toFloat()
        trackPlaybackTimeCollapsedTextPaint.textSize = DEFAULT_TRACK_TIMELINE_TEXT_SIZE.toFloat()
        trackPlaybackTimeTextPaint.color = FILLED_TIMELINE_COLOR
        trackPlaybackTimeCollapsedTextPaint.color = FILLED_TIMELINE_COLOR
        onClickPaint.color = DEFAULT_ON_CLICK_SHAPE_COLOR

        expandAnimator.duration = EXPAND_ANIMATION_DURATION_MS
        expandAnimator.interpolator = AccelerateDecelerateInterpolator()
        expandAnimator.addUpdateListener(ExpandValueAnimatorListener())
        expandAnimator.addListener(ExpandAnimatorListener())

        collapseAnimator.duration = COLLAPSE_ANIMATION_DURATION_MS
        collapseAnimator.addUpdateListener(CollapseValueAnimatorListener())
        collapseAnimator.addListener(CollapseAnimatorListener())

        setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (currentState == STATE_COLLAPSED) {
                        if (event.y >= height - COLLAPSED_BOTTOM_VIEW_HEIGHT) {

                            when {
                                isMotionEventInRect(collapsedNextIconRect, event) -> {
                                    collapsedNextButtonWasPressed = true
                                    playSoundEffect(SoundEffectConstants.CLICK)
                                    onControlClickListener?.let { it.onNextButtonClicked() }
                                    timelineAnimator.cancel()
                                    invalidate()
                                    return@setOnTouchListener true
                                }
                                isMotionEventInRect(collapsedPauseIconRect, event) -> {
                                    collapsedPauseButtonWasPressed = true
                                    playSoundEffect(SoundEffectConstants.CLICK)
//                                    when (playbackState) {
//                                        PLAYBACK_STATE_PAUSED -> {
//                                            onControlClickListener?.let { it.onPlayClicked() }
//                                            playbackState = PLAYBACK_STATE_PLAYING
//                                            if (timelineAnimator.isPaused) {
//                                                timelineAnimator.resume()
//                                            } else timelineAnimator.start()
//                                        }
//                                        PLAYBACK_STATE_PLAYING -> {
//                                            onControlClickListener?.let { it.onPauseClicked() }
//                                            playbackState = PLAYBACK_STATE_PAUSED
//                                            timelineAnimator.pause()
//                                        }
//                                    }
                                    handleNewPlaybackState()
                                    controlButtonPaint.alpha = 100
                                    invalidate()
                                    return@setOnTouchListener true
                                }
                                else -> {
                                    expandAnimator.start()
                                    collapsedPauseIconRect.offsetTo(width, height)
                                    collapsedNextIconRect.offsetTo(width, height)
                                    playSoundEffect(SoundEffectConstants.CLICK)
                                    return@setOnTouchListener true
                                }
                            }
                        }
                    } else if (currentState != STATE_COLLAPSING && currentState != STATE_EXPANDING) {

                        when {
                            isMotionEventInRect(expandedPreviousIconRect, event) -> {
                                expandedPreviousButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                onControlClickListener?.onPreviousButtonClicked()
                                timelineAnimator.cancel()
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(albumCoverRect, event) -> {
                                albumCoverWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedPauseIconRect, event) -> {
                                expandedPauseButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
//                                when (playbackState) {
//                                    PLAYBACK_STATE_PAUSED -> {
//                                        onControlClickListener?.let { it.onPlayClicked() }
//                                        playbackState = PLAYBACK_STATE_PLAYING
//                                        if (timelineAnimator.isPaused) {
//                                            timelineAnimator.resume()
//                                        } else timelineAnimator.start()
//                                    }
//                                    PLAYBACK_STATE_PLAYING -> {
//                                        onControlClickListener?.let { it.onPauseClicked() }
//                                        playbackState = PLAYBACK_STATE_PAUSED
//                                        timelineAnimator.pause()
//                                    }
//                                }
                                handleNewPlaybackState()
                                controlButtonPaint.alpha = 100
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedPlayIconRect, event) -> {
                                expandedPlayButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
//                                when (playbackState) {
//                                    PLAYBACK_STATE_PAUSED -> {
//                                        onControlClickListener?.let { it.onPlayClicked() }
//                                        playbackState = PLAYBACK_STATE_PLAYING
//                                        if (timelineAnimator.isPaused) {
//                                            timelineAnimator.resume()
//                                        } else timelineAnimator.start()
//                                    }
//                                    PLAYBACK_STATE_PLAYING -> {
//                                        onControlClickListener?.let { it.onPauseClicked() }
//                                        playbackState = PLAYBACK_STATE_PAUSED
//                                        timelineAnimator.pause()
//                                    }
//                                }
                                handleNewPlaybackState()
                                controlButtonPaint.alpha = 100
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedNextIconRect, event) -> {
                                expandedNextButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                onControlClickListener?.let { it.onNextButtonClicked() }
                                timelineAnimator.cancel()
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedShuffleIconRect, event) -> {
                                expandedShuffleButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                onControlClickListener?.let { it.onShuffleClicked() }
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedRepeatIconRect, event) -> {
                                expandedRepeatButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                onControlClickListener?.let { it.onRepeatButtonClicked() }
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedDotsIconRect, event) -> {
                                expandedDotsButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                onControlClickListener?.let { it.onMoreButtonClicked() }
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedAddIconRect, event) -> {
                                expandedAddButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                onControlClickListener?.let { it.onAddButtonClicked() }
                                invalidate()
                                return@setOnTouchListener true
                            }

                        }

                        if (event.y <= EXPANDED_ALBUM_COVER_SIZE + EXPANDED_ALBUM_COVER_MARGIN) {
                            collapseAnimator.start()
                            playSoundEffect(SoundEffectConstants.CLICK)
                            return@setOnTouchListener true
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (collapsedNextButtonWasPressed) {
                        collapsedNextButtonWasPressed = false
                        invalidate()
                    }

                    if (albumCoverWasPressed) {
                        albumCoverWasPressed = false
                        invalidate()
                    }

                    if (collapsedPauseButtonWasPressed) {
                        collapsedPauseButtonWasPressed = false
                        controlButtonPaint.alpha = 255
                        invalidate()
                    }
                    if (expandedPreviousButtonWasPressed) {
                        expandedPreviousButtonWasPressed = false
                        invalidate()
                    }
                    if (expandedPlayButtonWasPressed) {
                        expandedPlayButtonWasPressed = false
                        controlButtonPaint.alpha = 255
                        invalidate()
                    }
                    if (expandedPauseButtonWasPressed) {
                        expandedPauseButtonWasPressed = false
                        controlButtonPaint.alpha = 255
                        invalidate()
                    }
                    if (expandedNextButtonWasPressed) {
                        expandedNextButtonWasPressed = false
                        invalidate()
                    }
                    if (expandedRepeatButtonWasPressed) {
                        expandedRepeatButtonWasPressed = false
                        invalidate()
                    }
                    if (expandedShuffleButtonWasPressed) {
                        expandedShuffleButtonWasPressed = false
                        invalidate()
                    }
                    if (expandedDotsButtonWasPressed) {
                        expandedDotsButtonWasPressed = false
                        invalidate()
                    }
                    if (expandedAddButtonWasPressed) {
                        expandedAddButtonWasPressed = false
                        invalidate()
                    }
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val availableWidth = width
        val availableHeight = height

        if (!rectsWereMeasured) {
            measureTrackInfoTextViews(availableWidth, availableHeight)
            initRects(availableWidth, availableHeight)
            initDefaultValues(availableWidth, availableHeight)
            rectsWereMeasured = !rectsWereMeasured
        }

        // Base BottomAudioViewRect
        canvas.drawRect(0f, currentViewHeight, availableWidth.toFloat(), availableHeight.toFloat(), backgroundPaint)


        //   canvas.drawLine(0f, currentViewHeight, availableWidth.toFloat(), currentViewHeight, elevationPaint)

        if (albumCoverWasPressed) {
            canvas.drawBitmap(albumCoverBitmap, null, albumCoverRect, albumRectPaint.apply {
                alpha = 175
            })
        } else {
            canvas.drawBitmap(albumCoverBitmap, null, albumCoverRect, null)
        }
        if (trackTitleMeasuredWidth > availableWidth - ViewUtilities.dpToPx(96, context) - DEFAULT_MARGIN * 8 - trackDurationTextViewMeasuredWidth) {
            val collapsedTrackTitle = TextUtils.ellipsize(currentTrack.title, expandedTrackTitlePaint, availableWidth.toFloat() - ViewUtilities.dpToPx(96, context) - DEFAULT_MARGIN * 8 - trackDurationTextViewMeasuredWidth, TextUtils.TruncateAt.END)
            canvas.drawText(collapsedTrackTitle, 0, collapsedTrackTitle.length, DEFAULT_MARGIN * 3f + COLLAPSED_ALBUM_COVER_SIZE, trackTitleTopY, trackTitlePaint)
        } else {
            canvas.drawText(currentTrack.title, DEFAULT_MARGIN * 3f + COLLAPSED_ALBUM_COVER_SIZE, trackTitleTopY, trackTitlePaint)
        }

        collapsedNextIconDrawable?.let {
            it.bounds = collapsedNextIconRect
            it.draw(canvas)
        }

        if (playbackState == PLAYBACK_STATE_PLAYING) {
            collapsedPauseIconDrawable?.let {
                it.bounds = collapsedPauseIconRect
                it.draw(canvas)
            }
        } else {
            collapsedPlayIconDrawable?.let {
                it.bounds = collapsedPauseIconRect
                it.draw(canvas)
            }
        }

        canvas.drawText(parseSecondsToText(timeElapsedSinceTrackStartedToBePlayed),
                availableWidth - DEFAULT_MARGIN * 4 - ViewUtilities.dpToPx(28, context) * 3 - trackDurationTextViewMeasuredWidth,
                availableHeight.toFloat() - ViewUtilities.dpToPx(24, context),
                trackPlaybackTimeCollapsedTextPaint)

        if (collapsedNextButtonWasPressed) {
            drawOnClickShape(canvas, collapsedNextIconRect)
        }

        if (collapsedPauseButtonWasPressed) {
            drawOnClickShape(canvas, collapsedPauseIconRect)
        }

        if (currentState == STATE_EXPANDED) {

            if (expandedPreviousButtonWasPressed) {
                drawOnClickShape(canvas, expandedPreviousIconRect)
            }

            if (expandedNextButtonWasPressed) {
                drawOnClickShape(canvas, expandedNextIconRect)
            }

            if (expandedRepeatButtonWasPressed) {
                drawOnClickShape(canvas, expandedRepeatIconRect)
            }

            if (expandedShuffleButtonWasPressed) {
                drawOnClickShape(canvas, expandedShuffleIconRect)
            }

            if (expandedDotsButtonWasPressed) {
                drawOnClickShape(canvas, expandedDotsIconRect)
            }

            if (expandedAddButtonWasPressed) {
                drawOnClickShape(canvas, expandedAddIconRect)
            }

            canvas.drawText(parseSecondsToText(timeElapsedSinceTrackStartedToBePlayed),
                    DEFAULT_MARGIN * 2f + ViewUtilities.dpToPx(12, context),
                    availableHeight.toFloat() - ViewUtilities.dpToPx(131, context) - ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(16, context) - ViewUtilities.dpToPx(48, context) + ViewUtilities.dpToPx(32, context),
                    trackPlaybackTimeTextPaint)

            canvas.drawText(parseSecondsToText(currentTrack.duration),
                    availableWidth.toFloat() - DEFAULT_MARGIN * 2 - ViewUtilities.dpToPx(12, context) - trackDurationTextViewMeasuredWidth,
                    availableHeight.toFloat() - ViewUtilities.dpToPx(131, context) - ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(16, context) - ViewUtilities.dpToPx(48, context) + ViewUtilities.dpToPx(32, context),
                    trackPlaybackTimeTextPaint)

            canvas.drawLine(DEFAULT_MARGIN * 2f + ViewUtilities.dpToPx(12, context),
                    availableHeight.toFloat() - ViewUtilities.dpToPx(131, context) - ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(16, context) - ViewUtilities.dpToPx(48, context),
                    availableWidth.toFloat() - DEFAULT_MARGIN * 2 - ViewUtilities.dpToPx(12, context),
                    availableHeight.toFloat() - ViewUtilities.dpToPx(131, context) - ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(16, context) - ViewUtilities.dpToPx(48, context),
                    trackBaseTimelinePaint)

            canvas.drawCircle(currentPlaybackTimelineX,
                    availableHeight.toFloat() - ViewUtilities.dpToPx(131, context) - ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(16, context) - ViewUtilities.dpToPx(48, context),
                    DEFAULT_TRACK_TIMELINE_CONTROL_VIEW_RADIUS.toFloat(), trackFilledTimelinePaint)

            canvas.drawLine(DEFAULT_MARGIN * 2f + ViewUtilities.dpToPx(12, context),
                    availableHeight.toFloat() - ViewUtilities.dpToPx(131, context) - ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(16, context) - ViewUtilities.dpToPx(48, context),
                    currentPlaybackTimelineX,
                    availableHeight.toFloat() - ViewUtilities.dpToPx(131, context) - ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(16, context) - ViewUtilities.dpToPx(48, context),
                    trackFilledTimelinePaint)

            expandedAddDrawable?.let {
                it.bounds = expandedAddIconRect
                it.draw(canvas)
            }
            expandedDotsDrawable?.let {
                it.bounds = expandedDotsIconRect
                it.draw(canvas)
            }
            expandedRepeatDrawable?.let {
                it.bounds = expandedRepeatIconRect
                it.draw(canvas)
            }
            expandedShuffleDrawable?.let {
                it.bounds = expandedShuffleIconRect
                it.draw(canvas)
            }

            expandedPreviousIconDrawable?.let {
                it.bounds = expandedPreviousIconRect
                it.draw(canvas)
            }

            expandedNextIconDrawable?.let {
                it.bounds = expandedNextIconRect
                it.draw(canvas)
            }

            canvas.drawCircle(width / 2f, availableHeight.toFloat() - ViewUtilities.dpToPx(131, context), ViewUtilities.dpToPx(41, context).toFloat(), controlButtonPaint)

            if (playbackState == PLAYBACK_STATE_PLAYING) {
                expandedPauseIconDrawable?.let {
                    it.bounds = expandedPlayIconRect
                    it.draw(canvas)
                }
            } else {
                expandedPlayIconDrawable?.let {
                    it.bounds = expandedPlayIconRect
                    it.draw(canvas)
                }
            }
            if (::ellipsizedTrackBand.isInitialized && ellipsizedTrackBand.isNotEmpty()) {
                canvas.drawText(ellipsizedTrackBand, 0, ellipsizedTrackBand.length, availableWidth / 2 - EXPANDED_ALBUM_COVER_SIZE / 2f + EXPANDED_ALBUM_COVER_MARGIN,
                        EXPANDED_ALBUM_COVER_SIZE.toFloat() + EXPANDED_ALBUM_COVER_MARGIN + DEFAULT_MARGIN * 2 + 100f, trackBandPaint)
            } else {
                canvas.drawText(currentTrack.band, trackBandLeftBorder,
                        EXPANDED_ALBUM_COVER_SIZE.toFloat() + EXPANDED_ALBUM_COVER_MARGIN + DEFAULT_MARGIN * 2 + 100f, trackBandPaint)
            }

            if (::ellipsizedTrackTitle.isInitialized && ellipsizedTrackTitle.isNotEmpty()) {
                canvas.drawText(ellipsizedTrackTitle, 0, ellipsizedTrackTitle.length, availableWidth / 2 - EXPANDED_ALBUM_COVER_SIZE / 2f + EXPANDED_ALBUM_COVER_MARGIN,
                        EXPANDED_ALBUM_COVER_SIZE.toFloat() + EXPANDED_ALBUM_COVER_MARGIN + DEFAULT_MARGIN * 2, expandedTrackTitlePaint)
            } else {
                canvas.drawText(currentTrack.title, trackTitleLeftBorder,
                        EXPANDED_ALBUM_COVER_SIZE.toFloat() + EXPANDED_ALBUM_COVER_MARGIN + DEFAULT_MARGIN * 2, expandedTrackTitlePaint)
            }
        }

    }

    private fun measureTrackInfoTextViews(availableWidth: Int, availableHeight: Int) {

        if (trackDurationTextViewMeasuredWidth == 0f) {
            trackDurationTextViewMeasuredWidth = trackPlaybackTimeTextPaint.measureText(parseSecondsToText(currentTrack.duration))
        }

        if (trackBandMeasuredWidth == 0f) {
            trackBandMeasuredWidth = trackBandPaint.measureText(currentTrack.band)

            val availableForBandLeftBorder = availableWidth / 2 - EXPANDED_ALBUM_COVER_SIZE / 2f
            val availableForBandRightBorder = availableWidth / 2 + EXPANDED_ALBUM_COVER_SIZE / 2f

            val availableForBandWidth = availableForBandRightBorder - availableForBandLeftBorder

            if (trackBandMeasuredWidth > availableForBandWidth) {
                ellipsizedTrackBand = TextUtils.ellipsize(currentTrack.band, expandedTrackTitlePaint, availableForBandWidth, TextUtils.TruncateAt.END)
            } else {
                val delta = availableForBandWidth - trackBandMeasuredWidth
                val margin = delta / 2
                trackBandLeftBorder = EXPANDED_ALBUM_COVER_MARGIN + margin
            }

            if (trackBandMeasuredWidth > availableWidth - EXPANDED_ALBUM_COVER_MARGIN * 2)
                ellipsizedTrackBand = TextUtils.ellipsize(currentTrack.band, trackBandPaint, availableWidth - EXPANDED_ALBUM_COVER_MARGIN * 4f - DEFAULT_MARGIN * 2, TextUtils.TruncateAt.END)
        }

        if (trackTitleMeasuredWidth == 0f) {
            trackTitleMeasuredWidth = expandedTrackTitlePaint.measureText(currentTrack.title)

            val availableForTitleLeftBorder = availableWidth / 2 - EXPANDED_ALBUM_COVER_SIZE / 2f
            val availableForTitleRightBorder = availableWidth / 2 + EXPANDED_ALBUM_COVER_SIZE / 2f

            val availableForTitleWidth = availableForTitleRightBorder - availableForTitleLeftBorder

            if (trackTitleMeasuredWidth > availableForTitleWidth - DEFAULT_MARGIN * 8) {
                ellipsizedTrackTitle = TextUtils.ellipsize(currentTrack.title, expandedTrackTitlePaint, availableForTitleWidth - DEFAULT_MARGIN * 8, TextUtils.TruncateAt.END)
            } else {
                val delta = availableForTitleWidth - trackTitleMeasuredWidth
                val margin = delta / 2
                trackTitleLeftBorder = EXPANDED_ALBUM_COVER_MARGIN + margin
            }
        }

    }

    private fun initDefaultValues(availableWidth: Int, availableHeight: Int) {

        if (currentViewHeight == 0f) {
            currentViewHeight = availableHeight - COLLAPSED_BOTTOM_VIEW_HEIGHT.toFloat()
        }

        if (currentAlbumCoverCenterX == 0f) {
            currentAlbumCoverCenterX = DEFAULT_MARGIN + COLLAPSED_ALBUM_COVER_SIZE / 2f
        }

        if (currentAlbumCoverCenterY == 0f) {
            currentAlbumCoverCenterY = availableHeight - DEFAULT_MARGIN - COLLAPSED_ALBUM_COVER_SIZE / 2f
        }

        if (albumCoverCenterXDx == 0f) {
            albumCoverCenterXDx = (availableWidth / 2f) - (DEFAULT_MARGIN + COLLAPSED_ALBUM_COVER_SIZE / 2)
        }

        if (albumCoverCenterYDy == 0f) {
            albumCoverCenterYDy = (availableHeight - (DEFAULT_MARGIN + COLLAPSED_ALBUM_COVER_SIZE / 2)) - (EXPANDED_ALBUM_COVER_MARGIN + EXPANDED_ALBUM_COVER_SIZE / 2f)
        }

        if (trackTitleTopY == 0f) {
            trackTitleTopY = (availableHeight - COLLAPSED_ALBUM_COVER_SIZE / 2f)
        }

        timelineAnimator = ValueAnimator.ofFloat(0f, abs(DEFAULT_MARGIN * 2f + ViewUtilities.dpToPx(12, context) - availableWidth.toFloat() + DEFAULT_MARGIN * 2 + ViewUtilities.dpToPx(12, context)))
        timelineAnimator.duration = currentTrack.duration * 1000L
        timelineAnimator.interpolator = LinearInterpolator()
        timelineAnimator.addUpdateListener(TimelineValueAnimatorListener())
        timelineAnimator.addListener(TimelineAnimatorListener())
    }

    private fun initRects(availableWidth: Int, availableHeight: Int) {
        albumCoverRect = Rect(DEFAULT_MARGIN,
                availableHeight - COLLAPSED_BOTTOM_VIEW_HEIGHT + DEFAULT_MARGIN,
                DEFAULT_MARGIN + COLLAPSED_ALBUM_COVER_SIZE,
                availableHeight - DEFAULT_MARGIN)

        bottomAudioViewRect = Rect(0,
                availableHeight - COLLAPSED_BOTTOM_VIEW_HEIGHT,
                availableWidth,
                availableHeight)

        collapsedNextIconRect = Rect(availableWidth - DEFAULT_MARGIN * 3 - ViewUtilities.dpToPx(28, context),
                availableHeight - COLLAPSED_BOTTOM_VIEW_HEIGHT + DEFAULT_MARGIN * 2,
                availableWidth - DEFAULT_MARGIN * 3,
                availableHeight - DEFAULT_MARGIN * 2)

        collapsedPauseIconRect = Rect(availableWidth - DEFAULT_MARGIN * 2 - ViewUtilities.dpToPx(28, context) * 3,
                availableHeight - COLLAPSED_BOTTOM_VIEW_HEIGHT + DEFAULT_MARGIN * 2,
                availableWidth - DEFAULT_MARGIN - ViewUtilities.dpToPx(28, context) * 2,
                availableHeight - DEFAULT_MARGIN * 2)

        expandedPlayIconRect = Rect(width / 2 - ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(16, context),
                availableHeight - ViewUtilities.dpToPx(131, context) - ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(16, context),
                width / 2 + ViewUtilities.dpToPx(41, context) - ViewUtilities.dpToPx(16, context),
                availableHeight - ViewUtilities.dpToPx(131, context) + ViewUtilities.dpToPx(41, context) - ViewUtilities.dpToPx(16, context))

        expandedNextIconRect = Rect(width / 2 + ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(16, context),
                availableHeight - ViewUtilities.dpToPx(131, context) - ViewUtilities.dpToPx(41, context),
                width / 2 + ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(56, context) + ViewUtilities.dpToPx(48, context),
                availableHeight - ViewUtilities.dpToPx(131, context) + ViewUtilities.dpToPx(41, context))

        expandedPreviousIconRect = Rect(width / 2 - ViewUtilities.dpToPx(41, context) - ViewUtilities.dpToPx(56, context) - ViewUtilities.dpToPx(48, context),
                availableHeight - ViewUtilities.dpToPx(131, context) - ViewUtilities.dpToPx(41, context),
                width / 2 - ViewUtilities.dpToPx(41, context) - ViewUtilities.dpToPx(16, context),
                availableHeight - ViewUtilities.dpToPx(131, context) + ViewUtilities.dpToPx(41, context))

        expandedDotsIconRect = Rect(availableWidth - DEFAULT_MARGIN * 2 - ViewUtilities.dpToPx(16, context),
                EXPANDED_ALBUM_COVER_SIZE + EXPANDED_ALBUM_COVER_MARGIN + DEFAULT_MARGIN,
                availableWidth - DEFAULT_MARGIN * 2,
                EXPANDED_ALBUM_COVER_SIZE + EXPANDED_ALBUM_COVER_MARGIN + ViewUtilities.dpToPx(32, context) + DEFAULT_MARGIN)

        expandedAddIconRect = Rect(DEFAULT_MARGIN * 2,
                EXPANDED_ALBUM_COVER_SIZE + EXPANDED_ALBUM_COVER_MARGIN + DEFAULT_MARGIN,
                DEFAULT_MARGIN * 2 + ViewUtilities.dpToPx(32, context),
                EXPANDED_ALBUM_COVER_SIZE + EXPANDED_ALBUM_COVER_MARGIN + ViewUtilities.dpToPx(32, context) + DEFAULT_MARGIN)

        expandedRepeatIconRect = Rect(availableWidth - DEFAULT_MARGIN * 2 - ViewUtilities.dpToPx(32, context),
                availableHeight - DEFAULT_MARGIN * 2 - ViewUtilities.dpToPx(32, context),
                availableWidth - DEFAULT_MARGIN * 2,
                availableHeight - DEFAULT_MARGIN * 2)

        expandedShuffleIconRect = Rect(DEFAULT_MARGIN * 2,
                availableHeight - DEFAULT_MARGIN * 2 - ViewUtilities.dpToPx(32, context),
                DEFAULT_MARGIN * 2 + ViewUtilities.dpToPx(32, context),
                availableHeight - DEFAULT_MARGIN * 2)

        expandedPauseIconRect = Rect(width / 2 - ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(16, context),
                availableHeight - ViewUtilities.dpToPx(131, context) - ViewUtilities.dpToPx(41, context) + ViewUtilities.dpToPx(16, context),
                width / 2 + ViewUtilities.dpToPx(41, context) - ViewUtilities.dpToPx(16, context),
                availableHeight - ViewUtilities.dpToPx(131, context) + ViewUtilities.dpToPx(41, context) - ViewUtilities.dpToPx(16, context))
    }

    private fun isMotionEventInRect(targetRect: Rect, motionEvent: MotionEvent) = motionEvent.x >= targetRect.left - 20f
            && motionEvent.x <= targetRect.right + 20f
            && motionEvent.y >= targetRect.top - 20f
            && motionEvent.y <= targetRect.bottom + 20f

    private fun drawOnClickShape(canvas: Canvas, targetRect: Rect) {
        canvas.drawCircle(targetRect.exactCenterX(), targetRect.exactCenterY(), abs(targetRect.right - targetRect.left) / 2f + ViewUtilities.dpToPx(8, context), onClickPaint)
    }

    private fun parseSecondsToText(sec: Int): String {
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private inner class ExpandValueAnimatorListener : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val animatedValue = animation.animatedValue as Float

            currentViewHeight = height - (height + DEFAULT_MARGIN) / 100 * animatedValue

            val offsetX = (albumCoverCenterXDx) / 100 * animatedValue
            val previousAlbumCoverCenterX = currentAlbumCoverCenterX
            currentAlbumCoverCenterX = DEFAULT_MARGIN + COLLAPSED_ALBUM_COVER_SIZE / 2 + offsetX

            val offsetY = (albumCoverCenterYDy) / 100 * animatedValue
            val previousAlbumCoverCenterY = currentAlbumCoverCenterY
            currentAlbumCoverCenterY = height - DEFAULT_MARGIN - COLLAPSED_ALBUM_COVER_SIZE / 2 - offsetY

            albumCoverRect.offset(currentAlbumCoverCenterX.toInt() - previousAlbumCoverCenterX.toInt(),
                    currentAlbumCoverCenterY.toInt() - previousAlbumCoverCenterY.toInt())

            val calculatedAlbumSizeDelta = ((EXPANDED_ALBUM_COVER_SIZE - COLLAPSED_ALBUM_COVER_SIZE) / 2) / 100 * animatedValue
            albumCoverSize = calculatedAlbumSizeDelta.toInt()

            trackTitlePaint.alpha = 0
            trackPlaybackTimeCollapsedTextPaint.alpha = 0

        }
    }

    private inner class CollapseValueAnimatorListener : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val animatedValue = animation.animatedValue

            currentViewHeight = (height - COLLAPSED_BOTTOM_VIEW_HEIGHT) / 100 * animatedValue as Float

            val offsetX = (albumCoverCenterXDx) / 100 * animatedValue
            val previousAlbumCoverCenterX = currentAlbumCoverCenterX
            currentAlbumCoverCenterX = width / 2 - offsetX

            val offsetY = (albumCoverCenterYDy) / 100 * animatedValue
            val previousAlbumCoverCenterY = currentAlbumCoverCenterY
            currentAlbumCoverCenterY = (EXPANDED_ALBUM_COVER_MARGIN + EXPANDED_ALBUM_COVER_SIZE / 2) + offsetY

            albumCoverRect.offset(currentAlbumCoverCenterX.toInt() - previousAlbumCoverCenterX.toInt(),
                    currentAlbumCoverCenterY.toInt() - previousAlbumCoverCenterY.toInt())

            val calculatedAlbumSizeDelta = ((EXPANDED_ALBUM_COVER_SIZE - COLLAPSED_ALBUM_COVER_SIZE) / 2) / 100 * animatedValue
            albumCoverSize = calculatedAlbumSizeDelta.toInt()

            trackTitlePaint.alpha = (255 / 100) * animatedValue.toInt()

            trackPlaybackTimeCollapsedTextPaint.alpha = (255 / 100) * animatedValue.toInt()
        }
    }

    private inner class ExpandAnimatorListener : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {}
        override fun onAnimationCancel(animation: Animator?) {}

        override fun onAnimationEnd(animation: Animator?) {
            currentState = STATE_EXPANDED
        }

        override fun onAnimationStart(animation: Animator?) {
            currentState = STATE_EXPANDING
            isCollapsed = false

            collapsedPlayIconDrawable?.let {
                it.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.white), PorterDuff.Mode.SRC_IN)
            }
        }
    }

    private inner class CollapseAnimatorListener : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {}
        override fun onAnimationCancel(animation: Animator?) {}

        override fun onAnimationEnd(animation: Animator?) {
            currentState = STATE_COLLAPSED
            isCollapsed = true

            collapsedPauseIconRect.offsetTo(width - DEFAULT_MARGIN * 2 - ViewUtilities.dpToPx(28, context) * 3,
                    height - COLLAPSED_BOTTOM_VIEW_HEIGHT + DEFAULT_MARGIN * 2)
            collapsedNextIconRect.offsetTo(width - DEFAULT_MARGIN * 3 - ViewUtilities.dpToPx(28, context),
                    height - COLLAPSED_BOTTOM_VIEW_HEIGHT + DEFAULT_MARGIN * 2)
        }

        override fun onAnimationStart(animation: Animator?) {
            currentState = STATE_COLLAPSING

            collapsedPlayIconDrawable?.let {
                it.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
            }
        }
    }

    private inner class TimelineValueAnimatorListener : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val animatedValue = animation.animatedValue as Float
            val delta = abs(animatedValue - timeLineAnimationLastAnimatedValue)
            timeLineAnimationLastAnimatedValue = animatedValue
            timeElapsedSinceTrackStartedToBePlayed = calculateTimeElapsedBasedOnCurrentX(currentPlaybackTimelineX).toInt()
            currentPlaybackTimelineX += delta
        }
    }

    private fun calculateTimeElapsedBasedOnCurrentX(currentX: Float): Float {
        return (currentX - DEFAULT_MARGIN * 2f - ViewUtilities.dpToPx(12, context)) / (abs(-DEFAULT_MARGIN * 2 - ViewUtilities.dpToPx(12, context) + width - DEFAULT_MARGIN * 2 - ViewUtilities.dpToPx(12, context)) / currentTrack.duration)
    }

    fun handleNewPlaybackState() {
        when (playbackState) {
            PLAYBACK_STATE_PAUSED -> {
                onControlClickListener?.let { it.onPlayClicked() }
                resumeNow()
            }
            PLAYBACK_STATE_PLAYING -> {
                onControlClickListener?.let { it.onPauseClicked() }
                pauseNow()
            }
        }
    }

    fun pauseNow() {
        playbackState = PLAYBACK_STATE_PAUSED
        timelineAnimator.pause()
    }

    fun resumeNow() {
        playbackState = PLAYBACK_STATE_PLAYING
        if (timelineAnimator.isPaused) {
            timelineAnimator.resume()
        } else timelineAnimator.start()
    }

    private inner class TimelineAnimatorListener : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {

        }

        override fun onAnimationEnd(animation: Animator?) {
            onControlClickListener?.onNextButtonClicked()
        }

        override fun onAnimationCancel(animation: Animator?) {

        }

        override fun onAnimationStart(animation: Animator?) {
            playbackState = PLAYBACK_STATE_PLAYING
        }

    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.trackTimelineX = currentPlaybackTimelineX
        savedState.timeElapsedSinceStart = timeElapsedSinceTrackStartedToBePlayed
        savedState.trackDuration = currentTrack.duration
        savedState.trackBand = currentTrack.band!!
        savedState.trackTitle = currentTrack.title!!
        savedState.playbackState = playbackState

        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }


        this.currentPlaybackTimelineX = state.trackTimelineX
        this.timeElapsedSinceTrackStartedToBePlayed = state.timeElapsedSinceStart
        this.currentTrack.band = state.trackBand
        this.currentTrack.title = state.trackTitle
        this.currentTrack.duration = state.trackDuration
        this.playbackState = state.playbackState

        super.onRestoreInstanceState(state.superState)
    }

    internal class SavedState : View.BaseSavedState {

        var timeElapsedSinceStart: Int = 0
        var trackTimelineX: Float = 0f
        var trackTitle: String = ""
        var trackBand: String = ""
        var playbackState: Int = 1 // PLAYBACK_STATE_PAUSED
        var trackDuration: Int = 0

        constructor(superState: Parcelable) : super(superState)

        private constructor(`in`: Parcel) : super(`in`) {
            this.timeElapsedSinceStart = `in`.readInt()
            this.trackTimelineX = `in`.readFloat()
            this.trackTitle = `in`.readString()
            this.trackBand = `in`.readString()
            this.playbackState = `in`.readInt()
            this.trackDuration = `in`.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(this.timeElapsedSinceStart)
            out.writeFloat(this.trackTimelineX)
            out.writeString(this.trackTitle)
            out.writeString(this.trackBand)
            out.writeInt(this.playbackState)
            out.writeInt(this.trackDuration)
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