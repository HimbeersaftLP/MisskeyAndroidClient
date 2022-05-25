package jp.panta.misskeyandroidclient.ui.notification

import android.widget.TextView
import androidx.databinding.BindingAdapter
import jp.panta.misskeyandroidclient.R
import net.pantasystem.milktea.model.notification.Notification
import net.pantasystem.milktea.model.notification.UnknownNotification

object NotificationHelper {

    @JvmStatic
    @BindingAdapter("notification")
    fun TextView.setUnknownNotificationMessage(n: Notification) {

        if(n is UnknownNotification) {
            this.text = String.format(context.getString(R.string.unknown_notification_msg), n.id.notificationId, n.rawType, n.userId.id)
        }
    }
}