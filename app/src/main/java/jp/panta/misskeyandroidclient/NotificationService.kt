package jp.panta.misskeyandroidclient

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import jp.panta.misskeyandroidclient.model.account.Account
import jp.panta.misskeyandroidclient.model.messaging.MessageRelation
import jp.panta.misskeyandroidclient.model.notification.*
import jp.panta.misskeyandroidclient.model.notification.Notification
import jp.panta.misskeyandroidclient.streaming.ChannelBody
import jp.panta.misskeyandroidclient.streaming.channel.ChannelAPI
import jp.panta.misskeyandroidclient.view.SafeUnbox
import jp.panta.misskeyandroidclient.view.notification.notificationMessageScope
import java.util.*
import jp.panta.misskeyandroidclient.viewmodel.notification.NotificationViewData.Type.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
class NotificationService : Service() {
    companion object{
        private const val TAG = "NotificationService"
        private const val MESSAGE_CHANEL_ID = "jp.panta.misskeyandroidclient.NotificationService.MESSAGE_CHANEL_ID"

    }

    private lateinit var mBinder: NotificationBinder

    private val coroutineScope = CoroutineScope(Job() + Dispatchers.Main)

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startObserve()
        Log.d(TAG, "serviceを開始した")
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        mBinder = NotificationBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    private fun startObserve(){

        val miApplication = applicationContext
        if(miApplication is MiApplication){

            /*miApplication.getAccounts().flatMapLatest { acList ->
                acList.map{ a ->
                    miApplication.getChannelAPI(a).connect(ChannelAPI.Type.MAIN).map {
                        a to it
                    }
                }.merge()
            }.filterNot {
                mStopNotificationAccountMap.contains(it.first.accountId)
            }.map {
                (it.second as? ChannelBody.Main.Notification)?.let{ body ->
                    it.first to body
                }
            }.filterNotNull().onEach {
                val notification = miApplication.getGetters().notificationRelationGetter.get(it.first, it.second.body)
                showNotification(notification)
            }.launchIn(coroutineScope + Dispatchers.IO)
            */

            miApplication.messageStreamFilter.getAllMergedAccountMessages().onEach {
                val msgRelation = miApplication.getGetters().messageRelationGetter.get(it)
                showMessageNotification(msgRelation)
            }.launchIn(coroutineScope + Dispatchers.IO)

        }


    }



    private fun showMessageNotification(message: MessageRelation){

        val builder = NotificationCompat.Builder(this, MESSAGE_CHANEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(message.user.getDisplayUserName())
            .setContentText(SafeUnbox.unbox(message.message.text))
        builder.priority = NotificationCompat.PRIORITY_DEFAULT

        with(makeNotificationManager(MESSAGE_CHANEL_ID)){
            notify(6, builder.build())
        }
    }

    private fun makeNotificationManager(id: String): NotificationManager{
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val name = getString(R.string.app_name)
        val description = "THE NOTIFICATION"

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if(notificationManager.getNotificationChannel(id) == null){
                val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
                channel.description = description
                notificationManager.createNotificationChannel(channel)
            }
        }
        return notificationManager

    }






    inner class NotificationBinder : Binder()


}
