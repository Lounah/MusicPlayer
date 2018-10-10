package com.lounah.musicplayer.presentation.filenavigator

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.lounah.musicplayer.util.ViewUtilities
import java.io.File

class FileContentsAdapter(var directory: File,
                          private val onDirectoryChangedListener: OnDirectoryChangedListener)
    : RecyclerView.Adapter<FileContentsAdapter.ViewHolder>() {

    private var directories = emptyArray<File>()

    interface OnDirectoryClickListener {
        fun onDirectoryClick(view: View, directory: File)
    }

    interface OnDirectoryChangedListener {
        fun onDirectoryChanged(newDir: File)
    }

    init {
        setCurrentDirectory(directory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = TextView(parent.context).apply {
            textSize = ViewUtilities.spToPx(8f, context).toFloat()
            val verticalPadding = ViewUtilities.dpToPx(14, context)
            val horizontalPadding = ViewUtilities.dpToPx(16, context)
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }
        itemView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return ViewHolder(itemView, object : OnDirectoryClickListener {
            override fun onDirectoryClick(view: View, directory: File) {
                setCurrentDirectory(directory)
                onDirectoryChangedListener.onDirectoryChanged(directory)
            }
        })
    }

    override fun getItemCount() = directories.size + 1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(when {
                position > 0 -> directories[position - 1]
                else -> directory.parentFile
            })


    // TODO: Вынести listFiles() в отдельный поток -- файлов может быть много
    fun setCurrentDirectory(directory: File) {
        this.directory = directory
        if (directory.listFiles() != null) {
            updateDataSet(directory.listFiles { dir, name -> File(dir, name).isDirectory })
        }
    }

    class ViewHolder(itemView: View, listener: OnDirectoryClickListener) : RecyclerView.ViewHolder(itemView) {
        private val textView = itemView as TextView

        init {
            itemView.setOnClickListener { view ->
                listener.onDirectoryClick(view, view.tag as File)
            }
        }

        fun bind(directory: File) {
            if (directory.name == "0") {
                textView.text = "<"
            } else textView.text = directory.name
            itemView.tag = directory
        }
    }

    private fun updateDataSet(newDirectories: Array<File>) {
        directories = newDirectories
        notifyDataSetChanged()
    }

}