package jp.panta.misskeyandroidclient.model.gallery

import jp.panta.misskeyandroidclient.model.file.AppFile

class UpdateGalleryPost (
    val id: GalleryPost.Id,
    val title: String,
    val files: List<AppFile>,
    val description: String?,
    val isSensitive: Boolean,
    val tags: List<String>
)