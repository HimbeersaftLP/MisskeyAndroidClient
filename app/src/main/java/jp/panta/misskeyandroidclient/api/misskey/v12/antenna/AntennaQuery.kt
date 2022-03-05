package jp.panta.misskeyandroidclient.api.misskey.v12.antenna

import kotlinx.serialization.Serializable

@Serializable
data class AntennaQuery (
    val i: String,
    val antennaId: String?,
    val limit: Int?
)