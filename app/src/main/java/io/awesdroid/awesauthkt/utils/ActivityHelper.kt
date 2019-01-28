package io.awesdroid.awesauthkt.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import java.lang.ref.WeakReference

/**
 * @auther Awesdroid
 */
object ActivityHelper {
    private const val TAG = "ActivityHelper"
    private var activity: WeakReference<Activity>? = null

    fun setActivity(activity: Activity) {
        Log.d(TAG, "setActivity(): activity = $activity, this = $this")
        this.activity = WeakReference(activity)
    }

    fun clear() {
        Log.d(TAG, "clear(): activity = $activity")
        activity?.clear()
        activity = null
    }

    fun getActivity(): Activity? {
        return activity?.get()
    }

    @JvmStatic
    fun getContext(): Context {
        return activity?.get() as Context
    }
}


