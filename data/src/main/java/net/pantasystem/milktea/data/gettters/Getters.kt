package net.pantasystem.milktea.data.gettters

import net.pantasystem.milktea.data.model.drive.FilePropertyDataSource
import net.pantasystem.milktea.data.model.group.GroupDataSource
import net.pantasystem.milktea.data.model.messaging.impl.MessageDataSource
import net.pantasystem.milktea.data.model.notes.NoteDataSource
import net.pantasystem.milktea.data.model.notes.NoteDataSourceAdder
import net.pantasystem.milktea.data.model.notes.NoteRepository
import net.pantasystem.milktea.data.model.notification.NotificationDataSource
import net.pantasystem.milktea.data.model.users.UserDataSource

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Getters @Inject constructor(
    noteDataSource: NoteDataSource,
    noteRepository: NoteRepository,
    userDataSource: UserDataSource,
    filePropertyDataSource: FilePropertyDataSource,
    notificationDataSource: NotificationDataSource,
    messageDataSource: MessageDataSource,
    groupDataSource: GroupDataSource,
    loggerFactory: net.pantasystem.milktea.common.Logger.Factory
) {
    val noteRelationGetter = NoteRelationGetter(
        noteRepository,
        userDataSource,
        filePropertyDataSource,
        loggerFactory.create("NoteRelationGetter")
    )

    val notificationRelationGetter = NotificationRelationGetter(
        userDataSource,
        notificationDataSource,
        noteRelationGetter,
        noteDataSourceAdder = NoteDataSourceAdder(
            userDataSource, noteDataSource, filePropertyDataSource
        )
    )

    val messageRelationGetter =
        MessageRelationGetter(messageDataSource, userDataSource, groupDataSource)
}