package jp.panta.misskeyandroidclient.ui.settings.viewmodel.reaction

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import jp.panta.misskeyandroidclient.MiApplication
import jp.panta.misskeyandroidclient.util.eventbus.EventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.pantasystem.milktea.data.infrastructure.settings.SettingStore
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.model.notes.reaction.LegacyReaction
import net.pantasystem.milktea.model.notes.reaction.ReactionSelection
import net.pantasystem.milktea.model.notes.reaction.usercustom.ReactionUserSetting
import net.pantasystem.milktea.model.notes.reaction.usercustom.ReactionUserSettingDao
import net.pantasystem.milktea.model.setting.ReactionPickerType

class ReactionPickerSettingViewModel(
    private val account: Account,
    private val reactionUserSettingDao: ReactionUserSettingDao,
    private val settingStore: SettingStore,
) : ViewModel(), ReactionSelection {

    @Suppress("UNCHECKED_CAST")
    class Factory(private val ar: Account, val miApplication: MiApplication) : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val settingStore = miApplication.getSettingStore()
            return ReactionPickerSettingViewModel(ar, miApplication.reactionUserSettingDao, settingStore) as T
        }
    }

    var reactionPickerType = settingStore.reactionPickerType
        private set
    val reactionSettingsList = MutableLiveData<List<ReactionUserSetting>>()
    val reactionSelectEvent = EventBus<ReactionUserSetting>()

    private var mExistingSettingList: List<ReactionUserSetting>? = null
    private val mReactionSettingReactionNameMap = LinkedHashMap<String, ReactionUserSetting>()

    init{
        loadSetReactions()
    }

    private fun loadSetReactions(){
        viewModelScope.launch(Dispatchers.IO){
            try{
                val rawSettings = reactionUserSettingDao
                    .findByInstanceDomain(account.instanceDomain)
                mExistingSettingList = rawSettings?: emptyList()
                var settingReactions = rawSettings
                    ?: LegacyReaction.defaultReaction.mapIndexed(::toReactionUserSettingFromTextTypeReaction)
                if(settingReactions.isEmpty()){
                    settingReactions = LegacyReaction.defaultReaction.mapIndexed(::toReactionUserSettingFromTextTypeReaction)
                }
                mReactionSettingReactionNameMap.clear()
                mReactionSettingReactionNameMap.putAll(settingReactions.map{
                    it.reaction to it
                })
                reactionSettingsList.postValue(settingReactions)


            }catch(e: Exception){
                Log.e("ReactionPickerSettingVM", "load set reaction error", e)
            }
        }
    }

    fun save(){
        val userSettings = this.reactionSettingsList.value
        userSettings?.forEachIndexed { index, reactionUserSetting ->
            reactionUserSetting.weight = index
        }
        val ex = mExistingSettingList?: emptyList()
        val removed = ex.filter{ out ->
            userSettings?.any{ inner ->
                out.instanceDomain == inner.instanceDomain && out.reaction == inner.reaction
            } == false
        }
        viewModelScope.launch(Dispatchers.IO){
            try{
                reactionUserSettingDao.deleteAll(removed)
                userSettings?.let{
                    reactionUserSettingDao.insertAll(userSettings)
                }
            }catch(e: Exception){
                Log.e("ReactionPickerSettingVM", "save error", e)
            }
        }
    }

    // delete reaction
    override fun selectReaction(reaction: String) {
        mReactionSettingReactionNameMap[reaction]?.let{
            reactionSelectEvent.event = it
        }
    }

    fun deleteReaction(reaction: String) {
        mReactionSettingReactionNameMap.remove(reaction)
        reactionSettingsList.postValue(
            mReactionSettingReactionNameMap.values.toList()
        )
    }

    fun addReaction(reaction: String){
        mReactionSettingReactionNameMap[reaction] =
            ReactionUserSetting(
                reaction,
                account.instanceDomain,
                mReactionSettingReactionNameMap.size
            )
        reactionSettingsList.postValue(mReactionSettingReactionNameMap.values.toList())
    }

    fun putSortedList(list: List<ReactionUserSetting>){
        mReactionSettingReactionNameMap.clear()
        mReactionSettingReactionNameMap.putAll(list.map{
            it.reaction to it
        })
        reactionSettingsList.postValue(mReactionSettingReactionNameMap.values.toList())
    }

    fun setReactionPickerType(type: ReactionPickerType){
        settingStore.reactionPickerType = type
        reactionPickerType = type
    }

    private fun toReactionUserSettingFromTextTypeReaction(index: Int, reaction: String): ReactionUserSetting {
        return ReactionUserSetting(
            reaction,
            account.instanceDomain,
            index
        )
    }
}