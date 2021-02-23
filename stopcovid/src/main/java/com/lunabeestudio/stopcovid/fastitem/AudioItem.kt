/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.View
import android.widget.ImageSwitcher
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem
import java.io.File

class AudioItem(context: Context, audioFile: File) : BaseItem<AudioItem.ViewHolder>(
    R.layout.item_audio, ::ViewHolder, R.id.item_audio
) {
    private val audioMediaPlayer: MediaPlayer = MediaPlayer.create(context, Uri.fromFile(audioFile))
    var playTalkbackText: String? = null
    var pauseTalkbackText: String? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.imageView1.contentDescription = playTalkbackText
        holder.imageView2.contentDescription = pauseTalkbackText
        holder.imageView1.setOnClickListener {
            onClick(holder)
        }
        holder.imageView2.setOnClickListener {
            onClick(holder)
        }
    }

    private fun onClick(holder: ViewHolder) {
        if (holder.imageSwitcher.currentView == holder.imageView1) {
            audioMediaPlayer.start()
            audioMediaPlayer.setOnCompletionListener {
                holder.imageSwitcher.showNext()
            }
        } else {
            audioMediaPlayer.pause()
            audioMediaPlayer.setOnCompletionListener(null)
        }
        holder.imageSwitcher.showNext()
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        audioMediaPlayer.pause()
        audioMediaPlayer.release()
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imageSwitcher: ImageSwitcher = v.findViewById(R.id.imageSwitcher)
        val imageView1: ImageView = v.findViewById(R.id.imageView1)
        val imageView2: ImageView = v.findViewById(R.id.imageView2)
    }
}

fun audioItem(context: Context, audioFile: File, block: (AudioItem.() -> Unit)): AudioItem = AudioItem(context, audioFile).apply(block)