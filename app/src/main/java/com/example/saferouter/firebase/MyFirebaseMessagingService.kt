package com.example.saferouter.firebase

import com.google.firebase.auth.ktx.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val uid = Firebase.auth.currentUser?.uid ?: return

        Firebase.firestore.collection("users")
            .document(uid)
            .update("fcmToken", token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Luego te doy el código para mostrar notificación elegante
    }
}
