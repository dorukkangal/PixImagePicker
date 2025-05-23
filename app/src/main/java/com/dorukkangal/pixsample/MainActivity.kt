@file:Suppress("MemberVisibilityCanBePrivate")

package com.dorukkangal.pixsample

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.dorukkangal.pix.models.Flash
import com.dorukkangal.pix.models.Mode
import com.dorukkangal.pix.models.Options
import com.dorukkangal.pix.models.Ratio
import com.dorukkangal.pix.models.VideoOptions
import com.dorukkangal.pixsample.databinding.ActivityMainBinding
import com.dorukkangal.pixsample.samples.FragmentSample
import com.dorukkangal.pixsample.samples.NavControllerSample
import com.dorukkangal.pixsample.samples.ViewPager2Sample
import com.dorukkangal.pixsample.samples.settings.SettingsActivity

/**
 * Created By Akshay Sharma on 18,June,2021
 * https://ak1.io
 */
internal const val TAG = "Pix logs"

var options = Options()

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
    }

    override fun onResume() {
        super.onResume()
        options = getOptionsByPreference(this)
    }

    private fun getOptionsByPreference(mainActivity: MainActivity): Options {
        val sp = PreferenceManager.getDefaultSharedPreferences(mainActivity)
        return Options().apply {
            isFrontFacing = sp.getBoolean("frontFacing", false)
            ratio = when (sp.getString("ratio", "0")) {
                "1" -> Ratio.RATIO_4_3
                "2" -> Ratio.RATIO_16_9
                else -> Ratio.RATIO_AUTO
            }
            flash = when (sp.getString("flash", "0")) {
                "1" -> Flash.Disabled
                "2" -> Flash.On
                "3" -> Flash.Off
                else -> Flash.Auto
            }
            mode = when (sp.getString("mode", "0")) {
                "1" -> Mode.Photo
                "2" -> Mode.Video
                else -> Mode.Photo
            }
            videoOptions = VideoOptions().apply {
                videoDurationLimitInSeconds = try {
                    sp.getString("videoDuration", "30")?.toInt() ?: 30
                } catch (e: Exception) {
                    sp.apply {
                        edit().putString("videoDuration", "30").commit()
                    }
                    30
                }
            }
            /**
             * allow to user can select 5 image first time.
             */
            count = try {
                sp.getString("count", "5")?.toInt() ?: 5
            } catch (e: Exception) {
                sp.apply {
                    edit().putString("count", "5").commit()
                }
                5
            }
            spanCount = sp.getString("spanCount", "4")?.toInt() ?: 4
        }
    }

    fun fragmentSampleClick(view: View) =
        startActivity(Intent(this, FragmentSample::class.java))

    fun navControllerSampleClick(view: View) =
        startActivity(Intent(this, NavControllerSample::class.java))

    fun viewPager2SampleClick(view: View) =
        startActivity(Intent(this, ViewPager2Sample::class.java))

    fun openSettings(view: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
