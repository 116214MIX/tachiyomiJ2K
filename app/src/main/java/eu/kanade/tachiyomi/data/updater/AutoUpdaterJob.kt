package eu.kanade.tachiyomi.data.updater

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.coroutines.coroutineScope
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AutoUpdaterJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        try {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                return@coroutineScope Result.failure()
            }
            val result = UpdateChecker.getUpdateChecker().checkForUpdate()
            if (result is UpdateResult.NewUpdate<*> && !UpdaterService.isRunning()) {
                UpdaterNotifier(context).cancel()
                UpdaterNotifier.releasePageUrl = result.release.releaseLink
                UpdaterService.start(context, result.release.downloadLink, false)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "AutoUpdateRunner"
        const val ALWAYS = 0
        const val ONLY_ON_UNMETERED = 1
        const val NEVER = 2

        fun setupTask(context: Context) {
            val preferences = Injekt.get<PreferencesHelper>()
            val restrictions = preferences.shouldAutoUpdate()
            val wifiRestriction = if (restrictions == ONLY_ON_UNMETERED) {
                NetworkType.UNMETERED
            } else {
                NetworkType.CONNECTED
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(wifiRestriction)
                .setRequiresDeviceIdle(true)
                .build()

            val request = OneTimeWorkRequestBuilder<AutoUpdaterJob>()
                .addTag(TAG)
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancelTask(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        }
    }
}
