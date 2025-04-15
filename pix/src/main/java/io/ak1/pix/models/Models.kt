package io.ak1.pix.models

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Created By Akshay Sharma on 17,June,2021
 * https://ak1.io
 */
@SuppressLint("ParcelCreator")
@Parcelize
data class Img(
    val headerDate: String = "",
    val contentUrl: Uri = Uri.EMPTY,
    val scrollerDate: String = "",
    val mediaType: Int = 1
) : Parcelable {
    @IgnoredOnParcel
    var selected = false

    @IgnoredOnParcel
    var position = 0
}

@SuppressLint("ParcelCreator")
@Parcelize
data class Options(
    var ratio: Ratio = Ratio.RATIO_AUTO,
    var count: Int = 1,
    var spanCount: Int = 4,
    var path: String = "Pix/Camera",
    var isFrontFacing: Boolean = false,
    var mode: Mode = Mode.Photo,
    var flash: Flash = Flash.Auto,
    var preSelectedUrls: ArrayList<Uri> = ArrayList(),
    var videoOptions: VideoOptions = VideoOptions()
) : Parcelable

@Parcelize
enum class Mode : Parcelable {
    Photo,
    Video
}

@SuppressLint("ParcelCreator")
@Parcelize
data class VideoOptions(
    var videoBitrate: Int? = null,
    var audioBitrate: Int? = null,
    var videoFrameRate: Int? = null,
    var videoDurationLimitInSeconds: Int = 10
) : Parcelable

@Parcelize
enum class Flash : Parcelable {
    Disabled, On, Off, Auto
}

@Parcelize
enum class Ratio : Parcelable {
    RATIO_4_3, RATIO_16_9, RATIO_AUTO
}

internal data class ModelList(
    var list: ArrayList<Img> = ArrayList(),
    var selection: ArrayList<Img> = ArrayList()
)
