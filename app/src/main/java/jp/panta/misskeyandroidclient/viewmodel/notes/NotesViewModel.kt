package jp.panta.misskeyandroidclient.viewmodel.notes

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.model.api.MisskeyAPI
import jp.panta.misskeyandroidclient.model.auth.ConnectionInstance
import jp.panta.misskeyandroidclient.model.notes.CreateNote
import jp.panta.misskeyandroidclient.model.notes.CreateReaction
import jp.panta.misskeyandroidclient.model.notes.DeleteNote
import jp.panta.misskeyandroidclient.model.notes.Note
import jp.panta.misskeyandroidclient.model.users.User
import jp.panta.misskeyandroidclient.util.eventbus.EventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotesViewModel(
    private val connectionInstance: ConnectionInstance,
    private val misskeyAPI: MisskeyAPI
) : ViewModel(){

    val statusMessage = EventBus<String>()

    val errorStatusMessage = EventBus<String>()

    val reNoteTarget = EventBus<PlaneNoteViewData>()

    val quoteRenoteTarget = EventBus<PlaneNoteViewData>()

    val replyTarget = EventBus<PlaneNoteViewData>()

    val reactionTarget = EventBus<PlaneNoteViewData>()

    val submittedNotesOnReaction = EventBus<PlaneNoteViewData>()

    val shareTarget = EventBus<PlaneNoteViewData>()

    val targetUser = EventBus<User>()


    fun setTargetToReNote(note: PlaneNoteViewData){
        //reNoteTarget.postValue(note)
        Log.d("NotesViewModel", "登録しました: $note")
        reNoteTarget.event = note
    }

    fun setTargetToReply(note: PlaneNoteViewData){
        replyTarget.event = note
    }

    fun setTargetToShare(note: PlaneNoteViewData){
        shareTarget.event = note
    }

    fun setTargetToUser(user: User){
        targetUser.event = user
    }

    fun postRenote(){
        val renoteId = reNoteTarget.event?.toShowNote?.id
        if(renoteId != null){
            val request = CreateNote(i = connectionInstance.getI()!!, text = null, renoteId = renoteId)
            misskeyAPI.create(request).enqueue(object : Callback<Note?>{
                override fun onResponse(call: Call<Note?>, response: Response<Note?>) {
                    statusMessage.event = "renoteしました"
                }

                override fun onFailure(call: Call<Note?>, t: Throwable) {
                    errorStatusMessage.event = "renote失敗しました"
                }
            })
        }
    }

    fun putQuoteRenoteTarget(){
        quoteRenoteTarget.event = reNoteTarget.event
    }

    //直接送信
    fun postReaction(planeNoteViewData: PlaneNoteViewData, reaction: String){
        val myReaction = planeNoteViewData.myReaction.value

        viewModelScope.launch(Dispatchers.IO){
            //リアクション解除処理をする
            submittedNotesOnReaction.event = planeNoteViewData
            Log.d("NotesViewModel", "postReaction(n, n)")
            try{
                if(myReaction != null){
                    misskeyAPI.deleteReaction(
                        DeleteNote(
                            i = connectionInstance.getI()!!,
                            noteId = planeNoteViewData.toShowNote.id
                        )
                    ).execute()

                }
                misskeyAPI.createReaction(CreateReaction(
                    i = connectionInstance.getI()!!,
                    reaction = reaction,
                    noteId = planeNoteViewData.toShowNote.id
                )).execute()
            }catch(e: Exception){
                Log.e("NotesViewModel", "postReaction error", e)
            }

        }
    }

    fun setTargetToReaction(planeNoteViewData: PlaneNoteViewData){
        reactionTarget.event = planeNoteViewData
    }

    //setTargetToReactionが呼び出されている必要がある
    fun postReaction(reaction: String){
        val targetNote =  reactionTarget.event
        if(targetNote != null){
            postReaction(targetNote, reaction)
        }
    }
}