package jp.panta.misskeyandroidclient.viewmodel.list

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import jp.panta.misskeyandroidclient.api.list.CreateList
import jp.panta.misskeyandroidclient.api.list.ListId
import jp.panta.misskeyandroidclient.api.throwIfHasError
import jp.panta.misskeyandroidclient.model.I
import jp.panta.misskeyandroidclient.model.account.page.Page
import jp.panta.misskeyandroidclient.model.account.page.Pageable
import jp.panta.misskeyandroidclient.model.list.UserList
import jp.panta.misskeyandroidclient.util.eventbus.EventBus
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.Set
import kotlin.collections.any
import kotlin.collections.emptyList
import kotlin.collections.emptyMap
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.toMap
import kotlin.collections.toSet

@ExperimentalCoroutinesApi
class ListListViewModel(
    val miCore: MiCore
) : ViewModel(){

    @Suppress("UNCHECKED_CAST")
    class Factory(val miCore: MiCore) : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ListListViewModel( miCore) as T
        }
    }

    private val mUserListListFlow = MutableStateFlow<List<UserList>>(emptyList())

    val encryption = miCore.getEncryption()
    val userListList = MediatorLiveData<List<UserList>>().apply{
        viewModelScope.launch(Dispatchers.IO) {
            mUserListListFlow.collect {
                postValue(it)
            }
        }
    }


    val pagedUserList = MediatorLiveData<Set<UserList>>().apply{
        miCore.getCurrentAccount().filterNotNull().map { account ->
            loadListList(account.accountId).filter {  ul ->
                account.pages.any{ page ->
                    page.pageParams.listId == ul.id.userListId
                }
            }
        }.onEach {  list ->
            postValue(list.toSet())
        }.launchIn(viewModelScope + Dispatchers.IO)

        miCore.getCurrentAccount().filterNotNull().onEach {
            fetch()
        }.launchIn(viewModelScope + Dispatchers.IO)

    }

    private val mUserListIdMap = LinkedHashMap<UserList.Id, UserList>()


    val showUserDetailEvent = EventBus<UserList>()

    private val logger = miCore.loggerFactory.create("ListListViewModel")


    fun fetch() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val account = miCore.getAccountRepository().getCurrentAccount()
                loadListList(account.accountId)
            }.onSuccess {
                mUserListListFlow.value = it
            }.onFailure {
                logger.error("fetch error", e = it)
            }

        }
    }


    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun loadListList(accountId: Long): List<UserList>{
        val account = miCore.getAccountRepository().get(accountId)
        val i = account.getI(encryption)
        val res = miCore.getMisskeyAPIProvider().get(account).userList(I(i))
        res.throwIfHasError()

        val userListMap = res.body()?.map {
            it.toEntity(account)
        }?.map{
            it.id to it
        }?.toMap()?: emptyMap()
        mUserListIdMap.clear()
        mUserListIdMap.putAll(userListMap)
        return mUserListIdMap.values.toList()
    }

    /**
     * 他Activityで変更を加える場合onActivityResultで呼び出し変更を適応する
     */
    fun onUserListUpdated(userList: UserList?){
        userList?: return
        mUserListIdMap[userList.id] = userList
        mUserListListFlow.value = mUserListIdMap.values.toList()
    }

    /**
     * 他Activity等でUserListを正常に作成できた場合onActivityResultで呼び出し変更を適応する
     */
    private fun onUserListCreated(userList: UserList){

        mUserListIdMap[userList.id] = userList
        mUserListListFlow.value = mUserListIdMap.values.toList()


    }



    fun showUserListDetail(userList: UserList?){
        userList?.let{ ul ->
            showUserDetailEvent.event = ul
        }
    }

    fun toggleTab(userList: UserList?){
        userList?.let{ ul ->
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    val account = miCore.getAccountRepository().get(userList.id.accountId)
                    val exPage = account.pages.firstOrNull {
                        val pageable = it.pageable()
                        if(pageable is Pageable.UserListTimeline){
                            pageable.listId == ul.id.userListId
                        }else{
                            false
                        }
                    }
                    if(exPage == null){
                        val page = Page(account.accountId, ul.name, pageable =  Pageable.UserListTimeline(ul.id.userListId), weight = 0)
                        miCore.addPageInCurrentAccount(page)
                    }
                }
            }
        }
    }

    fun delete(userList: UserList?){
        userList?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val account = miCore.getAccountRepository().get(userList.id.accountId)
                val misskeyAPI = miCore.getMisskeyAPIProvider().get(account)
                val res = misskeyAPI.deleteList(
                    ListId(
                        i = account.getI(miCore.getEncryption()),
                        listId = userList.id.userListId
                    )
                )
                res.throwIfHasError()

            }.onSuccess {
                mUserListIdMap.remove(userList.id)
                mUserListListFlow.value = mUserListIdMap.values.toList()

            }
        }

    }

    fun createUserList(name: String){
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val account = miCore.getAccountRepository().getCurrentAccount()
                val res = miCore.getMisskeyAPIProvider().get(account).createList(CreateList(i = account.getI(miCore.getEncryption()), name))
                res.throwIfHasError()
                res.body()?.toEntity(account)
                    ?: throw IllegalStateException()
            }.onSuccess {
                onUserListCreated(it)
            }
        }

    }


}