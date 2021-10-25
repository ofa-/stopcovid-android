/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/10/06 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController
import androidx.core.view.isVisible
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemVideoPlayerBinding
import com.lunabeestudio.stopcovid.widget.TACVideoView
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VideoPlayerItem(private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main) : AbstractBindingItem<ItemVideoPlayerBinding>() {

    override val type: Int = R.id.item_video_player

    var url: String? = null
    var retryContentDescription: String? = null
    var autoPlay: Boolean = false

    private var mediaController: MediaController? = null

    // seek 100ms to load the first frame
    private var lastPosition: Int = 100
    private var videoPlaying: Boolean? = null
    private var runningUpdateLastPosition: Job? = null

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemVideoPlayerBinding {
        return ItemVideoPlayerBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemVideoPlayerBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.apply {
            progressBar.isVisible = true
            mediaController = MediaController(binding.root.context)
            mediaController?.setAnchorView(binding.root)
            retryImageView.contentDescription = retryContentDescription
            retryImageView.setOnClickListener {
                retryImageView.isVisible = false
                videoView.setVideoURI(Uri.parse(url))
                videoPlaying = true
                videoView.tacVideoViewListener?.onSurfaceCreated()
            }

            videoView.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                }
                setMediaController(mediaController)
                setVideoURI(Uri.parse(url))
                keepScreenOn = true
                tacVideoViewListener = object : TACVideoView.TACVideoViewListener {
                    override fun onPlay() {
                        videoPlaying = true
                        runningUpdateLastPosition?.cancel()
                        runningUpdateLastPosition = CoroutineScope(mainDispatcher).launch {
                            while (true) {
                                // keep last position (100ms min to have the thumbnail of the start of the video)
                                lastPosition = currentPosition.coerceAtLeast(100)
                                // refresh last position every second
                                delay(1000)
                            }
                        }
                    }

                    override fun onPause() {
                        videoPlaying = false
                    }

                    override fun onSurfaceCreated() {
                        val wasVideoPlaying = videoPlaying
                        progressBar.isVisible = true
                        start()
                        seekTo(lastPosition)
                        if (wasVideoPlaying == false || !autoPlay) {
                            pause()
                        }
                    }

                    override fun onSurfaceDestroyed() {
                        runningUpdateLastPosition?.cancel()
                        runningUpdateLastPosition = null
                    }
                }

                setOnPreparedListener {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    progressBar.isVisible = false
                    mediaController?.show()
                }

                setOnErrorListener { _, _, _ ->
                    progressBar.isVisible = false
                    retryImageView.isVisible = true
                    false
                }

                // Setup a loop
                setOnCompletionListener {
                    start()
                }
            }
        }
    }

    fun hideMediaController() {
        mediaController?.hide()
    }
}

fun videoPlayerItem(block: (VideoPlayerItem.() -> Unit)): VideoPlayerItem = VideoPlayerItem().apply(block)