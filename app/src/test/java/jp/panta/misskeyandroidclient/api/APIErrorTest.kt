package jp.panta.misskeyandroidclient.api

import jp.panta.misskeyandroidclient.api.notes.CreateNote
import jp.panta.misskeyandroidclient.model.I
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class APIErrorTest {

    private val  misskeyAPI = MisskeyAPIServiceBuilder.build("https://misskey.io")



    @Test(expected = APIError.ForbiddenException::class)
    suspend fun testClientError() {
        misskeyAPI.create(CreateNote("", text = null)).throwIfHasError()
    }



    @Test(expected = APIError.AuthenticationException::class)
    suspend fun testAuthenticationError() {
        val res = misskeyAPI.i(I(null))

        res.throwIfHasError()
    }


    @Test
    suspend fun testHasErrorBody() {
        val res = misskeyAPI.i(I(null))

        try{
            res.throwIfHasError()
        }catch(e: APIError) {
            assertNotNull(e.error)
        }
    }

}