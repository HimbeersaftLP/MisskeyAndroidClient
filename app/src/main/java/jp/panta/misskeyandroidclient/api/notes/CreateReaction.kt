package jp.panta.misskeyandroidclient.api.notes

import kotlinx.serialization.Serializable

@Serializable data class CreateReaction (val i: String, val noteId: String, val reaction: String)