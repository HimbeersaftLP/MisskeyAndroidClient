package jp.panta.misskeyandroidclient.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.wada811.databinding.dataBinding
import dagger.hilt.android.AndroidEntryPoint
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.SearchActivity
import jp.panta.misskeyandroidclient.databinding.FragmentSearchTopBinding
import jp.panta.misskeyandroidclient.ui.explore.ExploreFragment
import jp.panta.misskeyandroidclient.ui.notes.view.TimelineFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import net.pantasystem.milktea.model.account.page.Pageable


@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class SearchTopFragment : Fragment(R.layout.fragment_search_top){

    private val mBinding: FragmentSearchTopBinding by dataBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.searchViewPager.adapter = SearchPagerAdapter(this.childFragmentManager, requireContext())
        mBinding.searchTabLayout.setupWithViewPager(mBinding.searchViewPager)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_top_menu, menu)
        //context?.setMenuTint(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            //R.id.search ->
            R.id.search ->{
                activity?.startActivity(Intent(this.context, SearchActivity::class.java))
                activity?.overridePendingTransition(0, 0)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }
    class SearchPagerAdapter(supportFragmentManager: FragmentManager, context: Context) : FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT){
        val tabList = listOf(context.getString(R.string.title_featured), context.getString(R.string.explore))
        override fun getCount(): Int {
            return tabList.size
        }

        override fun getItem(position: Int): Fragment {
            return when(position){
                0 -> TimelineFragment.newInstance(Pageable.Featured(null))
                1 -> ExploreFragment()
                else -> throw IllegalArgumentException("range 0..1, list:$tabList")
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return tabList[position]
        }
    }
}