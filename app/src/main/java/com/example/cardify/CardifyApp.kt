package com.example.cardify

import android.app.Application
import com.example.cardify.data.GroupRepository
class CardifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserSession.init(this)
        GroupRepository.initialize(this)
    }
}
