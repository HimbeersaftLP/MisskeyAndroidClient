package jp.panta.misskeyandroidclient.viewmodel.notes.detail

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.panta.misskeyandroidclient.model.Encryption
import jp.panta.misskeyandroidclient.model.Page
import jp.panta.misskeyandroidclient.model.account.Account
import jp.panta.misskeyandroidclient.model.account.page.Pageable
import jp.panta.misskeyandroidclient.model.api.MisskeyAPI
import jp.panta.misskeyandroidclient.model.core.AccountRelation
import jp.panta.misskeyandroidclient.model.core.EncryptedConnectionInformation
import jp.panta.misskeyandroidclient.model.notes.Note
import jp.panta.misskeyandroidclient.model.notes.NoteRequest
import jp.panta.misskeyandroidclient.model.streming.NoteCapture
import jp.panta.misskeyandroidclient.model.streming.StreamingAdapter
import jp.panta.misskeyandroidclient.model.streming.note.NoteRegister
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import jp.panta.misskeyandroidclient.viewmodel.notes.DetermineTextLengthSettingStore
import jp.panta.misskeyandroidclient.viewmodel.notes.PlaneNoteViewData
import jp.panta.misskeyandroidclient.viewmodel.url.UrlPreviewLoadTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class NoteDetailViewModel(
    val account: Account,
    val show: Pageable.Show,
    val miCore: MiCore,
    val encryption: Encryption = miCore.getEncryption()
) : ViewModel(){

    private val request = NoteRequest(
        i = account.getI(encryption),
        noteId = show.noteId
    )

    private val misskeyAPI: MisskeyAPI = miCore.getMisskeyAPI(account)
    //private val streamingAdapter = StreamingAdapter(connectionInformation, encryption)


    private val noteCapture = miCore.getNoteCapture(account)
    private var noteRegister = NoteRegister()

    val notes = object : MutableLiveData<List<PlaneNoteViewData>>(){
        override fun onActive() {
            super.onActive()

            startStreaming()
        }

        override fun onInactive() {
            super.onInactive()

            stopStreaming()
        }
    }

    private val mNoteDetailId = UUID.randomUUID().toString()

    private fun startStreaming(){

        //noteCapture.attach(noteRegister)

    }

    private fun stopStreaming(){
        //noteCapture.detach(noteRegister)

    }


    fun loadDetail(){

        viewModelScope.launch(Dispatchers.IO){
            try{
                val rawDetail = misskeyAPI.showNote(request).execute().body()
                    ?:return@launch
                val detail = NoteDetailViewData(rawDetail, account, DetermineTextLengthSettingStore(miCore.getSettingStore()))
                loadUrlPreview(detail)
                var list: List<PlaneNoteViewData> = listOf(detail)
                notes.postValue(list)

                val conversation = loadConversation()?.asReversed()
                if(conversation != null){
                    list = ArrayList<PlaneNoteViewData>(conversation).apply{
                        addAll(list)
                    }
                    notes.postValue(list)
                }
                val children = loadChildren()
                if(children != null){
                    list = ArrayList<PlaneNoteViewData>(list).apply{
                        addAll(children)
                    }
                    notes.postValue(list)
                }
                //noteCapture.subscribeAll(noteRegister.registerId, list)

            }catch (e: Exception){

            }
        }

    }


    fun loadNewConversation(noteConversationViewData: NoteConversationViewData){
        Log.d("NoteDetailViewModel", "新たにConversationを読み込もうとした")
        viewModelScope.launch(Dispatchers.IO){
            try{
                val conversation = noteConversationViewData.conversation.value
                    ?: emptyList()
                getChildrenToIterate(noteConversationViewData, ArrayList(conversation))
            }catch(e: Exception){
                Log.e("NoteDetailViewModel", "loadNewConversation中にエラー発生", e)
            }
        }
    }

    private fun getChildrenToIterate(
        noteConversationViewData: NoteConversationViewData,
        conversation: ArrayList<PlaneNoteViewData>
    ): NoteConversationViewData{
        val next = noteConversationViewData.getNextNoteForConversation()
        println("ねくすと: ${next?.id}, :${next?.text}")
        return if(next == null){
            noteConversationViewData.conversation.postValue(conversation)
            noteConversationViewData.hasConversation.postValue(false)
            println("こんばーせーしょんの最終さいずは:${conversation.size}")
            noteConversationViewData
        }else{
            conversation.add(next)
            val children = misskeyAPI.children(NoteRequest(account.getI(encryption), limit = 100,noteId =  next.toShowNote.id)).execute().body()?.map{
                PlaneNoteViewData(it, account, DetermineTextLengthSettingStore(miCore.getSettingStore())).apply{
                    loadUrlPreview(this)
                }
            }
            noteConversationViewData.nextChildren = children
            getChildrenToIterate(noteConversationViewData, conversation)
        }
    }


    private fun loadConversation(): List<PlaneNoteViewData>?{
        return misskeyAPI.conversation(request).execute().body()?.map{
            PlaneNoteViewData(it, account, DetermineTextLengthSettingStore(miCore.getSettingStore())).apply{
                loadUrlPreview(this)
            }
        }
    }

    private fun loadChildren(): List<NoteConversationViewData>?{
        return loadChildren(id = show.noteId)?.filter{
            it.reNote?.id != show.noteId
        }?.map{
            val planeNoteViewData = PlaneNoteViewData(it, account, DetermineTextLengthSettingStore(miCore.getSettingStore()))
            val childInChild = loadChildren(planeNoteViewData.toShowNote.id)?.map{n ->
                PlaneNoteViewData(n, account, DetermineTextLengthSettingStore(miCore.getSettingStore())).apply{
                    loadUrlPreview(this)
                }
            }
            NoteConversationViewData(it, childInChild, account, DetermineTextLengthSettingStore(miCore.getSettingStore())).apply{
                this.hasConversation.postValue(this.getNextNoteForConversation() != null)
            }
        }

    }

    private fun loadChildren(id: String): List<Note>?{
        return misskeyAPI.children(NoteRequest(i = account.getI(encryption), limit = 100, noteId = id)).execute().body()
    }

    private fun loadUrlPreview(planeNoteViewData: PlaneNoteViewData){
        UrlPreviewLoadTask(
            miCore.getUrlPreviewStore(account),
            planeNoteViewData.urls,
            viewModelScope
        ).load(planeNoteViewData.urlPreviewLoadTaskCallback)
    }


}