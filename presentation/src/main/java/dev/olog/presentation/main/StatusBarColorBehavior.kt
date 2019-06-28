package dev.olog.presentation.main

import android.annotation.SuppressLint
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.olog.presentation.utils.removeLightStatusBar
import dev.olog.presentation.utils.setLightStatusBar
import dev.olog.presentation.interfaces.CanChangeStatusBarColor
import dev.olog.presentation.interfaces.HasSlidingPanel
import dev.olog.shared.extensions.lazyFast
import dev.olog.shared.theme.hasPlayerAppearance
import dev.olog.shared.utils.isMarshmallow
import javax.inject.Inject

class StatusBarColorBehavior @Inject constructor(
    private val activity: AppCompatActivity

) : DefaultLifecycleObserver, FragmentManager.OnBackStackChangedListener {

    private val slidingPanel by lazyFast { (activity as HasSlidingPanel?)?.getSlidingPanel() }

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        if (!isMarshmallow()){
            return
        }

        slidingPanel?.addPanelSlideListener(slidingPanelListener)?.also {
            activity.supportFragmentManager.addOnBackStackChangedListener(this)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        if (!isMarshmallow()){
            return
        }

        slidingPanel?.removePanelSlideListener(slidingPanelListener)?.also {
            activity.supportFragmentManager.removeOnBackStackChangedListener(this)
        }
    }

    override fun onBackStackChanged() {
        if (!isMarshmallow()){
            return
        }

        val fragment = searchForDetailFragmentOnPortraitMode()
        if (fragment == null){
            activity.window.setLightStatusBar()
        } else {
            if (slidingPanel?.state == BottomSheetBehavior.STATE_EXPANDED){
                activity.window.setLightStatusBar()
            } else {
                fragment.adjustStatusBarColor()
            }
        }
    }

    private fun searchForDetailFragmentOnPortraitMode(): CanChangeStatusBarColor? {
        val fm = activity.supportFragmentManager
        val backStackEntryCount = fm.backStackEntryCount - 1
        if (backStackEntryCount > -1) {
            val entry = fm.getBackStackEntryAt(backStackEntryCount)
            val fragment = fm.findFragmentByTag(entry.name)
            if (fragment is CanChangeStatusBarColor) {
                return fragment
            }
        }
        return null
    }

    private val slidingPanelListener = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }

        @SuppressLint("SwitchIntDef")
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_EXPANDED -> {
                    // TODO check if it needed
                    val playerApperance = (activity.hasPlayerAppearance())
                    if (playerApperance.isFullscreen() || playerApperance.isBigImage()) {
                        activity.window.removeLightStatusBar()
                    } else {
                        activity.window.setLightStatusBar()
                    }
                }
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    searchForDetailFragmentOnPortraitMode()?.adjustStatusBarColor() ?: activity.window.setLightStatusBar()
                }
            }
        }
    }

}