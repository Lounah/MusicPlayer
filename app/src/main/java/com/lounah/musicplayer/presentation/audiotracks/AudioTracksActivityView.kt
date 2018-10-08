package com.lounah.musicplayer.presentation.audiotracks

import com.lounah.musicplayer.presentation.model.AudioTrack

interface AudioTracksActivityView {
    fun renderTracks(trackList: List<AudioTrack>)
}