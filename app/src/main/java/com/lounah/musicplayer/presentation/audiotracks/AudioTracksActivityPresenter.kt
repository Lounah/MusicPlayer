package com.lounah.musicplayer.presentation.audiotracks

import android.provider.MediaStore
import com.lounah.musicplayer.core.executor.ExecutorSupplier
import com.lounah.musicplayer.presentation.model.AudioTrack
import java.util.concurrent.Callable
import java.util.concurrent.Future

class AudioTracksActivityPresenter(private var view: AudioTracksActivityView?) {

    private lateinit var requestTracksTask: Future<List<AudioTrack>>

    private val STUB_ALBUM_COVER__IMAGE_URL = "STUB_ALBUM_COVER__IMAGE_URL"

    /*
        Добавил кеш на случай, если захочу сохранять презентер при смене конфигурации
        пока не захотел
     */
    private var cachedTracks: List<AudioTrack>? = null

    fun detachView() {
        view = null
        if (::requestTracksTask.isInitialized)
            requestTracksTask.cancel(true)
    }

    fun loadTracksFromFolder(folderPath: String) {
        if (cachedTracks == null) {
            requestTracksTask = ExecutorSupplier.instance.backgroundThreadExecutor
                    .submit(Callable<List<AudioTrack>> { requestTracksFromFolder(folderPath) })
            try {
                val result = requestTracksTask.get()
                cachedTracks = result
                view?.renderTracks(result)
            } catch (exception: Exception) {

            }
        } else {
            view?.renderTracks(cachedTracks!!)
        }
    }

    private fun requestTracksFromFolder(folderPath: String): List<AudioTrack> {
        val audio = mutableListOf<AudioTrack>()

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.AudioColumns.ARTIST,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.DATA)

        val selection = "${MediaStore.Audio.AudioColumns.IS_MUSIC} != 0 AND ${MediaStore.Audio.AudioColumns.DATA} LIKE ?"

        val arguments = arrayOf("$folderPath/%")

        val cursor = (view as AudioTracksActivity).contentResolver.query(uri, projection, selection, arguments, null)

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val title = cursor.getString(0)
                val artist = cursor.getString(1)
                val duration = cursor.getInt(2) / 1000
                val path = cursor.getString(3)
                val imageURL = STUB_ALBUM_COVER__IMAGE_URL
                audio.add(AudioTrack(title, artist, duration = duration, path = path, albumCoverURL = imageURL))
            }
            cursor.close()
        }
        return audio
    }

}