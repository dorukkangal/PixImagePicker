package com.dorukkangal.pix.helpers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentActivity
import com.dorukkangal.pix.R
import com.dorukkangal.pix.models.Flash
import com.dorukkangal.pix.models.Mode
import com.dorukkangal.pix.models.Options
import com.dorukkangal.pix.models.PixViewModel
import com.dorukkangal.pix.utility.PixBindings
import com.dorukkangal.pix.utility.TAG
import com.dorukkangal.pix.utility.setTextColorRes

/**
 * Created By Akshay Sharma on 17,June,2021
 * https://ak1.io
 */
fun PixBindings.setDrawableIconForFlash(options: Options) {
    controlsLayout.flashImage.setImageResource(
        when (options.flash) {
            Flash.Off -> R.drawable.ic_flash_off_black_24dp
            Flash.On -> R.drawable.ic_flash_on_black_24dp
            else -> R.drawable.ic_flash_auto_black_24dp
        }
    )
}

fun ViewGroup.setOnClickForFLash(options: Options, callback: (Options) -> Unit) {
    val iv = getChildAt(0) as ImageView
    setOnClickListener {
        val height = height
        iv.animate()
            .translationY(height.toFloat())
            .setDuration(100)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    iv.translationY = -(height / 2).toFloat()
                    when (options.flash) {
                        Flash.Auto -> {
                            options.flash = Flash.Off
                        }

                        Flash.Off -> {
                            options.flash = Flash.On
                        }

                        else -> {
                            options.flash = Flash.Auto
                        }
                    }
                    callback(options)
                    iv.animate().translationY(0f).setDuration(50).setStartDelay(100)
                        .setListener(null).start()
                }
            })
            .start()
    }
}

