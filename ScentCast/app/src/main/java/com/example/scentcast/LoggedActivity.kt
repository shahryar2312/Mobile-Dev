package com.example.scentcast

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

/** Filter Logcat by this tag to watch the activity lifecycle (Lecture 5). */
const val LIFECYCLE_TAG = "ScentCastLifecycle"

/**
 * Base class for every screen in the app. It only logs each lifecycle
 * callback so the lifecycle can be observed and documented.
 */
open class LoggedActivity : ComponentActivity() {

    private fun log(callback: String) {
        Log.d(LIFECYCLE_TAG, "${javaClass.simpleName}: $callback")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log("onCreate")
    }

    override fun onStart() {
        super.onStart()
        log("onStart")
    }

    override fun onResume() {
        super.onResume()
        log("onResume")
    }

    override fun onPause() {
        super.onPause()
        log("onPause")
    }

    override fun onStop() {
        super.onStop()
        log("onStop")
    }

    override fun onRestart() {
        super.onRestart()
        log("onRestart")
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
    }
}
