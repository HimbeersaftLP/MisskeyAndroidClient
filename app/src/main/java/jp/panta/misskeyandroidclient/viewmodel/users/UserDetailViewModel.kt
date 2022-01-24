package jp.panta.misskeyandroidclient.viewmodel.users

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.panta.misskeyandroidclient.model.account.Account
import jp.panta.misskeyandroidclient.model.notes.NoteTranslationStore
import jp.panta.misskeyandroidclient.model.users.User
import jp.panta.misskeyandroidclient.model.users.UserDataSource
import jp.panta.misskeyandroidclient.util.eventbus.EventBus
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import jp.panta.misskeyandroidclient.viewmodel.notes.DetermineTextLengthSettingStore
import jp.panta.misskeyandroidclient.viewmodel.notes.PlaneNoteViewData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class UserDetailViewModel(
    val userId: User.Id?,
    private val fqdnUserName: String?,
    val miCore: MiCore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val translationStore: NoteTranslationStore
) : ViewModel() {
    private val logger = miCore.loggerFactory.create("UserDetailViewModel")

    val user = MutableLiveData<User.Detail>()
    private val userState = MutableStateFlow<User.Detail?>(null).apply {
        filterNotNull().onEach {
            user.postValue(it)
        }.launchIn(viewModelScope)
    }


    private val pinNotesState = userState.filterNotNull().map {
        it.pinnedNoteIds?.map { id ->
            miCore.getNoteDataSource().get(id)
        } ?: emptyList()
    }

    val pinNotes = MediatorLiveData<List<PlaneNoteViewData>>().apply {

        pinNotesState.map { notes ->
            notes.map { note ->
                PlaneNoteViewData(
                    miCore.getGetters().noteRelationGetter.get(note),
                    getAccount(),
                    DetermineTextLengthSettingStore(miCore.getSettingStore()),
                    miCore.getNoteCaptureAdapter(),
                    translationStore
                )
            }
        }.onEach {
            this.postValue(it)

        }.flatMapLatest {
            it.map { n ->
                n.eventFlow
            }.merge()
        }.catch { e ->
            logger.warning("", e = e)
        }.launchIn(viewModelScope + dispatcher)
    }

    val isMine = MutableLiveData<Boolean>()

    val isFollowing = MediatorLiveData<Boolean>().apply {
        addSource(user) {
            this.value = it.isFollowing
        }
    }


    val userName = MediatorLiveData<String>().apply {
        addSource(user) { user ->
            user.getDisplayUserName()
        }
    }

    val isBlocking = MediatorLiveData<Boolean>().apply {
        value = user.value?.isBlocking ?: false
        addSource(user) {
            value = it.isBlocking
        }
    }

    val isMuted = MediatorLiveData<Boolean>().apply {
        value = user.value?.isMuting ?: false
        addSource(user) {
            value = it.isMuting
        }
    }

    val isRemoteUser = MediatorLiveData<Boolean>().apply {
        addSource(user) {
            value = it.url != null
        }
    }
    val showFollowers = EventBus<User?>()
    val showFollows = EventBus<User?>()


    fun load() {
        viewModelScope.launch(dispatcher) {
            var user = userId?.let {
                runCatching {
                    miCore.getUserRepository().find(userId, true)
                }.getOrNull()
            }
            if (user == null) {
                user = fqdnUserName?.let {
                    val account = getAccount()
                    val userNameAndHost = fqdnUserName.split("@").filter { it.isNotBlank() }
                    val userName = userNameAndHost[0]
                    val host = userNameAndHost.getOrNull(1)
                    miCore.getUserRepository().findByUserName(account.accountId, userName, host)
                }
            }


            userState.value = user as? User.Detail
            user?.let {
                miCore.getUserRepositoryEventToFlow().from(user.id).mapNotNull {
                    (it as? UserDataSource.Event.Updated)?.user
                        ?: (it as? UserDataSource.Event.Created)?.user
                }.mapNotNull {
                    it as? User.Detail
                }.onEach {
                    userState.value = it
                }.launchIn(viewModelScope + Dispatchers.IO)
            }

        }

    }

    fun changeFollow() {
        viewModelScope.launch(Dispatchers.IO) {
            userState.value?.let {
                runCatching {
                    val user = miCore.getUserRepository().find(it.id) as User.Detail
                    if (user.isFollowing || user.hasPendingFollowRequestFromYou) {
                        miCore.getUserRepository().unfollow(user.id)
                    } else {
                        miCore.getUserRepository().follow(user.id)
                    }
                    miCore.getUserRepository().find(user.id) as User.Detail
                }.onSuccess {
                    userState.value = it
                }.onFailure {
                    logger.error("unmute", e = it)
                }
            }

        }
    }

    fun showFollows() {
        showFollows.event = user.value
    }

    fun showFollowers() {
        showFollowers.event = user.value
    }

    fun mute() {
        viewModelScope.launch(Dispatchers.IO) {
            userState.value?.let {
                runCatching {
                    miCore.getUserRepository().mute(it.id)
                    miCore.getUserRepository().find(it.id, true) as User.Detail
                }.onSuccess {
                    userState.value = it
                }.onFailure {
                    logger.error("unmute", e = it)
                }
            }
        }
    }

    fun unmute() {
        viewModelScope.launch(Dispatchers.IO) {
            userState.value?.let {
                runCatching {
                    miCore.getUserRepository().unmute(it.id)
                    miCore.getUserRepository().find(it.id, true) as User.Detail
                }.onSuccess {
                    userState.value = it
                }.onFailure {
                    logger.error("unmute", e = it)
                }
            }
        }
    }

    fun block() {
        viewModelScope.launch(Dispatchers.IO) {
            userState.value?.let {
                runCatching {
                    miCore.getUserRepository().block(it.id)
                    (miCore.getUserRepository().find(it.id, true) as User.Detail)
                }.onSuccess {
                    userState.value = it
                }
            }
        }
    }

    fun unblock() {
        viewModelScope.launch(Dispatchers.IO) {
            userState.value?.let {
                runCatching {
                    miCore.getUserRepository().unblock(it.id)
                    (miCore.getUserRepository().find(it.id, true) as User.Detail)
                }.onSuccess {
                    userState.value = it
                }
            }
        }
    }

    fun getProfileUrl(): String? {
        return mAc?.let {
            return user.value?.getProfileUrl(it)
        }
    }


    private var mAc: Account? = null
    private suspend fun getAccount(): Account {
        if (mAc != null) {
            return mAc!!
        }
        if (userId != null) {
            mAc = miCore.getAccountRepository().get(userId.accountId)
            return mAc!!
        }

        mAc = miCore.getAccountRepository().getCurrentAccount()
        return mAc!!
    }

}