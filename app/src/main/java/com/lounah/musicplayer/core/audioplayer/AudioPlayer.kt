package com.lounah.musicplayer.core.audioplayer

import android.content.Context
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.lounah.musicplayer.presentation.model.AudioTrack
import com.lounah.musicplayer.presentation.model.audioTrackToMediaSource
import com.lounah.musicplayer.util.SingletonHolder

class AudioPlayer private constructor(context: Context) {

    companion object : SingletonHolder<AudioPlayer, Context>(::AudioPlayer)

    private val trackSelector = DefaultTrackSelector()
    private val bandwidthMeter = DefaultBandwidthMeter()

    private var currentlyPlayingTrackIndex = -1

    var dataSourceFactory: DefaultDataSourceFactory

    var playbackEngine: ExoPlayer

    var track: AudioTrack? = null
        set(newValue) {
            field = newValue
            playbackEngine.prepare(audioTrackToMediaSource(field!!, dataSourceFactory))
        }

    var playbackQueue: List<AudioTrack> = mutableListOf()
        set(newValue) {
            field = newValue
            currentlyPlayingTrackIndex = -1
//            val concatenatingAudioSource = ConcatenatingMediaSource()
//            newValue
//                    .map { audioTrackToMediaSource(it, dataSourceFactory) }
//                    .forEach { concatenatingAudioSource.addMediaSource(it) }
//            playbackEngine.prepare(concatenatingAudioSource)
//            playbackEngine.seekTo(playbackEngine.nextWindowIndex, 10)
        }

    var isPaused = false

    init {
        dataSourceFactory = DefaultDataSourceFactory(context,
                Util.getUserAgent(context, "MusicPlayer"),
                bandwidthMeter)

        playbackEngine = ExoPlayerFactory.newSimpleInstance(context, trackSelector)
    }

    fun pause() {
        isPaused = true
        playbackEngine.playWhenReady = false
    }

    fun play() {
        playbackEngine.playWhenReady = true
        isPaused = false
    }

    fun stop() {
        playbackEngine.playWhenReady = false
        playbackEngine.stop()
        playbackEngine.release()
    }

    fun playAtIndexInQueue(index: Int) {
        if (!isPaused) {
            track = playbackQueue[index]
            currentlyPlayingTrackIndex = index
        }
        play()
    }

    fun playNextInQueue() {
        if (currentlyPlayingTrackIndex < playbackQueue.size - 1) {
            currentlyPlayingTrackIndex++
        } else {
            currentlyPlayingTrackIndex = 0
        }
        track = playbackQueue[currentlyPlayingTrackIndex]
        play()
    }

    fun playPreviousInQueue() {
        if (currentlyPlayingTrackIndex > 0) {
            currentlyPlayingTrackIndex--
        } else {
            currentlyPlayingTrackIndex = playbackQueue.size - 1
        }
        track = playbackQueue[currentlyPlayingTrackIndex]
        play()
    }

    fun isInPlayableState() = playbackEngine.isCurrentWindowDynamic
}