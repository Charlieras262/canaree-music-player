package dev.olog.presentation.widgets.bottomnavigator

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.android.HasAndroidInjector
import dev.olog.analytics.TrackerFacade
import dev.olog.core.extensions.findActivity
import dev.olog.presentation.R
import dev.olog.presentation.model.BottomNavigationPage
import dev.olog.presentation.model.PresentationPreferencesGateway
import javax.inject.Inject

internal class CanareeBottomNavigationView(
    context: Context,
    attrs: AttributeSet
) : BottomNavigationView(context, attrs) {

    @Inject
    internal lateinit var presentationPrefs: PresentationPreferencesGateway

    @Inject
    internal lateinit var trackerFacade: TrackerFacade

    private val navigator = BottomNavigator()

    init {
        (findActivity() as HasAndroidInjector)
            .androidInjector()
            .inject(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val lastLibraryPage = presentationPrefs.getLastBottomViewPage()
        selectedItemId = lastLibraryPage.toMenuId()

        setOnNavigationItemSelectedListener { menu ->
            val navigationPage = menu.itemId.toBottomNavigationPage()
            val libraryPage = presentationPrefs.getLastLibraryPage()
            saveLastPage(navigationPage)
            navigator.navigate(
                context as FragmentActivity,
                trackerFacade,
                navigationPage,
                libraryPage
            )
            true
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        setOnNavigationItemSelectedListener(null)
    }

    fun navigate(page: BottomNavigationPage) {
        selectedItemId = page.toMenuId()
    }

    fun navigateToLastPage() {
        val navigationPage = presentationPrefs.getLastBottomViewPage()
        val libraryPage = presentationPrefs.getLastLibraryPage()
        navigator.navigate(context as FragmentActivity, trackerFacade, navigationPage, libraryPage)
    }

    private fun saveLastPage(page: BottomNavigationPage) {
        presentationPrefs.setLastBottomViewPage(page)
    }

    private fun Int.toBottomNavigationPage(): BottomNavigationPage = when (this) {
        R.id.navigation_library -> BottomNavigationPage.LIBRARY
        R.id.navigation_search -> BottomNavigationPage.SEARCH
        R.id.navigation_queue -> BottomNavigationPage.QUEUE
        else -> throw IllegalArgumentException("invalid menu id")
    }

    private fun BottomNavigationPage.toMenuId(): Int = when (this) {
        BottomNavigationPage.LIBRARY -> R.id.navigation_library
        BottomNavigationPage.SEARCH -> R.id.navigation_search
        BottomNavigationPage.QUEUE -> R.id.navigation_queue
    }

}
