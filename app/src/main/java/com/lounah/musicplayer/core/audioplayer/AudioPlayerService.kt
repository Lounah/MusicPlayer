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
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.lounah.musicplayer.R
import com.lounah.musicplayer.presentation.audiotracks.AudioTracksActivity


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

        const val STATE_PAUSED = 6
        const val STATE_PLAYING = 7
        const val STATE_TIMELINE_CHANGED = 8
        const val STATE_NEXT_TRACK = 9
        const val STATE_PREV_TRACK = 10
        const val STATE_TRACK_INITIAL = 11
    }

    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioBecomingNoisy = AudioBecomingNoisyReceiver()
    private var audioBecomingNoisyIntentFilter: IntentFilter? = null

    private lateinit var audioPlayer: AudioPlayer
    private val playerEventListener = PlayerEventListener()

    private val messenger = Messenger(IncomingHandler())
    private val serviceClients = mutableListOf<Messenger>()

    private var playbackFolderAbsolutePath: String = ""

    override fun onCreate() {
        super.onCreate()

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

        playerNotificationManager.setFastForwardIncrementMs(0)
        playerNotificationManager.setRewindIncrementMs(0)

        playerNotificationManager.setNotificationListener(object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationStarted(notificationId: Int, notification: Notification) {
                startForeground(notificationId, notification)
            }

            override fun onNotificationCancelled(notificationId: Int) {
                stopSelf()
            }
        })

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
            AudioManager.AUDIOFOCUS_GAIN -> audioPlayer.playbackEngine.playWhenReady = true
            AudioManager.AUDIOFOCUS_LOSS -> audioPlayer.playbackEngine.playWhenReady = false
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> audioPlayer.playbackEngine.playWhenReady = false
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        playbackFolderAbsolutePath = intent.getStringExtra(AudioTracksActivity.AUDIO_FOLDER_ABSOLUTE_PATH)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(audioBecomingNoisy)
        mediaSession.release()
        audioPlayer.stop()
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

    // TODO: REQUEST AUDIO FOCUS
    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

            if (playbackState == Player.STATE_READY && !playWhenReady) {
                serviceClients.forEach {
                    sendMessage(it, STATE_PAUSED)
                }
            }
            if (playbackState == Player.STATE_READY && playWhenReady) {
                serviceClients.forEach {
                    sendMessage(it, STATE_PLAYING)
                }
            }
            if (playbackState == PlaybackStateCompat.STATE_SKIPPING_TO_NEXT) {
                serviceClients.forEach {
                    sendMessage(it, STATE_NEXT_TRACK)
                }
            }
            if (playbackState == PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS) {
                serviceClients.forEach {
                    sendMessage(it, STATE_PREV_TRACK)
                }
            }
            if (playbackState == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                serviceClients.forEach {
                    sendMessage(it, STATE_TRACK_INITIAL)
                }
            }
        }
    }

    private inner class NotificationDescriptionAdapter : PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player) = audioPlayer.track!!.title

        override fun getCurrentContentText(player: Player) = audioPlayer.track!!.band

        override fun getCurrentLargeIcon(player: Player,
                                         callback: PlayerNotificationManager.BitmapCallback)
                = BitmapFactory.decodeResource(resources, R.drawable.albumcoverxx)

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            val startAudioTracksActivityIntent = Intent(baseContext, AudioTracksActivity::class.java).apply {
                putExtra(AudioTracksActivity.AUDIO_FOLDER_ABSOLUTE_PATH, playbackFolderAbsolutePath)
            }
            return PendingIntent.getActivity(baseContext, 0, startAudioTracksActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    internal inner class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_REGISTER_CLIENT -> {
                    serviceClients += msg.replyTo
                }
                MESSAGE_UNREGISTER_CLIENT -> {
                    serviceClients -= msg.replyTo
                }
                MESSAGE_NEXT_TRACK -> {
                    audioPlayer.playNextInQueue()
                }
                MESSAGE_PREVIOUS_TRACK -> {
                    audioPlayer.playPreviousInQueue()
                }
                MESSAGE_PAUSE -> {
                    audioPlayer.pause()
                }
                MESSAGE_PLAY -> {
                    val index = msg.arg1
                    audioPlayer.playAtIndexInQueue(index)
                }
                else -> super.handleMessage(msg)
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