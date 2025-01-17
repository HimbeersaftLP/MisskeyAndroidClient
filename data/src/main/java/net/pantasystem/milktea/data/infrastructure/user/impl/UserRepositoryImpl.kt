package net.pantasystem.milktea.data.infrastructure.user.impl


import net.pantasystem.milktea.common.Logger
import net.pantasystem.milktea.common.throwIfHasError
import net.pantasystem.milktea.api.misskey.MisskeyAPI
import net.pantasystem.milktea.api.misskey.users.RequestUser
import net.pantasystem.milktea.data.api.misskey.MisskeyAPIProvider
import net.pantasystem.milktea.api.misskey.users.*
import net.pantasystem.milktea.api.misskey.users.report.ReportDTO
import net.pantasystem.milktea.common.Encryption
import net.pantasystem.milktea.model.notes.NoteDataSource
import net.pantasystem.milktea.data.infrastructure.notes.NoteDataSourceAdder
import net.pantasystem.milktea.data.infrastructure.toUser
import net.pantasystem.milktea.model.account.AccountRepository
import net.pantasystem.milktea.model.drive.FilePropertyDataSource
import net.pantasystem.milktea.model.user.User
import net.pantasystem.milktea.model.user.UserDataSource
import net.pantasystem.milktea.model.user.UserNotFoundException
import net.pantasystem.milktea.model.user.UserRepository
import net.pantasystem.milktea.model.user.report.Report
import retrofit2.Response
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class UserRepositoryImpl @Inject constructor(
    val userDataSource: UserDataSource,
    val noteDataSource: NoteDataSource,
    val filePropertyDataSource: FilePropertyDataSource,
    val accountRepository: AccountRepository,
    val misskeyAPIProvider: MisskeyAPIProvider,
    val encryption: Encryption,
    val loggerFactory: Logger.Factory,
) : UserRepository {
    private val logger: Logger by lazy {
        loggerFactory.create("UserRepositoryImpl")
    }
    private val noteDataSourceAdder = NoteDataSourceAdder(userDataSource, noteDataSource, filePropertyDataSource)

    override suspend fun find(userId: User.Id, detail: Boolean): User {
        val localResult = runCatching {
            userDataSource.get(userId).let{
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

        val account = accountRepository.get(userId.accountId)
        if(localResult.getOrNull() == null) {
            val res = misskeyAPIProvider.get(account).showUser(
                RequestUser(
                i = account.getI(encryption),
                userId = userId.id,
                detail = true
            )
            )
            res.throwIfHasError()
            res.body()?.let{
                val user = it.toUser(account, true)
                it.pinnedNotes?.forEach { dto ->
                    noteDataSourceAdder.addNoteDtoToDataSource(account, dto)
                }
                val result = userDataSource.add(user)
                logger.debug("add result: $result")
                return userDataSource.get(userId)
            }
        }

        throw UserNotFoundException(userId)
    }

    override suspend fun findByUserName(accountId: Long, userName: String, host: String?, detail: Boolean): User {
        val local = runCatching {
            userDataSource.get(accountId, userName, host).let{
                if(detail) {
                    it as? User.Detail
                }else it
            }
        }.getOrNull()

        logger.debug("local:$local")
        if(local != null) {
            return local
        }
        val account = accountRepository.get(accountId)
        val misskeyAPI = misskeyAPIProvider.get(account.instanceDomain)
        val res = misskeyAPI.showUser(
            RequestUser(
                i = account.getI(encryption),
                userName = userName,
                host = host,
                detail = detail
            )
        )
        logger.debug("res:$res")
        res.throwIfHasError()

        res.body()?.let {
            it.pinnedNotes?.forEach { dto ->
                noteDataSourceAdder.addNoteDtoToDataSource(account, dto)
            }
            val user = it.toUser(account, detail)
            userDataSource.add(user)
            return userDataSource.get(user.id)
        }

        throw UserNotFoundException(
            null,
            userName = userName,
            host = host
        )

    }

    override suspend fun searchByName(accountId: Long, name: String): List<User> {
        return userDataSource.all().asSequence().filter {
            it.id.accountId == accountId
        }.filter {
            it.displayName.startsWith(name)
        }.toList()
    }

    override suspend fun searchByUserName(
        accountId: Long,
        userName: String,
        host: String?
    ): List<User> {
        val ac = accountRepository.get(accountId)
        val i = ac.getI(encryption)
        val api = misskeyAPIProvider.get(ac)

        val results = SearchByUserAndHost(api)
            .search(
                RequestUser(
                userName = userName,
                host = host,
                i = i
            )
            )
            .throwIfHasError()

        return results.body()!!.map {
            it.toUser(ac, false).also { u ->
                userDataSource.add(u)
                userDataSource.get(u.id)
            }
        }
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
        val account = accountRepository.get(userId.accountId)
        val user = find(userId, true) as User.Detail
        val req = RequestUser(userId = userId.id, i = account.getI(encryption))
        logger.debug("follow req:$req")
        val res = misskeyAPIProvider.get(account).followUser(req)
        res.throwIfHasError()
        if(res.isSuccessful) {
            val updated = (find(userId, true) as User.Detail).copy(
                isFollowing = if(user.isLocked) user.isFollowing else true,
                hasPendingFollowRequestFromYou = if(user.isLocked) true else user.hasPendingFollowRequestFromYou
            )
            userDataSource.add(updated)
        }
        return res.isSuccessful
    }

    override suspend fun unfollow(userId: User.Id): Boolean {
        val account = accountRepository.get(userId.accountId)
        val user = find(userId, true) as User.Detail


        val res = if(user.isLocked) {
            misskeyAPIProvider.get(account)
                .cancelFollowRequest(CancelFollow(userId = userId.id, i = account.getI(encryption)))
        }else{
            misskeyAPIProvider.get(account)
                .unFollowUser(RequestUser(userId = userId.id, i = account.getI(encryption)))
        }
        res.throwIfHasError()
        if(res.isSuccessful) {
            val updated = user.copy(
                isFollowing = if(user.isLocked) user.isFollowing else false,
                hasPendingFollowRequestFromYou = if(user.isLocked) false else user.hasPendingFollowRequestFromYou
            )
            userDataSource.add(updated)
        }
        return res.isSuccessful
    }

    override suspend fun acceptFollowRequest(userId: User.Id): Boolean {
        val account = accountRepository.get(userId.accountId)
        val user = find(userId, true) as User.Detail
        if(!user.hasPendingFollowRequestToYou) {
            return false
        }
        val res = misskeyAPIProvider.get(account)
            .acceptFollowRequest(AcceptFollowRequest(i = account.getI(encryption), userId = userId.id))
            .throwIfHasError()
        if(res.isSuccessful) {
            userDataSource.add(user.copy(hasPendingFollowRequestToYou = false, isFollower = true))
        }
        return res.isSuccessful

    }

    override suspend fun rejectFollowRequest(userId: User.Id): Boolean {
        val account = accountRepository.get(userId.accountId)
        val user = find(userId, true) as User.Detail
        if(!user.hasPendingFollowRequestToYou) {
            return false
        }
        val res = misskeyAPIProvider.get(account).rejectFollowRequest(RejectFollowRequest(i = account.getI(encryption), userId = userId.id))
            .throwIfHasError()
        if(res.isSuccessful) {
            userDataSource.add(user.copy(hasPendingFollowRequestToYou = false, isFollower = false))
        }
        return res.isSuccessful
    }

    private suspend fun action(requestAPI: suspend (RequestUser)-> Response<Unit>, userId: User.Id, reducer: (User.Detail)-> User.Detail): Boolean {
        val account = accountRepository.get(userId.accountId)
        val res = requestAPI.invoke(RequestUser(userId = userId.id, i = account.getI(encryption)))
        res.throwIfHasError()
        if(res.isSuccessful) {

            val updated = reducer.invoke(find(userId, true) as User.Detail)
            userDataSource.add(updated)
        }
        return res.isSuccessful
    }

    override suspend fun report(report: Report): Boolean {
        val account = accountRepository.get(report.userId.accountId)
        val api = report.userId.getMisskeyAPI()
        val res = api.report(
            ReportDTO(
            i = account.getI(encryption),
            comment = report.comment,
            userId = report.userId.id
        )
        )
        res.throwIfHasError()
        return res.isSuccessful
    }

    private suspend fun User.Id.getMisskeyAPI(): MisskeyAPI {
        return misskeyAPIProvider.get(accountRepository.get(accountId))
    }
}