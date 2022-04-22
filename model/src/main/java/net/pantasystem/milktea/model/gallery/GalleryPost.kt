package net.pantasystem.milktea.model.gallery


import net.pantasystem.milktea.model.EntityId
import net.pantasystem.milktea.model.drive.FileProperty
import net.pantasystem.milktea.model.user.User
import java.util.*

sealed class GalleryPost {
    data class Id(
        val accountId: Long,
        val galleryId: String
    ) : EntityId

    abstract val id: Id
    abstract val createdAt: Date
    abstract val updatedAt: Date
    abstract val title: String
    abstract val description: String?
    abstract val userId: User.Id
    abstract val fileIds: List<FileProperty.Id>
    abstract val tags: List<String>
    abstract val isSensitive: Boolean

    data class Normal(
        override val id: Id,
        override val createdAt: Date,
        override val updatedAt: Date,
        override val title: String,
        override val description: String?,
        override val userId: User.Id,
        override val fileIds: List<FileProperty.Id>,
        override val tags: List<String>,
        override val isSensitive: Boolean
    ) : GalleryPost()

    data class Authenticated(
        override val id: Id,
        override val createdAt: Date,
        override val updatedAt: Date,
        override val title: String,
        override val description: String?,
        override val userId: User.Id,
        override val fileIds: List<FileProperty.Id>,
        override val tags: List<String>,
        override val isSensitive: Boolean,
        val likedCount: Int,
        val isLiked: Boolean
    ) : GalleryPost()
}



data class GalleryPostRelation(
    val galleryPost: GalleryPost,
    val files: List<FileProperty>,
    val user: User,
)