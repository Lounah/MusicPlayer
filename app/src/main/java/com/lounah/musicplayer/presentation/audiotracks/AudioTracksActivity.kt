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
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import com.lounah.musicplayer.presentation.model.PlaybackState
import com.lounah.musicplayer.presentation.uicomponents.BottomAudioView2
import kotlinx.android.synthetic.main.activity_audio_tracks.*

class AudioTracksActivity : AppCompatActivity(), AudioTracksActivityView {

    companion object {
        const val AUDIO_FOLDER_ABSOLUTE_PATH = "AUDIO_FOLDER_ABSOLUTE_PATH"
        private const val KEY_CURRENTLY_SELECTED_TRACK_INDEX = "KEY_CURRENTLY_SELECTED_TRACK_INDEX"
        private const val KEY_CURRENLTY_SELECTED_TRACK_STATE = "KEY_CURRENLTY_SELECTED_TRACK_STATE"

        fun start(context: Context, audioFolderPath: String) {
            val intent = Intent(context, AudioTracksActivity::class.java)
                    .putExtra(AUDIO_FOLDER_ABSOLUTE_PATH, audioFolderPath)
            context.startActivity(intent)
        }
    }

    private lateinit var audioTracksAdapter: AudioTracksRecyclerViewAdapter

    private lateinit var presenter: AudioTracksActivityPresenter

    private lateinit var player: AudioPlayer

