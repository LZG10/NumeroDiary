package com.leezg.app.nmerodiary.view_classes

import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.graphics.Paint
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.NestedScrollView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.afollestad.materialdialogs.datetime.timePicker
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.leezg.app.nmerodiary.BaseFragment
import com.leezg.app.nmerodiary.R
import com.leezg.app.nmerodiary.interfaces.ActionBarTitleCallback
import com.leezg.app.nmerodiary.interfaces.AppExecutorCallback
import com.leezg.app.nmerodiary.models.Field
import com.leezg.app.nmerodiary.models.Record
import com.leezg.app.nmerodiary.others.AppDatabase
import com.leezg.app.nmerodiary.others.Constant
import com.leezg.app.nmerodiary.others.DateTimeHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_modify_record.*
import me.yokeyword.fragmentation.ISupportFragment
import java.text.SimpleDateFormat
import java.util.*


class ModifyRecordFragment : BaseFragment() {

    private val dateTimeInputList = listOf(
        InputType.TYPE_DATETIME_VARIATION_TIME,     //Constant.TIME
        InputType.TYPE_DATETIME_VARIATION_DATE,     //Constant.DATE
        InputType.TYPE_CLASS_DATETIME
    )
    private var templateList: MutableList<Record> = mutableListOf()
    private var mFieldList: MutableList<Field> = mutableListOf()
    private var fieldNameList: MutableList<String> = mutableListOf()
    private var fieldUnitList: MutableList<String> = mutableListOf()
    private var recordID: Int = 0
    private var record: Record = Record()
    private var isNewRecord = false
    private var saveAsTemplate = false
    private var saveAndExit = false
    private var isNewTemplate = false
    private var isNewField = false
    private var dataString: String = ""

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this Fragment
        return inflater.inflate(R.layout.fragment_modify_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Room database
        database = AppDatabase.getInstance(_mActivity)!!

        // Display back button on the Toolbar
        _mActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Always hide the BottomNavigationView in this Fragment
        _mActivity.Main_BottomNav.visibility = View.GONE

        // Get arguments passed in from previous Fragment
        arguments?.let {
            recordID = it.getInt(PARM1)
            isNewTemplate = it.getBoolean(PARM2)
            isNewRecord = recordID == 0

            // If it's not new record, then retrieve all data and display it
            if (!isNewRecord) {
                // Send callback for database operation
                performDBAction(object : AppExecutorCallback {
                    override fun executeDBTransactions() {
                        // Retrieve all saved records
                        record = database.recordDAO().retrieveRecordByID(recordID.toString())!!
                        isNewRecord = record.isTemplate

                        _mActivity.runOnUiThread {
                            // Display retrieved data on the View components
                            displayRecordData()

                            // Refresh the menu to hide some options
                            _mActivity.invalidateOptionsMenu()
                        }
                    }
                })
                // Else inflate and add new row for new input field or display data on the field
            } else
                addRow(-1, null)
        }

        // Show or hide FloatingActionButton upon scrolling
        ModifyRecord_ScrollView.setOnScrollChangeListener { _: NestedScrollView?, _: Int, dy: Int, _: Int, oldDy: Int ->
            if (dy > oldDy)
                AddRecord_FAB.hide()
            else
                AddRecord_FAB.show()
        }

        // Inflate and add new row for new input field or display data on the field when the FloatingActionButton is clicked
        AddRecord_FAB.setOnClickListener {
            isNewField = true

            addRow(-1, null)
        }

        // Retrieve all saved field names for selection
        retrieveFieldData()
    }

