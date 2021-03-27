package jp.panta.misskeyandroidclient.model.users.impl

import jp.panta.misskeyandroidclient.api.throwIfHasError
import jp.panta.misskeyandroidclient.api.users.RequestUser
import jp.panta.misskeyandroidclient.api.users.toUser
import jp.panta.misskeyandroidclient.model.api.MisskeyAPI
import jp.panta.misskeyandroidclient.model.users.User
import jp.panta.misskeyandroidclient.model.users.UserNotFoundException
import jp.panta.misskeyandroidclient.model.users.UserRepository
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import retrofit2.Call

@Suppress("BlockingMethodInNonBlockingContext")
class UserRepositoryImpl(
    val miCore: MiCore
) : UserRepository{
    private val logger = miCore.loggerFactory.create("UserRepositoryImpl")

    override suspend fun find(userId: User.Id, detail: Boolean): User {
        val localResult = runCatching {
            miCore.getUserDataSource().get(userId).let{
                if(detail) {
                    it as? User.Detail
                }else it
            }
        }
        localResult.getOrNull()?.let{
            return it
        }

        val account = miCore.getAccount(userId.accountId)
        if(localResult.getOrNull() == null) {
            val res = miCore.getMisskeyAPI(account).showUser(RequestUser(
                i = account.getI(miCore.getEncryption()),
                userId = userId.id,
                detail = true
            )).execute()
            res.throwIfHasError()
            res?.body()?.let{
                val user = it.toUser(account, true)
                it.pinnedNotes?.forEach { dto ->
                    miCore.getGetters().noteRelationGetter.get(account, dto)
                }
                miCore.getUserDataSource().add(user)
                return user
            }
        }

        throw UserNotFoundException(userId)
    }

    override suspend fun findByUserName(accountId: Long, userName: String, host: String?, detail: Boolean): User {
        val local = runCatching {
            miCore.getUserDataSource().get(accountId, userName, host) as User.Detail
        }.getOrNull()

        if(local != null) {
            return local
        }
        val account = miCore.getAccountRepository().get(accountId)
        val misskeyAPI = miCore.getMisskeyAPIProvider().get(account.instanceDomain)
        val res = misskeyAPI.showUser(
            RequestUser(
                i = account.getI(miCore.getEncryption()),
                userName = userName,
                host = host
            )
        ).execute()
        res.throwIfHasError()

        res?.body()?.let {
            it.pinnedNotes?.forEach { dto ->
                miCore.getGetters().noteRelationGetter.get(account, dto)
            }
            val user = it.toUser(account, true)
            miCore.getUserDataSource().add(user)
            return user
        }

        throw UserNotFoundException(null, userName = userName, host = host)

    }

    override suspend fun mute(userId: User.Id): Boolean {
        return action(userId.getMisskeyAPI()::muteUser, userId) { user ->
            user.copy(isMuting = true)
        }
    }

    override suspend fun unmute(userId: User.Id): Boolean {
        return action(userId.getMisskeyAPI()::unmuteUser, userId) { user ->
            user.copy(isMuting = false)
        }
    }

    override suspend fun block(userId: User.Id): Boolean {
        return action(userId.getMisskeyAPI()::blockUser, userId) { user ->
            user.copy(isBlocking = true)
        }
    }

    override suspend fun unblock(userId: User.Id): Boolean {
        return action(userId.getMisskeyAPI()::unblockUser, userId) { user ->
            user.copy(isBlocking = false)
        }
    }

    override suspend fun follow(userId: User.Id): Boolean {
        val account = miCore.getAccountRepository().get(userId.accountId)
        val req = RequestUser(userId = userId.id, i = account.getI(miCore.getEncryption()))
        logger.debug("follow req:$req")
        val res = miCore.getMisskeyAPI(account).followUser(req).execute()
        res.throwIfHasError()
        if(res.isSuccessful) {
            val updated = (find(userId, true) as User.Detail).copy(isFollowing = true)
            miCore.getUserDataSource().add(updated)
        }
        return res.isSuccessful
    }

    override suspend fun unfollow(userId: User.Id): Boolean {
        val account = miCore.getAccountRepository().get(userId.accountId)
        val res = miCore.getMisskeyAPI(account).unFollowUser(RequestUser(userId = userId.id, i = account.getI(miCore.getEncryption()))).execute()
        res.throwIfHasError()
        if(res.isSuccessful) {
            val updated = (find(userId, true) as User.Detail).copy(isFollowing = true)
            miCore.getUserDataSource().add(updated)
        }
        return res.isSuccessful
    }

    private suspend fun action(requestAPI: (RequestUser)-> Call<Unit>, userId: User.Id, reducer: (User.Detail)-> User.Detail): Boolean {
        val account = miCore.getAccountRepository().get(userId.accountId)
        val res = requestAPI.invoke(RequestUser(userId = userId.id, i = account.getI(miCore.getEncryption()))).execute()
        res.throwIfHasError()
        if(res.isSuccessful) {

            val updated = reducer.invoke(find(userId, true) as User.Detail)
            miCore.getUserDataSource().add(updated)
        }
        return res.isSuccessful
    }

    private suspend fun User.Id.getMisskeyAPI(): MisskeyAPI {
        return miCore.getMisskeyAPI(miCore.getAccountRepository().get(accountId))
    }
}