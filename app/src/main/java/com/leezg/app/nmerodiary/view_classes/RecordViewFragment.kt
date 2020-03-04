package com.leezg.app.nmerodiary.view_classes


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.sqlite.db.SimpleSQLiteQuery
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.leezg.app.nmerodiary.BaseFragment
import com.leezg.app.nmerodiary.R
import com.leezg.app.nmerodiary.adapters.GenericRecyclerAdapter
import com.leezg.app.nmerodiary.interfaces.ActionBarTitleCallback
import com.leezg.app.nmerodiary.interfaces.AppExecutorCallback
import com.leezg.app.nmerodiary.interfaces.OnItemClickListener
import com.leezg.app.nmerodiary.models.Record
import com.leezg.app.nmerodiary.others.AppDatabase
import com.leezg.app.nmerodiary.others.Constant
import com.leezg.app.nmerodiary.others.SQLiteHelper
import com.mancj.materialsearchbar.MaterialSearchBar
import kotlinx.android.synthetic.main.fragment_folder_view.*
import kotlinx.android.synthetic.main.fragment_record_view.*
import kotlinx.android.synthetic.main.fragment_record_view.Empty_TxtView
import kotlinx.android.synthetic.main.fragment_record_view.RecordList_RecyclerView
import me.yokeyword.fragmentation.ISupportFragment