    private var currentlySelectedTrackIndex = -1
    private var currentlySelectedTrackState = PlaybackState.IDLE

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
            currentlySelectedTrackIndex = it[KEY_CURRENTLY_SELECTED_TRACK_INDEX] as Int
            currentlySelectedTrackState = it[KEY_CURRENLTY_SELECTED_TRACK_STATE] as PlaybackState
        }

        presenter = AudioTracksActivityPresenter(this)
        player = AudioPlayer.getInstance(applicationContext)

        initUI()
        presenter.loadTracksFromFolder(absolutePath)
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
        if (currentlySelectedTrackIndex == -1) {
            player.playbackQueue = trackList
            bottom_audio_view_activity_tracks.currentTrack = trackList[0]
            currentlySelectedTrackIndex = 0
            currentlySelectedTrackState = PlaybackState.IDLE
        }
    }

    private fun initUI() {
        initToolbar()
        initTracksRecyclerView()
        initBottomAudioView()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar_activity_tracks)
        supportActionBar?.title = resources.getString(R.string.audio_files)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initTracksRecyclerView() {
        rv_audio_tracks.layoutManager = LinearLayoutManager(this)

        rv_audio_tracks.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))

        audioTracksAdapter = AudioTracksRecyclerViewAdapter(object : AudioTracksRecyclerViewAdapter.OnTrackClickedCallback {
            override fun onTrackClicked(track: AudioTrack, position: Int) {
//                if (bottom_audio_view_activity_tracks.visibility == View.GONE) {
//                    bottom_audio_view_activity_tracks.visibility = View.VISIBLE
//                }
                if (position != currentlySelectedTrackIndex) {
                    bottom_audio_view_activity_tracks.currentTrack = audioTracksAdapter.audioTracks[position]
                }
                currentlySelectedTrackIndex = position

                when (audioTracksAdapter.audioTracks[currentlySelectedTrackIndex].playbackState) {
                    PlaybackState.IS_BEING_PLAYED -> {
                        sendMessage(activityMessenger,
                                AudioPlayerService.MESSAGE_PAUSE,
                                currentlySelectedTrackIndex,
                                audioService)
                        currentlySelectedTrackState = PlaybackState.IS_PAUSED
                    }
                    PlaybackState.IDLE -> {
                        sendMessage(activityMessenger,
                                AudioPlayerService.MESSAGE_PLAY,
                                currentlySelectedTrackIndex,
                                audioService)
                        currentlySelectedTrackState = PlaybackState.IS_BEING_PLAYED
                    }
                    PlaybackState.IS_PAUSED -> {
                        sendMessage(activityMessenger,
                                AudioPlayerService.MESSAGE_PLAY,
                                currentlySelectedTrackIndex,
                                audioService)
                        currentlySelectedTrackState = PlaybackState.IS_BEING_PLAYED
                    }
                }
            }
        })

        rv_audio_tracks.adapter = audioTracksAdapter
    }

    private fun initBottomAudioView() {
        //if (currentlySelectedTrackIndex == -1)
          //  bottom_audio_view_activity_tracks.visibility = View.GONE
        bottom_audio_view_activity_tracks.controlButtonClickListener = BottomViewControlButtonsListener()
        bottom_audio_view_activity_tracks.currentTrackStateChangeListener = BottomAudioViewTrackStateChangeListener()
        bottom_audio_view_activity_tracks.viewStateChangeListener = BottomAudioViewStateChangeListener()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENTLY_SELECTED_TRACK_INDEX, currentlySelectedTrackIndex)
        outState.putSerializable(KEY_CURRENLTY_SELECTED_TRACK_STATE, currentlySelectedTrackState)
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, AudioPlayerService::class.java)
        bindService(intent, audioServiceConnection, Context.BIND_AUTO_CREATE)
        startService(startMusicServiceIntent)
    }

    override fun onResume() {
        super.onResume()
        if (currentlySelectedTrackIndex != -1) {
            audioTracksAdapter.notifyItemSelected(currentlySelectedTrackIndex)
            audioTracksAdapter.audioTracks[currentlySelectedTrackIndex].playbackState = currentlySelectedTrackState
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
                    if (currentlySelectedTrackIndex != -1)
                        audioTracksAdapter.notifyItemSelected(currentlySelectedTrackIndex)
                    currentlySelectedTrackState = PlaybackState.IS_PAUSED
                    bottom_audio_view_activity_tracks.pauseNow()
                }
                AudioPlayerService.STATE_PLAYING -> {
                    if (currentlySelectedTrackIndex != -1)
                        audioTracksAdapter.notifyItemSelected(currentlySelectedTrackIndex)
                    currentlySelectedTrackState = PlaybackState.IS_BEING_PLAYED
                    bottom_audio_view_activity_tracks.resumeNow()
                }
                AudioPlayerService.STATE_NEXT_TRACK -> {
                    if (currentlySelectedTrackIndex != -1)
                        bottom_audio_view_activity_tracks.currentTrack = audioTracksAdapter.getNextItem(currentlySelectedTrackIndex)
                    currentlySelectedTrackIndex = audioTracksAdapter.getNextItemPosition(currentlySelectedTrackIndex)
                }
                AudioPlayerService.STATE_TRACK_INITIAL -> {
                    bottom_audio_view_activity_tracks.currentTrack = player.track!!
                }
            }
        }
    }

    private inner class BottomViewControlButtonsListener : BottomAudioView2.OnControlButtonClickListener {
        override fun onPlayButtonClicked() {
            sendMessage(activityMessenger,
                    AudioPlayerService.MESSAGE_PLAY,
                    currentlySelectedTrackIndex,
                    audioService)
        }

        override fun onPauseButtonClicked() {
            sendMessage(activityMessenger, AudioPlayerService.MESSAGE_PAUSE, messenger = audioService)
        }

        override fun onNextButtonClicked() {
            bottom_audio_view_activity_tracks.currentTrack = audioTracksAdapter.getNextItem(currentlySelectedTrackIndex)
            currentlySelectedTrackIndex = audioTracksAdapter.getNextItemPosition(currentlySelectedTrackIndex)
            sendMessage(activityMessenger, AudioPlayerService.MESSAGE_NEXT_TRACK, messenger = audioService)
        }

        override fun onPreviousButtonClicked() {
            bottom_audio_view_activity_tracks.currentTrack = audioTracksAdapter.getPreviousItem(currentlySelectedTrackIndex)
            currentlySelectedTrackIndex = audioTracksAdapter.getPreviousItemPosition(currentlySelectedTrackIndex)
            sendMessage(activityMessenger, AudioPlayerService.MESSAGE_PREVIOUS_TRACK, messenger = audioService)
        }

        override fun onShuffleClicked() {
            // No op
        }

        override fun onRepeatClicked() {
            // No op
        }

        override fun onShowAdditionalActionsClicked() {
            // No op
        }
    }

    private inner class BottomAudioViewStateChangeListener : BottomAudioView2.OnViewStateChangeListener {
        override fun onViewStateChanged(currentState: BottomAudioView2.ViewState) {
            if (currentState == BottomAudioView2.ViewState.EXPANDING)
                rv_audio_tracks.visibility = View.GONE else
                if (currentState == BottomAudioView2.ViewState.COLLAPSED) {
                    rv_audio_tracks.visibility = View.VISIBLE
                }
        }
    }

    private inner class BottomAudioViewTrackStateChangeListener : BottomAudioView2.OnTrackStateChangeListener {
        override fun onTrackEnded() {
            bottom_audio_view_activity_tracks.currentTrack = audioTracksAdapter.getNextItem(currentlySelectedTrackIndex)
            currentlySelectedTrackIndex = audioTracksAdapter.getNextItemPosition(currentlySelectedTrackIndex)
            sendMessage(activityMessenger, AudioPlayerService.MESSAGE_NEXT_TRACK, messenger = audioService)
        }
    }
}