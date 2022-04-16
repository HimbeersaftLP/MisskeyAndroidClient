package net.pantasystem.milktea.data.model.notes

import kotlinx.serialization.Serializable
import java.io.Serializable as JSerializable

@Serializable
data class Translation (val sourceLang: String, val text: String) : JSerializable