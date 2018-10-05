package com.lounah.musicplayer.presentation.model

data class AudioTrack(val title: String? = "",
                      val band: String? = "",
                      val genre: String? = "",
                      val duration: Int = 0,
                      val albumCoverURL: String? = "")