package jp.panta.misskeyandroidclient

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationView
import jp.panta.misskeyandroidclient.databinding.ActivityMainBinding
import jp.panta.misskeyandroidclient.databinding.NavHeaderMainBinding
import jp.panta.misskeyandroidclient.model.auth.ConnectionInstance
import jp.panta.misskeyandroidclient.util.BottomNavigationAdapter
import jp.panta.misskeyandroidclient.view.account.AccountSwitchingDialog
import jp.panta.misskeyandroidclient.view.drive.DriveFragment
import jp.panta.misskeyandroidclient.view.messaging.MessagingHistoryFragment
import jp.panta.misskeyandroidclient.view.notes.ActionNoteHandler
import jp.panta.misskeyandroidclient.view.notes.TabFragment
import jp.panta.misskeyandroidclient.view.notification.NotificationFragment
import jp.panta.misskeyandroidclient.view.search.SearchTopFragment
import jp.panta.misskeyandroidclient.view.settings.activities.TabSettingActivity
import jp.panta.misskeyandroidclient.viewmodel.account.AccountViewModel
import jp.panta.misskeyandroidclient.viewmodel.notes.NotesViewModel
import jp.panta.misskeyandroidclient.viewmodel.notes.NotesViewModelFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var mNotesViewModel: NotesViewModel
    private lateinit var mAccountViewModel: AccountViewModel

    private var bottomNavigationAdapter: MainBottomNavigationAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setTheme(R.style.AppThemeDark)
        setTheme()

        setContentView(R.layout.activity_main)

        val mainBinding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        fab.setOnClickListener{
            startActivity(Intent(this, NoteEditorActivity::class.java))
        }
        //replaceTimelineFragment()
        init()


        val miApplication = application as MiApplication

        var init = false
        miApplication.currentConnectionInstanceLiveData.observe(this, Observer {
            if(!init){
                mNotesViewModel = ViewModelProvider(this, NotesViewModelFactory(it, miApplication)).get(NotesViewModel::class.java)

                mAccountViewModel = ViewModelProvider(this)[AccountViewModel::class.java]
                Log.d("MainActivity", "NotesViewModelのコネクション情報: ${mNotesViewModel.connectionInstance}")

                init()

                //initViewModelListener()
                ActionNoteHandler(this, mNotesViewModel).initViewModelListener()
                initAccountViewModelListener()

                setHeaderProfile(mainBinding)

                init = true
                //observeTab()
                Log.d("MainActivity", "初期化処理")
            }

        })

        miApplication.isSuccessLoadConnectionInstance.observe(this, Observer {
            if(!it){
                startActivity(Intent(this, AuthActivity::class.java))
                //finish()
            }
        })

        test()

        startService(Intent(this, NotificationService::class.java))
    }

    private fun init(){
        val ci = (application as MiApplication).currentConnectionInstanceLiveData.value
        if(ci != null){
            //setFragment("home")
            //setHeaderProfile(ci)
            bottomNavigationAdapter = MainBottomNavigationAdapter()
        }

    }


    inner class MainBottomNavigationAdapter
        : BottomNavigationAdapter(bottom_navigation, supportFragmentManager, R.id.navigation_home, R.id.content_main){
        private val home = bottom_navigation.menu.findItem(R.id.navigation_home)
        var currentMenuItem: MenuItem? = null

        override fun viewChanged(menuItem: MenuItem, fragment: Fragment) {
            super.viewChanged(menuItem, fragment)
            when(menuItem.itemId){
                R.id.navigation_home -> changeTitle(getString(R.string.menu_home))
                R.id.navigation_search -> changeTitle(getString(R.string.search))
                R.id.navigation_notification -> changeTitle(getString(R.string.notification))
                R.id.navigation_message_list -> changeTitle(getString(R.string.message))
            }
            currentMenuItem = menuItem
        }
        override fun getItem(menuItem: MenuItem): Fragment? {
            return when(menuItem.itemId){
                R.id.navigation_home -> TabFragment()
                R.id.navigation_search -> SearchTopFragment()
                R.id.navigation_notification -> NotificationFragment()
                R.id.navigation_message_list -> MessagingHistoryFragment()
                else -> null
            }
        }


    }

    private val switchAccountButtonObserver = Observer<Int>{
        runOnUiThread{
            drawer_layout.closeDrawer(GravityCompat.START)
            val dialog = AccountSwitchingDialog()
            dialog.show(supportFragmentManager, "mainActivity")
        }
    }

    private val switchAccountObserver = Observer<ConnectionInstance>{
        (application as MiApplication).switchAccount(it)
    }

    private fun initAccountViewModelListener(){
        mAccountViewModel.switchAccount.removeObserver(switchAccountButtonObserver)
        mAccountViewModel.switchAccount.observe(this, switchAccountButtonObserver)

        mAccountViewModel.switchTargetConnectionInstance.removeObserver(switchAccountObserver)
        mAccountViewModel.switchTargetConnectionInstance.observe(this, switchAccountObserver)
    }
    fun changeTitle(title: String?){
        toolbar.title = title
    }

    private fun test(){
        //startActivity(Intent(this, AuthActivity::class.java))
    }

    private fun setHeaderProfile(activityMainBinding: ActivityMainBinding){


        DataBindingUtil.bind<NavHeaderMainBinding>(activityMainBinding.navView.getHeaderView(0))
        val headerBinding = DataBindingUtil.getBinding<NavHeaderMainBinding>(activityMainBinding.navView.getHeaderView(0))
        headerBinding?.accountViewModel = mAccountViewModel

        (application as MiApplication).currentAccountLiveData.observe(this, Observer {
            headerBinding?.user = it
        })
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }else if(bottomNavigationAdapter?.currentMenuItem?.itemId != R.id.navigation_home){
            bottomNavigationAdapter?.setCurrentFragment(R.id.navigation_home)
        }else{
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_tab_setting-> {
                startActivity(Intent(this,
                    TabSettingActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {

            R.id.nav_setting ->{
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            R.id.nav_drive ->{
                startActivity(Intent(this, DriveActivity::class.java))
            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return false
    }
}
