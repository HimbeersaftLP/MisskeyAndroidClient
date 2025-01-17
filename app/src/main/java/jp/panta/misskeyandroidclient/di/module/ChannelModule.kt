package jp.panta.misskeyandroidclient.di.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.pantasystem.milktea.common.Encryption
import net.pantasystem.milktea.data.api.misskey.MisskeyAPIProvider
import net.pantasystem.milktea.data.infrastructure.channel.impl.ChannelAPIAdapter
import net.pantasystem.milktea.model.channel.ChannelStateModel
import net.pantasystem.milktea.data.infrastructure.channel.impl.ChannelAPIAdapterWebImpl
import net.pantasystem.milktea.data.infrastructure.channel.impl.ChannelRepositoryImpl
import net.pantasystem.milktea.model.account.AccountRepository
import net.pantasystem.milktea.model.channel.ChannelRepository
import net.pantasystem.milktea.model.channel.ChannelStateModelOnMemory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChannelModule {

    @Provides
    @Singleton
    fun provideChannelStateModel(): ChannelStateModel {
        return ChannelStateModelOnMemory()
    }

    @Provides
    @Singleton
    fun provideChannelAPIAdapter(
        accountRepository: AccountRepository,
        encryption: Encryption,
        misskeyAPIProvider: MisskeyAPIProvider
    ): ChannelAPIAdapter {
        return ChannelAPIAdapterWebImpl(accountRepository, misskeyAPIProvider, encryption)
    }

    @Provides
    @Singleton
    fun provideChannelRepository(
        channelStateModel: ChannelStateModel,
        accountRepository: AccountRepository,
        channelAPIAdapter: ChannelAPIAdapter,
        ): ChannelRepository {
        return ChannelRepositoryImpl(
            channelStateModel = channelStateModel,
            accountRepository = accountRepository,
            channelAPIAdapter = channelAPIAdapter
        )
    }
}