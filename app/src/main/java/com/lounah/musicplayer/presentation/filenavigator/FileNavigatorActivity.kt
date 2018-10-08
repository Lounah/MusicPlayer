package com.lounah.musicplayer.presentation.filenavigator

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.lounah.musicplayer.R
import com.lounah.musicplayer.presentation.browsemedia.BrowseMediaFragment
import com.lounah.musicplayer.util.ViewUtilities
import java.io.File

class FileNavigatorActivity : AppCompatActivity() {

    private val KEY_CURRENT_DIRECTORY = "KEY_CURRENT_DIR"

    private lateinit var filesView: RecyclerView
    private lateinit var buttonSelect: Button
    private lateinit var currentlySelectedDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            currentlySelectedDir = savedInstanceState[KEY_CURRENT_DIRECTORY] as File
        }
        initUI()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        onBackPressed()
        return true
    }

    private fun initUI() {
        initToolbar()
        initFilesRecyclerView()
        initSelectFolderButton()
    }

    private fun initToolbar() {
        supportActionBar?.title = resources.getString(R.string.select_music_folder)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initFilesRecyclerView() {
        filesView = RecyclerView(this)

        filesView.layoutManager = LinearLayoutManager(this)

        filesView.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))

        filesView.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)

        if (Environment.getExternalStorageState() in setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)) {
            if (!::currentlySelectedDir.isInitialized)
                currentlySelectedDir = Environment.getExternalStorageDirectory()
            filesView.adapter = FileContentsAdapter(currentlySelectedDir,
                    object : FileContentsAdapter.OnDirectoryChangedListener {
                        override fun onDirectoryChanged(newDir: File) {
                            currentlySelectedDir = newDir
                        }
                    })
        }

        filesView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 20)
                    buttonSelect.visibility = View.GONE
                else if (dy < 0) buttonSelect.visibility = View.VISIBLE
            }
        })

        addContentView(filesView, filesView.layoutParams)
    }

    private fun initSelectFolderButton() {
        val DEFAULT_MARGIN_16_DP = ViewUtilities.dpToPx(16, this)
        val DEFAULT_MARGIN_8_DP = ViewUtilities.dpToPx(8, this)

        buttonSelect = Button(this)

        buttonSelect.text = resources.getString(R.string.select_folder)

        val buttonSelectLayoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER)

        buttonSelectLayoutParams.setMargins(DEFAULT_MARGIN_16_DP,
                DEFAULT_MARGIN_16_DP,
                DEFAULT_MARGIN_16_DP,
                DEFAULT_MARGIN_16_DP)
        buttonSelect.layoutParams = buttonSelectLayoutParams

        buttonSelect.background = ContextCompat.getDrawable(this, R.drawable.all_button_base_background)

        buttonSelect.setTextColor(ContextCompat.getColor(this, R.color.white))

        buttonSelect.setPadding(DEFAULT_MARGIN_16_DP,
                DEFAULT_MARGIN_8_DP,
                DEFAULT_MARGIN_16_DP,
                DEFAULT_MARGIN_8_DP)

        buttonSelect.setOnClickListener {
            val folderSelectedResultIntent = Intent().apply {
                putExtra(BrowseMediaFragment.SELECTED_FOLDER_KEY, currentlySelectedDir)
            }
            setResult(Activity.RESULT_OK, folderSelectedResultIntent)
            finish()
        }

        addContentView(buttonSelect, buttonSelect.layoutParams)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putSerializable(KEY_CURRENT_DIRECTORY, currentlySelectedDir)
    }
}