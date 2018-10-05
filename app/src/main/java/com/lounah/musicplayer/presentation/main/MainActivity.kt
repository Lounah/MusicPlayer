package com.lounah.musicplayer.presentation.main

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.lounah.musicplayer.R
import com.lounah.musicplayer.presentation.browsemedia.BrowseMediaFragment
import com.lounah.musicplayer.presentation.model.AudioTrack
import com.lounah.musicplayer.presentation.uicomponents.BottomAudioView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        bottom_audio_view.visibility = View.GONE

        bottom_audio_view.currentTrack = AudioTrack("ASTROWORLD", "Travis Scott", "Rap", 181)

        val handler = Handler(Looper.getMainLooper())

        bottom_audio_view.onControlClickListener = object : BottomAudioView.OnControlButtonsClickListener {
            override fun onPauseClicked() {

            }

            override fun onPlayClicked() {

            }

            override fun onShuffleClicked() {

            }

            override fun onMoreButtonClicked() {

            }

            override fun onNextButtonClicked() {
                bottom_audio_view.currentTrack = AudioTrack("CAROUSEL", "Travis Scott", "Rap", 26)
            }

            override fun onPreviousButtonClicked() {

            }

            override fun onRepeatButtonClicked() {

            }

            override fun onAddButtonClicked() {

            }

            override fun onPlaybackTimeChanged(newPlaybackTimeSec: Int) {

            }

        }

        supportFragmentManager?.beginTransaction()?.replace(R.id.main_fragment_container, BrowseMediaFragment())?.commit()
    }
}
