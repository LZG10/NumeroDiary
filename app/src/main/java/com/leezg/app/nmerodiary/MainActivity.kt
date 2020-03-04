package com.leezg.app.nmerodiary

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationAdapter
import com.leezg.app.nmerodiary.interfaces.ActionBarTitleCallback
import com.leezg.app.nmerodiary.view_classes.HomeFragment
import com.leezg.app.nmerodiary.view_classes.NotificationsFragment
import com.leezg.app.nmerodiary.view_classes.RecordViewFragment
import com.leezg.app.nmerodiary.view_classes.SettingsFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import me.yokeyword.fragmentation.ISupportFragment
import me.yokeyword.fragmentation.SupportActivity
import me.yokeyword.fragmentation.SupportFragment


class MainActivity : SupportActivity(), ActionBarTitleCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Toolbar
        setSupportActionBar(Main_Toolbar)
        Main_Toolbar.title = getString(R.string.app_name)

        // Setup BottomNavigationView
        val tabColors =
            resources.getIntArray(R.array.tab_colors)
        val navigationAdapter =
            AHBottomNavigationAdapter(this, R.menu.bottom_nav_menu)
        navigationAdapter.setupWithBottomNavigation(Main_BottomNav, tabColors)

        // Launch Fragment based on selected tab
        Main_BottomNav.setOnTabSelectedListener { position, _ ->
            // Get top Fragment
            val topFragment = topFragment
            val home = topFragment as SupportFragment

            when (position) {
                // Home
                0 -> {
                    val fragment = findFragment(HomeFragment::class.java)
                    if (fragment == null)
                        home.startWithPopTo(
                            HomeFragment.newInstance(),
                            HomeFragment::class.java,
                            false
                        )
                    else
                        home.start(HomeFragment.newInstance(), ISupportFragment.SINGLETASK)
                    true
                }
                1 -> {
                    val fragment = findFragment(RecordViewFragment::class.java)
                    if (fragment == null)
                        home.startWithPopTo(
                            RecordViewFragment.newInstance(),
                            HomeFragment::class.java,
                            false
                        )
                    else
                        home.start(RecordViewFragment.newInstance(), ISupportFragment.SINGLETASK)
                    //home.popTo(RecordViewFragment::class.java, false)
                    true
                }
                else -> {
                    val fragment = findFragment(NotificationsFragment::class.java)
                    if (fragment == null)
                        home.startWithPopTo(
                            NotificationsFragment.newInstance(),
                            RecordViewFragment::class.java,
                            false
                        )
                    else
                        home.start(NotificationsFragment.newInstance(), ISupportFragment.SINGLETASK)
                    //home.popTo(NotificationsFragment::class.java, false)
                    true
                }
            }
        }

        // If the root Fragment is not loaded, then load it as main Fragment
        if (findFragment(HomeFragment::class.java) == null) {
            // Initialize all View components
            initializeViewComponents()

            // Load root Fragment into the container
            //loadRootFragment(R.id.MainActivity_Container, HomeFragment.newInstance())
            loadRootFragment(R.id.MainActivity_Container, HomeFragment.newInstance(), true, true)
        }
    }

    // Initialize all View components
    private fun initializeViewComponents() {

    }

    // Set ActionBar title
    override fun setActionBarTitle(title: String) {
        supportActionBar?.title = title
    }

    /*override fun toggleUpBtnVisibility(enabled: Boolean, icon: Int) {
        Main_Toolbar.apply {
            setNavigationIcon(if (enabled) icon else 0)
            setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }*/
    override fun onBackPressedSupport() {
        if (supportFragmentManager.backStackEntryCount > 1)
            pop()
        else
            finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settings_action)
            start(SettingsFragment.newInstance(), SupportFragment.SINGLETASK)
        return super.onOptionsItemSelected(item)
    }
}
