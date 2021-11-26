package com.joshualorett.tvprototypes.sample

import android.animation.Animator
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import com.google.android.exoplayer2.*
import com.joshualorett.tvprototypes.playback.PlaybackControls
import java.util.*

class PlaybackVideoFragment : Fragment(R.layout.fragment_playback),
    Player.Listener, UserInteractionListener {
    private lateinit var surfaceView: SurfaceView
    private lateinit var loading: ProgressBar
    private lateinit var controls: PlaybackControls
    private lateinit var player: ExoPlayer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as ViewGroup).descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        surfaceView = view.findViewById(R.id.videoSurface)
        controls = view.findViewById(R.id.playbackControls)
        loading = view.findViewById(R.id.loadingIndicator)

        player = ExoPlayer.Builder(requireContext()).build().apply {
            setVideoSurfaceView(surfaceView)
        }
        val movie  = activity?.intent?.getSerializableExtra(DetailsActivity.MOVIE) as Movie
        val mediaItem: MediaItem = MediaItem.fromUri(Uri.parse(movie.videoUrl))
        player.apply {
            setMediaItem(mediaItem)
            prepare()
            addListener(this@PlaybackVideoFragment)
        }
        controls.eventListener = object: PlaybackControls.EventListener {
            override fun skipForward() {
                player.seekForward()
            }

            override fun skipBack() {
                player.seekBack()
            }

            override fun playPause(): Boolean {
                if (player.playbackState == Player.STATE_READY) {
                    val isPlaying = player.isPlaying
                    if (isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                    return !isPlaying
                }
                return false
            }

            override fun play(): Boolean {
                if (player.playbackState == Player.STATE_READY) {
                    player.play()
                    return player.isPlaying
                }
                return false
            }

            override fun pause(): Boolean {
                if (player.playbackState == Player.STATE_READY) {
                    player.pause()
                    return !player.isPlaying
                }
                return false
            }
        }
        controls.progressListener = object: PlaybackControls.ProgressListener {
            override fun progress(): Long {
                return player.duration - player.currentPosition
            }
        }
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            loading.visibility = View.GONE
            controls.load(player.duration)
            surfaceView.animate()
                .alpha(1F)
                .setDuration(300)
                .setListener(object: Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator?) {
                        surfaceView.visibility = View.VISIBLE
                        surfaceView.alpha = 0F
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        surfaceView.visibility = View.VISIBLE
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        surfaceView.visibility = View.VISIBLE
                    }

                    override fun onAnimationRepeat(animation: Animator?) {}

                })
                .interpolator = FastOutLinearInInterpolator()
        }
    }

    override fun onUserInteraction() {
        if (!controls.isVisible) {
            controls.show()
        }
    }
}

interface UserInteractionListener {
    fun onUserInteraction()
}
