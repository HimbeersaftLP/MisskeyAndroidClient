package jp.panta.misskeyandroidclient.gettters

import jp.panta.misskeyandroidclient.api.misskey.notification.NotificationDTO
import jp.panta.misskeyandroidclient.api.misskey.users.toUser
import jp.panta.misskeyandroidclient.model.account.Account
import jp.panta.misskeyandroidclient.model.notes.NoteDataSourceAdder
import jp.panta.misskeyandroidclient.model.notification.HasNote
import jp.panta.misskeyandroidclient.model.notification.Notification
import jp.panta.misskeyandroidclient.model.notification.NotificationRelation
import jp.panta.misskeyandroidclient.model.notification.NotificationDataSource
import jp.panta.misskeyandroidclient.model.users.UserDataSource

class NotificationRelationGetter(
    private val userDataSource: UserDataSource,
    private val notificationDataSource: NotificationDataSource,
    private val noteRelationGetter: NoteRelationGetter,
    private val noteDataSourceAdder: NoteDataSourceAdder
) {


    suspend fun get(account: Account, notificationDTO: NotificationDTO): NotificationRelation {
        val user = notificationDTO.user.toUser(account, false)
        userDataSource.add(user)
        val noteRelation = notificationDTO.note?.let{
            noteRelationGetter.get(noteDataSourceAdder.addNoteDtoToDataSource(account, it))
        }
        val notification = notificationDTO.toNotification(account)
        notificationDataSource.add(notification)
        return NotificationRelation(
            notification,
            user,
            noteRelation
        )
    }

    suspend fun get(notificationId: Notification.Id): NotificationRelation {
        val notification = notificationDataSource.get(notificationId)
        val user = userDataSource.get(notification.userId)
        val noteRelation = (notification as? HasNote)?.let{
            noteRelationGetter.get(it.noteId)
        }
        return NotificationRelation(notification, user, noteRelation)
    }

    suspend fun get(accountId: Long, notificationId: String): NotificationRelation {
        return get(Notification.Id(accountId, notificationId))
    }

}