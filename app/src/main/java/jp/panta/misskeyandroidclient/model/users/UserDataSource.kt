package jp.panta.misskeyandroidclient.model.users

import jp.panta.misskeyandroidclient.model.AddResult

interface UserDataSource {


    sealed class Event{
        abstract val userId: User.Id
        data class Updated(override val userId: User.Id, val user: User): Event()
        data class Removed(override val userId: User.Id): Event()
        data class Created(override val userId: User.Id, val user: User): Event()
    }

    fun interface Listener {
        fun on(e: Event)
    }

    fun addEventListener(listener: Listener)

    fun removeEventListener(listener: Listener)

    suspend fun get(userId: User.Id): User

    suspend fun get(accountId: Long, userName: String, host: String?): User

    suspend fun add(user: User): AddResult

    suspend fun addAll(users: List<User>): List<AddResult>

    suspend fun remove(user: User): Boolean
}