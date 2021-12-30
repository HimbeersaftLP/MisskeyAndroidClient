package jp.panta.misskeyandroidclient.model.users.impl

import jp.panta.misskeyandroidclient.api.throwIfHasError
import jp.panta.misskeyandroidclient.api.MisskeyAPI
import jp.panta.misskeyandroidclient.api.users.*
import jp.panta.misskeyandroidclient.api.users.report.ReportDTO
import jp.panta.misskeyandroidclient.model.notes.NoteDataSourceAdder
import jp.panta.misskeyandroidclient.model.users.User
import jp.panta.misskeyandroidclient.model.users.UserNotFoundException
import jp.panta.misskeyandroidclient.model.users.UserRepository
import jp.panta.misskeyandroidclient.model.users.report.Report
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import retrofit2.Response

@Suppress("BlockingMethodInNonBlockingContext")
class UserRepositoryImpl(
    val miCore: MiCore
) : UserRepository{
    private val logger = miCore.loggerFactory.create("UserRepositoryImpl")
    private val noteDataSourceAdder = NoteDataSourceAdder(miCore.getUserDataSource(), miCore.getNoteDataSource(), miCore.getFilePropertyDataSource())

    override suspend fun find(userId: User.Id, detail: Boolean): User {
        val localResult = runCatching {
            miCore.getUserDataSource().get(userId).let{
                if(detail) {
                    it as? User.Detail
                }else it
            }
        }.onFailure {
            logger.debug("ローカルにユーザーは存在しませんでした。:$userId")
        }
        localResult.getOrNull()?.let{
            return it
        }

        val account = miCore.getAccount(userId.accountId)
        if(localResult.getOrNull() == null) {
            val res = miCore.getMisskeyAPIProvider().get(account).showUser(RequestUser(
                i = account.getI(miCore.getEncryption()),
                userId = userId.id,
                detail = true
            ))
            res.throwIfHasError()
            res.body()?.let{
                val user = it.toUser(account, true)
                it.pinnedNotes?.forEach { dto ->
                    noteDataSourceAdder.addNoteDtoToDataSource(account, dto)
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
        )
        res.throwIfHasError()

        res.body()?.let {
            it.pinnedNotes?.forEach { dto ->
                noteDataSourceAdder.addNoteDtoToDataSource(account, dto)
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
        val user = find(userId, true) as User.Detail
        val req = RequestUser(userId = userId.id, i = account.getI(miCore.getEncryption()))
        logger.debug("follow req:$req")
        val res = miCore.getMisskeyAPIProvider().get(account).followUser(req)
        res.throwIfHasError()
        if(res.isSuccessful) {
            val updated = (find(userId, true) as User.Detail).copy(
                isFollowing = if(user.isLocked) user.isFollowing else true,
                hasPendingFollowRequestFromYou = if(user.isLocked) true else user.hasPendingFollowRequestFromYou
            )
            miCore.getUserDataSource().add(updated)
        }
        return res.isSuccessful
    }

    override suspend fun unfollow(userId: User.Id): Boolean {
        val account = miCore.getAccountRepository().get(userId.accountId)
        val user = find(userId, true) as User.Detail


        val res = if(user.isLocked) {
            miCore.getMisskeyAPIProvider().get(account)
                .cancelFollowRequest(CancelFollow(userId = userId.id, i = account.getI(miCore.getEncryption())))
        }else{
            miCore.getMisskeyAPIProvider().get(account)
                .unFollowUser(RequestUser(userId = userId.id, i = account.getI(miCore.getEncryption())))
        }
        res.throwIfHasError()
        if(res.isSuccessful) {
            val updated = user.copy(
                isFollowing = if(user.isLocked) user.isFollowing else false,
                hasPendingFollowRequestFromYou = if(user.isLocked) false else user.hasPendingFollowRequestFromYou
            )
            miCore.getUserDataSource().add(updated)
        }
        return res.isSuccessful
    }

    override suspend fun acceptFollowRequest(userId: User.Id): Boolean {
        val account = miCore.getAccountRepository().get(userId.accountId)
        val user = find(userId, true) as User.Detail
        if(!user.hasPendingFollowRequestToYou) {
            return false
        }
        val res = miCore.getMisskeyAPIProvider().get(account)
            .acceptFollowRequest(AcceptFollowRequest(i = account.getI(miCore.getEncryption()), userId = userId.id))
            .throwIfHasError()
        if(res.isSuccessful) {
            miCore.getUserDataSource().add(user.copy(hasPendingFollowRequestToYou = false, isFollower = true))
        }
        return res.isSuccessful

    }

    override suspend fun rejectFollowRequest(userId: User.Id): Boolean {
        val account = miCore.getAccountRepository().get(userId.accountId)
        val user = find(userId, true) as User.Detail
        if(!user.hasPendingFollowRequestToYou) {
            return false
        }
        val res = miCore.getMisskeyAPIProvider().get(account).rejectFollowRequest(RejectFollowRequest(i = account.getI(miCore.getEncryption()), userId = userId.id))
            .throwIfHasError()
        if(res.isSuccessful) {
            miCore.getUserDataSource().add(user.copy(hasPendingFollowRequestToYou = false, isFollower = false))
        }
        return res.isSuccessful
    }

    private suspend fun action(requestAPI: suspend (RequestUser)-> Response<Unit>, userId: User.Id, reducer: (User.Detail)-> User.Detail): Boolean {
        val account = miCore.getAccountRepository().get(userId.accountId)
        val res = requestAPI.invoke(RequestUser(userId = userId.id, i = account.getI(miCore.getEncryption())))
        res.throwIfHasError()
        if(res.isSuccessful) {

            val updated = reducer.invoke(find(userId, true) as User.Detail)
            miCore.getUserDataSource().add(updated)
        }
        return res.isSuccessful
    }

    override suspend fun report(report: Report): Boolean {
        val account = miCore.getAccountRepository().get(report.userId.accountId)
        val api = report.userId.getMisskeyAPI()
        val res = api.report(ReportDTO(
            i = account.getI(miCore.getEncryption()),
            comment = report.comment,
            userId = report.userId.id
        ))
        res.throwIfHasError()
        return res.isSuccessful
    }

    private suspend fun User.Id.getMisskeyAPI(): MisskeyAPI {
        return miCore.getMisskeyAPIProvider().get(miCore.getAccountRepository().get(accountId))
    }
}