    // Retrieve all saved field names for selection
    private fun retrieveFieldData() {
        // Send callback for database operation
        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                fieldNameList =
                    database.fieldDAO().retrieveDistinctFieldName(Constant.SAVED_FIELD_NAME)
                fieldUnitList =
                    database.fieldDAO().retrieveDistinctFieldUnit(Constant.SAVED_FIELD_UNIT)
            }
        })
    }

    // Display retrieved data on the View components
    private fun displayRecordData() {
        RecordTitle_TxtInput.editText?.setText(record.recordTitle)
        RecordRemarks_TxtInput.editText?.setText(record.recordRemarks)
        TimestampModified_TxtView.text = getString(
            R.string.last_modified_on,
            DateTimeHelper.getTimeAgo(record.timestampModified, _mActivity)
        )
        TimestampModified_TxtView.visibility =
            if (record.timestampModified > 0) View.VISIBLE else View.GONE

        // Inflate and add new row for new input field or display data on the field
        for (field in record.fieldList) {
            addRow(-1, field)
        }
    }

    // Delete record data from the database
    private fun deleteRecord() {
        MaterialDialog(_mActivity).show {
            title(R.string.warning)
            message(R.string.delete_record_warning)
            positiveButton(R.string.yes) {
                performDBAction(object : AppExecutorCallback {
                    override fun executeDBTransactions() {
                        database.fieldDAO().deleteFieldByIdentifier(record.identifier)
                        database.recordDAO().deleteRecord(record)

                        saveAndExit = true

                        setFragmentResult(ISupportFragment.RESULT_OK, Bundle().apply {
                            putString(Constant.KEY1, getString(R.string.record_deleted))
                        })
                        _mActivity.onBackPressed()
                    }
                })
            }
            negativeButton(android.R.string.cancel) {
                dismiss()
            }
        }
    }

    // Check for new changes
    private fun validateChanges(): Boolean {
        val tempFieldList: ArrayList<Field> = arrayListOf()

        // Get data from TextInput
        val recordTitle = RecordTitle_TxtInput.editText?.text.toString().trim()
        val recordRemarks = RecordRemarks_TxtInput.editText?.text.toString().trim()

        // Loop through the children in TableLayout and add all fields' data into the List
        for (rowIndex in 0 until RecordFields_TableLayout.childCount) {
            val field = collectFieldValues(rowIndex)
            tempFieldList.add(field)
        }

        return if (saveAsTemplate)
            true
        else if (isNewRecord && (recordTitle.isNotBlank() || recordRemarks.isNotBlank() || tempFieldList.size > 0))
            false
        else !record.equals(
            Record().apply {
                this.recordTitle = recordTitle
                this.recordRemarks = recordRemarks
                fieldList = tempFieldList

                if (isNewTemplate)
                    templateName = record.templateName
            })
        /*return !(!saveAsTemplate || !isNewRecord || recordTitle.isNotBlank() || recordRemarks.isNotBlank()) or !record.equals(
            Record().apply {
                this.recordTitle = recordTitle
                this.recordRemarks = recordRemarks
                fieldList = tempFieldList

                if (insertTemplate)
                    templateName = record.templateName
            })*/
    }

    // Save data into the database
    private fun collectRecordData() {
        val identifier = UUID.randomUUID().toString()
        saveAndExit = true
        mFieldList.clear()

        // Get data from TextInput
        val recordTitle = RecordTitle_TxtInput.editText?.text.toString().trim()
        val recordRemarks = RecordRemarks_TxtInput.editText?.text.toString().trim()

        // If title is blank, then show warning on the Snackbar
        if (TextUtils.isEmpty(recordTitle) && saveAndExit) {
            showSnackbar(
                ModifyRecord_CoordLayout,
                getString(R.string.blank_title_hint),
                Snackbar.LENGTH_SHORT,
                null,
                null,
                AddRecord_FAB
            )

            return
        }

        // Loop through the children in TableLayout and add all fields' data into the List
        for (rowIndex in 0 until RecordFields_TableLayout.childCount) {
            val field = collectFieldValues(rowIndex)
            mFieldList.add(field.apply { this.identifier = identifier })
        }

        val record = Record(
            if (isNewRecord) 0 else record.recordID,
            recordTitle,
            System.currentTimeMillis()
        ).apply {
            fieldList = mFieldList as ArrayList<Field>
            timestampCreated = if (isNewRecord) System.currentTimeMillis() else timestampCreated
            timestampModified = if (!isNewTemplate) System.currentTimeMillis() else 0
            this.recordRemarks = recordRemarks
            this.identifier = identifier
            isPinned = record.isPinned
            searchString = dataString + recordTitle + recordRemarks
        }

        /*updatedRecord = if (isNewRecord)
            Record(0, recordTitle, System.currentTimeMillis()).apply {
                fieldList = mFieldList as ArrayList<Field>
                timestampCreated = System.currentTimeMillis()
                this.recordRemarks = recordRemarks
            } else
            record.apply {
                this.recordTitle = recordTitle
                this.recordRemarks = recordRemarks
                fieldList = mFieldList as ArrayList<Field>
                timestampModified = System.currentTimeMillis()
            }*/

        if (isNewTemplate)
            inputTemplateName()
        else if (saveAndExit)
            saveRecord(record)
    }

    private fun saveRecord(record: Record) {
        // 1. Save or update record into the database based on whether it's new record
        // 2. Insert template as new record
        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                if (isNewRecord || record.isTemplate)
                    database.recordDAO().insertRecord(record)
                else
                    database.recordDAO().updateRecord(record)

                /*for (field in mFieldList)
                    database.fieldDAO().insertField(field)*/

                mFieldList.forEach { database.fieldDAO().insertField(it) }

                setFragmentResult(ISupportFragment.RESULT_OK, Bundle().apply {
                    putString(Constant.KEY1, getString(R.string.record_saved))
                })
                _mActivity.onBackPressed()
            }
        })
    }

    // Collect data from created fields and store it in Field object
    private fun collectFieldValues(rowIndex: Int): Field {
        val row = RecordFields_TableLayout.getChildAt(rowIndex) as RelativeLayout
        val txtInput = row.findViewById<TextInputLayout>(R.id.RecordField_TxtInput)
        val fieldName =
            row.findViewById<TextInputLayout>(R.id.RecordField_TxtInput).hint.toString()
                .trim()
        val fieldValue =
            if (txtInput.editText?.inputType !in dateTimeInputList) txtInput.editText?.text.toString().trim()
            else convertDateTimeToMillis(txtInput.editText!!, true, null)
        val checkedStatus = row.findViewById<CheckBox>(R.id.RecordField_CheckBox).isChecked
        val fieldUnit = txtInput.helperText ?: ""

        dataString += fieldName + fieldValue + fieldUnit

        return Field(fieldName, fieldValue, txtInput.editText?.inputType!!).apply {
            isChecked = checkedStatus
            fieldImgList = arrayListOf()
            this.fieldUnit = fieldUnit.toString().trim()
        }
    }

    private fun convertDateTimeToMillis(
        editText: EditText,
        fromDateTime: Boolean,
        fieldData: String?
    ): String {
        //val dateTimeString = ""
        var formatter = SimpleDateFormat()
        val fieldValue = editText.text.toString().trim()

        when (editText.inputType) {
            InputType.TYPE_DATETIME_VARIATION_TIME -> formatter = Constant.timeFormat
            InputType.TYPE_DATETIME_VARIATION_DATE -> formatter = Constant.dateFormat
            InputType.TYPE_CLASS_DATETIME -> formatter = Constant.dateTimeFormat
        }

        return if (fromDateTime && fieldValue.isNotBlank())
            (formatter.parse(fieldValue) as Date).time.toString()
        else if (fieldData != null && fieldData.isNotBlank())
            formatter.format(fieldData.toLong())
        else
            ""
    }

    // Inflate and add new row for new input field or display data on the field
    private fun addRow(position: Int, field: Field?) {
        val inflater = _mActivity.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val row = inflater.inflate(R.layout.record_list_row, null)
        val moreOptionsBtn = row.findViewById<TextView>(R.id.MoreOptions_Btn)
        val recordFieldTxtInput = row.findViewById<TextInputLayout>(R.id.RecordField_TxtInput)
        val recordFieldCheckBox = row.findViewById<CheckBox>(R.id.RecordField_CheckBox)

        if (field == null && isNewField && fieldNameList.size > 0) {
            showFieldSelectionDialog(
                Constant.fieldName,
                getString(R.string.select_field_name),
                recordFieldTxtInput,
                fieldNameList
            )

            isNewField = false
        }

        recordFieldTxtInput.also {
            it.helperText = field?.fieldUnit ?: ""

            it.editText?.inputType = field?.fieldType ?: it.editText?.inputType!!

            it.hint = field?.fieldName
                ?: getString(
                    R.string.new_field,
                    if (position == -1) RecordFields_TableLayout.childCount.plus(1) else position.plus(
                        1
                    )
                )

            it.editText?.setText(
                if (it.editText?.inputType in dateTimeInputList)
                    convertDateTimeToMillis(it.editText!!, false, field?.fieldValue)
                else
                    field?.fieldValue ?: ""
            )

            it.setEndIconOnClickListener { _ ->
                it.editText?.text?.clear()
            }

            it.editText?.setOnClickListener { _ ->
                if (it.editText!!.inputType in dateTimeInputList)
                    showDateTimeDialog(
                        it.editText?.inputType!!,
                        it.editText!!
                    )
            }
        }

        recordFieldCheckBox.also { it ->
            // Strike-through the text based on checked status
            it.setOnCheckedChangeListener { _, isChecked ->
                recordFieldTxtInput.also { txtInput ->
                    txtInput.editText?.paintFlags = if (isChecked)
                        txtInput.editText?.paintFlags!! or Paint.STRIKE_THRU_TEXT_FLAG
                    else
                        txtInput.editText?.paintFlags!! and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    txtInput.isEnabled = !isChecked
                }
            }

            it.isChecked = field?.isChecked ?: false
        }

        moreOptionsBtn.setOnClickListener {
            PopupMenu(_mActivity, moreOptionsBtn).apply {
                inflate(R.menu.modify_item_menu)

                // Disable the delete row option if there is only one row
                menu.getItem(3).isEnabled = RecordFields_TableLayout.childCount != 1

                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.edit_input_type_action -> {
                            MaterialDialog(_mActivity).show {
                                title(null, getString(R.string.select_input_type))
                                listItemsSingleChoice(
                                    R.array.field_type_option,
                                    initialSelection = Field.returnFieldType(recordFieldTxtInput.editText?.inputType!!)
                                ) { _, index, _ ->
                                    changeFieldType(
                                        recordFieldTxtInput,
                                        Field.getInputType(index)
                                    )
                                }
                            }
                            true
                        }
                        // Show dialog for inputting or clear field name
                        R.id.edit_field_name_action -> {
                            if (fieldNameList.size > 0)
                                showFieldSelectionDialog(
                                    Constant.fieldName,
                                    getString(R.string.select_field_name),
                                    recordFieldTxtInput,
                                    fieldNameList
                                )
                            else
                                showFieldInputDialog(
                                    Constant.fieldName,
                                    getString(R.string.new_field_name),
                                    recordFieldTxtInput
                                )
                            true
                        }
                        R.id.edit_unit_action -> {
                            if (fieldUnitList.size > 0)
                                showFieldSelectionDialog(
                                    Constant.fieldUnit,
                                    getString(R.string.select_field_unit),
                                    recordFieldTxtInput,
                                    fieldUnitList
                                )
                            else
                                showFieldInputDialog(
                                    Constant.fieldUnit,
                                    getString(R.string.new_field_unit),
                                    recordFieldTxtInput
                                )
                            true
                        }
                        // Remove the row from TableLayout
                        else -> {
                            deleteRow(RecordFields_TableLayout.indexOfChild(row), field)
                            true
                        }
                    }
                }
                show()
            }
        }

        // Append or insert the row into TableLayout at specific position
        RecordFields_TableLayout.addView(
            row,
            if (position == -1) RecordFields_TableLayout.childCount else position
        )
    }

    private fun showFieldSelectionDialog(
        type: String,
        title: String,
        recordFieldTxtInput: TextInputLayout,
        listItem: MutableList<String>
    ) {
        MaterialDialog(_mActivity).show {
            title(null, title)
            listItems(items = listItem, waitForPositiveButton = false) { _, index, _ ->
                if (type == Constant.fieldName)
                    recordFieldTxtInput.hint = listItem[index]
                else
                    recordFieldTxtInput.helperText = listItem[index]
                dismiss()
            }
            positiveButton(R.string.add) {
                showFieldInputDialog(
                    if (type == Constant.fieldName) Constant.fieldName else Constant.fieldUnit,
                    if (type == Constant.fieldName) getString(R.string.new_field_name) else getString(
                        R.string.new_field_unit
                    ), recordFieldTxtInput
                )
            }
        }
    }

    private fun showFieldInputDialog(
        type: String,
        title: String,
        recordFieldTxtInput: TextInputLayout
    ) {
        MaterialDialog(_mActivity).show {
            input(
                maxLength = Constant.MAX_INPUT_LENGTH
            ) { _, data ->
                val field = data.toString().trim()

                if ((type == Constant.fieldName && fieldNameList.contains(field)) || (type == Constant.fieldUnit && fieldUnitList.contains(
                        field
                    ))
                ) {
                    showSnackbar(
                        _mActivity.ModifyRecord_CoordLayout,
                        getString(R.string.data_exists),
                        Snackbar.LENGTH_SHORT,
                        null,
                        null,
                        AddRecord_FAB
                    )

                    return@input
                }

                performDBAction(object : AppExecutorCallback {
                    override fun executeDBTransactions() {
                        database.fieldDAO().insertField(Field().apply {
                            fieldID = 0
                            if (type == Constant.fieldName)
                                fieldName = field
                            else
                                fieldUnit = field
                            identifier =
                                if (type == Constant.fieldName) Constant.SAVED_FIELD_NAME else Constant.SAVED_FIELD_UNIT
                        })

                        retrieveFieldData()

                        _mActivity.runOnUiThread {
                            if (type == Constant.fieldName)
                                recordFieldTxtInput.hint = field
                            else
                                recordFieldTxtInput.helperText = field
                        }
                    }
                })
                dismiss()
            }
            positiveButton(R.string.done)
            negativeButton(R.string.clear) {
                getInputField().text.clear()
                noAutoDismiss()
            }
            title(null, title)
        }
    }

    private fun showDateTimeDialog(inputType: Int, editText: EditText) {
        when (inputType) {
            InputType.TYPE_DATETIME_VARIATION_DATE -> showDateDialog(false, editText)
            InputType.TYPE_DATETIME_VARIATION_TIME -> showTimeDialog(false, editText)
            else -> showDateDialog(true, editText)
        }
    }

    private fun showTimeDialog(showDateTime: Boolean, editText: EditText) {
        MaterialDialog(_mActivity).show {
            timePicker { _, date ->
                editText.setText(
                    if (!showDateTime) Constant.timeFormat.format(date.timeInMillis)
                    else Constant.dateTimeFormat.format(date.timeInMillis)
                )
            }
        }
    }

    private fun showDateDialog(showDateTime: Boolean, editText: EditText) {
        MaterialDialog(_mActivity).show {
            datePicker { _, time ->
                editText.setText(Constant.dateFormat.format(time.timeInMillis))

                if (showDateTime)
                    showTimeDialog(showDateTime, editText)
            }
        }
    }

    private fun changeFieldType(textInputLayout: TextInputLayout, inputType: Int) {
        textInputLayout.also {
            it.editText?.inputType = inputType
            it.isFocusable = inputType !in dateTimeInputList
        }
    }

    // Remove the row from TableLayout
    private fun deleteRow(position: Int, field: Field?) {
        RecordFields_TableLayout.removeViewAt(position)

        // Show message and action on the Snackbar
        showSnackbar(
            ModifyRecord_CoordLayout,
            getString(R.string.row_deleted),
            Snackbar.LENGTH_LONG,
            getString(R.string.undo),
            View.OnClickListener {
                addRow(position, field)
            },
            AddRecord_FAB
        )
    }

    private fun collectTemplateData(templateName: String) {
        mFieldList.clear()

        // Loop through the childs in TableLayout and add all fields' data into the List
        for (rowIndex in 0 until RecordFields_TableLayout.childCount) {
            val field = collectFieldValues(rowIndex)
            mFieldList.add(field)
        }

        // Get data from TextInput
        val recordTitle = RecordTitle_TxtInput.editText?.text.toString().trim()
        val recordRemarks = RecordRemarks_TxtInput.editText?.text.toString().trim()

        // If title is blank, then show warning on the Snackbar
        if (TextUtils.isEmpty(recordTitle)) {
            showSnackbar(
                ModifyRecord_CoordLayout,
                getString(R.string.blank_title_hint),
                Snackbar.LENGTH_SHORT,
                null,
                null,
                AddRecord_FAB
            )

            return
        }

        /*val record = Record(
            if (!insertTemplate) 0 else record.recordID,
            recordTitle,
            System.currentTimeMillis()
        ).apply {
            fieldList = mFieldList as ArrayList<Field>
            timestampCreated = if (isNewRecord) System.currentTimeMillis() else timestampCreated
            isTemplate = true
            this.templateName = templateName
            templateID = UUID.randomUUID().toString()
            this.recordRemarks = recordRemarks
        }*/

        saveTemplate(Record(
            if (!isNewTemplate) 0 else record.recordID, recordTitle, 0
        ).apply {
            fieldList = mFieldList as ArrayList<Field>
            timestampCreated = if (isNewRecord) System.currentTimeMillis() else timestampCreated
            isTemplate = true
            this.templateName = templateName
            templateID = UUID.randomUUID().toString()
            this.recordRemarks = recordRemarks
        })
    }

    private fun saveTemplate(record: Record) {
        var isPassed = true
        var errorMessage = ""

        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                templateList = database.recordDAO().retrieveAllTemplates()

                for (template in templateList) {
                    if (saveAsTemplate && template.templateName == record.templateName) {
                        isPassed = false
                        errorMessage = getString(R.string.template_name_exists)
                        break
                    }

                    if (!record.equals(template)) {
                        isPassed = false
                        errorMessage = getString(R.string.template_exists)
                        break
                    }
                }

                // Save or update data into the database based on whether it's new record
                if (isPassed) {
                    if ((isNewTemplate && recordID == 0) || saveAsTemplate)
                        database.recordDAO().insertRecord(record)
                    else
                        database.recordDAO().updateRecord(record)

                    setFragmentResult(ISupportFragment.RESULT_OK, Bundle().apply {
                        putString(Constant.KEY1, getString(R.string.template_saved))
                    })
                    _mActivity.onBackPressed()
                } else {
                    _mActivity.runOnUiThread {
                        // Show message on the Snackbar
                        showSnackbar(
                            ModifyRecord_CoordLayout,
                            errorMessage,
                            Snackbar.LENGTH_SHORT,
                            null,
                            null,
                            AddRecord_FAB
                        )
                    }
                }

                saveAsTemplate = false

                /*if (isPassed) {
                    setFragmentResult(ISupportFragment.RESULT_OK, null)
                    _mActivity.onBackPressed()
                }*/
            }
        })
    }

    private fun inputTemplateName() {
        MaterialDialog(_mActivity).show {
            input(
                maxLength = Constant.MAX_INPUT_LENGTH,
                prefill = if (isNewTemplate) record.templateName else ""
            ) { _, input ->
                collectTemplateData(input.toString().trim())
            }
            positiveButton(R.string.done)
            title(null, getString(R.string.enter_template_name))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu
        inflater.inflate(R.menu.modify_record_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        // Hide the settings menu
        menu.getItem(0).isVisible = false
        menu.getItem(2).isVisible = !isNewRecord
        menu.getItem(3).isVisible = !isNewRecord
        menu.getItem(3).title =
            getString(if (record.isPinned) R.string.unpin_record else R.string.pin_record)
        menu.getItem(4).isVisible = !isNewTemplate
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> _mActivity.onBackPressed()
            // Save data into the database
            R.id.save_record_action -> collectRecordData()
            // Delete record data from the database
            R.id.delete_record_action -> deleteRecord()
            R.id.pin_record_action -> pinRecord()
            R.id.save_template_action -> {
                saveAsTemplate = true

                inputTemplateName()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun pinRecord() {
        performDBAction(object : AppExecutorCallback {
            override fun executeDBTransactions() {
                database.recordDAO().updateRecord(record.apply { isPinned = !isPinned })

                setFragmentResult(
                    ISupportFragment.RESULT_OK,
                    Bundle().apply {
                        putString(
                            Constant.KEY1,
                            getString(if (record.isPinned) R.string.record_pinned else R.string.unpinned)
                        )
                    })
                _mActivity.onBackPressed()
            }
        })
    }

    override fun onBackPressedSupport(): Boolean {
        // 1. There is new changes in the record
        // 2. User save the record and then exit
        if (!(validateChanges() || saveAndExit)) {
            MaterialDialog(_mActivity).show {
                cancelable(false)
                title(R.string.warning)
                message(R.string.discard_changes_warning)
                positiveButton(R.string.yes) {
                    _mActivity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    _mActivity.onBackPressedSupport()
                }
                negativeButton(android.R.string.cancel) {
                    dismiss()
                }
            }
        } else {
            //_mActivity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            return super.onBackPressedSupport()
        }

        return true
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        // Set ActionBar title
        (_mActivity as ActionBarTitleCallback).setActionBarTitle(
            when {
                isNewTemplate -> if (recordID != 0) record.templateName else getString(R.string.new_template)
                !isNewRecord -> record.recordTitle
                else -> getString(R.string.new_record)
            }
        )
    }

    companion object {

        private val PARM1 = "parm1"
        private val PARM2 = "parm2"

        fun newInstance(recordID: Int, insertTemplate: Boolean): ModifyRecordFragment {
            val fragment = ModifyRecordFragment()
            val args = Bundle()
            args.putInt(PARM1, recordID)
            args.putBoolean(PARM2, insertTemplate)
            fragment.arguments = args
            return fragment
        }
    }
}