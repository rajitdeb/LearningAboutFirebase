package com.rajit.firebaseexampleapp.util

import com.google.firebase.firestore.DocumentSnapshot
import com.rajit.firebaseexampleapp.model.UserInfo

fun DocumentSnapshot.toUserInfo(): UserInfo {
    val userMap = data!!

    return UserInfo(
        uId = userMap["uid"] as String,
        fullName = userMap["fullName"] as String,
        email = userMap["email"] as String
    )
}