class RecordViewFragment : BaseFragment(), MaterialSearchBar.OnSearchActionListener,
    PopupMenu.OnMenuItemClickListener {

    private lateinit var database: AppDatabase
    private lateinit var mAdapter: GenericRecyclerAdapter

    private var recordList: MutableList<Record> = mutableListOf()
    private var templateList: MutableList<Record> = mutableListOf()
    private var templateNameList: MutableList<String> = mutableListOf()
    private val REQUEST_ADD_RECORD = 0
    private var searchText: String = ""
    private var orderBy: String = Constant.recordTitle
    private var sequence: Int = Constant.ASCENDING

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(Constant.KEY1, orderBy)
        outState.putInt(Constant.KEY2, sequence)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_record_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // If SavedInstanceState is not null, then retrieved the saved state
        if (savedInstanceState != null) {
            orderBy = savedInstanceState.getString(Constant.KEY1) ?: ""
            sequence = savedInstanceState.getInt(Constant.KEY2)
        }

        // Initialize Room database
        database = AppDatabase.getInstance(_mActivity)!!

        // Add Chip to display current sorting condition
        addChip(
            resources.getStringArray(R.array.column_name_option)[Record.returnColumnIndex(orderBy)],
            if (sequence == Constant.ASCENDING) Constant.ascText else Constant.descText
        )

        // Configure the MaterialSearchBar
        Record_SearchBar.also {
            it.setIconRippleStyle(true)
            it.setOnSearchActionListener(this)

            // Add TextWatcher for filtering data upon user input
            it.addTextChangeListener(object : TextWatcher {
                override fun afterTextChanged(editable: Editable?) {
                    // Get the search text
                    searchText = editable.toString().trim()

                    // Compose SQL query and retrieve selected records from database
                    searchAndRetrieveRecords(searchText, orderBy, sequence)
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
            })
            it.inflateMenu(R.menu.search_menu)
            it.menu.setOnMenuItemClickListener(this)
        }

        // Redirect to add new record when the EditText is clicked
        AddRecord_Btn.setOnClickListener {
            MaterialDialog(_mActivity).show {
                title(R.string.add)
                listItems(R.array.add_record_option) { _, index, _ ->
                    when (index) {
                        // Create new blank record
                        0 -> startForResult(
                            ModifyRecordFragment.newInstance(0, insertTemplate = false),
                            REQUEST_ADD_RECORD
                        )
                        // Create new record with saved template
                        else -> {
                            // If there is saved template, then redirect to create new record with selected template
                            if (templateList.size > 0) {
                                MaterialDialog(_mActivity).show {
                                    title(R.string.select_template)
                                    listItems(items = templateNameList) { _, index, _ ->
                                        startForResult(
                                            ModifyRecordFragment.newInstance(
                                                templateList[index].recordID,
                                                insertTemplate = false
                                            ),
                                            REQUEST_ADD_RECORD
                                        )
                                    }
                                }
                                // Else show message on the Snackbar
                            } else
                                showSnackbar(
                                    it.rootView,
                                    getString(R.string.no_saved_template),
                                    Snackbar.LENGTH_SHORT,
                                    null,
                                    null,
                                    null
                                )
                        }
                    }
                }
            }
        }

        // Initialize all View components
        initializeViewComponents()
    }

    override fun onEnterAnimationEnd(savedInstanceState: Bundle?) {
        super.onEnterAnimationEnd(savedInstanceState)

        // Compose SQL query and retrieve selected records from database
        searchAndRetrieveRecords("", orderBy, sequence)
    }

    // Initialize all View components
    private fun initializeViewComponents() {
        mAdapter = initializeAndPopulateRecyclerView(
            dataList = recordList as MutableList<Any>,
            viewType = 0,
            extraParm = "",
            recyclerView = RecordList_RecyclerView,
            callback = object : OnItemClickListener {
                override fun onItemClick(position: Int, view: View, model: Any) {
                    // Pin record
                    if (view.id == R.id.Pin_ImgView) {
                        pinRecord(model as Record)
                        // Modify record
                    } else
                        startForResult(
                            ModifyRecordFragment.newInstance(
                                (model as Record).recordID,
                                insertTemplate = false
                            ),
                            ISupportFragment.SINGLETASK
                        )
                }

                override fun onItemClick(position: Int, view: View, itemID: Int, model: Any) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            }
        )
    }

    // Retrieve all saved templates from database
    private fun retrieveAllTemplates() {
        templateNameList.clear()

        // Send callback for database operation
        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                // Retrieve all saved templates
                templateList = database.recordDAO().retrieveAllTemplates()

                // Add all template names into the List
                templateList.forEach { templateNameList.add(it.templateName) }
            }
        })
    }

    override fun onFragmentResult(requestCode: Int, resultCode: Int, data: Bundle?) {
        super.onFragmentResult(requestCode, resultCode, data)

        // If record is modified in previous Fragment, then proceed
        if (resultCode == ISupportFragment.RESULT_OK) {
            // Show message on the Snackbar
            showSnackbar(
                RecordViewFragment_RelativeLayout,
                data?.getString(Constant.KEY1).toString(),
                Snackbar.LENGTH_SHORT,
                null,
                null,
                null
            )

            // Send callback for database operation
            performDBAction(object : AppExecutorCallback {
                override fun executeDBTransactions() {
                    // Compose SQL query and retrieve selected records from database
                    searchAndRetrieveRecords(searchText, orderBy, sequence)
                }
            })
        }
    }

    // Initialize and populate the RecyclerView with retrieved records
    /*private fun initializeAndPopulateRecyclerView() {
        // Instantiate GenericRecyclerAdapter and attach it to RecyclerView
        mAdapter = GenericRecyclerAdapter(recordList as MutableList<Any>, 0, "", _mActivity)
        RecordList_RecyclerView.also {
            it.layoutManager = LinearLayoutManager(_mActivity)
            it.itemAnimator = DefaultItemAnimator()
            it.adapter = mAdapter
            it.isNestedScrollingEnabled = false
        }

        mAdapter.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(position: Int, view: View, model: Any) {
                // Pin record
                if (view.id == R.id.Pin_ImgView) {
                    pinRecord(model as Record)
                    // Modify record
                } else
                    startForResult(
                        ModifyRecordFragment.newInstance(
                            (model as Record).recordID,
                            insertTemplate = false
                        ),
                        ISupportFragment.SINGLETASK
                    )
            }

            override fun onItemClick(position: Int, view: View, itemID: Int, model: Any) {

            }
        })
    }*/

    // Pin the record
    private fun pinRecord(record: Record) {
        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                // Update the record with pin status
                database.recordDAO().updateRecord(record.apply { isPinned = !isPinned })

                // Show message on the Snackbar
                _mActivity.runOnUiThread {
                    Snackbar.make(
                        RecordViewFragment_RelativeLayout,
                        getString(if (record.isPinned) R.string.record_pinned else R.string.unpinned),
                        Snackbar.LENGTH_SHORT
                    ).show()

                    // Compose SQL query and retrieve selected records from database
                    searchAndRetrieveRecords(searchText, orderBy, sequence)
                }
            }
        })
    }

    // Show available columns for sorting dialog
    private fun showColumnNameDialog() {
        MaterialDialog(_mActivity).show {
            title(R.string.sort_by_field)
            listItems(res = R.array.column_name_option) { _, columnIndex, _ ->
                MaterialDialog(_mActivity).show {
                    title(R.string.sort_by_sequence)
                    listItems(res = R.array.sequence_option) { _, index, _ ->
                        // Get selected column and sequence for sorting
                        orderBy = Record.returnColumnName(columnIndex)
                        sequence = index

                        // Compose SQL query and retrieve selected records from database
                        searchAndRetrieveRecords(searchText, orderBy, sequence)

                        // Add Chip to display current sorting condition
                        addChip(
                            resources.getStringArray(R.array.column_name_option)[columnIndex],
                            resources.getStringArray(R.array.sequence_option)[index]
                        )
                    }
                }
            }
        }
    }

    // Add Chip to display current sorting condition
    private fun addChip(sortByText: String, sortSequenceText: String) {
        SearchFilter_ChipGroup.also {
            // Remove all Chips in the ChipGroup
            it.removeAllViews()

            // Add Chip for sorting column
            it.addView(Chip(_mActivity).also { chip ->
                // If column is selected, then set column name as Chip text, else set default text
                chip.text = if (sortByText.isNotBlank()) sortByText else Constant.titleText
            })

            // Add Chip for sorting sequence
            it.addView(Chip(_mActivity).also { chip ->
                // If column is selected, then set column name as Chip text, else set default text
                chip.text =
                    if (sortSequenceText.isNotBlank()) sortSequenceText else Constant.ascText
            })
        }
    }

    // Compose SQL query and retrieve selected records from database
    private fun searchAndRetrieveRecords(searchText: String, orderBy: String, sequence: Int) {
        // Send callback for database operation
        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                // Compose SQL string by using passed in parameters
                val sqlString =
                    SQLiteHelper.composeSQLString(Constant.Record, searchText, orderBy, sequence)
                recordList = database.recordDAO().searchAndSortRecords(SimpleSQLiteQuery(sqlString))

                // Refresh the RecyclerView with updated item list
                _mActivity.runOnUiThread {
                    Empty_TxtView.visibility =
                        if (recordList.size == 0) View.VISIBLE else View.GONE
                    mAdapter.setItems(recordList)
                }
            }
        })
    }

    override fun onButtonClicked(buttonCode: Int) {

    }

    override fun onSearchStateChanged(enabled: Boolean) {

    }

    override fun onSearchConfirmed(text: CharSequence?) {
        //searchRecord(text.toString().trim())
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.sort_action)
            showColumnNameDialog()
        return true
    }

    /*override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu
        inflater.inflate(R.menu.all_records_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    *//*override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        menu.getItem(1).isVisible = !isNewRecord
    }*//*

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit_template_action -> editTemplate()
            R.id.delete_template_action -> deleteTemplate()
        }
        return super.onOptionsItemSelected(item)
    }*/

    override fun onSupportVisible() {
        super.onSupportVisible()

        // Send callback for database operation
        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                // Retrieve all saved templates from database
                retrieveAllTemplates()
            }
        })

        // Set ActionBar title
        (_mActivity as ActionBarTitleCallback).setActionBarTitle(getString(R.string.app_name))
    }

    companion object {

        fun newInstance(): RecordViewFragment {
            return RecordViewFragment()
        }
    }
}
