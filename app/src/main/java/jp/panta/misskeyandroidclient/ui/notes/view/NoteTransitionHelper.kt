package jp.panta.misskeyandroidclient.ui.notes.view

import android.app.Activity
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.databinding.BindingAdapter
import jp.panta.misskeyandroidclient.Activities
import jp.panta.misskeyandroidclient.NoteDetailActivity
import net.pantasystem.milktea.model.notes.Note
import jp.panta.misskeyandroidclient.putActivity

object NoteTransitionHelper {

    @JvmStatic
    @BindingAdapter("clickedView", "transitionDestinationNote")
    fun View.transitionNoteDetail(clickedView: View?, transitionDestinationNote: Note?){
        transitionDestinationNote?: return
        val clicked = clickedView?: this
        clicked.setOnClickListener {
            val context = this.context
            val intent = NoteDetailActivity.newIntent(context, transitionDestinationNote.id)
            intent.putActivity(Activities.ACTIVITY_IN_APP)

            if(context is Activity){
                val compat = ActivityOptionsCompat.makeSceneTransitionAnimation(context, this, "note")
                context.startActivity(intent, compat.toBundle())
            }else{
                context.startActivity(intent)
            }
        }

    }
}