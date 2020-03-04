package com.leezg.app.nmerodiary


import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.leezg.app.nmerodiary.adapters.GenericRecyclerAdapter
import com.leezg.app.nmerodiary.interfaces.AppExecutorCallback
import com.leezg.app.nmerodiary.interfaces.OnItemClickListener
import com.leezg.app.nmerodiary.others.AppExecutors
import com.leezg.app.nmerodiary.others.Constant
import kotlinx.android.synthetic.main.activity_main.*
import me.yokeyword.fragmentation.SupportFragment


abstract class BaseFragment : SupportFragment() {

    //private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Room database
        //database = AppDatabase.getInstance(_mActivity)!!
    }

    // Initialize and populate the RecyclerView with retrieved data
    protected fun initializeAndPopulateRecyclerView(
        dataList: MutableList<Any>,
        viewType: Int,
        extraParm: Any,
        recyclerView: RecyclerView,
        callback: OnItemClickListener
    ): GenericRecyclerAdapter {
        // Instantiate GenericRecyclerAdapter and attach it to RecyclerView
        val mAdapter = GenericRecyclerAdapter(dataList, viewType, extraParm, _mActivity)
        recyclerView.also {
            it.layoutManager = LinearLayoutManager(_mActivity)
            it.itemAnimator = DefaultItemAnimator()
            it.adapter = mAdapter
            it.isNestedScrollingEnabled = false
        }

        mAdapter.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(position: Int, view: View, model: Any) {
                callback.onItemClick(position, view, model)
            }

            override fun onItemClick(position: Int, view: View, itemID: Int, model: Any) {

            }
        })

        return mAdapter
    }

    // Receive callback for database operation
    protected fun performDBAction(appExecutorCallback: AppExecutorCallback) {
        AppExecutors.getInstance()!!.diskIO().execute {
            appExecutorCallback.executeDBTransactions()
        }
    }

    // Show message on Snackbar
    protected fun showSnackbar(
        layout: View,
        message: String,
        duration: Int,
        actionText: String?,
        onClick: View.OnClickListener?,
        anchorView: View?
    ) {
        if (_mActivity != null) {
            Snackbar.make(layout, message, duration)
                .setAnchorView(if (anchorView == null) _mActivity.Main_BottomNav else null)
                .setAction(actionText, onClick).show()
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        // If current visible Fragment is not in excluded Fragment list, then hide the back button on Toolbar
        if (getVisibleFragment(_mActivity) !in Constant.excludedFragmentList)
            _mActivity.supportActionBar?.setDisplayHomeAsUpEnabled(false)

        // Hide the BottomNavigationView in excluded Fragment list
        _mActivity?.Main_BottomNav?.visibility =
            if (getVisibleFragment(_mActivity) in Constant.excludedFragmentList) View.GONE else View.VISIBLE
    }

    // Get current visible Fragment
    private fun getVisibleFragment(_mActivity: Context): String {
        val currentBackStack: Int
        val visibleFragment: String

        //*** Get Fragment class name (Start)
        if ((_mActivity as AppCompatActivity).supportFragmentManager.backStackEntryCount > 0) {
            currentBackStack = _mActivity.supportFragmentManager.backStackEntryCount
            visibleFragment =
                _mActivity.supportFragmentManager.getBackStackEntryAt(currentBackStack - 1).name!!
        } else
            visibleFragment = Constant.HOME_FRAGMENT

        return visibleFragment.substring(
            visibleFragment.lastIndexOf(".") + 1,
            visibleFragment.length
        )
        //*** (End)
    }
}
