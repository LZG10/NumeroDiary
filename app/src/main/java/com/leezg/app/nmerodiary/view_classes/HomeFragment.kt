package com.leezg.app.nmerodiary.view_classes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.sqlite.db.SimpleSQLiteQuery
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.leezg.app.nmerodiary.BaseFragment
import com.leezg.app.nmerodiary.R
import com.leezg.app.nmerodiary.adapters.GenericRecyclerAdapter
import com.leezg.app.nmerodiary.interfaces.ActionBarTitleCallback
import com.leezg.app.nmerodiary.interfaces.AppExecutorCallback
import com.leezg.app.nmerodiary.interfaces.OnItemClickListener
import com.leezg.app.nmerodiary.models.Folder
import com.leezg.app.nmerodiary.others.AppDatabase
import com.leezg.app.nmerodiary.others.Constant
import com.leezg.app.nmerodiary.others.SQLiteHelper
import kotlinx.android.synthetic.main.fragment_home.*
import me.yokeyword.fragmentation.ISupportFragment

class HomeFragment : BaseFragment() {

    private lateinit var database: AppDatabase
    private lateinit var mAdapter: GenericRecyclerAdapter

    private var folderList: MutableList<Folder> = mutableListOf()
    private val REQUEST_ADD_FOLDER = 0
    private val REQUEST_VIEW_FOLDER = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this Fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Room database
        database = AppDatabase.getInstance(_mActivity)!!

        retrieveAllFolders()

        // Initialize all View components
        initializeViewComponents()

        AddFolder_Btn.setOnClickListener {
            MaterialDialog(_mActivity).show {
                title(R.string.select_template)
                listItems(res = R.array.add_folder_option) { _, index, _ ->
                    startForResult(
                        FolderViewFragment.newInstance(
                            0, if (index == 0) Constant.NORMAL else Constant.CONDITION
                        ),
                        REQUEST_ADD_FOLDER
                    )
                }
            }
        }
    }

    private fun retrieveAllFolders() {
        // Send callback for database operation
        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                val sqlString =
                    SQLiteHelper.composeSQLString(Constant.Folder, "", "", Constant.ASCENDING)
                folderList = database.folderDAO().retrieveAllFolders(SimpleSQLiteQuery(sqlString))

                _mActivity.runOnUiThread {
                    mAdapter.setItems(folderList)
                }
            }
        })
    }

    // Initialize all View components
    private fun initializeViewComponents() {
        mAdapter = initializeAndPopulateRecyclerView(
            dataList = folderList as MutableList<Any>,
            viewType = 1,
            extraParm = "",
            recyclerView = FolderList_RecyclerView,
            callback = object : OnItemClickListener {
                override fun onItemClick(position: Int, view: View, model: Any) {
                    startForResult(
                        FolderViewFragment.newInstance((model as Folder).folderID, -1),
                        REQUEST_VIEW_FOLDER
                    )
                }

                override fun onItemClick(
                    position: Int,
                    view: View,
                    itemID: Int,
                    model: Any
                ) {

                }
            }
        )
    }

    override fun onFragmentResult(requestCode: Int, resultCode: Int, data: Bundle?) {
        super.onFragmentResult(requestCode, resultCode, data)

        if (resultCode == ISupportFragment.RESULT_OK) {
            retrieveAllFolders()
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        // Set ActionBar title
        (_mActivity as ActionBarTitleCallback).setActionBarTitle(getString(R.string.app_name))
    }

    companion object {

        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }
}