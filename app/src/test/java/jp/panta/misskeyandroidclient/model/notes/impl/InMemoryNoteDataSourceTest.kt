package jp.panta.misskeyandroidclient.model.notes.impl

import net.pantasystem.milktea.api.misskey.notes.NoteDTO
import net.pantasystem.milktea.api.misskey.users.UserDTO
import jp.panta.misskeyandroidclient.logger.TestLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import net.pantasystem.milktea.common.Logger
import net.pantasystem.milktea.model.AddResult
import net.pantasystem.milktea.model.account.Account
import net.pantasystem.milktea.data.infrastructure.notes.impl.InMemoryNoteDataSource
import net.pantasystem.milktea.data.infrastructure.toNote
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InMemoryNoteDataSourceTest {

    private lateinit var loggerFactory: Logger.Factory
    private lateinit var account: Account

    @Before
    fun setUp() {
        loggerFactory = TestLogger.Factory()
        account = Account(
            remoteId = "piyo",
            instanceDomain = "",
            encryptedToken = "",
            userName = "piyoName",
            instanceType = Account.InstanceType.MISSKEY
        )
    }

    @Test
    fun testAdd() {
        val noteDataSource = InMemoryNoteDataSource(loggerFactory)

        val dto = NoteDTO(
            "",
            Clock.System.now(),
            renoteCount = 0,
            replyCount = 0,
            userId = "hoge",
            user = UserDTO("hoge", "hogeName")
        )
        val note = dto.toNote(account)
        runBlocking {
            val result = noteDataSource.add(
                note
            )
            delay(10)

            assertEquals(AddResult.CREATED, result)
            val old = note.copy()
            delay(10)

            assertEquals(AddResult.UPDATED, noteDataSource.add(note))
            delay(10)

            assertEquals(AddResult.CANCEL, noteDataSource.add(old))
            delay(10)

            assertEquals(AddResult.CANCEL, noteDataSource.add(old.copy()))
        }

    }


    @Test
    fun testUpdateNote(): Unit = runBlocking {
        val noteDataSource = InMemoryNoteDataSource(loggerFactory)

        val dto = NoteDTO(
            "note-1",
            Clock.System.now(),
            renoteCount = 0,
            replyCount = 0,
            userId = "hoge",
            user = UserDTO("hoge", "hogeName")
        )
        val note = dto.toNote(account)
        noteDataSource.add(
            note
        )

        val dtoParsed = dto.toNote(account)
        assertEquals(AddResult.UPDATED, noteDataSource.add(dtoParsed))

        assertTrue(dtoParsed === noteDataSource.get(dtoParsed.id))
    }
}