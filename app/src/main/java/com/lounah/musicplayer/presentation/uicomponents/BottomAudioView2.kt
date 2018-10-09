package com.lounah.musicplayer.presentation.uicomponents

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.content.ContextCompat
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import com.lounah.musicplayer.presentation.model.AudioTrack
import android.util.TypedValue
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.animation.AccelerateDecelerateInterpolator
import com.lounah.musicplayer.R
import com.lounah.musicplayer.util.ViewUtilities.drawOnClickShape
import com.lounah.musicplayer.util.ViewUtilities.isMotionEventInRect
import kotlin.math.abs
import android.view.animation.LinearInterpolator


class BottomAudioView2 constructor(context: Context, attributeSet: AttributeSet?, defStyleRes: Int = 0)
    : View(context, attributeSet, defStyleRes) {

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    interface OnControlButtonClickListener {
        fun onPlayButtonClicked()
        fun onPauseButtonClicked()
        fun onNextButtonClicked()
        fun onPreviousButtonClicked()
        fun onShuffleClicked()
        fun onRepeatClicked()
        fun onShowAdditionalActionsClicked()
    }

    interface OnViewStateChangeListener {
        fun onViewStateChanged(currentState: ViewState)
    }

    interface OnTrackStateChangeListener {
        fun onTrackEnded()
    }

    enum class AudioPlaybackState {
        IDLE, PLAYING, PAUSED
    }

    enum class ViewState {
        COLLAPSED, EXPANDING, EXPANDED, COLLAPSING
    }

    var currentTrack: AudioTrack = AudioTrack()
        set(newValue) {
            field = newValue
            collapsedTrackTitleMeasuredWidth = 0f
            expandedTrackTitleMeasuredWidth = 0f
            trackBandMeasuredWidth = 0f
            currentPlaybackTime = 0
            timeLineAnimationLastAnimatedValue = 0f
            currentTimelineX = pxFromPercentOfWidth(8.9f).toFloat()
            if (::timelineAnimator.isInitialized) {
                timelineAnimator.duration = currentTrack.duration * 1000L
                timelineAnimator.start()
            }
            measureTextViews()
            invalidate()
        }

    var currentViewState: ViewState = ViewState.COLLAPSED
        set(newValue) {
            field = newValue
            viewStateChangeListener?.onViewStateChanged(field)
            invalidate()
        }

    var currentAudioPlaybackState: AudioPlaybackState = AudioPlaybackState.IDLE
        set(newValue) {
            field = newValue
            invalidate()
        }

    /*
        Время, которое прошло с начала прослушивания текущего трека (сек)
     */
    var currentPlaybackTime: Int = 0
        set(newValue) {
            field = newValue
            invalidate()
        }

    var controlButtonClickListener: OnControlButtonClickListener? = null
        set(newValue) {
            field = newValue
            invalidate()
        }

    var viewStateChangeListener: OnViewStateChangeListener? = null
        set(newValue) {
            field = newValue
            invalidate()
        }

    var currentTrackStateChangeListener: OnTrackStateChangeListener? = null
        set(newValue) {
            field = newValue
            invalidate()
        }

    /*
        DEFAULT VALUES
     */
    private val COLLAPSED_VIEW_HEIGHT = 60.dpToPx.toFloat()

    private val DEFAULT_BACKGROUND_COLOR = Color.WHITE
    private val DEFAULT_TEXT_COLOR = ContextCompat.getColor(context, R.color.textColorDefault)
    private val TEXT_COLOR_GREY_LIGHT = ContextCompat.getColor(context, R.color.textColorGrey)

    private val EXPAND_ANIMATION_DURATION_MS = 200L
    private val COLLAPSE_ANIMATION_DURATION_MS = 200L
    private val DEFAULT_TIMELINE_COLOR = ContextCompat.getColor(context, R.color.blueLight)
    private val FILLED_TIMELINE_COLOR = ContextCompat.getColor(context, R.color.blueDark)
    private val DEFAULT_TRACK_TIMELINE_CONTROL_VIEW_RADIUS = 7.dpToPx
    private val DEFAULT_ON_CLICK_SHAPE_COLOR = ContextCompat.getColor(context, R.color.greyLight)
    private val EXPANDED_TRACK_TITLE_TEXT_SIZE = 24f.spToPx
    private val EXPANDED_TRACK_BAND_TEXT_SIZE = 16f.spToPx

    /*
        ALBUM COVER, COLLAPSED STATE
     */
    private var COLLAPSED_ALBUM_COVER_SIZE = 40.dpToPx
    private var COLLAPSED_ALBUM_COVER_MARGIN = 10.dpToPx

    /*
        TRACK TITLE, COLLAPSED STATE
     */
    private var COLLAPSED_TRACK_TITLE_TEXT_SIZE = 16f.spToPx
    private var COLLAPSED_TRACK_TITLE_MARGIN_TOP = 20.dpToPx
    private var COLLAPSED_TRACK_TITLE_MARGIN_BOTTOM = 20.dpToPx

    /*
        NEXT BUTTON, COLLAPSED STATE
     */
    private var COLLAPSED_BUTTON_NEXT_SIZE = 28.dpToPx
    private var COLLAPSED_BUTTON_NEXT_MARGIN_END = 18.dpToPx
    private var COLLAPSED_BUTTON_NEXT_MARGIN_TOP = 16.dpToPx
    private var COLLAPSED_BUTTON_NEXT_MARGIN_BOTTOM = 16.dpToPx
    private var COLLAPSED_BUTTON_NEXT_MARGIN_START = 20.dpToPx

    /*
        PLAY BUTTON, COLLAPSED STATE
     */
    private var COLLAPSED_BUTTON_PLAY_SIZE = 28.dpToPx
    private var COLLAPSED_BUTTON_PLAY_MARGIN_END = 0.dpToPx
    private var COLLAPSED_BUTTON_PLAY_MARGIN_TOP = 16.dpToPx
    private var COLLAPSED_BUTTON_PLAY_MARGIN_BOTTOM = 16.dpToPx
    private var COLLAPSED_BUTTON_PLAY_MARGIN_START = 14.dpToPx

    /*
        PAUSE BUTTON, COLLAPSED STATE
    */
    private var COLLAPSED_BUTTON_PAUSE_SIZE = 28.dpToPx
    private var COLLAPSED_BUTTON_PAUSE_MARGIN_END = 0.dpToPx
    private var COLLAPSED_BUTTON_PAUSE_MARGIN_TOP = 16.dpToPx
    private var COLLAPSED_BUTTON_PAUSE_MARGIN_BOTTOM = 16.dpToPx
    private var COLLAPSED_BUTTON_PAUSE_MARGIN_START = 14.dpToPx

    /*
        TRACK DURATION, COLLAPSED STATE
     */
    private var COLLAPSED_TRACK_DURATION_TEXT_SIZE = 12f.spToPx
    private var COLLAPSED_TRACK_DURATION_TEXT_MARGIN_START = 0.dpToPx
    private var COLLAPSED_TRACK_DURATION_TEXT_MARGIN_END = 0.dpToPx
    private var COLLAPSED_TRACK_DURATION_TEXT_MARGIN_TOP = 25.dpToPx
    private var COLLAPSED_TRACK_DURATION_TEXT_MARGIN_BOTTOM = 21.dpToPx

    /*
        SHUFFLE BUTTON, EXPANDED STATE
     */
    private var EXPANDED_BUTTON_SHUFFLE_SIZE = 24.dpToPx
    private var EXPANDED_BUTTON_SHUFFLE_MARGIN_START = 20.dpToPx
    private var EXPANDED_BUTTON_SHUFFLE_MARGIN_TOP = 20.dpToPx
    private var EXPANDED_BUTTON_SHUFFLE_MARGIN_END = 20.dpToPx
    private var EXPANDED_BUTTON_SHUFFLE_MARGIN_BOTTOM = 20.dpToPx

    /*
        REPEAT BUTTON, EXPANDED STATE
     */
    private var EXPANDED_BUTTON_REPEAT_SIZE = 24.dpToPx
    private var EXPANDED_BUTTON_REPEAT_MARGIN_START = 20.dpToPx
    private var EXPANDED_BUTTON_REPEAT_MARGIN_TOP = 20.dpToPx
    private var EXPANDED_BUTTON_REPEAT_MARGIN_END = 20.dpToPx
    private var EXPANDED_BUTTON_REPEAT_MARGIN_BOTTOM = 20.dpToPx

    /*
        PREVIOUS BUTTON, EXPANDED STATE
     */
    private var EXPANDED_BUTTON_PREVIOUS_SIZE = 48.dpToPx
    private var EXPANDED_BUTTON_PREVIOUS_MARGIN_START = 51.dpToPx
    private var EXPANDED_BUTTON_PREVIOUS_MARGIN_TOP = 0.dpToPx
    private var EXPANDED_BUTTON_PREVIOUS_MARGIN_END = 58.dpToPx
    private var EXPANDED_BUTTON_PREVIOUS_MARGIN_BOTTOM = 84.dpToPx

    /*
        PLAY BUTTON, EXPANDED STATE
    */
    private var EXPANDED_BUTTON_PLAY_SIZE = 48.dpToPx
    private var EXPANDED_BUTTON_PLAY_MARGIN_START = 0.dpToPx
    private var EXPANDED_BUTTON_PLAY_MARGIN_TOP = 31.dpToPx
    private var EXPANDED_BUTTON_PLAY_MARGIN_END = 58.dpToPx
    private var EXPANDED_BUTTON_PLAY_MARGIN_BOTTOM = 84.dpToPx

    /*
        PAUSE BUTTON, EXPANDED STATE
    */
    private var EXPANDED_BUTTON_PAUSE_SIZE = 48.dpToPx
    private var EXPANDED_BUTTON_PAUSE_MARGIN_START = 0.dpToPx
    private var EXPANDED_BUTTON_PAUSE_MARGIN_TOP = 31.dpToPx
    private var EXPANDED_BUTTON_PAUSE_MARGIN_END = 58.dpToPx
    private var EXPANDED_BUTTON_PAUSE_MARGIN_BOTTOM = 84.dpToPx

    /*
        NEXT BUTTON, EXPANDED STATE
    */
    private var EXPANDED_BUTTON_NEXT_SIZE = 48.dpToPx
    private var EXPANDED_BUTTON_NEXT_MARGIN_START = 0.dpToPx
    private var EXPANDED_BUTTON_NEXT_MARGIN_TOP = 0.dpToPx
    private var EXPANDED_BUTTON_NEXT_MARGIN_END = 48.dpToPx
    private var EXPANDED_BUTTON_NEXT_MARGIN_BOTTOM = 84.dpToPx

    /*
        TIMELINE, EXPANDED STATE
     */
    private var EXPANDED_TIMELINE_MARGIN_START = 32.dpToPx
    private var EXPANDED_TIMELINE_MARGIN_TOP = 16.dpToPx
    private var EXPANDED_TIMELINE_MARGIN_END = 32.dpToPx
    private var EXPANDED_TIMELINE_MARGIN_BOTTOM = 48.dpToPx

    /*
        PLAYBACK DURATION, EXPANDED STATE
     */
    private var EXPANDED_PLAYBACK_DURATION_MARGIN_START = 32.dpToPx
    private var EXPANDED_PLAYBACK_DURATION_MARGIN_TOP = 10.dpToPx
    private var EXPANDED_PLAYBACK_DURATION_MARGIN_END = 0.dpToPx
    private var EXPANDED_PLAYBACK_DURATION_MARGIN_BOTTOM = 24.dpToPx

    /*
        TRACK DURATION, EXPANDED STATE
    */
    private var EXPANDED_TRACK_DURATION_MARGIN_START = 32.dpToPx
    private var EXPANDED_TRACK_DURATION_MARGIN_TOP = 10.dpToPx
    private var EXPANDED_TRACK_DURATION_MARGIN_END = 32.dpToPx
    private var EXPANDED_TRACK_DURATION_MARGIN_BOTTOM = 24.dpToPx

    /*
        ADD ICON, EXPANDED STATE
     */
    private var EXPANDED_ADD_ICON_SIZE = 24.dpToPx
    private var EXPANDED_ADD_ICON_MARGIN_START = 18.dpToPx
    private var EXPANDED_ADD_ICON_MARGIN_TOP = 0.dpToPx
    private var EXPANDED_ADD_ICON_MARGIN_END = 16.dpToPx
    private var EXPANDED_ADD_ICON_MARGIN_BOTTOM = 0.dpToPx

    /*
        TRACK TITLE, EXPANDED STATE
     */
    private var EXPANDED_TRACK_TITLE_MARGIN_START = 56.dpToPx
    private var EXPANDED_TRACK_TITLE_MARGIN_TOP = 32.dpToPx
    private var EXPANDED_TRACK_TITLE_MARGIN_END = 56.dpToPx
    private var EXPANDED_TRACK_TITLE_MARGIN_BOTTOM = 0.dpToPx

    /*
        TRACK_BAND, EXPANDED STATE
     */
    private var EXPANDED_TRACK_BAND_MARGIN_START = 56.dpToPx
    private var EXPANDED_TRACK_BAND_MARGIN_TOP = 4.dpToPx
    private var EXPANDED_TRACK_BAND_MARGIN_END = 56.dpToPx
    private var EXPANDED_TRACK_BAND_MARGIN_BOTTOM = 36.dpToPx

    /*
        DOTS ICON, EXPANDED STATE
     */
    private var EXPANDED_DOTS_ICON_WIDTH = 12.dpToPx
    private var EXPANDED_DOTS_ICON_HEIGHT = 24.dpToPx
    private var EXPANDED_DOTS_ICON_MARGIN_START = 21.dpToPx
    private var EXPANDED_DOTS_ICON_MARGIN_TOP = 0.dpToPx
    private var EXPANDED_DOTS_ICON_MARGIN_END = 24.dpToPx
    private var EXPANDED_DOTS_ICON_MARGIN_BOTTOM = 0.dpToPx

    /*
        ALBUM COVER, EXPANDED STATE
     */
    private var EXPANDED_ALBUM_COVER_SIZE: Int = 0
    private var EXPANDED_ALBUM_COVER_WIDTH = 0F
    private var EXPANDED_ALBUM_COVER_HEIGHT = 0F
    private var EXPANDED_ALBUM_COVER_MARGIN_START: Int = 0
    private var EXPANDED_ALBUM_COVER_MARGIN_TOP: Int = 0
    private var EXPANDED_ALBUM_COVER_MARGIN_END: Int = 0
    private var EXPANDED_ALBUM_COVER_MARGIN_BOTTOM: Int = 0


    private val collapsedPauseIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pause_28)
    private val collapsedNextIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_mini_player_next_28)

    private val collapsedPlayIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_play_48)?.apply {
        colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
    }

    private val expandedPauseIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pause_48)
    private val expandedPlayIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_play_48)

    private val expandedNextIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_skip_next_48)?.apply {
        colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
    }

    private val expandedPreviousIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_skip_previous_48)?.apply {
        colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
    }

    private val expandedShuffleDrawable = ContextCompat.getDrawable(context, R.drawable.ic_shuffle_24)?.apply {
        colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
    }

    private val expandedRepeatDrawable = ContextCompat.getDrawable(context, R.drawable.ic_repeat_24)?.apply {
        colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
    }

    private val expandedDotsDrawable = ContextCompat.getDrawable(context, R.drawable.ic_ic_more_24dp)?.apply {
        colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
    }

    private val expandedAddDrawable = ContextCompat.getDrawable(context, R.drawable.ic_add_outline_24)?.apply {
        colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.blue), PorterDuff.Mode.SRC_IN)
    }

    private val expandedDropdownDrawable = ContextCompat.getDrawable(context, R.drawable.ic_dropdown_24)?.apply {
        colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.greyOptions), PorterDuff.Mode.SRC_IN)
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_BACKGROUND_COLOR
    }
    private val collapsedTrackTitleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_TEXT_COLOR
        textSize = COLLAPSED_TRACK_TITLE_TEXT_SIZE.toFloat()
    }
    private val expandedTrackTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_TEXT_COLOR
        textSize = EXPANDED_TRACK_TITLE_TEXT_SIZE.toFloat()
    }
    private val trackBandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.blue)
        textSize = EXPANDED_TRACK_BAND_TEXT_SIZE.toFloat()
    }
    private val controlButtonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.blue)
    }

    private val trackBaseTimelinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_TIMELINE_COLOR
        strokeWidth = 3.dpToPx.toFloat()
    }
    private val trackFilledTimelinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = FILLED_TIMELINE_COLOR
        strokeWidth = 3.dpToPx.toFloat()
    }
    private val trackPlaybackTimeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = COLLAPSED_TRACK_DURATION_TEXT_SIZE.toFloat()
        color = FILLED_TIMELINE_COLOR
    }
    private val trackPlaybackTimeCollapsedTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = COLLAPSED_TRACK_DURATION_TEXT_SIZE.toFloat()
        color = FILLED_TIMELINE_COLOR
    }
    private val onClickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DEFAULT_ON_CLICK_SHAPE_COLOR
    }

    private val albumRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var albumCoverBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.albumcoverxx)

    private lateinit var collapsedNextIconRect: Rect
    private lateinit var collapsedPauseIconRect: Rect
    private lateinit var collapsedPlayIconRect: Rect
    private lateinit var expandedShuffleButtonRect: Rect
    private lateinit var expandedRepeatButtonRect: Rect
    private lateinit var expandedPreviousButtonRect: Rect
    private lateinit var expandedPlayButtonRect: Rect
    private lateinit var expandedPauseButtonRect: Rect
    private lateinit var expandedNextButtonRect: Rect
    private lateinit var expandedPlaybackTimeRect: Rect
    private lateinit var expandedTrackDurationRect: Rect
    private lateinit var expandedAddIconRect: Rect
    private lateinit var expandedDotsIconRect: Rect
    private lateinit var albumCoverRect: RectF

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

    private var rectsWereMeasured = false

    private var playbackTimeMeasuredWidth = 0f
    private var playbackTimeMeasuredHeight = 0f

    private var expandedTrackTitleMeasuredWidth = 0f
    private var collapsedTrackTitleMeasuredWidth = 0f
    private var trackBandMeasuredWidth = 0f
    private var expandedTrackTitleLeftBorder: Float = 0f
    private var expandedTrackBandLeftBorder: Float = 0f

    private lateinit var ellipsizedTrackTitle: CharSequence
    private lateinit var ellipsizedTrackBand: CharSequence

    private val expandAnimator = ValueAnimator.ofFloat(0f, 100f)
    private val collapseAnimator = ValueAnimator.ofFloat(0f, 100f)

    private lateinit var timelineAnimator: ValueAnimator
    private var timeLineAnimationLastAnimatedValue = 0f
    private var currentTimelineX: Float = 0f

    private var albumCoverCenterXDx = 0f
    private var albumCoverCenterYDy = 0f

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

    private var currentViewHeight = 0f
        set(newValue) {
            field = newValue
            invalidate()
        }

    private var albumCoverXDelta: Float = COLLAPSED_ALBUM_COVER_SIZE.toFloat()
        set(newValue) {
            if (currentViewState == ViewState.EXPANDING) {
                albumCoverRect.right = currentAlbumCoverCenterX + COLLAPSED_ALBUM_COVER_SIZE / 2 + newValue + COLLAPSED_ALBUM_COVER_MARGIN
                albumCoverRect.left = currentAlbumCoverCenterX - COLLAPSED_ALBUM_COVER_SIZE / 2 - newValue - COLLAPSED_ALBUM_COVER_MARGIN
                field = newValue
            } else {
                if (currentViewState == ViewState.COLLAPSING) {
                    albumCoverRect.right = currentAlbumCoverCenterX + EXPANDED_ALBUM_COVER_WIDTH / 2 - newValue
                    albumCoverRect.left = currentAlbumCoverCenterX - EXPANDED_ALBUM_COVER_WIDTH / 2 + newValue
                    field = newValue
                }
            }
            invalidate()
        }

    private var albumCoverYDelta: Float = COLLAPSED_ALBUM_COVER_SIZE.toFloat()
        set(newValue) {
            if (currentViewState == ViewState.EXPANDING) {
                albumCoverRect.top = currentAlbumCoverCenterY - COLLAPSED_ALBUM_COVER_SIZE / 2 - newValue - COLLAPSED_ALBUM_COVER_MARGIN
                albumCoverRect.bottom = currentAlbumCoverCenterY + COLLAPSED_ALBUM_COVER_SIZE / 2 + newValue + COLLAPSED_ALBUM_COVER_MARGIN
                field = newValue
            } else {
                if (currentViewState == ViewState.COLLAPSING) {
                    albumCoverRect.top = currentAlbumCoverCenterY - EXPANDED_ALBUM_COVER_HEIGHT / 2 + newValue
                    albumCoverRect.bottom = currentAlbumCoverCenterY + EXPANDED_ALBUM_COVER_HEIGHT / 2 - newValue
                    field = newValue
                }
            }
            invalidate()
        }

    init {

        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        expandAnimator.duration = EXPAND_ANIMATION_DURATION_MS
        expandAnimator.interpolator = AccelerateDecelerateInterpolator()
        expandAnimator.addUpdateListener(ExpandValueAnimatorListener())
        expandAnimator.addListener(ExpandAnimatorListener())

        collapseAnimator.duration = COLLAPSE_ANIMATION_DURATION_MS
        collapseAnimator.addUpdateListener(CollapseValueAnimatorListener())
        collapseAnimator.addListener(CollapseAnimatorListener())

        setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (currentViewState == ViewState.COLLAPSED) {
                        if (event.y >= height - COLLAPSED_VIEW_HEIGHT) {

                            when {
                                isMotionEventInRect(collapsedNextIconRect, event) -> {
                                    collapsedNextButtonWasPressed = true
                                    playSoundEffect(SoundEffectConstants.CLICK)
                                    controlButtonClickListener?.onNextButtonClicked()
                                    timelineAnimator.cancel()
                                    invalidate()
                                    return@setOnTouchListener true
                                }
                                isMotionEventInRect(collapsedPauseIconRect, event) -> {
                                    collapsedPauseButtonWasPressed = true
                                    playSoundEffect(SoundEffectConstants.CLICK)
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
                    } else if (currentViewState != ViewState.COLLAPSING && currentViewState != ViewState.EXPANDING) {

                        when {
                            isMotionEventInRect(expandedPreviousButtonRect, event) -> {
                                expandedPreviousButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                controlButtonClickListener?.onPreviousButtonClicked()
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
                            isMotionEventInRect(expandedPauseButtonRect, event) -> {
                                expandedPauseButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                handleNewPlaybackState()
                                controlButtonPaint.alpha = 100
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedPlayButtonRect, event) -> {
                                expandedPlayButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                handleNewPlaybackState()
                                controlButtonPaint.alpha = 100
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedNextButtonRect, event) -> {
                                expandedNextButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                controlButtonClickListener?.onNextButtonClicked()
                                timelineAnimator.cancel()
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedShuffleButtonRect, event) -> {
                                expandedShuffleButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                controlButtonClickListener?.onShuffleClicked()
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedRepeatButtonRect, event) -> {
                                expandedRepeatButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                controlButtonClickListener?.onRepeatClicked()
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedDotsIconRect, event) -> {
                                expandedDotsButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                controlButtonClickListener?.onShowAdditionalActionsClicked()
                                invalidate()
                                return@setOnTouchListener true
                            }
                            isMotionEventInRect(expandedAddIconRect, event) -> {
                                expandedAddButtonWasPressed = true
                                playSoundEffect(SoundEffectConstants.CLICK)
                                controlButtonClickListener?.onShowAdditionalActionsClicked()
                                invalidate()
                                return@setOnTouchListener true
                            }

                        }

                        if (event.y <= EXPANDED_ALBUM_COVER_SIZE + EXPANDED_ALBUM_COVER_MARGIN_BOTTOM) {
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

        if (!rectsWereMeasured) {
            initDefaultValues()
            measureTextViews()
            initializeRects()
            rectsWereMeasured = true
        }

        // BASE VIEW BACKGROUND
        // это говно временное
        if (currentViewState == ViewState.EXPANDED) {
            canvas.drawLine(0f, 20f, width.toFloat(), 20f, Paint().apply {
                color = Color.BLACK
                strokeWidth = 16.dpToPx.toFloat()
            })
            canvas.drawTopRoundRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint, 35f)
            expandedDropdownDrawable?.let {
                it.bounds = Rect(width / 2 - 12.dpToPx, pxFromPercentOfHeight(3.2f), width / 2 + 12.dpToPx, pxFromPercentOfHeight(3.2f) + 24.dpToPx)
                it.draw(canvas)
            }
        } else {
            canvas.drawRect(0f, currentViewHeight, width.toFloat(), height.toFloat(), backgroundPaint)
        }
        // ALBUM COVER
        if (albumCoverWasPressed) {
            canvas.drawBitmap(albumCoverBitmap, null, albumCoverRect, albumRectPaint.apply {
                alpha = 175
            })
        } else {
            canvas.drawBitmap(albumCoverBitmap, null, albumCoverRect, null)
        }

        // TRACK TITLE
        if (collapsedTrackTitleMeasuredWidth > width - pxFromPercentOfWidth(47.2f) - playbackTimeMeasuredWidth) {
            val collapsedTrackTitle = TextUtils.ellipsize(currentTrack.title, expandedTrackTitlePaint, width - pxFromPercentOfWidth(47.2f) - playbackTimeMeasuredWidth, TextUtils.TruncateAt.END)
            canvas.drawText(collapsedTrackTitle, 0, collapsedTrackTitle.length, pxFromPercentOfWidth(17.2f).toFloat(), height - COLLAPSED_TRACK_TITLE_MARGIN_BOTTOM.toFloat(), collapsedTrackTitleTextPaint)
        } else {
            canvas.drawText(currentTrack.title, pxFromPercentOfWidth(17.2f).toFloat(), height - COLLAPSED_TRACK_TITLE_MARGIN_BOTTOM.toFloat(), collapsedTrackTitleTextPaint)
        }

        // TRACK DURATION
        canvas.drawText(currentPlaybackTime.toTimeText,
                width - pxFromPercentOfWidth(22.1f) - playbackTimeMeasuredWidth -COLLAPSED_BUTTON_PLAY_SIZE,
                height - COLLAPSED_TRACK_TITLE_MARGIN_BOTTOM.toFloat(),
                trackPlaybackTimeCollapsedTextPaint)

        when(currentAudioPlaybackState) {
            AudioPlaybackState.IDLE -> {
                collapsedPlayIconDrawable?.let {
                    it.bounds = collapsedPlayIconRect
                    it.draw(canvas)
                }
            }
            AudioPlaybackState.PLAYING -> {
                collapsedPauseIconDrawable?.let {
                    it.bounds = collapsedPauseIconRect
                    it.draw(canvas)
                }
            }
            AudioPlaybackState.PAUSED -> {
                collapsedPlayIconDrawable?.let {
                    it.bounds = collapsedPlayIconRect
                    it.draw(canvas)
                }
            }
        }

        collapsedNextIconDrawable?.let {
            it.bounds = collapsedNextIconRect
            it.draw(canvas)
        }

        if (collapsedNextButtonWasPressed) {
            drawOnClickShape(canvas, collapsedNextIconRect, context, onClickPaint)
        }

        if (collapsedPauseButtonWasPressed) {
            drawOnClickShape(canvas, collapsedPauseIconRect, context, onClickPaint)
        }

        if (currentViewState == ViewState.EXPANDED) {

            if (expandedPreviousButtonWasPressed) {
                drawOnClickShape(canvas, expandedPreviousButtonRect, context, onClickPaint)
            }

            if (expandedNextButtonWasPressed) {
                drawOnClickShape(canvas, expandedNextButtonRect, context, onClickPaint)
            }

            if (expandedRepeatButtonWasPressed) {
                drawOnClickShape(canvas, expandedRepeatButtonRect, context, onClickPaint)
            }

            if (expandedShuffleButtonWasPressed) {
                drawOnClickShape(canvas, expandedShuffleButtonRect, context, onClickPaint)
            }

            if (expandedDotsButtonWasPressed) {
                drawOnClickShape(canvas, expandedDotsIconRect, context, onClickPaint)
            }

            if (expandedAddButtonWasPressed) {
                drawOnClickShape(canvas, expandedAddIconRect, context, onClickPaint)
            }

            expandedShuffleDrawable?.let {
                it.bounds = expandedShuffleButtonRect
                it.draw(canvas)
            }
            expandedRepeatDrawable?.let {
                it.bounds = expandedRepeatButtonRect
                it.draw(canvas)
            }
            expandedPreviousIconDrawable?.let {
                it.bounds = expandedPreviousButtonRect
                it.draw(canvas)
            }
            expandedNextIconDrawable?.let {
                it.bounds = expandedNextButtonRect
                it.draw(canvas)
            }
            expandedAddDrawable?.let {
                it.bounds = expandedAddIconRect
                it.draw(canvas)
            }
            expandedDotsDrawable?.let {
                it.bounds = expandedDotsIconRect
                it.draw(canvas)
            }
            canvas.drawCircle(width / 2f, height - EXPANDED_BUTTON_PLAY_MARGIN_BOTTOM - EXPANDED_BUTTON_PLAY_SIZE / 2F, 36.dpToPx.toFloat(), controlButtonPaint)

            when(currentAudioPlaybackState) {
                AudioPlaybackState.IDLE -> {
                    expandedPlayIconDrawable?.let {
                        it.bounds = expandedPlayButtonRect
                        it.draw(canvas)
                    }
                }
                AudioPlaybackState.PLAYING -> {
                    expandedPauseIconDrawable?.let {
                        it.bounds = expandedPauseButtonRect
                        it.draw(canvas)
                    }
                }
                AudioPlaybackState.PAUSED -> {
                    expandedPlayIconDrawable?.let {
                        it.bounds = expandedPlayButtonRect
                        it.draw(canvas)
                    }
                }
            }

            // Прошедшее с начала прослушивания время
            canvas.drawText(currentPlaybackTime.toTimeText,
                    EXPANDED_PLAYBACK_DURATION_MARGIN_START.toFloat(),
                    height - pxFromPercentOfHeight(26.2f).toFloat(),
                    trackPlaybackTimeTextPaint)

            // Общее время трека
            canvas.drawText(currentTrack.duration.toTimeText,
                    width - playbackTimeMeasuredWidth - EXPANDED_TRACK_DURATION_MARGIN_END,
                    height - pxFromPercentOfHeight(26.2f).toFloat(),
                    trackPlaybackTimeTextPaint)

            // Полоска со временем
            canvas.drawLine(EXPANDED_TIMELINE_MARGIN_START.toFloat(),
                    height - pxFromPercentOfHeight(31.2f).toFloat(),
                    width - EXPANDED_TIMELINE_MARGIN_END.toFloat(),
                    height - pxFromPercentOfHeight(31.2f).toFloat(),
                    trackBaseTimelinePaint)

            // Индикатор на закрашенной полосе со временем, прошедшим с начала трека
            canvas.drawCircle(currentTimelineX,
                    height - pxFromPercentOfHeight(31.2f).toFloat(),
                    DEFAULT_TRACK_TIMELINE_CONTROL_VIEW_RADIUS.toFloat(),
                    trackFilledTimelinePaint)

            // Закрашенная полоса со временем, прошедшим с начала трека
            canvas.drawLine(currentTimelineX,
                    height - pxFromPercentOfHeight(31.2f).toFloat(),
                    EXPANDED_TIMELINE_MARGIN_START.toFloat(),
                    height - pxFromPercentOfHeight(31.2f).toFloat(),
                    trackFilledTimelinePaint)

            if (::ellipsizedTrackBand.isInitialized && ellipsizedTrackBand.isNotEmpty()) {
                canvas.drawText(ellipsizedTrackBand, 0, ellipsizedTrackBand.length,
                        EXPANDED_TRACK_TITLE_MARGIN_START.toFloat(),
                        height - pxFromPercentOfHeight(37.5f).toFloat(), trackBandPaint)
            } else {
                canvas.drawText(currentTrack.band, expandedTrackBandLeftBorder,
                        height - pxFromPercentOfHeight(37.5f).toFloat(), trackBandPaint)
            }

            if (::ellipsizedTrackTitle.isInitialized && ellipsizedTrackTitle.isNotEmpty()) {
                canvas.drawText(ellipsizedTrackTitle, 0, ellipsizedTrackTitle.length,
                        EXPANDED_TRACK_TITLE_MARGIN_START.toFloat(),
                        height - pxFromPercentOfHeight(41.4f).toFloat(), expandedTrackTitlePaint)
            } else {
                canvas.drawText(currentTrack.title, expandedTrackTitleLeftBorder,
                        height - pxFromPercentOfHeight(41.4f).toFloat(), expandedTrackTitlePaint)
            }
        }
    }

    private fun initDefaultValues() {

        COLLAPSED_ALBUM_COVER_SIZE = pxFromPercentOfWidth(11.1f)
        COLLAPSED_ALBUM_COVER_MARGIN = pxFromPercentOfWidth(2.8f)

        /*
            TRACK TITLE, COLLAPSED STATE
         */
        COLLAPSED_TRACK_TITLE_TEXT_SIZE = 16f.spToPx
        COLLAPSED_TRACK_TITLE_MARGIN_TOP = pxFromPercentOfHeight(3.3f)
        COLLAPSED_TRACK_TITLE_MARGIN_BOTTOM = pxFromPercentOfHeight(3.3f)

        /*
            NEXT BUTTON, COLLAPSED STATE
         */
        COLLAPSED_BUTTON_NEXT_SIZE = 28.dpToPx
        COLLAPSED_BUTTON_NEXT_MARGIN_END = pxFromPercentOfWidth(5f)
        COLLAPSED_BUTTON_NEXT_MARGIN_TOP = pxFromPercentOfHeight(2.5f)
        COLLAPSED_BUTTON_NEXT_MARGIN_BOTTOM = pxFromPercentOfHeight(2.5f)
        COLLAPSED_BUTTON_NEXT_MARGIN_START = pxFromPercentOfWidth(5.6f)

        /*
            PLAY BUTTON, COLLAPSED STATE
         */
        COLLAPSED_BUTTON_PLAY_SIZE = 24.dpToPx
        COLLAPSED_BUTTON_PLAY_MARGIN_END = 0.dpToPx
        COLLAPSED_BUTTON_PLAY_MARGIN_TOP = pxFromPercentOfHeight(2.3f)
        COLLAPSED_BUTTON_PLAY_MARGIN_BOTTOM = pxFromPercentOfHeight(2.3f)
        COLLAPSED_BUTTON_PLAY_MARGIN_START = pxFromPercentOfWidth(3.9f)

        /*
            PAUSE BUTTON, COLLAPSED STATE
        */
        COLLAPSED_BUTTON_PAUSE_SIZE = 28.dpToPx
        COLLAPSED_BUTTON_PAUSE_MARGIN_END = 0.dpToPx
        COLLAPSED_BUTTON_PAUSE_MARGIN_TOP = pxFromPercentOfHeight(2.5f)
        COLLAPSED_BUTTON_PAUSE_MARGIN_BOTTOM = pxFromPercentOfHeight(2.5f)
        COLLAPSED_BUTTON_PAUSE_MARGIN_START = pxFromPercentOfWidth(3.9f)

        /*
            TRACK DURATION, COLLAPSED STATE
         */
        COLLAPSED_TRACK_DURATION_TEXT_SIZE = 12f.spToPx
        COLLAPSED_TRACK_DURATION_TEXT_MARGIN_START = 0.dpToPx
        COLLAPSED_TRACK_DURATION_TEXT_MARGIN_END = 0.dpToPx
        COLLAPSED_TRACK_DURATION_TEXT_MARGIN_TOP = 25.dpToPx
        COLLAPSED_TRACK_DURATION_TEXT_MARGIN_BOTTOM = 21.dpToPx

        /*
            SHUFFLE BUTTON, EXPANDED STATE
         */
        EXPANDED_BUTTON_SHUFFLE_SIZE = 24.dpToPx
        EXPANDED_BUTTON_SHUFFLE_MARGIN_START = pxFromPercentOfWidth(5.6f)
        EXPANDED_BUTTON_SHUFFLE_MARGIN_TOP = pxFromPercentOfHeight(3.1f)
        EXPANDED_BUTTON_SHUFFLE_MARGIN_END = pxFromPercentOfWidth(5.6f)
        EXPANDED_BUTTON_SHUFFLE_MARGIN_BOTTOM = pxFromPercentOfHeight(3.1f)

        /*
            REPEAT BUTTON, EXPANDED STATE
         */
        EXPANDED_BUTTON_REPEAT_SIZE = 24.dpToPx
        EXPANDED_BUTTON_REPEAT_MARGIN_START = pxFromPercentOfWidth(5.6f)
        EXPANDED_BUTTON_REPEAT_MARGIN_TOP = pxFromPercentOfHeight(3.1f)
        EXPANDED_BUTTON_REPEAT_MARGIN_END = pxFromPercentOfWidth(5.6f)
        EXPANDED_BUTTON_REPEAT_MARGIN_BOTTOM = pxFromPercentOfHeight(3.1f)

        /*
            PREVIOUS BUTTON, EXPANDED STATE
         */
        EXPANDED_BUTTON_PREVIOUS_SIZE = 48.dpToPx
        EXPANDED_BUTTON_PREVIOUS_MARGIN_START = pxFromPercentOfWidth(14.2f)
        EXPANDED_BUTTON_PREVIOUS_MARGIN_TOP = 0.dpToPx
        EXPANDED_BUTTON_PREVIOUS_MARGIN_END = pxFromPercentOfWidth(12.8f)
        EXPANDED_BUTTON_PREVIOUS_MARGIN_BOTTOM = pxFromPercentOfHeight(15.9f)

        /*
            PLAY BUTTON, EXPANDED STATE
        */
        EXPANDED_BUTTON_PLAY_SIZE = 48.dpToPx
        EXPANDED_BUTTON_PLAY_MARGIN_START = 0.dpToPx
        EXPANDED_BUTTON_PLAY_MARGIN_TOP = pxFromPercentOfHeight(4.8f)
        EXPANDED_BUTTON_PLAY_MARGIN_END = pxFromPercentOfWidth(16.1f)
        EXPANDED_BUTTON_PLAY_MARGIN_BOTTOM = pxFromPercentOfHeight(15.9f)

        /*
            PAUSE BUTTON, EXPANDED STATE
        */
        EXPANDED_BUTTON_PAUSE_SIZE = 48.dpToPx
        EXPANDED_BUTTON_PAUSE_MARGIN_START = 0.dpToPx
        EXPANDED_BUTTON_PAUSE_MARGIN_TOP = pxFromPercentOfHeight(4.8f)
        EXPANDED_BUTTON_PAUSE_MARGIN_END = pxFromPercentOfWidth(16.1f)
        EXPANDED_BUTTON_PAUSE_MARGIN_BOTTOM = pxFromPercentOfHeight(14.9f)

        /*
            NEXT BUTTON, EXPANDED STATE
        */
        EXPANDED_BUTTON_NEXT_SIZE = 48.dpToPx
        EXPANDED_BUTTON_NEXT_MARGIN_START = 0.dpToPx
        EXPANDED_BUTTON_NEXT_MARGIN_TOP = 0.dpToPx
        EXPANDED_BUTTON_NEXT_MARGIN_END = pxFromPercentOfWidth(13.6f)
        EXPANDED_BUTTON_NEXT_MARGIN_BOTTOM = pxFromPercentOfHeight(13.5f)

        /*
            TIMELINE, EXPANDED STATE
         */
        EXPANDED_TIMELINE_MARGIN_START = pxFromPercentOfWidth(8.9f)
        EXPANDED_TIMELINE_MARGIN_TOP = pxFromPercentOfHeight(2.5f)
        EXPANDED_TIMELINE_MARGIN_END = pxFromPercentOfWidth(8.9f)
        EXPANDED_TIMELINE_MARGIN_BOTTOM = pxFromPercentOfHeight(7.5f)

        /*
            PLAYBACK DURATION, EXPANDED STATE
         */
        EXPANDED_PLAYBACK_DURATION_MARGIN_START = pxFromPercentOfWidth(8.9f)
        EXPANDED_PLAYBACK_DURATION_MARGIN_TOP = pxFromPercentOfHeight(1.6f)
        EXPANDED_PLAYBACK_DURATION_MARGIN_END = 0.dpToPx
        EXPANDED_PLAYBACK_DURATION_MARGIN_BOTTOM = pxFromPercentOfHeight(3.8f)

        /*
            TRACK DURATION, EXPANDED STATE
        */
        EXPANDED_TRACK_DURATION_MARGIN_START = pxFromPercentOfWidth(8.9f)
        EXPANDED_TRACK_DURATION_MARGIN_TOP = pxFromPercentOfHeight(1.6f)
        EXPANDED_TRACK_DURATION_MARGIN_END = pxFromPercentOfWidth(8.9f)
        EXPANDED_TRACK_DURATION_MARGIN_BOTTOM = pxFromPercentOfHeight(3.8f)

        /*
            ADD ICON, EXPANDED STATE
         */
        EXPANDED_ADD_ICON_SIZE = 24.dpToPx
        EXPANDED_ADD_ICON_MARGIN_START = pxFromPercentOfWidth(6.4f)
        EXPANDED_ADD_ICON_MARGIN_TOP = 0.dpToPx
        EXPANDED_ADD_ICON_MARGIN_END = pxFromPercentOfWidth(4.2f)
        EXPANDED_ADD_ICON_MARGIN_BOTTOM = 0.dpToPx

        /*
            TRACK TITLE, EXPANDED STATE
         */
        EXPANDED_TRACK_TITLE_MARGIN_START = pxFromPercentOfWidth(13.5f)
        EXPANDED_TRACK_TITLE_MARGIN_TOP = pxFromPercentOfHeight(5.3f)
        EXPANDED_TRACK_TITLE_MARGIN_END = pxFromPercentOfWidth(13.5f)
        EXPANDED_TRACK_TITLE_MARGIN_BOTTOM = 0.dpToPx

        /*
            TRACK_BAND, EXPANDED STATE
         */
        EXPANDED_TRACK_BAND_MARGIN_START = pxFromPercentOfWidth(13.5f)
        EXPANDED_TRACK_BAND_MARGIN_TOP = pxFromPercentOfHeight(0.6f)
        EXPANDED_TRACK_BAND_MARGIN_END = pxFromPercentOfWidth(13.5f)
        EXPANDED_TRACK_BAND_MARGIN_BOTTOM = pxFromPercentOfHeight(5.6f)

        /*
            DOTS ICON, EXPANDED STATE
         */
        EXPANDED_DOTS_ICON_WIDTH = pxFromPercentOfWidth(3.3f)
        EXPANDED_DOTS_ICON_HEIGHT = pxFromPercentOfHeight(3.8f)
        EXPANDED_DOTS_ICON_MARGIN_START = pxFromPercentOfWidth(5f)
        EXPANDED_DOTS_ICON_MARGIN_TOP = 0.dpToPx
        EXPANDED_DOTS_ICON_MARGIN_END = pxFromPercentOfWidth(7.2f)
        EXPANDED_DOTS_ICON_MARGIN_BOTTOM = 0.dpToPx

        /*
            ALBUM COVER, EXPANDED STATE
         */
        EXPANDED_ALBUM_COVER_SIZE = pxFromPercentOfWidth(69.2f)
        EXPANDED_ALBUM_COVER_WIDTH = pxFromPercentOfWidth(69.2f).toFloat()
        EXPANDED_ALBUM_COVER_HEIGHT = pxFromPercentOfHeight(40.4f).toFloat()
        EXPANDED_ALBUM_COVER_MARGIN_START = pxFromPercentOfWidth(15.4f)
        EXPANDED_ALBUM_COVER_MARGIN_TOP = 48.dpToPx
        EXPANDED_ALBUM_COVER_MARGIN_END = pxFromPercentOfWidth(15.4f)
        EXPANDED_ALBUM_COVER_MARGIN_BOTTOM = pxFromPercentOfHeight(5.6f)

        if (currentAlbumCoverCenterX == 0f) {
            currentAlbumCoverCenterX = COLLAPSED_ALBUM_COVER_MARGIN + COLLAPSED_ALBUM_COVER_SIZE / 2f
        }

        if (currentAlbumCoverCenterY == 0f) {
            currentAlbumCoverCenterY = height - COLLAPSED_BUTTON_PLAY_SIZE / 2F - COLLAPSED_ALBUM_COVER_MARGIN
        }

        if (albumCoverCenterXDx == 0f) {
            albumCoverCenterXDx = (width / 2f) - (COLLAPSED_ALBUM_COVER_MARGIN + COLLAPSED_ALBUM_COVER_SIZE / 2f)
        }

        if (currentViewHeight == 0f) {
            currentViewHeight = height - COLLAPSED_VIEW_HEIGHT
        }


        if (albumCoverCenterYDy == 0f) {
            val top = EXPANDED_ALBUM_COVER_MARGIN_TOP.toFloat()
            val bottom = height.toFloat() - pxFromPercentOfHeight(51.8f)
            val centerY = height - bottom - (bottom - top) / 2f
            albumCoverCenterYDy = (height.toFloat() - COLLAPSED_ALBUM_COVER_MARGIN.toFloat() - COLLAPSED_ALBUM_COVER_SIZE.toFloat() / 2f) - (centerY)
        }

        if (currentTimelineX == 0f) {
            currentTimelineX = EXPANDED_TIMELINE_MARGIN_START.toFloat()
        }

        timelineAnimator = ValueAnimator.ofFloat(0f, abs(width - pxFromPercentOfWidth(17.8f)).toFloat())
        timelineAnimator.duration = (currentTrack.duration - currentPlaybackTime) * 1000L
        timelineAnimator.interpolator = LinearInterpolator()
        timelineAnimator.addUpdateListener(TimelineValueAnimatorListener())
        timelineAnimator.addListener(TimelineAnimatorListener())
    }

    private fun measureTextViews() {

        playbackTimeMeasuredWidth = trackPlaybackTimeTextPaint.measureText(currentTrack.duration.toTimeText)
        trackBandMeasuredWidth = trackBandPaint.measureText(currentTrack.band)
        expandedTrackTitleMeasuredWidth = expandedTrackTitlePaint.measureText(currentTrack.title)
        collapsedTrackTitleMeasuredWidth = collapsedTrackTitleTextPaint.measureText(currentTrack.title)

        val availableForBandLeftBorder = EXPANDED_TRACK_TITLE_MARGIN_START.toFloat()
        val availableForBandRightBorder = width - EXPANDED_TRACK_TITLE_MARGIN_START.toFloat()

        val availableForBandWidth = availableForBandRightBorder - availableForBandLeftBorder

        if (trackBandMeasuredWidth > availableForBandWidth) {
            ellipsizedTrackBand = TextUtils.ellipsize(currentTrack.band, expandedTrackTitlePaint, availableForBandWidth, TextUtils.TruncateAt.END)
        } else {
            val delta = availableForBandWidth - trackBandMeasuredWidth
            val margin = delta / 2
            expandedTrackBandLeftBorder = EXPANDED_TRACK_TITLE_MARGIN_START.toFloat() + margin
        }

        val availableForTitleLeftBorder = EXPANDED_TRACK_TITLE_MARGIN_START.toFloat()
        val availableForTitleRightBorder = width - EXPANDED_TRACK_TITLE_MARGIN_START.toFloat()

        val availableForTitleWidth = availableForTitleRightBorder - availableForTitleLeftBorder

        if (expandedTrackTitleMeasuredWidth > availableForTitleWidth) {
            ellipsizedTrackTitle = TextUtils.ellipsize(currentTrack.title, expandedTrackTitlePaint, availableForTitleWidth, TextUtils.TruncateAt.END)
        } else {
            val delta = availableForTitleWidth - expandedTrackTitleMeasuredWidth
            val margin = delta / 2
            expandedTrackTitleLeftBorder = EXPANDED_TRACK_TITLE_MARGIN_START.toFloat() + margin
        }
    }

    private fun initializeRects() {

        collapsedPlayIconRect = Rect(
                width - pxFromPercentOfWidth(18.3f) - COLLAPSED_BUTTON_PLAY_SIZE,
                height - COLLAPSED_BUTTON_PLAY_MARGIN_BOTTOM - COLLAPSED_BUTTON_PLAY_SIZE - 2.dpToPx,
                width - pxFromPercentOfWidth(18.3f),
                height - COLLAPSED_BUTTON_PLAY_MARGIN_BOTTOM - 2.dpToPx
        )

        collapsedPauseIconRect = Rect(
                width - pxFromPercentOfWidth(18.3f) - COLLAPSED_BUTTON_PAUSE_SIZE,
                height - COLLAPSED_BUTTON_PAUSE_MARGIN_BOTTOM - COLLAPSED_BUTTON_PAUSE_SIZE,
                width - pxFromPercentOfWidth(18.3f),
                height - COLLAPSED_BUTTON_PAUSE_MARGIN_BOTTOM
        )

        collapsedNextIconRect = Rect(
            width - COLLAPSED_BUTTON_NEXT_MARGIN_END - COLLAPSED_BUTTON_NEXT_SIZE,
                height - COLLAPSED_BUTTON_NEXT_SIZE - COLLAPSED_BUTTON_NEXT_MARGIN_BOTTOM,
                width - COLLAPSED_BUTTON_NEXT_MARGIN_END,
                height - COLLAPSED_BUTTON_NEXT_MARGIN_BOTTOM
        )

        expandedShuffleButtonRect = Rect(
                EXPANDED_BUTTON_SHUFFLE_MARGIN_START,
                height - EXPANDED_BUTTON_SHUFFLE_MARGIN_BOTTOM - EXPANDED_BUTTON_SHUFFLE_SIZE,
                EXPANDED_BUTTON_SHUFFLE_MARGIN_START + EXPANDED_BUTTON_SHUFFLE_SIZE,
                height - EXPANDED_BUTTON_SHUFFLE_MARGIN_BOTTOM)

        expandedRepeatButtonRect = Rect(
                width - EXPANDED_BUTTON_REPEAT_SIZE - EXPANDED_BUTTON_REPEAT_MARGIN_END,
                height - EXPANDED_BUTTON_REPEAT_MARGIN_BOTTOM - EXPANDED_BUTTON_SHUFFLE_SIZE,
                width - EXPANDED_BUTTON_REPEAT_MARGIN_END,
                height - EXPANDED_BUTTON_REPEAT_MARGIN_BOTTOM)

        expandedPreviousButtonRect = Rect(
                EXPANDED_BUTTON_PREVIOUS_MARGIN_START,
                height - EXPANDED_BUTTON_PREVIOUS_MARGIN_BOTTOM - EXPANDED_BUTTON_PREVIOUS_SIZE,
                EXPANDED_BUTTON_PREVIOUS_MARGIN_START + EXPANDED_BUTTON_PREVIOUS_SIZE,
                height - EXPANDED_BUTTON_PREVIOUS_MARGIN_BOTTOM)

        expandedPlayButtonRect = Rect(
                width / 2 - EXPANDED_BUTTON_PLAY_SIZE / 2,
                height - EXPANDED_BUTTON_PLAY_MARGIN_BOTTOM - EXPANDED_BUTTON_PLAY_SIZE,
                width / 2 + EXPANDED_BUTTON_PLAY_SIZE / 2,
                height - EXPANDED_BUTTON_PLAY_MARGIN_BOTTOM)

        expandedPauseButtonRect = Rect(
                width / 2 - EXPANDED_BUTTON_PLAY_SIZE / 2,
                height - EXPANDED_BUTTON_PLAY_MARGIN_BOTTOM - EXPANDED_BUTTON_PLAY_SIZE,
                width / 2 + EXPANDED_BUTTON_PLAY_SIZE / 2,
                height - EXPANDED_BUTTON_PLAY_MARGIN_BOTTOM)

        expandedNextButtonRect = Rect(
                width - EXPANDED_BUTTON_NEXT_MARGIN_END - EXPANDED_BUTTON_NEXT_SIZE,
                height - EXPANDED_BUTTON_PREVIOUS_MARGIN_BOTTOM - EXPANDED_BUTTON_PREVIOUS_SIZE,
                width - EXPANDED_BUTTON_NEXT_MARGIN_END,
                height - EXPANDED_BUTTON_PREVIOUS_MARGIN_BOTTOM)

        expandedPlaybackTimeRect = Rect(
                EXPANDED_PLAYBACK_DURATION_MARGIN_START,
                height - pxFromPercentOfHeight(22.4f) - EXPANDED_PLAYBACK_DURATION_MARGIN_BOTTOM - COLLAPSED_TRACK_DURATION_TEXT_SIZE,
                EXPANDED_PLAYBACK_DURATION_MARGIN_START + playbackTimeMeasuredWidth.toInt(),
                height - pxFromPercentOfHeight(22.4f) - EXPANDED_PLAYBACK_DURATION_MARGIN_BOTTOM)

        expandedTrackDurationRect = Rect(
                width - playbackTimeMeasuredWidth.toInt() - EXPANDED_TRACK_DURATION_MARGIN_END,
                height - pxFromPercentOfHeight(22.4f) - EXPANDED_PLAYBACK_DURATION_MARGIN_BOTTOM - COLLAPSED_TRACK_DURATION_TEXT_SIZE,
                width - EXPANDED_TRACK_DURATION_MARGIN_END,
                height - pxFromPercentOfHeight(22.4f) - EXPANDED_PLAYBACK_DURATION_MARGIN_BOTTOM
        )

        expandedAddIconRect = Rect(
                EXPANDED_ADD_ICON_MARGIN_START,
                height - pxFromPercentOfHeight(38.3f) - EXPANDED_ADD_ICON_SIZE,
                EXPANDED_ADD_ICON_MARGIN_START + EXPANDED_ADD_ICON_SIZE,
                height - pxFromPercentOfHeight(38.3f)
        )

        expandedDotsIconRect = Rect(
                width - 36.dpToPx,
                height - pxFromPercentOfHeight(38.9f) - EXPANDED_DOTS_ICON_HEIGHT,
                width - 24.dpToPx,
                height - pxFromPercentOfHeight(38.9f)
        )

        albumCoverRect = RectF(
                COLLAPSED_ALBUM_COVER_MARGIN.toFloat(),
                height - COLLAPSED_ALBUM_COVER_SIZE.toFloat() - COLLAPSED_ALBUM_COVER_MARGIN,
                COLLAPSED_ALBUM_COVER_SIZE + COLLAPSED_ALBUM_COVER_MARGIN.toFloat(),
                height - COLLAPSED_ALBUM_COVER_MARGIN.toFloat()
        )

    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)

        savedState.playbackTime = currentPlaybackTime
        savedState.trackDuration = currentTrack.duration
        savedState.playbackState = when (currentAudioPlaybackState) {
            AudioPlaybackState.IDLE -> 0
            AudioPlaybackState.PAUSED -> 1
            AudioPlaybackState.PLAYING -> 2
        }
        savedState.trackBand = currentTrack.band!!
        savedState.trackTitle = currentTrack.title!!

        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        this.currentPlaybackTime = state.playbackTime
        this.currentTrack.band = state.trackBand
        this.currentTrack.title = state.trackTitle
        this.currentTrack.duration = state.trackDuration
        this.currentAudioPlaybackState = when (state.playbackState) {
            0 -> AudioPlaybackState.IDLE
            1 -> AudioPlaybackState.PAUSED
            2 -> AudioPlaybackState.PLAYING
            else -> AudioPlaybackState.IDLE
        }

        super.onRestoreInstanceState(state.superState)
    }


    internal class SavedState : View.BaseSavedState {

        var playbackTime: Int = 0
        var trackDuration: Int = 0
        var playbackState: Int = 0 // 0 -- IDLE, 1 -- PAUSED, 2 -- PLAYING
        lateinit var trackTitle: String
        lateinit var trackBand: String

        constructor(superState: Parcelable) : super(superState)

        private constructor(`in`: Parcel) : super(`in`) {
            this.playbackTime = `in`.readInt()
            this.trackDuration = `in`.readInt()
            this.playbackState = `in`.readInt()
            this.trackTitle = `in`.readString()
            this.trackBand = `in`.readString()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(this.playbackTime)
            out.writeInt(this.trackDuration)
            out.writeInt(this.playbackState)
            out.writeString(this.trackTitle)
            out.writeString(this.trackBand)
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

    private inner class ExpandValueAnimatorListener : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val animatedValue = animation.animatedValue as Float

            currentViewHeight = height - COLLAPSED_VIEW_HEIGHT - (height) / 100 * animatedValue

            val offsetX = (albumCoverCenterXDx) / 100 * animatedValue
            val previousAlbumCoverCenterX = currentAlbumCoverCenterX
            currentAlbumCoverCenterX = COLLAPSED_ALBUM_COVER_MARGIN + COLLAPSED_ALBUM_COVER_SIZE / 2 + offsetX

            val offsetY = (albumCoverCenterYDy) / 100 * animatedValue
            val previousAlbumCoverCenterY = currentAlbumCoverCenterY
            currentAlbumCoverCenterY = height - COLLAPSED_ALBUM_COVER_MARGIN - COLLAPSED_ALBUM_COVER_SIZE / 2 - offsetY

            albumCoverRect.offset(currentAlbumCoverCenterX - previousAlbumCoverCenterX,
                    currentAlbumCoverCenterY - previousAlbumCoverCenterY)

            val calculatedAlbumCoverXDelta = ((EXPANDED_ALBUM_COVER_WIDTH - COLLAPSED_ALBUM_COVER_SIZE) / 2f) / 100 * animatedValue
            val calculatedAlbumCoverYDelta = ((EXPANDED_ALBUM_COVER_HEIGHT - COLLAPSED_ALBUM_COVER_SIZE) / 2f) / 100 * animatedValue

            albumCoverXDelta = calculatedAlbumCoverXDelta
            albumCoverYDelta = calculatedAlbumCoverYDelta

            collapsedTrackTitleTextPaint.alpha = 0
            trackPlaybackTimeCollapsedTextPaint.alpha = 0

        }
    }

    private inner class CollapseValueAnimatorListener : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val animatedValue = animation.animatedValue

            currentViewHeight = (height - COLLAPSED_VIEW_HEIGHT) / 100 * animatedValue as Float

            val offsetX = (albumCoverCenterXDx) / 100 * animatedValue
            val previousAlbumCoverCenterX = currentAlbumCoverCenterX
            currentAlbumCoverCenterX = width / 2 - offsetX

            val offsetY = (albumCoverCenterYDy) / 100 * animatedValue
            val top = EXPANDED_ALBUM_COVER_MARGIN_TOP.toFloat()
            val bottom = height.toFloat() - pxFromPercentOfHeight(51.8f)
            val centerY = height - bottom - (bottom - top) / 2f
            val previousAlbumCoverCenterY = currentAlbumCoverCenterY
            currentAlbumCoverCenterY = centerY + offsetY

            albumCoverRect.offset(currentAlbumCoverCenterX - previousAlbumCoverCenterX,
                    currentAlbumCoverCenterY - previousAlbumCoverCenterY)

            val calculatedAlbumCoverXDelta = ((EXPANDED_ALBUM_COVER_WIDTH - COLLAPSED_ALBUM_COVER_SIZE) / 2f) / 100 * animatedValue
            val calculatedAlbumCoverYDelta = ((EXPANDED_ALBUM_COVER_HEIGHT - COLLAPSED_ALBUM_COVER_SIZE) / 2f) / 100 * animatedValue

            albumCoverXDelta = calculatedAlbumCoverXDelta
            albumCoverYDelta = calculatedAlbumCoverYDelta

            collapsedTrackTitleTextPaint.alpha = (255 / 100) * animatedValue.toInt()

            trackPlaybackTimeCollapsedTextPaint.alpha = (255 / 100) * animatedValue.toInt()
        }
    }

    private inner class ExpandAnimatorListener : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {}
        override fun onAnimationCancel(animation: Animator?) {}

        override fun onAnimationEnd(animation: Animator?) {
            currentViewState = ViewState.EXPANDED

           // collapseAnimator.start()
        }

        override fun onAnimationStart(animation: Animator?) {
            currentViewState = ViewState.EXPANDING

            collapsedPlayIconDrawable?.let {
                it.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.white), PorterDuff.Mode.SRC_IN)
            }
        }
    }

    private inner class CollapseAnimatorListener : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {}
        override fun onAnimationCancel(animation: Animator?) {}

        override fun onAnimationEnd(animation: Animator?) {
            currentViewState = ViewState.COLLAPSED

            collapsedPlayIconRect.offsetTo(width - pxFromPercentOfWidth(18.3f) - COLLAPSED_BUTTON_PLAY_SIZE,
                    height - COLLAPSED_BUTTON_PLAY_MARGIN_BOTTOM - COLLAPSED_BUTTON_PLAY_SIZE - 2.dpToPx)

            collapsedPauseIconRect.offsetTo(width - pxFromPercentOfWidth(18.3f) - COLLAPSED_BUTTON_PAUSE_SIZE,
                    height - COLLAPSED_BUTTON_PAUSE_MARGIN_BOTTOM - COLLAPSED_BUTTON_PAUSE_SIZE)
            collapsedNextIconRect.offsetTo(width - COLLAPSED_BUTTON_NEXT_MARGIN_END - COLLAPSED_BUTTON_NEXT_SIZE,
                    height - COLLAPSED_BUTTON_NEXT_SIZE - COLLAPSED_BUTTON_NEXT_MARGIN_BOTTOM)
        }

        override fun onAnimationStart(animation: Animator?) {
            currentViewState = ViewState.COLLAPSING

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
            currentPlaybackTime = calculateTimeElapsedBasedOnCurrentX(currentTimelineX).toInt()
            currentTimelineX += delta
        }
    }

    private fun calculateTimeElapsedBasedOnCurrentX(currentX: Float): Float {
        val pxPerSecond = (width - pxFromPercentOfWidth(8.9f) - currentX - pxFromPercentOfWidth(8.9f)) / currentTrack.duration
        return currentX / pxPerSecond
    }


    private inner class TimelineAnimatorListener : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {

        }

        override fun onAnimationEnd(animation: Animator?) {
        }

        override fun onAnimationCancel(animation: Animator?) {

        }

        override fun onAnimationStart(animation: Animator?) {
            currentAudioPlaybackState = AudioPlaybackState.PLAYING
        }

    }

    private fun handleNewPlaybackState() {
        when (currentAudioPlaybackState) {
            AudioPlaybackState.PLAYING -> {
                controlButtonClickListener?.onPauseButtonClicked()
                pauseNow()
            }
            AudioPlaybackState.PAUSED, AudioPlaybackState.IDLE -> {
                controlButtonClickListener?.onPlayButtonClicked()
                resumeNow()
            }
        }
    }

    fun pauseNow() {
        currentAudioPlaybackState = AudioPlaybackState.PAUSED
        timelineAnimator.pause()
    }

    fun resumeNow() {
        currentAudioPlaybackState = AudioPlaybackState.PLAYING
        if (timelineAnimator.isPaused) {
            timelineAnimator.resume()
        } else timelineAnimator.start()
    }

    val Int.toTimeText: String
        get()  {
            val minutes = (this % 3600) / 60
            val seconds = this % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val Int.dpToPx: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    val Int.pxToDp: Int
        get() = (this / Resources.getSystem().displayMetrics.density).toInt()

    val Float.spToPx: Int
        get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, context.resources.displayMetrics).toInt()

    fun Canvas.drawTopRoundRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint, radius: Float) {
        drawRoundRect(left, top, right, bottom, radius, radius, paint)
        drawRect(
                left,
                top + radius,
                right,
                bottom,
                paint
        )
    }

    fun pxFromPercentOfHeight(percent: Float) = (height / 100 * percent).toInt()
    fun pxFromPercentOfWidth(percent: Float) = (width / 100 * percent).toInt()
}