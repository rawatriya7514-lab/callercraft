package com.craftcaller.app

import android.app.Application
import com.craftcaller.app.data.Auth
import com.craftcaller.app.data.Cloud
import com.craftcaller.app.data.Db
import com.craftcaller.app.data.Prefs
import com.craftcaller.app.data.Repo
import com.craftcaller.app.service.CallNotification
import com.craftcaller.app.service.CallerIDService
import com.craftcaller.app.util.Cctx
import com.craftcaller.app.util.Perms

/**
 * One process; every component (Activity UI, ringing service, screening
 * service, broadcast receivers) shares the same:
 *   Db (local), Cloud (community directory), CallerHub (cross-component state).
 */
class CraftCallerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Cctx.app = this
        Db.init(this)
        Cloud.init(this)
        Auth.init(this)
        CallNotification.channels(this)
        if (Prefs.onboarded &&
            Perms.granted(this, android.Manifest.permission.READ_PHONE_STATE)
        ) {
            CallerIDService.start(this)
        }
    }
}
