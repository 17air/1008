package com.example.cardify

import android.app.Application
import com.example.cardify.data.LocalGroupRepository

class CardifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserSession.init(this)
        LocalGroupRepository.initialize()
    }
}
