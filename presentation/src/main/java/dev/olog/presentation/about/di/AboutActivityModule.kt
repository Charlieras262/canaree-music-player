package dev.olog.presentation.about.di

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import dagger.Provides
import dev.olog.presentation.about.AboutActivity
import dev.olog.presentation.dagger.PerActivity
import dev.olog.presentation.navigator.NavigatorAbout
import dev.olog.presentation.navigator.NavigatorAboutImpl
import dev.olog.presentation.pro.BillingImpl
import dev.olog.presentation.pro.IBilling

@Module(includes = [AboutActivityModule.Bindings::class])
class AboutActivityModule(
        private val activity: AboutActivity
) {

    @Provides
    fun provideActivity(): AppCompatActivity = activity

    @Module
    interface Bindings {

        @Binds
        fun provideNavigatorAbout(navigatorImpl: NavigatorAboutImpl): NavigatorAbout

        @Binds
        @PerActivity
        fun provideBilling(impl: BillingImpl): IBilling

    }

}