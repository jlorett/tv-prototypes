package com.joshualorett.tvprototypes.playback

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.joshualorett.tvprototypes.R
import com.joshualorett.tvprototypes.getStringForTime
import java.util.*

class PlaybackControls @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    locale: Locale = Locale.getDefault()
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val root = LayoutInflater.from(context).inflate(R.layout.playback_controls, this)
    private val formatBuilder = StringBuilder()
    private val timeFormatter = Formatter(formatBuilder, locale)
    private val refreshRateMs = 50L
    private val refreshProgressAction = Runnable { refreshProgress() }
    private val hideTimeoutMs = 3000L
    private val hideControlAction = Runnable { hide() }
    private var state: State = State.Loading
    private var duration: Long = -1L
    private var durationReciprocal: Double = 0.0
    private var hideAtMs = -1L

    // Attributes
    private val seekBarColor: Int
    private val thumbColor: Int

    val seekBar: SeekBar = root.findViewById(R.id.seekbar)
    val progress: TextView = root.findViewById(R.id.progress)
    val playPauseIndicator: ImageView = root.findViewById(R.id.playPauseIndicator)
    var eventListener: EventListener? = null
    var progressListener: ProgressListener? = null

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.PlaybackControls, 0, 0)
        seekBarColor =  attributes.getColor(R.styleable.PlaybackControls_seekBarColor, Color.WHITE)
        thumbColor =  attributes.getColor(R.styleable.PlaybackControls_thumbColor, Color.WHITE)
        attributes.recycle()
        setup()
    }

    private fun setup() {
        seekBar.apply {
            min = 0
            max = 100
            progressDrawable.setTint(seekBarColor)
            thumb.setTint(thumbColor)
            setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    v.performClick()
                    true
                } else {
                    false
                }
            }
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) {
                        seekBar?.progress = progress
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            setOnClickListener {
                broadcastPlayPause()
            }
            setOnKeyListener { v, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) {
                    return@setOnKeyListener false
                }
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        eventListener?.skipBack()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        eventListener?.skipForward()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    fun load(duration: Long) {
        this.duration = duration
        durationReciprocal = seekBar.max/duration.toDouble()
        state = State.Ready
        refreshProgress()
    }

    fun show() {
        seekBar.requestFocus()
        if (!isVisible) {
            this.animate()
                .alpha(1F)
                .setDuration(300)
                .setListener(object: Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator?) {
                        visibility = View.VISIBLE
                        alpha = 0F
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        visibility = View.VISIBLE
                        alpha = 1F
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        visibility = View.VISIBLE
                        alpha = 1F
                    }

                    override fun onAnimationRepeat(animation: Animator?) {}

                }).interpolator = FastOutLinearInInterpolator()
        }
        scheduleHide()
    }

    fun hide() {
        if (isVisible) {
            this.animate()
                .alpha(0F)
                .setDuration(300)
                .setListener(object: Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator?) {
                        visibility = View.VISIBLE
                        alpha = 1F
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        visibility = View.INVISIBLE
                        alpha = 0F
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        visibility = View.INVISIBLE
                        alpha = 0F
                    }

                    override fun onAnimationRepeat(animation: Animator?) {}

                }).interpolator = LinearOutSlowInInterpolator()
        }
        removeCallbacks(hideControlAction)
        hideAtMs = -1L
    }

    private fun refreshProgress() {
        if (state is State.Ready) {
            val progressMs = progressListener?.progress() ?: 0L
            progress.text = progressMs.getStringForTime(formatBuilder, timeFormatter)
            seekBar.progress = getSeekBarProgress(progressMs)
            // Cancel any pending updates and schedule a new one if necessary.
            removeCallbacks(refreshProgressAction)
            postDelayed(refreshProgressAction, refreshRateMs)
        }
    }

    private fun scheduleHide() {
        removeCallbacks(hideControlAction)
        hideAtMs = SystemClock.uptimeMillis() + hideTimeoutMs
        if (isAttachedToWindow) {
            postDelayed(hideControlAction, hideTimeoutMs)
        }
    }

    /**
     * Always should produce a number between [0, 100]
     */
    private fun getSeekBarProgress(progressMs: Long): Int {
        return seekBar.max - (progressMs * durationReciprocal).toInt()
    }

    private fun animatePlayPauseToggle() {
        playPauseIndicator.animate()
            .scaleXBy(1.5F)
            .scaleYBy(1.5F)
            .alpha(0F)
            .setDuration(300)
            .setListener(object: Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    playPauseIndicator.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator?) {
                    playPauseIndicator.visibility = View.INVISIBLE
                    playPauseIndicator.scaleX = 1f
                    playPauseIndicator.scaleY = 1f
                    playPauseIndicator.alpha = 1F
                }

                override fun onAnimationCancel(animation: Animator?) {
                    playPauseIndicator.visibility = View.INVISIBLE
                    playPauseIndicator.scaleX = 1f
                    playPauseIndicator.scaleY = 1f
                    playPauseIndicator.alpha = 1F
                }

                override fun onAnimationRepeat(animation: Animator?) {}
            })
            .interpolator = FastOutLinearInInterpolator()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (hideAtMs != -1L) {
            val delayMs = hideAtMs - SystemClock.uptimeMillis()
            if (delayMs <= 0) {
                hide()
            } else {
                postDelayed(hideControlAction, delayMs)
            }
        } else if (isVisible) {
            scheduleHide()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(refreshProgressAction)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            removeCallbacks(hideControlAction)
        } else if (event.action == KeyEvent.ACTION_UP) {
            scheduleHide()
        }
        val keyCode = event.keyCode
        if (event.action == KeyEvent.ACTION_DOWN) {
            return if (event.repeatCount == 0) {
                when (keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> broadcastPlayPause()
                    KeyEvent.KEYCODE_MEDIA_PLAY -> broadcastPlay()
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> broadcastPause()
                    KeyEvent.KEYCODE_MEDIA_REWIND -> eventListener?.skipBack()
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (isVisible && seekBar.hasFocus()) {
                            hide()
                        } else {
                            return false
                        }
                    }
                    else -> return super.dispatchKeyEvent(event)
                }
                true
            } else {
                // Repeatable events when down is held
                when (keyCode) {
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> eventListener?.skipForward()
                    KeyEvent.KEYCODE_MEDIA_REWIND -> eventListener?.skipBack()
                    else -> return super.dispatchKeyEvent(event)
                }
                true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun broadcastPlayPause() {
        val isPlaying = eventListener?.playPause() == true
        playPauseIndicator.isEnabled = !isPlaying
        animatePlayPauseToggle()
    }

    private fun broadcastPlay() {
        val isPlaying = eventListener?.play() == true
        playPauseIndicator.isEnabled = isPlaying
        animatePlayPauseToggle()
    }

    private fun broadcastPause() {
        val isPaused = eventListener?.pause() == true
        playPauseIndicator.isEnabled = isPaused
        animatePlayPauseToggle()
    }

    interface EventListener {
        fun skipForward()
        fun skipBack()
        fun playPause(): Boolean
        fun play(): Boolean
        fun pause(): Boolean
    }

    interface ProgressListener {
        fun progress(): Long
    }

    sealed interface State {
        object Loading: State
        object Ready: State
    }
}


