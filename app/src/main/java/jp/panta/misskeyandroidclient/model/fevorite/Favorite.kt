package jp.panta.misskeyandroidclient.model.fevorite

import jp.panta.misskeyandroidclient.api.misskey.notes.NoteDTO

data class Favorite(val id: String, val createdAt: String, val note: NoteDTO, val noteId: String)