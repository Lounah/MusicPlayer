package com.lounah.musicplayer.presentation.audiotracks

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import com.lounah.musicplayer.R
import com.lounah.musicplayer.core.audioplayer.AudioPlayer
import com.lounah.musicplayer.core.audioplayer.AudioPlayerService
import com.lounah.musicplayer.presentation.model.AudioTrack
import com.lounah.musicplayer.presentation.uicomponents.BottomAudioView
import kotlinx.android.synthetic.main.activity_audio_tracks.*
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.*
import android.util.Log

class AudioTracksActivity : AppCompatActivity(), AudioTracksActivityView {

    companion object {
        const val AUDIO_FOLDER_ABSOLUTE_PATH = "AUDIO_FOLDER_ABSOLUTE_PATH"
        private const val KEY_CURRENTLY_PLAYING_TRACK_INDEX = "KEY_CURRENTLY_PLAYING_TRACK_INDEX"

        fun start(context: Context, audioFolderPath: String) {
            val intent = Intent(context, AudioTracksActivity::class.java)
                    .putExtra(AUDIO_FOLDER_ABSOLUTE_PATH, audioFolderPath)
            context.startActivity(intent)
        }
    }

    private lateinit var audioTracksAdapter: AudioTracksRecyclerViewAdapter

    private lateinit var presenter: AudioTracksActivityPresenter

    private lateinit var player: AudioPlayer

    private var currentlyPlayingTrackIndex = -1

    private val activityMessenger = Messenger(AudioTracksHandler())
    private var audioService: Messenger? = null
    private var serviceWasBound = false
    private lateinit var startMusicServiceIntent: Intent

    private val audioServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            audioService = Messenger(service)
            sendMessage(activityMessenger, AudioPlayerService.MESSAGE_REGISTER_CLIENT, messenger = audioService)
            serviceWasBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            audioService = null
            sendMessage(activityMessenger, AudioPlayerService.MESSAGE_UNREGISTER_CLIENT, messenger = audioService)
            serviceWasBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_tracks)

        val absolutePath = intent.getStringExtra(AUDIO_FOLDER_ABSOLUTE_PATH)
        startMusicServiceIntent = Intent(this, AudioPlayerService::class.java).apply {
            putExtra(AUDIO_FOLDER_ABSOLUTE_PATH, absolutePath)
        }
        savedInstanceState?.let {
            currentlyPlayingTrackIndex = it[KEY_CURRENTLY_PLAYING_TRACK_INDEX] as Int
        }

        presenter = AudioTracksActivityPresenter(this)
        player = AudioPlayer.getInstance(applicationContext)

        initUI()
        presenter.loadTracksFromFolder(absolutePath)

