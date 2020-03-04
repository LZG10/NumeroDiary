package com.leezg.app.nmerodiary.view_classes


import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.google.android.material.snackbar.Snackbar
import com.leezg.app.nmerodiary.R
import com.leezg.app.nmerodiary.interfaces.ActionBarTitleCallback
import com.leezg.app.nmerodiary.models.Field
import com.leezg.app.nmerodiary.models.Record
import com.leezg.app.nmerodiary.others.AppDatabase
import com.leezg.app.nmerodiary.others.AppExecutors
import com.leezg.app.nmerodiary.others.Constant
import kotlinx.android.synthetic.main.activity_main.*
import me.yokeyword.fragmentation.ExtraTransaction
import me.yokeyword.fragmentation.ISupportFragment
import me.yokeyword.fragmentation.ISupportFragment.RESULT_OK
import me.yokeyword.fragmentation.SupportFragmentDelegate
import me.yokeyword.fragmentation.anim.FragmentAnimator


class SettingsFragment : PreferenceFragmentCompat(), ISupportFragment {

    private lateinit var database: AppDatabase

    private var templateList: MutableList<Record> = mutableListOf()
    private var fieldList: MutableList<Field> = mutableListOf()
    private var templateNameList: MutableList<String> = mutableListOf()
    private var fieldUnitList: MutableList<String> = mutableListOf()
    private var fieldNameList: MutableList<String> = mutableListOf()
    private val REQUEST_ADD_TEMPLATE = 1

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)
    }

    companion object {

        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }

    // Retrieve all saved fields from database
    private fun retrieveAllFieldUnits() {
        fieldUnitList.clear()
        fieldNameList.clear()
        fieldList.clear()

        AppExecutors.getInstance()!!.diskIO().execute {
            // Retrieve all saved fields
            fieldList =
                database.fieldDAO().retrieveAllFields(
                    Constant.fieldUnit,
                    Constant.SAVED_FIELD_UNIT,
                    Constant.SAVED_FIELD_NAME
                )

            // Add all saved units and field names into the List
            fieldList.forEach {
                if (it.identifier == Constant.SAVED_FIELD_UNIT)
                    fieldUnitList.add(it.fieldUnit)

                if (it.identifier == Constant.SAVED_FIELD_NAME)
                    fieldNameList.add(it.fieldName)
            }
        }
    }

    // Retrieve all saved templates from database
    private fun retrieveAllTemplates() {
        templateNameList.clear()
        templateList.clear()

        AppExecutors.getInstance()!!.diskIO().execute {
            // Retrieve all saved templates
            templateList = database.recordDAO().retrieveAllTemplates()

            // Add all template names into the List
            templateList.forEach { templateNameList.add(it.templateName) }
        }
    }

    // Show input dialog for field name or unit
    private fun showFieldInputDialog(
        index: Int,
        isUpdate: Boolean,
        dialogTitle: Int,
        type: String
    ) {
        MaterialDialog(_mActivity!!).show {
            title(dialogTitle)
            input(
                prefill = if (index > -1) if (type == Constant.fieldUnit) fieldUnitList[index] else fieldNameList[index] else "",
                maxLength = Constant.MAX_INPUT_LENGTH
            ) { _, fieldValue ->
                saveFieldData(Field().apply {
                    fieldID = if (isUpdate) fieldList[index].fieldID else 0
                    if (type == Constant.fieldUnit)
                        fieldUnit = fieldValue.toString().trim()
                    else
                        fieldName = fieldValue.toString().trim()
                    identifier =
                        if (type == Constant.fieldUnit) Constant.SAVED_FIELD_UNIT else Constant.SAVED_FIELD_NAME
                }, isUpdate, type)
            }
        }
    }

    private fun editFieldData(type: String) {
        if ((type == Constant.fieldUnit && fieldUnitList.size > 0) || (type == Constant.fieldName && fieldNameList.size > 0)) {
            MaterialDialog(_mActivity!!).show {
                title(
                    null,
                    if (type == Constant.fieldUnit) getString(R.string.edit_unit) else getString(R.string.edit_field_name)
                )
                listItems(
                    items = if (type == Constant.fieldUnit) fieldUnitList else fieldNameList,
                    waitForPositiveButton = false
                ) { _, index, _ ->
                    // Show input dialog for field name or unit
                    showFieldInputDialog(
                        index = index, isUpdate = true,
                        dialogTitle = R.string.edit_unit,
                        type = type
                    )
                    dismiss()
                }
                positiveButton(R.string.add) {
                    // Show input dialog for field name or unit
                    showFieldInputDialog(
                        index = -1,
                        isUpdate = false,
                        dialogTitle = R.string.add,
                        type = type
                    )
                    dismiss()
                }
                negativeButton(R.string.delete) {
                    deleteFieldUnit(type)
                    dismiss()
                }
            }
        } else {
            // Show input dialog for field name or unit
            showFieldInputDialog(
                index = -1,
                isUpdate = false,
                dialogTitle = if (type == Constant.fieldUnit) R.string.add_new_unit else R.string.add_new_field_name,
                type = type
            )
        }
    }

    private fun deleteFieldUnit(type: String) {
        MaterialDialog(_mActivity!!).show {
            title(if (type == Constant.fieldUnit) R.string.delete_unit else R.string.delete_field_name)
            positiveButton(R.string.done)
            negativeButton(android.R.string.cancel)
            listItemsMultiChoice(
                waitForPositiveButton = true,
                items = if (type == Constant.fieldUnit) fieldUnitList else fieldNameList
            ) { _, index, _ ->
                MaterialDialog(_mActivity!!).show {
                    title(R.string.warning)
                    message(R.string.delete_item_warning)
                    positiveButton(R.string.yes) {
                        AppExecutors.getInstance()!!.diskIO().execute {
                            for (i in index)
                                database.fieldDAO().deleteField(fieldList[i])

                            _mActivity!!.runOnUiThread {
                                Snackbar.make(
                                    getView()!!,
                                    getString(R.string.item_deleted),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }

                            retrieveAllFieldUnits()
                        }
                    }
                    negativeButton(android.R.string.cancel) {
                        dismiss()
                    }
                }
            }
        }
    }

    private fun saveFieldData(field: Field, isUpdate: Boolean, type: String) {
        if ((type == Constant.fieldUnit && fieldUnitList.contains(field.fieldUnit)) || (type == Constant.fieldName && fieldNameList.contains(
                field.fieldName
            ))
        ) {
            Snackbar.make(
                view!!,
                getString(R.string.data_exists),
                Snackbar.LENGTH_SHORT
            ).show()

            return
        }

        AppExecutors.getInstance()!!.diskIO().execute {
            if (isUpdate)
                database.fieldDAO().updateField(field)
            else
                database.fieldDAO().insertField(field)

            _mActivity!!.runOnUiThread {
                Snackbar.make(
                    view!!,
                    getString(R.string.item_saved),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        retrieveAllFieldUnits()
    }

    private fun editTemplate() {
        if (templateList.size > 0) {
            MaterialDialog(_mActivity!!).show {
                title(R.string.edit_template)
                listItems(items = templateNameList, waitForPositiveButton = false) { _, index, _ ->
                    startForResult(
                        ModifyRecordFragment.newInstance(
                            templateList[index].recordID,
                            insertTemplate = true
                        ), REQUEST_ADD_TEMPLATE
                    )
                    dismiss()
                }
                positiveButton(R.string.add) {
                    addTemplate()
                    dismiss()
                }
                negativeButton(R.string.delete) {
                    deleteTemplate()
                    dismiss()
                }
            }
        } else
            addTemplate()
    }

    private fun addTemplate() {
        startForResult(
            ModifyRecordFragment.newInstance(recordID = 0, insertTemplate = true),
            REQUEST_ADD_TEMPLATE
        )
    }

    private fun deleteTemplate() {
        MaterialDialog(_mActivity!!).show {
            title(R.string.delete_template)
            positiveButton(R.string.done)
            negativeButton(android.R.string.cancel) {
                dismiss()
            }
            listItemsMultiChoice(
                waitForPositiveButton = true,
                items = templateNameList
            ) { _, index, _ ->
                MaterialDialog(_mActivity!!).show {
                    title(R.string.warning)
                    message(R.string.delete_template_warning)
                    positiveButton(R.string.yes) {
                        AppExecutors.getInstance()!!.diskIO().execute {
                            for (i in index)
                                database.recordDAO().deleteRecord(templateList[i])

                            Snackbar.make(
                                getView()!!,
                                getString(R.string.template_deleted),
                                Snackbar.LENGTH_SHORT
                            )
                                //.setAnchorView(if (anchorView == null) _mActivity.Main_BottomNav else null)
                                //.setAction(actionText, onClick)
                                .show()

                            // Retrieve all template data from database
                            retrieveAllTemplates()
                        }
                    }
                    negativeButton(android.R.string.cancel) {
                        dismiss()
                    }
                }
            }
        }
    }

    override fun onFragmentResult(requestCode: Int, resultCode: Int, data: Bundle?) {
        mDelegate.onFragmentResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == REQUEST_ADD_TEMPLATE) {
            Snackbar.make(view!!, getString(R.string.template_saved), Snackbar.LENGTH_SHORT).show()

            retrieveAllTemplates()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        // Hide the settings menu
        menu.getItem(0).isVisible = false
    }

    val mDelegate = SupportFragmentDelegate(this)
    protected var _mActivity: FragmentActivity? = null

    override fun getSupportDelegate(): SupportFragmentDelegate? {
        return mDelegate
    }

    /**
     * Perform some extra transactions.
     * 额外的事务：自定义Tag，添加SharedElement动画，操作非回退栈Fragment
     */
    override fun extraTransaction(): ExtraTransaction? {
        return mDelegate.extraTransaction()
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mDelegate.onAttach(activity)
        _mActivity = mDelegate.activity
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDelegate.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Room database
        database = AppDatabase.getInstance(_mActivity!!)!!

        // Always hide the BottomNavigationView in this Fragment
        _mActivity?.Main_BottomNav?.visibility = View.GONE

        findPreference<Preference>(getString(R.string.editFieldName))?.setOnPreferenceClickListener {
            editFieldData(Constant.fieldName)
            true
        }

        findPreference<Preference>(getString(R.string.editTemplate))?.setOnPreferenceClickListener {
            editTemplate()
            true
        }

        findPreference<Preference>(getString(R.string.editUnit))?.setOnPreferenceClickListener {
            editFieldData(Constant.fieldUnit)
            true
        }

        retrieveAllTemplates()
        retrieveAllFieldUnits()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            _mActivity?.onBackPressed()

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateAnimation(
        transit: Int,
        enter: Boolean,
        nextAnim: Int
    ): Animation? {
        return mDelegate.onCreateAnimation(transit, enter, nextAnim)
    }

    override fun onActivityCreated(@Nullable savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mDelegate.onActivityCreated(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mDelegate.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        mDelegate.onResume()
    }

    override fun onPause() {
        super.onPause()
        mDelegate.onPause()
    }

    override fun onDestroyView() {
        mDelegate.onDestroyView()
        super.onDestroyView()
    }

    override fun onDestroy() {
        mDelegate.onDestroy()
        super.onDestroy()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        mDelegate.onHiddenChanged(hidden)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        mDelegate.setUserVisibleHint(isVisibleToUser)
    }

    /**
     * Causes the Runnable r to be added to the action queue.
     *
     *
     * The runnable will be run after all the previous action has been run.
     *
     *
     * 前面的事务全部执行后 执行该Action
     *
     */
    @Deprecated("Use {@link #post(Runnable)} instead.")
    override fun enqueueAction(runnable: Runnable?) {
        mDelegate.enqueueAction(runnable)
    }

    /**
     * Causes the Runnable r to be added to the action queue.
     *
     *
     * The runnable will be run after all the previous action has been run.
     *
     *
     * 前面的事务全部执行后 执行该Action
     */
    override fun post(runnable: Runnable?) {
        mDelegate.post(runnable)
    }

    /**
     * Called when the enter-animation end.
     * 入栈动画 结束时,回调
     */
    override fun onEnterAnimationEnd(savedInstanceState: Bundle?) {
        mDelegate.onEnterAnimationEnd(savedInstanceState)
    }


    /**
     * Lazy initial，Called when fragment is first called.
     *
     *
     * 同级下的 懒加载 ＋ ViewPager下的懒加载  的结合回调方法
     */
    override fun onLazyInitView(@Nullable savedInstanceState: Bundle?) {
        mDelegate.onLazyInitView(savedInstanceState)
    }

    /**
     * Called when the fragment is visible.
     * 当Fragment对用户可见时回调
     *
     *
     * Is the combination of  [onHiddenChanged() + onResume()/onPause() + setUserVisibleHint()]
     */
    override fun onSupportVisible() {
        mDelegate.onSupportVisible()

        // Display back button on the Toolbar
        (_mActivity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        (_mActivity as ActionBarTitleCallback).setActionBarTitle(getString(R.string.settings))
    }

    /**
     * Called when the fragment is invivible.
     *
     *
     * Is the combination of  [onHiddenChanged() + onResume()/onPause() + setUserVisibleHint()]
     */
    override fun onSupportInvisible() {
        mDelegate.onSupportInvisible()
    }

    /**
     * Return true if the fragment has been supportVisible.
     */
    override fun isSupportVisible(): Boolean {
        return mDelegate.isSupportVisible
    }

    /**
     * Set fragment animation with a higher priority than the ISupportActivity
     * 设定当前Fragmemt动画,优先级比在SupportActivity里高
     */
    override fun onCreateFragmentAnimator(): FragmentAnimator? {
        return mDelegate.onCreateFragmentAnimator()
    }

    /**
     * 获取设置的全局动画 copy
     *
     * @return FragmentAnimator
     */
    override fun getFragmentAnimator(): FragmentAnimator? {
        return mDelegate.fragmentAnimator
    }

    /**
     * 设置Fragment内的全局动画
     */
    override fun setFragmentAnimator(fragmentAnimator: FragmentAnimator?) {
        mDelegate.fragmentAnimator = fragmentAnimator
    }

    /**
     * 按返回键触发,前提是SupportActivity的onBackPressed()方法能被调用
     *
     * @return false则继续向上传递, true则消费掉该事件
     */
    override fun onBackPressedSupport(): Boolean {
        (_mActivity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        return mDelegate.onBackPressedSupport()
    }

    /**
     * 类似 [Activity.setResult]
     *
     *
     * Similar to [Activity.setResult]
     *
     * @see .startForResult
     */
    override fun setFragmentResult(resultCode: Int, bundle: Bundle?) {
        mDelegate.setFragmentResult(resultCode, bundle)
    }

    /**
     * 在start(TargetFragment,LaunchMode)时,启动模式为SingleTask/SingleTop, 回调TargetFragment的该方法
     * 类似 [Activity.onNewIntent]
     *
     *
     * Similar to [Activity.onNewIntent]
     *
     * @param args putNewBundle(Bundle newBundle)
     * @see .start
     */
    override fun onNewBundle(args: Bundle?) {
        mDelegate.onNewBundle(args)
    }

    /**
     * 添加NewBundle,用于启动模式为SingleTask/SingleTop时
     *
     * @see .start
     */
    override fun putNewBundle(newBundle: Bundle?) {
        mDelegate.putNewBundle(newBundle)
    }

    /**
     * Launch an fragment for which you would like a result when it poped.
     */
    fun startForResult(toFragment: ISupportFragment?, requestCode: Int) {
        mDelegate.startForResult(toFragment, requestCode)
    }
}
