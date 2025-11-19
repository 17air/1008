package com.example.cardify

import android.app.Application
import com.example.cardify.data.GroupRepository
import com.google.firebase.FirebaseApp
class CardifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        UserSession.init(this)
        GroupRepository.initialize(this)
    }
}
