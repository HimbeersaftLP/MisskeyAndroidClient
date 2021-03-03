package jp.panta.misskeyandroidclient.streaming.impl

import jp.panta.misskeyandroidclient.Logger
import jp.panta.misskeyandroidclient.model.Encryption
import jp.panta.misskeyandroidclient.model.UnauthorizedException
import jp.panta.misskeyandroidclient.model.account.Account
import jp.panta.misskeyandroidclient.model.account.AccountRepository
import jp.panta.misskeyandroidclient.streaming.Socket
import jp.panta.misskeyandroidclient.streaming.network.SocketImpl
import okhttp3.OkHttpClient
import jp.panta.misskeyandroidclient.streaming.SocketWithAccountProvider as ISocketWithAccountProvider

/**
 * SocketをAccountに基づきいい感じにリソースを取得できるようにする
 */
class SocketWithAccountProviderImpl(
    val encryption: Encryption,
    val accountRepository: AccountRepository,
    loggerFactory: Logger.Factory,
    val instanceCreatedListener: (account: Account, socket: Socket)-> Unit = { _, s -> s.connect() },
    val okHttpClient: OkHttpClient = OkHttpClient()
) : ISocketWithAccountProvider{

    private val logger = loggerFactory.create("SocketProvider")

    private val accountIdWithSocket = mutableMapOf<Long, Socket>()

    override fun get(accountId: Long): Socket? {
        synchronized(accountIdWithSocket) {
            return accountIdWithSocket[accountId]
        }
    }

    override fun get(account: Account): Socket {
        synchronized(accountIdWithSocket) {
            var socket = accountIdWithSocket[account.accountId]
            if(socket != null){
                return socket
            }


            var uri = account.instanceDomain
            val urlResult = Regex("^(http|https).*").find(uri)?.groups?.firstOrNull()
            if(urlResult != null) {
                val protocol = if(urlResult.value == "https") {
                    "wss"
                }else{
                    "ws"
                }
                uri = protocol + uri.substring(urlResult.range.last, uri.length)
            }
            if(!uri.endsWith("/")) {
                uri = "$uri/"
            }
            uri = "${uri}streaming"
            try {
                val i = account.getI(encryption)
                uri = "${uri}?i=$i"

            }catch (e: UnauthorizedException) {
                logger.debug("未認証アカウント:id=${account.accountId}, baseURL=${account.instanceDomain}")
            }

            socket = SocketImpl(
                url = uri,
                okHttpClient
            )
            instanceCreatedListener.invoke(account, socket)
            accountIdWithSocket[account.accountId] = socket

            return socket
        }



    }
}