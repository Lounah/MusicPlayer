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
import android.widget.Toast
import com.lounah.musicplayer.presentation.model.PlaybackState
import com.lounah.musicplayer.presentation.uicomponents.BottomAudioView
import kotlinx.android.synthetic.main.activity_audio_tracks.*
import java.lang.ref.WeakReference

class AudioTracksActivity : AppCompatActivity(), AudioTracksActivityView {

    companion object {
        const val AUDIO_FOLDER_ABSOLUTE_PATH = "AUDIO_FOLDER_ABSOLUTE_PATH"
        private const val KEY_CURRENTLY_SELECTED_TRACK_INDEX = "KEY_CURRENTLY_SELECTED_TRACK_INDEX"
        private const val KEY_CURRENTLY_SELECTED_TRACK_STATE = "KEY_CURRENLTY_SELECTED_TRACK_STATE"

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

    private lateinit var activityMessenger: Messenger
    private var audioService: Messenger? = null
    private var serviceWasBound = false
    private lateinit var startMusicServiceIntent: Intent

    private val audioServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            activityMessenger = Messenger(AudioTracksHandler(this@AudioTracksActivity))
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

    private lateinit var absolutePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_tracks)

        absolutePath = intent.getStringExtra(AUDIO_FOLDER_ABSOLUTE_PATH)

        startMusicServiceIntent = Intent(this, AudioPlayerService::class.java).apply {
            putExtra(AUDIO_FOLDER_ABSOLUTE_PATH, absolutePath)
        }
        savedInstanceState?.let {
            currentlySelectedTrackIndex = it[KEY_CURRENTLY_SELECTED_TRACK_INDEX] as Int
            currentlySelectedTrackState = it[KEY_CURRENTLY_SELECTED_TRACK_STATE] as PlaybackState
        }

        presenter = AudioTracksActivityPresenter(this)
        player = AudioPlayer.getInstance(applicationContext)

        initUI()

