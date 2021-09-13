package eu.kanade.tachiyomi

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import eu.kanade.tachiyomi.data.image.coil.CoilSetup
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import org.acra.ACRA
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.conscrypt.Conscrypt
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.injectLazy
import uy.kohesive.injekt.registry.default.DefaultRegistrar
import java.security.Security

// @ReportsCrashes(
//    formUri = "https://collector.tracepot.com/e90773ff",
//    reportType = org.acra.sender.HttpSender.Type.JSON,
//    httpMethod = org.acra.sender.HttpSender.Method.PUT,
//    buildConfigClass = BuildConfig::class,
//    excludeMatchingSharedPreferencesKeys = [".*username.*", ".*password.*", ".*token.*"]
// )
open class App : Application(), LifecycleObserver {

    val preferences: PreferencesHelper by injectLazy()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        // TLS 1.3 support for Android 10 and below
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        Injekt = InjektScope(DefaultRegistrar())
        Injekt.importModule(AppModule(this))

        CoilSetup(this)
        setupAcra()
        setupNotificationChannels()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Reset Incognito Mode on relaunch
        preferences.incognitoMode().set(false)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    @Suppress("unused")
    fun onAppBackgrounded() {
        // App in background
        if (!SecureActivityDelegate.isAuthenticating && preferences.lockAfter().getOrDefault() >= 0) {
            SecureActivityDelegate.locked = true
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    protected open fun setupAcra() {
        initAcra {
            reportFormat = StringFormat.JSON
            buildConfigClass = BuildConfig::class.java
            excludeMatchingSharedPreferencesKeys = arrayOf(".*username.*", ".*password.*", ".*token.*")
            httpSender {
                uri = "https://collector.tracepot.com/e90773ff"
                httpMethod = org.acra.sender.HttpSender.Method.PUT
            }
        }
        ACRA.init(this)
    }

    protected open fun setupNotificationChannels() {
        Notifications.createChannels(this)
    }
}
