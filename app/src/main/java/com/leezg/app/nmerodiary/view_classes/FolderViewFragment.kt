package com.leezg.app.nmerodiary.view_classes

import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.sqlite.db.SimpleSQLiteQuery
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.datetime.datePicker
import com.afollestad.materialdialogs.datetime.timePicker
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.leezg.app.nmerodiary.BaseFragment
import com.leezg.app.nmerodiary.R
import com.leezg.app.nmerodiary.adapters.GenericRecyclerAdapter
import com.leezg.app.nmerodiary.interfaces.ActionBarTitleCallback
import com.leezg.app.nmerodiary.interfaces.AppExecutorCallback
import com.leezg.app.nmerodiary.interfaces.OnItemClickListener
import com.leezg.app.nmerodiary.models.Folder
import com.leezg.app.nmerodiary.models.FolderCondition
import com.leezg.app.nmerodiary.models.Record
import com.leezg.app.nmerodiary.others.AppDatabase
import com.leezg.app.nmerodiary.others.Constant
import com.leezg.app.nmerodiary.others.SQLiteHelper
import kotlinx.android.synthetic.main.fragment_folder_view.*
import me.yokeyword.fragmentation.ISupportFragment
import java.util.*


class FolderViewFragment : BaseFragment(), View.OnKeyListener {

    private lateinit var folderConditionDialog: MaterialDialog
    private lateinit var selectFieldBtn: MaterialButton
    private lateinit var conditionValuesTxtInput: TextInputLayout
    private lateinit var conditionOperatorTxtView: TextView
    private lateinit var conditionValuesChipGroup: ChipGroup

    private lateinit var database: AppDatabase
    private lateinit var mAdapter: GenericRecyclerAdapter

    private var folderConditionsList: MutableList<FolderCondition> = mutableListOf()
    private var columnNamesList: MutableList<String> = mutableListOf()
    private var conditionOperatorsList: MutableList<Int> = mutableListOf()
    private var valuesList: MutableList<MutableList<String>> = mutableListOf()
    private var conditionValuesList: MutableList<String> = mutableListOf()
    private var fieldNameList: MutableList<String> = mutableListOf()
    private var recordList: MutableList<Record> = mutableListOf()
    private var folder = Folder()
    private var isMultipleValues = false
    private var isNewFolder = false
    private var mFolderID = 0
    private var mFolderType = 0
    private var folderIdentifier = ""
    private var conditionOperatorIndex = -1
    private var dbColumnName = ""
    private var chipFieldName = ""
    private var chipCondOperator = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_folder_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Room database
        database = AppDatabase.getInstance(_mActivity)!!

        // Display back button on the Toolbar
        _mActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get arguments passed in from previous Fragment
        arguments?.let {
            mFolderID = it.getInt(PARM1)
            mFolderType = it.getInt(PARM2)
            isNewFolder = mFolderID == 0

            // If it's new folder, then create new folder and store into database
            if (isNewFolder) {
                folderIdentifier = UUID.randomUUID().toString()
                createNewFolder()
                // Else retrieve existing folder data from database
            } else
                retrieveFolderData()
        }

        // Retrieve all saved field names for selection
        retrieveAllFieldNames()