        presenter.loadTracksFromFolder(absolutePath)
    }

    override fun onStart() {
        super.onStart()
        Intent(this, AudioPlayerService::class.java).also { intent ->
            bindService(intent, audioServiceConnection, Context.BIND_AUTO_CREATE)
        }
        startService(startMusicServiceIntent)
    }

    override fun onResume() {
        super.onResume()
        if (currentlySelectedTrackIndex != -1) {
            audioTracksAdapter.notifyItemSelected(currentlySelectedTrackIndex)
            audioTracksAdapter.audioTracks[currentlySelectedTrackIndex].playbackState = currentlySelectedTrackState
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceWasBound) {
            unbindService(audioServiceConnection)
            serviceWasBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
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

    override fun renderTracks(trackList: List<AudioTrack>) {
        if (trackList.isEmpty()) {
            renderEmptyState()
        } else {
            audioTracksAdapter.updateDataSet(trackList)
            if (currentlySelectedTrackIndex == -1) {
                renderInitialState(trackList)
            }
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

                if (position != currentlySelectedTrackIndex) {
                    bottom_audio_view_activity_tracks.currentTrack = audioTracksAdapter.audioTracks[position]
                }

                currentlySelectedTrackIndex = position

                handleNewTrackState(audioTracksAdapter.audioTracks[currentlySelectedTrackIndex])
            }
        })

        rv_audio_tracks.adapter = audioTracksAdapter
    }

    private fun initBottomAudioView() {
        bottom_audio_view_activity_tracks.controlButtonClickListener = BottomViewControlButtonsListener()
        bottom_audio_view_activity_tracks.currentTrackStateChangeListener = BottomAudioViewTrackStateChangeListener()
        bottom_audio_view_activity_tracks.viewStateChangeListener = BottomAudioViewStateChangeListener()
    }

    private fun renderEmptyState() {
        tv_audio_tracks_empty_state.visibility = View.VISIBLE
        rv_audio_tracks.visibility = View.GONE
        bottom_audio_view_activity_tracks.visibility = View.GONE
    }

    private fun renderInitialState(initialTrackList: List<AudioTrack>) {
        player.playbackQueue = initialTrackList
        bottom_audio_view_activity_tracks.currentTrack = initialTrackList[0]
        currentlySelectedTrackIndex = 0
        currentlySelectedTrackState = PlaybackState.IDLE
        sendMessage(activityMessenger, AudioPlayerService.MESSAGE_PAUSE, messenger = audioService)
    }

    private fun handleNewTrackState(selectedTrack: AudioTrack) {
        when (selectedTrack.playbackState) {
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

    private fun sendMessage(replyTo: Messenger, what: Int, arg: Int = 0, messenger: Messenger?, obj: Any? = null) {
        val message = Message.obtain()
        message.what = what
        message.replyTo = replyTo
        message.arg1 = arg
        message.obj = obj
        messenger?.send(message)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENTLY_SELECTED_TRACK_INDEX, currentlySelectedTrackIndex)
        outState.putSerializable(KEY_CURRENTLY_SELECTED_TRACK_STATE, currentlySelectedTrackState)
    }

    private class AudioTracksHandler(audioTracksActivity: AudioTracksActivity) : Handler() {
        private val activityRef = WeakReference<AudioTracksActivity>(audioTracksActivity)
        override fun handleMessage(msg: Message) {
            val activity = activityRef.get()
            activity?.let {
                when (msg.what) {
                    AudioPlayerService.STATE_PAUSED -> {
                        it.apply {
                            if (currentlySelectedTrackIndex != -1)
                                audioTracksAdapter.notifyItemSelected(currentlySelectedTrackIndex)
                            currentlySelectedTrackState = PlaybackState.IS_PAUSED
                            bottom_audio_view_activity_tracks.pauseNow()
                        }
                    }
                    AudioPlayerService.STATE_PLAYING -> {
                        it.apply {
                            if (currentlySelectedTrackIndex != -1)
                                audioTracksAdapter.notifyItemSelected(currentlySelectedTrackIndex)
                            currentlySelectedTrackState = PlaybackState.IS_BEING_PLAYED
                            bottom_audio_view_activity_tracks.resumeNow()
                        }
                    }
                    AudioPlayerService.STATE_NEXT_TRACK -> {
                        it.apply {
                            if (currentlySelectedTrackIndex != -1)
                                bottom_audio_view_activity_tracks.currentTrack = audioTracksAdapter.getNextItem(currentlySelectedTrackIndex)
                            currentlySelectedTrackIndex = audioTracksAdapter.getNextItemPosition(it.currentlySelectedTrackIndex)
                        }
                    }
                    AudioPlayerService.STATE_SEEK_PROCEED -> {
                        it.audioTracksAdapter.notifyItemSelected(it.currentlySelectedTrackIndex)
                    }
                    AudioPlayerService.STATE_TRACK_INITIAL -> {
                        it.bottom_audio_view_activity_tracks.currentTrack = it.player.track!!
                    }
                    AudioPlayerService.NOTIFICATION_CANCELLED -> {
                        // ну такое себе
                        it.finish()
                    }
                    AudioPlayerService.STATE_TRACK_ENDED -> {
                        it.apply {
                            bottom_audio_view_activity_tracks.currentTrack = audioTracksAdapter.getNextItem(currentlySelectedTrackIndex)
                            currentlySelectedTrackIndex = audioTracksAdapter.getNextItemPosition(currentlySelectedTrackIndex)
                            sendMessage(activityMessenger, AudioPlayerService.MESSAGE_NEXT_TRACK, messenger = audioService)
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }

    private inner class BottomViewControlButtonsListener : BottomAudioView.OnControlButtonClickListener {
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
        }

        override fun onRepeatClicked() {
        }

        override fun onShowAdditionalActionsClicked() {
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private inner class BottomAudioViewStateChangeListener : BottomAudioView.OnViewStateChangeListener {
        override fun onViewStateChanged(currentState: BottomAudioView.ViewState) {
            if (currentState == BottomAudioView.ViewState.EXPANDING)
                rv_audio_tracks.visibility = View.GONE else
                if (currentState == BottomAudioView.ViewState.COLLAPSED) {
                    rv_audio_tracks.visibility = View.VISIBLE
                }
        }
    }

    private inner class BottomAudioViewTrackStateChangeListener : BottomAudioView.OnTrackStateChangeListener {
        override fun onTrackEnded() {}

        override fun onTimelineChanged(newTimeSec: Int) {
            if (audioTracksAdapter.audioTracks[currentlySelectedTrackIndex].playbackState == PlaybackState.IDLE)
                audioTracksAdapter.notifyItemSelected(currentlySelectedTrackIndex)
            sendMessage(activityMessenger, arg = newTimeSec, what = AudioPlayerService.MESSAGE_TIMELINE_CHANGED, messenger = audioService)
        }
    }
}