//        if (player.track != null) {
//            bottom_audio_view_activity_tracks.visibility = View.VISIBLE
//            bottom_audio_view_activity_tracks.currentTrack = player.track!!
//            bottom_audio_view_activity_tracks.playbackState = 2
//        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (serviceWasBound) {
            unbindService(audioServiceConnection)
            serviceWasBound = false
        }
        stopService(startMusicServiceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
    }

    override fun renderTracks(trackList: List<AudioTrack>) {
        audioTracksAdapter.updateDataSet(trackList)
        if (currentlyPlayingTrackIndex == -1) {
            player.playbackQueue = trackList
        }
    }

    private fun initUI() {
        initToolbar()
        initTracksRecyclerView()
        initBottomAudioView()
    }

    private fun initToolbar() {
        supportActionBar?.title = resources.getString(R.string.audio_files)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initTracksRecyclerView() {
        rv_audio_tracks.layoutManager = LinearLayoutManager(this)

        rv_audio_tracks.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))

        audioTracksAdapter = AudioTracksRecyclerViewAdapter(object : AudioTracksRecyclerViewAdapter.OnTrackClickedCallback {
            override fun onTrackClicked(track: AudioTrack, position: Int) {
                if (bottom_audio_view_activity_tracks.visibility == View.GONE) {
                    bottom_audio_view_activity_tracks.visibility = View.VISIBLE
                }
                bottom_audio_view_activity_tracks.currentTrack = audioTracksAdapter.audioTracks[position]
                currentlyPlayingTrackIndex = position
                // TODO: handle animation on track changes

            }
        })

        rv_audio_tracks.adapter = audioTracksAdapter
    }

    private fun initBottomAudioView() {
        if (currentlyPlayingTrackIndex == -1)
            bottom_audio_view_activity_tracks.visibility = View.GONE
        bottom_audio_view_activity_tracks.onControlClickListener = BottomViewControlButtonsListener()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENTLY_PLAYING_TRACK_INDEX, currentlyPlayingTrackIndex)
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, AudioPlayerService::class.java)
        bindService(intent, audioServiceConnection, Context.BIND_AUTO_CREATE)
        startService(startMusicServiceIntent)
    }

    override fun onStop() {
        super.onStop()
        if (serviceWasBound) {
            unbindService(audioServiceConnection)
            serviceWasBound = false
        }
    }

    private fun sendMessage(replyTo: Messenger, what: Int, arg: Int = 0, messenger: Messenger?, obj: Any? = null) {
        val message = Message.obtain()
        message.what = what
        message.replyTo = replyTo
        message.arg1 = arg
        message.obj = obj
        messenger?.send(message)
    }

    private inner class AudioTracksHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                AudioPlayerService.STATE_PAUSED -> {
                    bottom_audio_view_activity_tracks.pauseNow()
                }
                AudioPlayerService.STATE_PLAYING -> {
                    bottom_audio_view_activity_tracks.resumeNow()
                }
                AudioPlayerService.STATE_NEXT_TRACK -> {
                    bottom_audio_view_activity_tracks.currentTrack = audioTracksAdapter.getNextItem(currentlyPlayingTrackIndex)
                    currentlyPlayingTrackIndex = audioTracksAdapter.getNextItemPosition(currentlyPlayingTrackIndex)
                }
                AudioPlayerService.STATE_TRACK_INITIAL -> {
                    bottom_audio_view_activity_tracks.currentTrack = player.track!!
                }
            }
        }
    }

    private inner class BottomViewControlButtonsListener : BottomAudioView.OnControlButtonsClickListener {

        override fun onPauseClicked() {
            sendMessage(activityMessenger, AudioPlayerService.MESSAGE_PAUSE, messenger = audioService)
        }

        override fun onPlayClicked() {
            sendMessage(activityMessenger,
                    AudioPlayerService.MESSAGE_PLAY,
                    currentlyPlayingTrackIndex,
                    audioService)
        }

        override fun onShuffleClicked() {

        }

        override fun onMoreButtonClicked() {

        }

        override fun onNextButtonClicked() {
            bottom_audio_view_activity_tracks.currentTrack = audioTracksAdapter.getNextItem(currentlyPlayingTrackIndex)
            currentlyPlayingTrackIndex = audioTracksAdapter.getNextItemPosition(currentlyPlayingTrackIndex)
            sendMessage(activityMessenger, AudioPlayerService.MESSAGE_NEXT_TRACK, messenger = audioService)
        }

        override fun onPreviousButtonClicked() {
            bottom_audio_view_activity_tracks.currentTrack = audioTracksAdapter.getPreviousItem(currentlyPlayingTrackIndex)
            currentlyPlayingTrackIndex = audioTracksAdapter.getPreviousItemPosition(currentlyPlayingTrackIndex)
            sendMessage(activityMessenger, AudioPlayerService.MESSAGE_PREVIOUS_TRACK, messenger = audioService)
        }

        override fun onRepeatButtonClicked() {

        }

        override fun onAddButtonClicked() {

        }

        override fun onPlaybackTimeChanged(newPlaybackTimeSec: Int) {

        }

        override fun onVisibilityChanged(newVisibilityState: Int) {
            if (newVisibilityState == BottomAudioView.STATE_EXPANDING)
                rv_audio_tracks.visibility = View.GONE else
                if (newVisibilityState == BottomAudioView.STATE_COLLAPSED) {
                    rv_audio_tracks.visibility = View.VISIBLE
                }
        }
    }
}