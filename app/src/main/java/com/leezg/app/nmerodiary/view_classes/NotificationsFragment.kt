package com.leezg.app.nmerodiary.view_classes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.leezg.app.nmerodiary.BaseFragment
import com.leezg.app.nmerodiary.R

class NotificationsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this Fragment
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    companion object {

        private val PARM1 = "parm1"

        fun newInstance(): NotificationsFragment {
            /*val fragment = GroupEventFragment()
            val args = Bundle()
            args.putString(PARM1, groupID)
            fragment.arguments = args
            return fragment*/
            return NotificationsFragment()
        }
    }
}