@SuppressLint("ClickableViewAccessibility,RestrictedApi")
internal fun PixBindings.setupClickControls(
    model: PixViewModel,
    cameraXManager: CameraXManager?,
    options: Options,
    onModeChange: (Mode) -> Unit,
    callback: (Int, Uri) -> Unit,
) {
    controlsLayout.selectionPhoto.apply {
        when (options.mode) {
            Mode.Photo -> setTextColorRes(R.color.text_color_selected)
            else -> setTextColorRes(R.color.text_color_unselected)
        }

        setOnClickListener {
            if (options.mode != Mode.Photo) {
                onModeChange(Mode.Photo)
            }
        }
    }
    controlsLayout.selectionVideo.apply {
        when (options.mode) {
            Mode.Video -> setTextColorRes(R.color.text_color_selected)
            else -> setTextColorRes(R.color.text_color_unselected)
        }

        setOnClickListener {
            if (options.mode != Mode.Video) {
                onModeChange(Mode.Video)
            }
        }
    }

    controlsLayout.primaryClickBackground.apply {
        setColorFilter(
            ContextCompat.getColor(
                context,
                when (options.mode) {
                    Mode.Photo -> R.color.surface_color_pix
                    else -> R.color.video_counter_color_pix
                }
            )
        )
    }

    controlsLayout.primaryClickButton.apply {
        var videoCounterProgress: Int

        val videoCounterHandler = Handler(Looper.getMainLooper())
        lateinit var videoCounterRunnable: Runnable

        var isRecording = false
        setOnClickListener {
            if (options.mode == Mode.Photo) {
                if (options.count <= model.selectionListSize) {
                    gridLayout.sendButton.context.toast(model.selectionListSize)
                    return@setOnClickListener
                }
                cameraXManager?.takePhoto { uri, exc ->
                    if (exc == null) {
                        val newUri = Uri.parse(uri.toString())
                        callback(3, newUri)
                    } else {
                        Log.e(TAG, "$exc")
                    }
                }
                photoTakingAnim()
                isEnabled = false
                Handler(Looper.getMainLooper()).postDelayed({
                    isEnabled = true
                }, 1000L)
            } else if (options.mode == Mode.Video) {
                if (options.count <= model.selectionListSize) {
                    gridLayout.sendButton.context.toast(model.selectionListSize)
                } else {
                    if (!isRecording) {
                        callback(4, Uri.EMPTY)
                        isRecording = true
                        videoCounterLayout.videoCounterLayout.show()
                        videoCounterProgress = 0
                        videoCounterLayout.videoPbr.progress = 0
                        videoCounterRunnable = object : Runnable {
                            override fun run() {
                                ++videoCounterProgress

                                videoCounterLayout.videoPbr.progress = videoCounterProgress
                                videoCounterLayout.videoCounter.text =
                                    videoCounterProgress.counterText


                                if (videoCounterProgress > options.videoOptions.videoDurationLimitInSeconds) {
                                    gridLayout.initialRecyclerviewContainer.apply {
                                        alpha = 1f
                                        translationY = 0f
                                    }
                                    callback(5, Uri.EMPTY)
                                    isRecording = false
                                    videoCounterLayout.videoCounterLayout.hide()
                                    videoCounterHandler.removeCallbacks(videoCounterRunnable)
                                    videoRecordingEndAnim()
                                    cameraXManager?.recording?.stop()
                                } else {
                                    videoCounterHandler.postDelayed(this, 1000)
                                }
                            }
                        }
                        videoCounterHandler.postDelayed(videoCounterRunnable, 1000)
                        videoRecordingStartAnim()
                        val maxVideoDuration = options.videoOptions.videoDurationLimitInSeconds
                        videoCounterLayout.videoPbr.max = maxVideoDuration / 1000
                        videoCounterLayout.videoPbr.invalidate()
                        gridLayout.initialRecyclerviewContainer.animate().translationY(500f)
                            .alpha(0f)
                            .setDuration(200).start()
                        cameraXManager?.takeVideo { uri, exc ->
                            if (exc == null) {
                                callback(3, uri)
                            } else {
                                Log.e(TAG, "$exc")
                            }
                        }
                    } else {
                        gridLayout.initialRecyclerviewContainer.apply {
                            alpha = 1f
                            translationY = 0f
                        }
                        callback(5, Uri.EMPTY)
                        isRecording = false
                        videoCounterLayout.videoCounterLayout.hide()
                        videoCounterHandler.removeCallbacks(videoCounterRunnable)
                        videoRecordingEndAnim()
                        cameraXManager?.recording?.stop()
                    }
                }
            }
        }
        gridLayout.selectionOk.setOnClickListener { callback(0, Uri.EMPTY) }
        gridLayout.sendButton.setOnClickListener { callback(0, Uri.EMPTY) }
        gridLayout.selectionBack.setOnClickListener { callback(1, Uri.EMPTY) }
        gridLayout.selectionCheck.setOnClickListener {
            gridLayout.selectionCheck.hide()
            callback(2, Uri.EMPTY)
        }
    }
    controlsLayout.flashButton.setOnClickForFLash(options) {
        setDrawableIconForFlash(it)
        cameraXManager?.imageCapture?.flashMode = when (options.flash) {
            Flash.Auto -> ImageCapture.FLASH_MODE_AUTO
            Flash.Off -> ImageCapture.FLASH_MODE_OFF
            Flash.On -> ImageCapture.FLASH_MODE_ON
            else -> ImageCapture.FLASH_MODE_AUTO
        }
    }
    controlsLayout.lensFacing.setOnClickListener {
        val oa1 = ObjectAnimator.ofFloat(
            controlsLayout.lensFacing,
            "scaleX",
            1f,
            0f
        ).setDuration(150)
        val oa2 = ObjectAnimator.ofFloat(
            controlsLayout.lensFacing,
            "scaleX",
            0f,
            1f
        ).setDuration(150)
        oa1.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                controlsLayout.lensFacing.setImageResource(R.drawable.ic_photo_camera)
                oa2.start()
            }
        })
        oa1.start()
        options.isFrontFacing = !options.isFrontFacing
        cameraXManager?.bindCameraUseCases(this)
    }
}

fun PixBindings.longSelectionStatus(
    enabled: Boolean
) {
    val colorPrimaryDark = fragmentPix.root.context.color(R.color.primary_color_pix)
    val colorSurface = fragmentPix.root.context.color(R.color.surface_color_pix)

    if (enabled) {
        gridLayout.selectionCheck.hide()
        gridLayout.selectionCount.setTextColor(colorSurface)
        gridLayout.topbar.setBackgroundColor(colorPrimaryDark)
        DrawableCompat.setTint(gridLayout.selectionBack.drawable, colorSurface)
        DrawableCompat.setTint(gridLayout.selectionCheck.drawable, colorSurface)
    } else {
        gridLayout.selectionCheck.show()
        DrawableCompat.setTint(gridLayout.selectionBack.drawable, colorPrimaryDark)
        DrawableCompat.setTint(gridLayout.selectionCheck.drawable, colorPrimaryDark)
        gridLayout.topbar.setBackgroundColor(colorSurface)
    }
}

fun PixBindings.setSelectionText(fragmentActivity: FragmentActivity, size: Int = 0) {
    gridLayout.selectionCount.text = if (size == 0) {
        gridLayout.selectionOk.hide()
        fragmentActivity.resources.getString(R.string.pix_tap_to_select)
    } else {
        gridLayout.selectionOk.show()
        "$size ${fragmentActivity.resources.getString(R.string.pix_selected)}"
    }
    gridLayout.imgCount.text = size.toString()
}
