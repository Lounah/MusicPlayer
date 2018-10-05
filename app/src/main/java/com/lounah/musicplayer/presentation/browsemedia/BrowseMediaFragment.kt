package com.lounah.musicplayer.presentation.browsemedia

import android.util.Log
import com.lounah.musicplayer.R
import com.lounah.musicplayer.presentation.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_browse_media.*
import android.content.Intent
import android.net.Uri
import android.os.Environment


class BrowseMediaFragment : BaseFragment() {

    override val TAG = "BROWSE MEDIA FRAGMENT"
    override val layoutRes = R.layout.fragment_browse_media

    override fun setUpToolbarTitle(resId: Int) {

    }

    override fun initUI() {
        btn_fragment_browse_media_choose_folder.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("*/*");
            startActivityForResult(intent, 100)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val path = data?.dataString
        super.onActivityResult(requestCode, resultCode, data)
    }

}