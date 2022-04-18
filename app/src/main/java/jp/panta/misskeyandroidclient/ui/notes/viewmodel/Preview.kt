package jp.panta.misskeyandroidclient.ui.notes.viewmodel

import net.pantasystem.milktea.model.file.File
import net.pantasystem.milktea.data.infrastructure.url.UrlPreview
import java.io.Serializable

sealed class Preview : Serializable {

    data class UrlWrapper(
        val urlPreview: UrlPreview
    ) : Preview()

    data class FileWrapper(
        val file: File
    ) : Preview()
}