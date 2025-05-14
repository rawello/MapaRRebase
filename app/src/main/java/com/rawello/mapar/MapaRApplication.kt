package com.rawello.mapar

import android.app.Application
import com.rawello.mapar.di.AppComponent
import com.rawello.mapar.di.AppModule
import com.rawello.mapar.di.DaggerAppComponent

class MapaRApplication : Application() {

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }
}