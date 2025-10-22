package com.example.cardify

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class CardifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val firebaseApp = FirebaseApp.initializeApp(this)
        if (firebaseApp != null) {
            FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            UserSession.initialize()
        } else {
            Log.w("CardifyApp", "FirebaseApp initialization failed. Check google-services.json configuration.")
        }
    }
}
