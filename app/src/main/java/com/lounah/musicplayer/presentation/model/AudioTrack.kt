package com.lounah.musicplayer.presentation.model

import android.net.Uri
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

data class AudioTrack(var title: String? = "",
                      var band: String? = "",
                      val genre: String? = "",
                      var duration: Int = 0,
                      val albumCoverURL: String? = "",
                      var playbackState: PlaybackState? = PlaybackState.IDLE,
                      val path: String? = "")

enum class PlaybackState {
 IS_BEING_PLAYED, IS_PAUSED, IDLE
}

 fun audioTrackToMediaSource(track: AudioTrack, dataSourceFactory: DefaultDataSourceFactory): MediaSource
        = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(track.path))