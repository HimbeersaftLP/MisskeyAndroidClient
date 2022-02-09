package jp.panta.misskeyandroidclient.model.instance.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import jp.panta.misskeyandroidclient.model.DataBase
import jp.panta.misskeyandroidclient.model.emoji.Emoji
import jp.panta.misskeyandroidclient.model.instance.Meta
import jp.panta.misskeyandroidclient.model.instance.MetaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RoomMetaRepositoryTest {

    private lateinit var metaRepository: MetaRepository

    private lateinit var database: DataBase

    private lateinit var sampleMeta: Meta

    @Before
    fun setUp(){
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DataBase::class.java).build()

        metaRepository = RoomMetaRepository(database.metaDAO(), database.emojiAliasDAO(), database)

        sampleMeta =  Meta(
            bannerUrl = "https://hogehoge.io/hogehoge.jpg",
            cacheRemoteFiles = true,
            description = "hogehogeTest",
            disableGlobalTimeline = false,
            disableLocalTimeline = false,
            disableRegistration = false,
            driveCapacityPerLocalUserMb = 1000,
            driveCapacityPerRemoteUserMb = 2000,
            enableDiscordIntegration = true,
            enableEmail = false,
            enableEmojiReaction = true,
            enableGithubIntegration = true,
            enableRecaptcha = true,
            enableServiceWorker = true,
            enableTwitterIntegration = true,
            errorImageUrl = "https://error.img",
            feedbackUrl = "https://feedback.com",
            iconUrl = "https://favicon.png",
            maintainerEmail = "",
            maintainerName = "",
            mascotImageUrl = "",
            maxNoteTextLength = 500,
            name = "",
            recaptchaSiteKey = "key",
            secure = true,
            swPublicKey = "swPublicKey",
            toSUrl = "toSUrl",
            version = "12.0.1",
            emojis = listOf(Emoji("wakaru"), Emoji("kawaii"), Emoji("ai"), Emoji("test"), Emoji("nemui"), Emoji("hoge")),
            uri = "https://test.misskey.io"
        )

    }

    @Test
    fun addAndGetMetaTest(){
        runBlocking{
            val added = metaRepository.add(sampleMeta)
            val got = metaRepository.get(added.uri)
            assertNotNull(got)
            assertEquals(added, got)
            assertEquals(added.emojis?.size!!, got?.emojis?.size!!)
        }
    }

    @Test
    fun doubleAddTest(){
        val emojis =  sampleMeta.emojis?: emptyList()
        val arrayList = ArrayList<Emoji>(emojis)
        arrayList.add(Emoji("added"))
        val updated = sampleMeta.copy(emojis = arrayList)
        runBlocking{
            val added = metaRepository.add(updated)
            val got = metaRepository.get(added.uri)
            assertNotNull(got)
            assertEquals(added, got)
            assertEquals(added.emojis?.size!!, got?.emojis?.size!!)

        }
    }
}