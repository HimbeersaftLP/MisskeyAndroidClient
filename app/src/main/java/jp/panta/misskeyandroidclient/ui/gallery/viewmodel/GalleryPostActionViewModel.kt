package jp.panta.misskeyandroidclient.ui.gallery.viewmodel

import androidx.lifecycle.ViewModel
import jp.panta.misskeyandroidclient.util.eventbus.EventBus


sealed class Action {
    object OpenCreationEditor : Action()
}

class GalleryPostActionViewModel : ViewModel() {

    val viewAction = EventBus<Action>()

    //val showGalleryEditor = _showGalleryEditor

    fun showEditor() {
        viewAction.event = Action.OpenCreationEditor

    }
}