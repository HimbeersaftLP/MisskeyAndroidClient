package jp.panta.misskeyandroidclient.model.notes.impl

import jp.panta.misskeyandroidclient.api.notes.DeleteNote
import jp.panta.misskeyandroidclient.api.notes.NoteRequest
import jp.panta.misskeyandroidclient.api.throwIfHasError
import jp.panta.misskeyandroidclient.model.AddResult
import jp.panta.misskeyandroidclient.model.notes.*
import jp.panta.misskeyandroidclient.model.notes.reaction.CreateReaction
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import jp.panta.misskeyandroidclient.viewmodel.notes.editor.PostNoteTask
import kotlinx.coroutines.*

class NoteRepositoryImpl(
    val miCore: MiCore
) : NoteRepository {

    private val logger = miCore.loggerFactory.create("NoteRepositoryImpl")
    private val noteDataSourceAdder: NoteDataSourceAdder by lazy {
        NoteDataSourceAdder(miCore.getUserDataSource(), miCore.getNoteDataSource(), miCore.getFilePropertyDataSource())
    }

    override suspend fun create(createNote: CreateNote): Note {
        val task = PostNoteTask(miCore.getEncryption(), createNote, createNote.author, miCore.loggerFactory, miCore.getFilePropertyDataSource())
        val result = runCatching {task.execute(
            miCore.getFileUploaderProvider().get(createNote.author)
        )?: throw IllegalStateException("ファイルのアップロードに失敗しました")
        }.runCatching {
            getOrThrow().let {
                miCore.getMisskeyAPIProvider().get(createNote.author).create(it).body()?.createdNote
            }
        }

        if(result.isFailure) {
            val exDraft = createNote.draftNoteId?.let{ miCore.getDraftNoteDAO().getDraftNote(createNote.author.accountId, createNote.draftNoteId) }
            miCore.getDraftNoteDAO().fullInsert(task.toDraftNote(exDraft))
        }
        val noteDTO = result.getOrThrow()
        require(noteDTO != null)
        createNote.draftNoteId?.let{
            miCore.getDraftNoteDAO().deleteDraftNote(createNote.author.accountId, draftNoteId = it)
        }
        miCore.getSettingStore().setNoteVisibility(createNote)

        return noteDataSourceAdder.addNoteDtoToDataSource(createNote.author, noteDTO)

    }

    override suspend fun delete(noteId: Note.Id): Boolean {
        val account = miCore.getAccountRepository().get(noteId.accountId)
        return miCore.getMisskeyAPIProvider().get(account).delete(
            DeleteNote(i = account.getI(miCore.getEncryption()), noteId = noteId.noteId)
        ).isSuccessful
    }

    override suspend fun find(noteId: Note.Id): Note {
        val account = miCore.getAccount(noteId.accountId)

        var note = runCatching {
            miCore.getNoteDataSource().get(noteId)
        }.getOrNull()
        if(note != null) {
            return note
        }

        logger.debug("request notes/show=$noteId")
        note = runCatching {
            miCore.getMisskeyAPIProvider().get(account).showNote(NoteRequest(
                i = account.getI(miCore.getEncryption()),
                noteId = noteId.noteId
            ))
        }.getOrNull()?.body()?.let{
            noteDataSourceAdder.addNoteDtoToDataSource(account, it)
        }
        note?: throw NoteNotFoundException(noteId)
        return note
    }

    @ExperimentalCoroutinesApi
    override suspend fun toggleReaction(createReaction: CreateReaction): Boolean {
        val account = miCore.getAccount(createReaction.noteId.accountId)
        var note = find(createReaction.noteId)
        if(note.myReaction?.isNotBlank() == true) {
            if(!unreaction(createReaction.noteId)) {
                return false
            }

            if(note.myReaction == createReaction.reaction) {
                logger.debug("同一のリアクションが選択されています。")
                return true
            }
            note = miCore.getNoteDataSource().get(createReaction.noteId)
        }


        return runCatching {
            if(postReaction(createReaction) && !miCore.getNoteCaptureAPI(account).isCaptured(createReaction.noteId.noteId)) {
                miCore.getNoteDataSource().add(note.onIReacted(createReaction.reaction))
            }
            true
        }.getOrThrow()


    }

    override suspend fun unreaction(noteId: Note.Id): Boolean {
        val note = find(noteId)
        val account = miCore.getAccountRepository().get(noteId.accountId)
        return postUnReaction(noteId)
                && (miCore.getNoteCaptureAPI(account).isCaptured(noteId.noteId)
                || (note.myReaction != null
                && miCore.getNoteDataSource().add(note.onIUnReacted()) != AddResult.CANCEL))
    }

    private suspend fun postReaction(createReaction: CreateReaction): Boolean {
        val account = miCore.getAccount(createReaction.noteId.accountId)
        val res = miCore.getMisskeyAPIProvider().get(account).createReaction(
            jp.panta.misskeyandroidclient.api.notes.CreateReaction(
                i = account.getI(miCore.getEncryption()),
                noteId = createReaction.noteId.noteId,
                reaction = createReaction.reaction
            )
        )
        res.throwIfHasError()
        return res.isSuccessful
    }

    private suspend fun postUnReaction(noteId: Note.Id): Boolean {
        val note = find(noteId)
        val account  = miCore.getAccount(noteId.accountId)
        val res = miCore.getMisskeyAPIProvider().get(account).deleteReaction(DeleteNote(
            noteId = note.id.noteId,
            i = account.getI(miCore.getEncryption())
        ))
        res.throwIfHasError()
        return res.isSuccessful

    }
}