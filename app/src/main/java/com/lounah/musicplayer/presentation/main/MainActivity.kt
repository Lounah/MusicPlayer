package com.lounah.musicplayer.presentation.main

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.lounah.musicplayer.R
import com.lounah.musicplayer.presentation.browsemedia.BrowseMediaFragment
import com.lounah.musicplayer.presentation.model.AudioTrack
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottom_audio_view.currentTrack = AudioTrack("ffsdf", "safsafasf", duration = 192)

        supportFragmentManager?.beginTransaction()?.replace(R.id.main_fragment_container, BrowseMediaFragment())?.commit()
    }
}
