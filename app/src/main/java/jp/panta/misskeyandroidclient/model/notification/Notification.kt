package jp.panta.misskeyandroidclient.model.notification

import jp.panta.misskeyandroidclient.api.notes.Note
import jp.panta.misskeyandroidclient.api.users.User
import java.io.Serializable
import java.util.*

data class Notification(
    val id: String,
    val createdAt: Date,
    val type: String,
    val userId: String,
    val user: User,
    val note: Note?,
    val reaction: String?
): Serializable