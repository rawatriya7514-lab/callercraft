package com.craftcaller.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.repository.ContactsRepository
import com.craftcaller.app.util.Prefs

/**
 * Background "contribute to community caller-ID" job:
 * reads the phonebook and publishes name -> number into the shared directory.
 *
 * PRIVACY CONTRACT (hard rule): this worker uploads PHONEBOOK DATA — it must
 * NEVER run without the user's explicit informed community-upload consent
 * (`Prefs.communityUploadConsent`, default OFF, set through the explainer in
 * Settings). Without consent this is a guaranteed no-op, and enqueuing while
 * unconsented is skipped outright.
 */
class ContactSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = Prefs(applicationContext)
            if (!prefs.communityUploadConsent) {
                // No informed consent on record: contribute nothing. Ever.
                Result.success()
            } else {
                val contacts = ContactsRepository(applicationContext).loadDeviceContacts()
                if (prefs.onboardingDone && contacts.isNotEmpty()) {
                    FirebaseRepository.uploadCommunityContacts(contacts)
                }
                Result.success()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            if (!Prefs(context).communityUploadConsent) {
                android.util.Log.i("ContactSyncWorker", "enqueue skipped: community upload consent is off")
                return
            }
            WorkManager.getInstance(context).enqueueUniqueWork(
                "contact_sync",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<ContactSyncWorker>().build()
            )
        }
    }
}

/** Convenience helper so workers don't need to carry a DB reference. */
internal val Context.smsDb get() = DatabaseProvider.get(this)
