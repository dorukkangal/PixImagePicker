package com.dorukkangal.pix.helpers

import android.animation.Animator
import android.view.animation.AccelerateDecelerateInterpolator
import com.dorukkangal.pix.utility.PixBindings

/**
 * Created By Akshay Sharma on 17,June,2021
 * https://ak1.io
 */
fun PixBindings.photoTakingAnim() {
    val adInterpolator = AccelerateDecelerateInterpolator()
    controlsLayout.primaryClickBackground.animate().apply {
        scaleX(0.6f)
        scaleY(0.6f)
        duration = 20
        interpolator = adInterpolator

        setListener(
            object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) = Unit

                override fun onAnimationRepeat(animation: Animator) = Unit

                override fun onAnimationEnd(animation: Animator) {
                    controlsLayout.primaryClickBackground.animate().apply {
                        scaleX(1f)
                        scaleY(1f)
                        duration = 20
                        interpolator = adInterpolator
                        startDelay = 20
                    }.start()
                }

                override fun onAnimationCancel(animation: Animator) {
                    controlsLayout.primaryClickBackground.animate().apply {
                        scaleX(1f)
                        scaleY(1f)
                        duration = 20
                        interpolator = adInterpolator
                        startDelay = 20
                    }.start()
                }
            }
        )
    }.start()
}

fun PixBindings.videoRecordingStartAnim() {
    val adInterpolator = AccelerateDecelerateInterpolator()
    controlsLayout.primaryClickBackground.animate().apply {
        scaleX(0.6f)
        scaleY(0.6f)
        duration = 300
        interpolator = adInterpolator
    }.start()
    controlsLayout.flashButton.animate().apply {
        alpha(0f)
        duration = 300
        interpolator = adInterpolator
    }.start()
    controlsLayout.selectionBottom.animate().apply {
        alpha(0f)
        duration = 300
        interpolator = adInterpolator
    }.start()
    controlsLayout.lensFacing.animate().apply {
        alpha(0f)
        duration = 300
        interpolator = adInterpolator
    }.start()
}

fun PixBindings.videoRecordingEndAnim() {
    val adInterpolator = AccelerateDecelerateInterpolator()
    controlsLayout.primaryClickBackground.animate().apply {
        scaleX(1f)
        scaleY(1f)
        duration = 300
        interpolator = adInterpolator
    }.start()
    controlsLayout.flashButton.animate().apply {
        alpha(1f)
        duration = 300
        interpolator = adInterpolator
    }.start()
    controlsLayout.selectionBottom.animate().apply {
        scaleX(1f)
        scaleY(1f)
        duration = 300
        interpolator = adInterpolator
    }.start()
    controlsLayout.lensFacing.animate().apply {
        alpha(1f)
        duration = 300
        interpolator = adInterpolator
    }.start()
}
