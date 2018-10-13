package com.lounah.musicplayer.core.audioplayer

import android.app.Notification
import com.google.android.exoplayer2.Player
import android.media.AudioManager
import android.content.Intent
import android.content.BroadcastReceiver
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.lounah.musicplayer.R
import com.lounah.musicplayer.presentation.audiotracks.AudioTracksActivity
import java.lang.ref.WeakReference


class AudioPlayerService : Service(), AudioManager.OnAudioFocusChangeListener {

    companion object {
        private val CHANNEL_ID = "MUSIC PLAYER CHANNEL"
        private val NOTIFICATION_ID = 101
        private val TAG = AudioPlayerService::class.java.simpleName

        const val MESSAGE_REGISTER_CLIENT = 0
        const val MESSAGE_UNREGISTER_CLIENT = 1
        const val MESSAGE_NEXT_TRACK = 2
        const val MESSAGE_PREVIOUS_TRACK = 3
        const val MESSAGE_PAUSE = 4
        const val MESSAGE_PLAY = 5
        const val MESSAGE_TIMELINE_CHANGED = 6

        const val STATE_PAUSED = 7
        const val STATE_PLAYING = 8
        const val NOTIFICATION_CANCELLED = 14
        const val STATE_SEEK_PROCEED = 13
        const val STATE_NEXT_TRACK = 10
        const val STATE_TRACK_INITIAL = 12
        const val STATE_TRACK_ENDED = 15
    }

    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioBecomingNoisy = AudioBecomingNoisyReceiver()
    private var audioBecomingNoisyIntentFilter: IntentFilter? = null

    private lateinit var audioPlayer: AudioPlayer
    private val playerEventListener = PlayerEventListener()

    private lateinit var messenger: Messenger
    private var serviceClient: Messenger? = null

    private var playbackFolderAbsolutePath: String = ""

    private var audioFocusResult: Int = -1

    override fun onCreate() {
        super.onCreate()

        messenger = Messenger(IncomingHandler(this))
        audioPlayer = AudioPlayer.getInstance(applicationContext)
        audioPlayer.playbackEngine.addListener(playerEventListener)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioBecomingNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                this,
                CHANNEL_ID,
                R.string.app_name,
                NOTIFICATION_ID,
                NotificationDescriptionAdapter())

        playerNotificationManager.setNotificationListener(object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationStarted(notificationId: Int, notification: Notification) {
                startForeground(notificationId, notification)
            }

            override fun onNotificationCancelled(notificationId: Int) {
                stopSelf()
            }
        })

        playerNotificationManager.setFastForwardIncrementMs(0)
        playerNotificationManager.setRewindIncrementMs(0)

        playerNotificationManager.setPlayer(audioPlayer.playbackEngine)
        mediaSession = MediaSessionCompat(this, TAG)
        mediaSession.isActive = true
        playerNotificationManager.setMediaSessionToken(mediaSession.sessionToken)
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(audioPlayer.playbackEngine, null)

        registerReceiver(audioBecomingNoisy, audioBecomingNoisyIntentFilter)
    }

    override fun onBind(intent: Intent?) = messenger.binder!!

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioPlayer.playbackEngine.playWhenReady = true
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                audioPlayer.playbackEngine.playWhenReady = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                audioPlayer.playbackEngine.playWhenReady = false
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        playbackFolderAbsolutePath = intent.getStringExtra(AudioTracksActivity.AUDIO_FOLDER_ABSOLUTE_PATH)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(audioBecomingNoisy)
        mediaSession.release()
        audioPlayer.stop()
        audioManager.abandonAudioFocus(this)
        mediaSessionConnector.setPlayer(null, null)
        playerNotificationManager.setPlayer(null)
        audioPlayer.playbackEngine.removeListener(playerEventListener)
    }

    private inner class AudioBecomingNoisyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (audioPlayer.playbackEngine.playWhenReady) {
                audioPlayer.pause()
            }
        }
    }

    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (::audioManager.isInitialized) {
                audioFocusResult = audioManager.requestAudioFocus(
                        this@AudioPlayerService,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN)
            }
            if (playbackState == Player.STATE_READY && !playWhenReady) {
                serviceClient?.let {
                    sendMessage(it, STATE_PAUSED)
                }
                audioPlayer.isPaused = true
            }
            if (playbackState == Player.STATE_READY && playWhenReady && audioFocusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                serviceClient?.let {
                    sendMessage(it, STATE_PLAYING)
                }
            }
            if (playbackState == Player.TIMELINE_CHANGE_REASON_RESET) {
                serviceClient?.let {
                    sendMessage(it, NOTIFICATION_CANCELLED)
                }
            }
            if (playbackState == Player.STATE_ENDED) {
                serviceClient?.let {
                    sendMessage(it, STATE_TRACK_ENDED)
                }
            }
        }

        override fun onPositionDiscontinuity(reason: Int) {
            when (reason) {
                Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> {
                    serviceClient?.let {
                        sendMessage(it, STATE_TRACK_INITIAL)
                    }
                }
            }
        }

        override fun onSeekProcessed() {
            serviceClient?.let {
                sendMessage(it, STATE_SEEK_PROCEED)
            }
        }
    }

    private inner class NotificationDescriptionAdapter : PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player) = audioPlayer.track!!.title

        override fun getCurrentContentText(player: Player) = audioPlayer.track!!.band

        override fun getCurrentLargeIcon(player: Player,
                                         callback: PlayerNotificationManager.BitmapCallback) = BitmapFactory.decodeResource(resources, R.drawable.albumcoverxx)

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            val startAudioTracksActivityIntent = Intent(baseContext, AudioTracksActivity::class.java).apply {
                putExtra(AudioTracksActivity.AUDIO_FOLDER_ABSOLUTE_PATH, playbackFolderAbsolutePath)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            return PendingIntent.getActivity(baseContext, 0, startAudioTracksActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    internal class IncomingHandler(service: AudioPlayerService) : Handler() {
        private val serviceRef = WeakReference<AudioPlayerService>(service)
        override fun handleMessage(msg: Message) {
            val audioPlayerService = serviceRef.get()
            audioPlayerService?.let {
                when (msg.what) {
                    MESSAGE_REGISTER_CLIENT -> {
                        it.serviceClient = msg.replyTo
                    }
                    MESSAGE_UNREGISTER_CLIENT -> {
                        it.serviceClient = null
                    }
                    MESSAGE_NEXT_TRACK -> {
                        it.audioPlayer.playNextInQueue()
                    }
                    MESSAGE_PREVIOUS_TRACK -> {
                        it.audioPlayer.playPreviousInQueue()
                    }
                    MESSAGE_PAUSE -> {
                        it.audioPlayer.pause()
                    }
                    MESSAGE_PLAY -> {
                        val index = msg.arg1
                        it.audioPlayer.playAtIndexInQueue(index)
                    }
                    MESSAGE_TIMELINE_CHANGED -> {
                        val newTime = msg.arg1
                        it.audioPlayer.seekTo(newTime)
                    }
                    else -> super.handleMessage(msg)
                }
            }
        }
    }

    private fun sendMessage(messenger: Messenger, what: Int, arg: Int = 0, obj: Any? = null) {
        val msg = Message.obtain()
        msg.what = what
        msg.arg1 = arg
        messenger.send(msg)
    }

}