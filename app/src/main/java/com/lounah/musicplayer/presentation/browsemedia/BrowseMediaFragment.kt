package com.lounah.musicplayer.presentation.browsemedia

import android.Manifest
import android.app.Activity
import com.lounah.musicplayer.R
import com.lounah.musicplayer.presentation.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_browse_media.*
import android.content.Intent
import android.content.pm.PackageManager
import com.lounah.musicplayer.presentation.audiotracks.AudioTracksActivity
import com.lounah.musicplayer.presentation.filenavigator.FileNavigatorActivity
import java.io.File


class BrowseMediaFragment : BaseFragment() {

    companion object {
        const val READ_EXTERNAL_STORAGE_REQUEST_CODE = 101
        const val READ_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
        const val SELECTED_FOLDER_KEY = "SELECTED_FOLDER_KEY"
        const val MUSIC_DIR_REQUEST_CODE = 112
    }

    override val TAG = "BROWSE MEDIA FRAGMENT"
    override val layoutRes = R.layout.fragment_browse_media

    override fun initUI() {
        btn_fragment_browse_media_choose_folder.setOnClickListener {
            checkForPermissionsOrBrowseMedia()
        }
    }

    private fun checkForPermissionsOrBrowseMedia() {
        requestPermissions(arrayOf(READ_EXTERNAL_STORAGE_PERMISSION), READ_EXTERNAL_STORAGE_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                    val openFileNavigatorActivityIntent = Intent(activity, FileNavigatorActivity::class.java)
                    startActivityForResult(openFileNavigatorActivityIntent, MUSIC_DIR_REQUEST_CODE)
                } else {

                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MUSIC_DIR_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val audioTrackFolder = data?.extras!![SELECTED_FOLDER_KEY] as File
            AudioTracksActivity.start(context!!, audioTrackFolder.absolutePath)
        }
    }
}
