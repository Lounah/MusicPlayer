package com.lounah.musicplayer.presentation.main

import android.os.*
import android.support.v7.app.AppCompatActivity
import com.lounah.musicplayer.R
import com.lounah.musicplayer.presentation.browsemedia.BrowseMediaFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpDefaultFragment()
    }

    private fun setUpDefaultFragment() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_fragment_container, BrowseMediaFragment())
                .commit()
    }
}
