package com.example.cardify

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.CopyOnWriteArrayList

object UserSession {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val listeners = CopyOnWriteArrayList<(Boolean) -> Unit>()
    @Volatile
    private var initializing = false

    fun initialize(onReady: (Boolean) -> Unit = {}) {
        if (auth.currentUser != null) {
            onReady(true)
            return
        }
        listeners.add(onReady)
        if (!initializing) {
            synchronized(this) {
                if (!initializing) {
                    initializing = true
                    auth.signInAnonymously()
                        .addOnCompleteListener { task ->
                            initializing = false
                            if (!task.isSuccessful) {
                                Log.e("UserSession", "Anonymous sign-in failed", task.exception)
                            }
                            notifyListeners(task.isSuccessful)
                        }
                }
            }
        }
    }

    private fun notifyListeners(success: Boolean) {
        listeners.forEach { listener ->
            listener(success)
        }
        listeners.clear()
    }

    val userId: String
        get() = auth.currentUser?.uid.orEmpty()

    val userName: String
        get() {
            val displayName = auth.currentUser?.displayName
            if (!displayName.isNullOrBlank()) {
                return displayName
            }
            val suffix = userId.takeLast(4).ifEmpty { "0000" }
            return "게스트$suffix"
        }
}
