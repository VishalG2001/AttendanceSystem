package com.example.recycleviewpractice

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initiateRealm()

    }
    private fun initiateRealm(){
        Realm.init(this)
        val config = RealmConfiguration.Builder()
            .name("attendance.realm")
            .schemaVersion(2)
            .deleteRealmIfMigrationNeeded()
            .build()
        Realm.setDefaultConfiguration(config)
    }
}
