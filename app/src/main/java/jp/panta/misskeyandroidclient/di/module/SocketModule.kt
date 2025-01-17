package jp.panta.misskeyandroidclient.di.module

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.pantasystem.milktea.common.Logger
import net.pantasystem.milktea.data.infrastructure.notes.NoteCaptureAPIAdapterImpl
import net.pantasystem.milktea.data.infrastructure.notes.NoteCaptureAPIWithAccountProvider
import net.pantasystem.milktea.data.infrastructure.notes.NoteCaptureAPIWithAccountProviderImpl
import net.pantasystem.milktea.data.streaming.SocketWithAccountProvider
import net.pantasystem.milktea.data.streaming.channel.ChannelAPIWithAccountProvider
import net.pantasystem.milktea.data.streaming.impl.SocketWithAccountProviderImpl
import net.pantasystem.milktea.model.account.AccountRepository
import net.pantasystem.milktea.model.notes.NoteCaptureAPIAdapter
import net.pantasystem.milktea.model.notes.NoteDataSource
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class SocketBindsModule {

    @Binds
    @Singleton
    abstract fun provideSocketWithAccountProvider(
        socketWithAccountProviderImpl: SocketWithAccountProviderImpl
    ): SocketWithAccountProvider

    @Binds
    @Singleton
    abstract fun provideNoteCaptureAPIWithAccountProvider(
        provider: NoteCaptureAPIWithAccountProviderImpl
    ) : NoteCaptureAPIWithAccountProvider
}


@InstallIn(SingletonComponent::class)
@Module
object SocketModule {
    @Singleton
    @Provides
    fun provideChannelAPIProvider(
        loggerFactory: Logger.Factory,
        socketWithAccountProvider: SocketWithAccountProvider
    ): ChannelAPIWithAccountProvider {
        return ChannelAPIWithAccountProvider(socketWithAccountProvider, loggerFactory)
    }

    @Singleton
    @Provides
    fun provideNoteCaptureAPIAdapter(
        accountRepository: AccountRepository,
        coroutineScope: CoroutineScope,
        loggerFactory: Logger.Factory,
        noteCaptureAPIWithAccountProvider: NoteCaptureAPIWithAccountProvider,
        noteDataSource: NoteDataSource,
    ): NoteCaptureAPIAdapter {
        return NoteCaptureAPIAdapterImpl(
            accountRepository = accountRepository,
            cs = coroutineScope,
            loggerFactory = loggerFactory,
            noteCaptureAPIWithAccountProvider = noteCaptureAPIWithAccountProvider,
            noteDataSource = noteDataSource,
            dispatcher = Dispatchers.IO
        )
    }
}