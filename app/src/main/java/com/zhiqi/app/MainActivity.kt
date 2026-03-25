package com.zhiqi.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.zhiqi.app.security.AppLockManager
import com.zhiqi.app.ui.ZhiQiApp
import com.zhiqi.app.ui.ZhiQiTheme

class MainActivity : ComponentActivity() {
    private lateinit var appLockManager: AppLockManager
    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            appLockManager.onAppForegrounded()
        }

        override fun onStop(owner: LifecycleOwner) {
            appLockManager.onAppBackgrounded()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appLockManager = AppLockManager(applicationContext)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        setContent {
            ZhiQiTheme {
                ZhiQiApp(lockManager = appLockManager)
            }
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
        super.onDestroy()
    }
}
