package com.example.cardify

import android.app.Application
import com.example.cardify.data.GroupRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class CardifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserSession.init(this)
        initializeFirebase()
        GroupRepository.initialize()
    }

    private fun initializeFirebase() {
        runCatching { FirebaseApp.initializeApp(this) }
        runCatching {
            val firestore = FirebaseFirestore.getInstance()
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }
    }
}
