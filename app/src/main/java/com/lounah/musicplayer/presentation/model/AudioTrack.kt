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
                      var isBeingPlayed: Boolean? = false,
                      val path: String? = "")

 fun audioTrackToMediaSource(track: AudioTrack, dataSourceFactory: DefaultDataSourceFactory): MediaSource
        = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(track.path))