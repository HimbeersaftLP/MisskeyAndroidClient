package jp.panta.misskeyandroidclient.model.users.nickname


interface UserNicknameRepository {

    suspend fun save(nickname: UserNickname)

    suspend fun findOne(id: UserNickname.Id): UserNickname


}