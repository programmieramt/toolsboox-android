package com.toolsboox.plugin.calendar.nw

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.toolsboox.plugin.calendar.ui.CalendarWebDavSyncPresenter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * One-shot WebDAV calendar sync outside of the day view, e.g. after the ink launcher wrote strokes.
 */
class CalendarWebDavSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME = "calendar-webdav-sync"

        /**
         * Enqueues a sync after [delaySeconds]. A newer request replaces a pending one, so a burst of
         * writes results in a single sync after the last write.
         */
        fun enqueue(context: Context, delaySeconds: Long) {
            val request = OneTimeWorkRequestBuilder<CalendarWebDavSyncWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun syncPresenter(): CalendarWebDavSyncPresenter
    }

    override suspend fun doWork(): Result {
        val syncPresenter = EntryPointAccessors
            .fromApplication(applicationContext, WorkerEntryPoint::class.java)
            .syncPresenter()
        syncPresenter.sync(applicationContext, UUID.randomUUID())
        return Result.success()
    }
}
