package com.friday.mistakenotebook

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.friday.mistakenotebook.data.backup.BackupManager
import com.friday.mistakenotebook.ui.theme.FridayNotebookTheme
import com.friday.mistakenotebook.ui.navigation.FridayNotebookNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var backupManager: BackupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FridayNotebookTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FridayNotebookNavHost()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // 退到后台时自动备份，结果只记日志，不打扰用户
        lifecycleScope.launch {
            if (!backupManager.autoBackup()) {
                Log.w(TAG, "onStop 自动备份失败")
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