        // Initialize all View components
        initializeViewComponents()
    }

    // Retrieve existing folder data from database
    private fun retrieveFolderData() {
        columnNamesList.clear()
        conditionOperatorsList.clear()
        valuesList.clear()

        // Send callback for database operation
        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                // Retrieve folder data
                folder = database.folderDAO().retrieveFolderByID(mFolderID)
                folderIdentifier = folder.identifier

                // Retrieve folder conditions for filter purpose
                folderConditionsList =
                    database.folderConditionDAO().retrieveConditionByID(mFolderID)

                _mActivity.runOnUiThread {
                    // Loop through folder conditions and add it into the Lists
                    folderConditionsList.forEach {
                        columnNamesList.add(it.columnName)
                        conditionOperatorsList.add(it.conditionOperator)
                        valuesList.add(it.values.toMutableList())

                        // Add Chip to display filtered conditions
                        addChip(
                            it.displayColumnName,
                            resources.getStringArray(R.array.condition_operator_option)[it.conditionOperator].toLowerCase(),
                            it.folderConditionID
                        )
                    }

                    // Compose SQL query and retrieve selected records from database
                    filterAndRetrieveRecords("", 0)
                }
            }
        })
    }

    // Create new folder and store into database
    private fun createNewFolder() {
        // Send callback for database operation
        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                // Store new folder data
                database.folderDAO().insertFolder(
                    Folder(0, "", 0).apply {
                        timestampCreated = System.currentTimeMillis()
                        identifier = folderIdentifier
                        folderType = mFolderType
                    })

                // Retrieve the newly created folder ID
                mFolderID = database.folderDAO().retrieveFolderIDByIdentifier(folderIdentifier)

                // Compose SQL query and retrieve selected records from database
                filterAndRetrieveRecords("", 0)
            }
        })
    }

    // Retrieve all saved field names for selection
    private fun retrieveAllFieldNames() {
        fieldNameList.clear()

        // Send callback for database operation
        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                // Add all field names into the List
                fieldNameList.addAll(resources.getStringArray(R.array.all_column_names_option))
                fieldNameList.addAll(database.fieldDAO().retrieveDistinctFieldName(Constant.SAVED_FIELD_NAME))
            }
        })
    }

    // Compose SQL query and retrieve selected records from database
    private fun filterAndRetrieveRecords(actionType: String, conditionIndex: Int) {
        // Send callback for database operation
        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                if (actionType.isNotBlank()) {
                    if (actionType == Constant.INSERT)
                        database.folderConditionDAO().insertFolderCondition(createFolderCondition())
                    else
                        database.folderConditionDAO().deleteFolderConditionByID(
                            mFolderID,
                            conditionIndex
                        )
                }

                // Compose SQL string by using passed in parameters
                val sqlString =
                    SQLiteHelper.composeSQLString(
                        Constant.Record,
                        columnNamesList,
                        conditionOperatorsList,
                        valuesList
                    )

                // Retrieve matching records with specified conditions
                recordList = database.recordDAO().searchAndSortRecords(SimpleSQLiteQuery(sqlString))

                _mActivity.runOnUiThread {
                    // Show or hide the blank TextView based on list size
                    Empty_TxtView.visibility =
                        if (recordList.size == 0) View.VISIBLE else View.GONE

                    // Refresh the RecyclerView with updated item list
                    mAdapter.setItems(recordList)
                }
            }
        })
    }

    // Delete folder data from database
    private fun deleteFolder() {
        MaterialDialog(_mActivity).show {
            title(R.string.warning)
            message(R.string.delete_folder_warning)
            positiveButton(R.string.yes) {
                // Send callback for database operation
                performDBAction(object : AppExecutorCallback {
                    override fun executeDBTransactions() {
                        // Delete folder data and conditions
                        database.folderDAO().deleteFolder(folder)
                        database.folderConditionDAO().deleteFolderCondition(mFolderID)

                        // Return Fragment result to parent Fragment
                        setFragmentResult(ISupportFragment.RESULT_OK, null)
                        _mActivity.onBackPressed()
                    }
                })
            }
            negativeButton(android.R.string.cancel) {
                // Dismiss the dialog
                dismiss()
            }
        }
    }

    // Show dialog to allow user to choose condition operator
    private fun showConditionOperatorDialog() {
        MaterialDialog(_mActivity).show {
            title(R.string.select_condition_operator)
            listItems(R.array.condition_operator_option) { _, index, conditionOperator ->
                conditionOperatorIndex = index
                chipCondOperator = conditionOperator.toString().toLowerCase()

                conditionValuesChipGroup.removeAllViews()
                conditionValuesList.clear()
                conditionOperatorsList.add(conditionOperatorIndex)
                conditionOperatorTxtView.text = conditionOperator

                // Enable TextInputLayout helper text and display hint on it
                conditionValuesTxtInput.also {
                    it.isHelperTextEnabled = (conditionOperator in Constant.multiValCondOperator)
                    it.helperText =
                        if (conditionOperator in Constant.multiValCondOperator) getString(R.string.allow_multiple_values) else ""
                }

                isMultipleValues = conditionOperator in Constant.multiValCondOperator
                conditionValuesTxtInput.isEnabled = true
            }
        }
    }

    // Instantiate and return FolderCondition object
    private fun createFolderCondition(): FolderCondition {
        return FolderCondition(0).apply {
            folderID = mFolderID
            identifier = folderIdentifier
            columnName = dbColumnName
            conditionOperator = conditionOperatorIndex
            values = valuesList[columnNamesList.size.minus(1)] as ArrayList<String>
            displayColumnName = selectFieldBtn.text.toString()
        }
    }

    private fun submitDataInput() {
        if (validateDataInput()) {
            valuesList.add(if (!isMultipleValues) mutableListOf(conditionValuesTxtInput.editText?.text.toString()) else conditionValuesList)

            filterAndRetrieveRecords(Constant.INSERT, 0)

            addChip(chipFieldName, chipCondOperator, -1)

            // Reset the Buttons' text
            selectFieldBtn.text = getString(R.string.select_field)
            conditionOperatorTxtView.text = getString(R.string.select_condition_operator)
            conditionValuesChipGroup.removeAllViews()
        }

        conditionValuesTxtInput.editText?.text?.clear()
    }

    private fun validateDataInput(): Boolean {
        var errorMessage = ""
        val listSize = conditionValuesList.size

        // If maximum character is exceeded
        if (conditionValuesTxtInput.editText?.text?.count()!! > Constant.MAX_INPUT_LENGTH)
            errorMessage = getString(R.string.maximum_char_exceeded)

        // If [Are] operator is selected and less than 1 input
        if (conditionOperatorIndex == 1 && listSize < 1)
            errorMessage = getString(R.string.no_input_entered)

        // If [Between] operator is selected and less than 2 inputs
        if (conditionOperatorIndex == 2 && (listSize > 2 || listSize < 2))
            errorMessage = getString(R.string.inputs_required, 2)

        // If there is error, then show error message on Snackbar
        if (errorMessage.isNotBlank()) {
            conditionValuesList.clear()
            conditionValuesChipGroup.removeAllViews()

            // Show message and action on the Snackbar
            showSnackbar(
                FolderView_RelativeLayout,
                errorMessage,
                Snackbar.LENGTH_SHORT,
                null,
                null,
                null
            )
            return false
        }

        return true
    }

    private fun showDateTimeDialog() {
        MaterialDialog(_mActivity).show {
            datePicker { _, date ->
                valuesList.add(mutableListOf(date.timeInMillis.toString()))
                conditionValuesList.add(date.timeInMillis.toString())

                MaterialDialog(_mActivity).show {
                    timePicker { _, time ->
                        valuesList.removeAt(valuesList.size.minus(1))
                        valuesList.add(mutableListOf(time.timeInMillis.toString()))

                        conditionValuesList.removeAt(conditionValuesList.size.minus(1))
                        conditionValuesList.add(time.timeInMillis.toString())

                        filterAndRetrieveRecords(Constant.INSERT, 0)
                    }
                }
            }
        }
    }

    private fun showFieldNameDialog() {
        MaterialDialog(_mActivity).show {
            title(R.string.select_field)
            listItems(items = fieldNameList) { _, index, fieldName ->
                dbColumnName =
                    if (Record.returnColumnName(index).isBlank()) fieldNameList[index] else Record.returnColumnName(
                        index
                    )
                columnNamesList.add(dbColumnName)
                selectFieldBtn.text = fieldName
                chipFieldName = fieldName.toString()

                conditionOperatorTxtView.isEnabled = true
            }
        }
    }

    // Add Chip to display filtered conditions
    private fun addChip(columnName: String, conditionOperator: String, conditionIndex: Int) {
        Log.d(Constant.LOG_TAG, "Column name: $columnName, Condition operator: $conditionOperator")

        val value = Gson().toJson(valuesList[FolderCondition_ChipGroup.childCount])

        FolderCondition_ChipGroup.also {
            // Add Chip to display folder condition
            it.addView(Chip(_mActivity).also { chip ->
                // If column is selected, then set column name as Chip text, else set default text
                chip.text =
                    getString(
                        R.string.folder_condition_chip_text,
                        columnName,
                        conditionOperator,
                        value
                    )
                chip.isCloseIconVisible = true
                chip.tag = if (conditionIndex > -1) conditionIndex else -1
                chip.setOnCloseIconClickListener { c ->
                    val index = it.indexOfChild(chip)
                    it.removeViewAt(index)
                    columnNamesList.removeAt(index)
                    conditionOperatorsList.removeAt(index)
                    valuesList.removeAt(index)

                    filterAndRetrieveRecords(Constant.DELETE, c.tag.toString().toInt())
                }
            })
        }
    }

    // Initialize all View components
    private fun initializeViewComponents() {
        mAdapter = initializeAndPopulateRecyclerView(
            dataList = recordList as MutableList<Any>,
            viewType = 2,
            extraParm = "",
            recyclerView = RecordList_RecyclerView,
            callback = object : OnItemClickListener {
                override fun onItemClick(position: Int, view: View, model: Any) {
                    startForResult(
                        ModifyRecordFragment.newInstance(
                            (model as Record).recordID, insertTemplate = false
                        ), ISupportFragment.SINGLETASK
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

        folderConditionDialog = setupFolderConditionDialog()
    }

    private fun setupFolderConditionDialog(): MaterialDialog {
        folderConditionDialog = MaterialDialog(_mActivity).customView(
            R.layout.folder_condition_layout,
            scrollable = true
        )
        val customView = folderConditionDialog.getCustomView()
        customView.also {
            selectFieldBtn = it.findViewById(R.id.SelectField_Btn)
            selectFieldBtn.also { materialButton ->
                materialButton.text = getString(R.string.select_field)
                materialButton.setOnClickListener {
                    showFieldNameDialog()
                }
            }

            conditionOperatorTxtView = it.findViewById(R.id.ConditionOperator_TxtView)
            conditionOperatorTxtView.also { textView ->
                textView.text = getString(R.string.select_condition_operator)
                textView.isEnabled = false

                // Show dialog to allow user to choose condition operator when the TextView is clicked
                textView.setOnClickListener {
                    showConditionOperatorDialog()
                }
            }

            conditionValuesTxtInput = it.findViewById(R.id.ConditionValues_TxtInput)
            conditionValuesTxtInput.also { textInputLayout ->
                textInputLayout.hint = getString(R.string.enter_values)
                textInputLayout.editText?.setOnKeyListener(this)
                textInputLayout.counterMaxLength = Constant.MAX_INPUT_LENGTH
                textInputLayout.isEnabled = false
            }

            conditionValuesChipGroup = it.findViewById(R.id.ConditionValues_ChipGroup)
        }

        folderConditionDialog.also {
            it.positiveButton(R.string.done) {
                submitDataInput()
            }

            it.negativeButton(R.string.select_date_time) {
                showDateTimeDialog()
            }
        }

        return folderConditionDialog
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu
        inflater.inflate(R.menu.folder_view_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onKey(view: View, keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == EditorInfo.IME_ACTION_SEARCH || keyCode == EditorInfo.IME_ACTION_DONE || event.action == KeyEvent.ACTION_DOWN &&
            event.keyCode == KEYCODE_ENTER && isMultipleValues
        ) {
            var value = ""

            if (view.id == R.id.ConditionValues_EditTxt) {
                conditionValuesTxtInput.apply {
                    value = editText?.text.toString()
                    conditionValuesList.add(value)
                    editText?.text?.clear()
                }
                conditionValuesChipGroup.addView(Chip(_mActivity).also {
                    it.text = if (value.trim().isBlank()) getString(R.string.blank) else value
                    it.isCloseIconVisible = true
                    it.setOnCloseIconClickListener { c ->
                        val index = conditionValuesChipGroup.indexOfChild(it)
                        conditionValuesChipGroup.removeViewAt(index)
                        conditionValuesList.removeAt(index)
                    }
                })
                return true
            }
        }
        return false
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        // Hide the settings menu
        menu.getItem(0).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> _mActivity.onBackPressed()
            R.id.add_condition_action -> folderConditionDialog.show()
            R.id.delete_folder_action -> deleteFolder()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        // Set ActionBar title
        (_mActivity as ActionBarTitleCallback).setActionBarTitle(
            when {
                isNewFolder -> getString(R.string.new_folder)
                else -> folder.folderName
            }
        )
    }

    companion object {

        private val PARM1 = "parm1"
        private val PARM2 = "parm2"

        fun newInstance(folderID: Int, folderType: Int): FolderViewFragment {
            val fragment = FolderViewFragment()
            val args = Bundle()
            args.putInt(PARM1, folderID)
            args.putInt(PARM2, folderType)
            fragment.arguments = args
            return fragment
        }
    }
}
