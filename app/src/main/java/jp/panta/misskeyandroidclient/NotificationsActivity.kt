package jp.panta.misskeyandroidclient

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.wada811.databinding.dataBinding
import dagger.hilt.android.AndroidEntryPoint
import jp.panta.misskeyandroidclient.databinding.ActivityNotificationsBinding
import jp.panta.misskeyandroidclient.ui.notes.view.ActionNoteHandler
import jp.panta.misskeyandroidclient.ui.notes.viewmodel.NotesViewModel
import jp.panta.misskeyandroidclient.ui.notification.NotificationMentionFragment
import jp.panta.misskeyandroidclient.viewmodel.confirm.ConfirmViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import net.pantasystem.milktea.data.infrastructure.settings.SettingStore
import javax.inject.Inject

@ExperimentalCoroutinesApi
@FlowPreview
@AndroidEntryPoint
class NotificationsActivity : AppCompatActivity() {

    @Inject
    lateinit var settingStore: SettingStore

    val binding: ActivityNotificationsBinding by dataBinding()
    val notesViewModel by viewModels<NotesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme()
        setContentView(R.layout.activity_notifications)
        setSupportActionBar(binding.notificationToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ActionNoteHandler(
            this,
            notesViewModel,
            ViewModelProvider(this)[ConfirmViewModel::class.java],
            settingStore
        ).initViewModelListener()

        showNotificationFragment()
    }

    private fun showNotificationFragment() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.notificationBase, NotificationMentionFragment())
        ft.commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
