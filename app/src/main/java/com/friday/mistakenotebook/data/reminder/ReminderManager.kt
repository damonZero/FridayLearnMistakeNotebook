package com.friday.mistakenotebook.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.friday.mistakenotebook.MainActivity
import com.friday.mistakenotebook.R
import com.friday.mistakenotebook.data.local.MistakeNotebookDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

// ---------- 偏好存储（DataStore） ----------

private val Context.reminderDataStore by androidx.datastore.preferences.preferencesDataStore(
    name = "reminder_settings"
)

object ReminderPrefs {
    private val KEY_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("reminder_enabled")
    private val KEY_HOUR = androidx.datastore.preferences.core.intPreferencesKey("reminder_hour")
    private val KEY_MINUTE = androidx.datastore.preferences.core.intPreferencesKey("reminder_minute")

    fun enabledFlow(context: Context) =
        context.reminderDataStore.data.map { it[KEY_ENABLED] ?: false }

    fun timeFlow(context: Context) = context.reminderDataStore.data.map {
        Pair(it[KEY_HOUR] ?: 19, it[KEY_MINUTE] ?: 0)
    }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.reminderDataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setTime(context: Context, hour: Int, minute: Int) {
        context.reminderDataStore.edit {
            it[KEY_HOUR] = hour
            it[KEY_MINUTE] = minute
        }
    }
}

// ---------- 调度（WorkManager 周期任务，免精确闹钟特批权限，重启后自动恢复） ----------

object ReminderScheduler {
    private const val WORK_NAME = "daily_review_reminder"
    const val CHANNEL_ID = "review_reminder"

    fun schedule(context: Context, hour: Int, minute: Int) {
        val request = androidx.work.PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(nextRunDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** 距离下一次 hour:minute 的毫秒数（已过期则算明天） */
    fun nextRunDelayMillis(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour, minute)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }
}

// ---------- 通知 ----------

object ReminderNotifier {
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderScheduler.CHANNEL_ID,
                "复习提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "每天提醒孩子复习到期的错题" }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Android 13+ 需要运行时请求通知权限 */
    fun needsPermissionRequest(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

    fun showDailyReminder(context: Context, dueCount: Int, samples: List<String>) {
        ensureChannel(context)
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val preview = samples.joinToString("\n") { "• $it" }
        val bigText = if (preview.isBlank()) {
            "今天有 $dueCount 道错题到期，打开 App 开始复习吧！"
        } else {
            "$preview\n…等共 $dueCount 道题到期"
        }

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("今天有 $dueCount 道错题待复习")
            .setContentText("打开 App 开始今日复习")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}

// ---------- 每日任务 ----------

/** 短命 Room 实例（Worker 不走 Hilt，每日一次开销可忽略） */
private object ReminderDbHolder {
    @Volatile
    private var instance: MistakeNotebookDatabase? = null

    fun get(context: Context): MistakeNotebookDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MistakeNotebookDatabase::class.java,
                MistakeNotebookDatabase.DATABASE_NAME
            ).build().also { instance = it }
        }
}

/**
 * 每日提醒任务：按学习进度实时统计当天到期错题，
 * 有到期才提醒（列前 3 条预览），没有则静默
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val enabled = ReminderPrefs.enabledFlow(applicationContext).first()
        if (!enabled) return Result.success()

        val context = applicationContext
        if (!ReminderNotifier.areNotificationsEnabled(context)) return Result.success()

        val db = ReminderDbHolder.get(context)
        val zone = ZoneId.systemDefault()
        val dueUntil = LocalDate.now().plusDays(1).atStartOfDay(zone)
            .toInstant().toEpochMilli()

        val dueCount = db.questionDao().getTodayReviewCount(dueUntil).first()
        if (dueCount <= 0) return Result.success()

        val samples = db.questionDao()
            .getQuestionsForReview(dueUntil)
            .first()
            .take(3)
            .map { it.content.replace("\n", " ").take(28) }

        ReminderNotifier.showDailyReminder(applicationContext, dueCount, samples)
        return Result.success()
    }
}
