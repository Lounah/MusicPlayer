package com.lounah.musicplayer.presentation.audiotracks

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.lounah.musicplayer.presentation.model.AudioTrack
import com.lounah.musicplayer.presentation.model.PlaybackState
import com.lounah.musicplayer.presentation.uicomponents.AudioTrackItemView

class AudioTracksRecyclerViewAdapter(private val onTrackClickedCallback: OnTrackClickedCallback) : RecyclerView.Adapter<AudioTracksRecyclerViewAdapter.ViewHolder>() {

    val audioTracks = mutableListOf<AudioTrack>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = AudioTrackItemView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return AudioTracksRecyclerViewAdapter.ViewHolder(itemView)
    }

    override fun getItemCount() = audioTracks.size

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = audioTracks[position]
        viewHolder.itemView.setOnClickListener {
            onTrackClickedCallback.onTrackClicked(item, position)
        }
        viewHolder.bind(item)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: AudioTrack) {
            (itemView as AudioTrackItemView).currentTrack = item
        }
    }

    fun updateDataSet(newTracks: List<AudioTrack>) {
        audioTracks.clear()
        audioTracks.addAll(newTracks)
        notifyDataSetChanged()
    }

    fun getNextItem(currentItemPosition: Int) = audioTracks[getNextItemPosition(currentItemPosition)]

    fun getNextItemPosition(currentItemPosition: Int): Int {
        return if (currentItemPosition < audioTracks.size - 1) {
            currentItemPosition + 1
        } else {
            0
        }
    }

    fun getPreviousItem(currentItemPosition: Int) = audioTracks[getPreviousItemPosition(currentItemPosition)]

    fun getPreviousItemPosition(currentItemPosition: Int): Int {
        return if (currentItemPosition > 0) {
            currentItemPosition - 1
        } else {
            audioTracks.size - 1
        }
    }

    fun notifyItemSelected(position: Int) {
        var lastPlayedIndex = -1
        audioTracks.forEachIndexed { index, track ->
            if (track.playbackState != PlaybackState.IDLE) {
                lastPlayedIndex = index
            }
        }
        if (position != lastPlayedIndex) {
            if (lastPlayedIndex != -1)
                audioTracks[lastPlayedIndex].playbackState = PlaybackState.IDLE
            audioTracks[position].playbackState = PlaybackState.IS_BEING_PLAYED
        } else {
            if (audioTracks[position].playbackState == PlaybackState.IS_BEING_PLAYED)
                audioTracks[position].playbackState = PlaybackState.IS_PAUSED
            else audioTracks[position].playbackState = PlaybackState.IS_BEING_PLAYED
        }
        notifyItemRangeChanged(0, audioTracks.size - 1)
    }

    interface OnTrackClickedCallback {
        fun onTrackClicked(track: AudioTrack, position: Int)
    }
}