package com.automattic.portkey.compose.frame

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.automattic.portkey.R
import java.util.Random

class FrameSaveNotifier(private val context: Context, private val service: FrameSaveService) {
    private var mNotificationManager: NotificationManager
    private var mNotificationBuilder: NotificationCompat.Builder

    // used to hold notification data for everything (only one outstanding foreground notification
    // for the live FrameSaveService instance
    private val sNotificationData: NotificationData

    private inner class NotificationData {
        internal var mNotificationId: Int = 0
        internal var mTotalMediaItems: Int = 0
        internal var mCurrentMediaItem: Int = 0
        internal val mediaItemToProgressMap = HashMap<String, Float>()
    }

    init {
        sNotificationData = NotificationData()
        mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationBuilder = NotificationCompat.Builder(
            context.getApplicationContext(),
            context.getString(R.string.notification_channel_transient_id)
        )
        mNotificationBuilder.setSmallIcon(android.R.drawable.stat_sys_upload)
            .setColor(context.resources.getColor(R.color.primary_50))
            .setOnlyAlertOnce(true)
    }

    private fun buildNotificationTitleForFrameSaveProcess(title: String): String {
        return String.format(context.getString(R.string.story_saving_title), title)
    }

    private fun buildNotificationSubtitleForFrameSaveProcess(): String {
        if (sNotificationData.mTotalMediaItems == 1) {
            return context.getString(R.string.story_saving_subtitle_frames_remaining_singular)
        } else {
            return String.format(context.getString(R.string.story_saving_subtitle_frames_remaining_plural),
                sNotificationData.mTotalMediaItems - getCurrentMediaItem())
        }
    }

    private fun updateForegroundNotification(title: String? = null) {
        updateNotificationBuilder(title)
        updateNotificationProgress()
    }

    private fun updateNotificationBuilder(title: String?) {
        // set the Notification's title and prepare the Notifications message text,
        // i.e. "Saving story... 3 frames remaining"
        if (sNotificationData.mTotalMediaItems > 0) {
            // only media items are being uploaded
            // check if special case for ONE media item
            if (title != null) {
                mNotificationBuilder.setContentTitle(buildNotificationTitleForFrameSaveProcess(title))
            } else {
                mNotificationBuilder.setContentTitle(buildNotificationTitleForFrameSaveProcess(
                    context.getString(R.string.story_saving_untitled))
                )
            }
            mNotificationBuilder.setContentText(buildNotificationSubtitleForFrameSaveProcess())
        }
    }

    private fun getCurrentMediaItem(): Int {
        return if (sNotificationData.mCurrentMediaItem >= sNotificationData.mTotalMediaItems)
            sNotificationData.mTotalMediaItems - 1
        else
            sNotificationData.mCurrentMediaItem
    }

    @Synchronized fun updateNotificationProgressForMedia(id: String, progress: Float) {
        if (sNotificationData.mTotalMediaItems == 0) {
            return
        }

        // TODO leaving this check for future WPAndroid integration / FluxC work proof
        // only update if media item is in our map - this check is performed because
        // it could happen that a media item is already done uploading but we receive an upload
        // progress event from FluxC after that. We just need to avoid re-adding the item to the map.
        val currentProgress = sNotificationData.mediaItemToProgressMap.get(id)
        // also, only set updates in increments of 5% per media item to avoid lots of notification updates
        if (currentProgress != null && progress > currentProgress + 0.05f) {
            setProgressForMediaItem(id, progress)
            updateNotificationProgress()
        }
    }

    @Synchronized fun incrementUploadedMediaCountFromProgressNotification(id: String, success: Boolean = false) {
        sNotificationData.mCurrentMediaItem++
        if (success) {
            setProgressForMediaItem(id, 1f)
        }
        if (!removeNotificationAndStopForegroundServiceIfNoItemsInQueue()) {
            // update Notification now
            updateForegroundNotification()
        }
    }

    @Synchronized private fun removeNotificationAndStopForegroundServiceIfNoItemsInQueue(): Boolean {
        if (sNotificationData.mCurrentMediaItem == sNotificationData.mTotalMediaItems) {
            mNotificationManager.cancel(sNotificationData.mNotificationId)
            // reset the notification id so a new one is generated next time the service is started
            sNotificationData.mNotificationId = 0
            resetNotificationCounters()
            service.stopForeground(true)
            return true
        }
        return false
    }

    @Synchronized private fun resetNotificationCounters() {
        sNotificationData.mCurrentMediaItem = 0
        sNotificationData.mTotalMediaItems = 0
        sNotificationData.mediaItemToProgressMap.clear()
    }

    private fun updateNotificationProgress() {
        if (sNotificationData.mTotalMediaItems == 0) {
            return
        }

        mNotificationBuilder.setProgress(
            100,
            Math.ceil((getCurrentOverallProgress() * 100).toDouble()).toInt(),
            false
        )
        doNotify(sNotificationData.mNotificationId.toLong(), mNotificationBuilder.build())
    }

    @Synchronized private fun setProgressForMediaItem(id: String, progress: Float) {
        sNotificationData.mediaItemToProgressMap.put(id, progress)
    }

    private fun getCurrentOverallProgress(): Float {
        val currentMediaProgress = getCurrentMediaProgress()
        var overAllProgress: Float
        overAllProgress = (if (sNotificationData.mTotalMediaItems > 0)
            sNotificationData.mCurrentMediaItem / sNotificationData.mTotalMediaItems
        else
            0).toFloat()
        overAllProgress += currentMediaProgress
        return overAllProgress
    }

    private fun getCurrentMediaProgress(): Float {
        var currentMediaProgress = 0.0f
        val size = sNotificationData.mediaItemToProgressMap.size
        for (oneItemProgess in sNotificationData.mediaItemToProgressMap.values) {
            currentMediaProgress += oneItemProgess / size
        }
        return currentMediaProgress
    }

    // TODO: uncomment these lines when migrating code to WPAndroid
    @Synchronized private fun doNotify(
        id: Long,
        notification: Notification
//        notificationType: NotificationType?
    ) {
        try {
            mNotificationManager.notify(id.toInt(), notification)
            // TODO track notification when integrating in WPAndroid
            // note: commented out code left on purpose
//            if (notificationType != null) {
//                mSystemNotificationsTracker.trackShownNotification(notificationType)
//            }
        } catch (runtimeException: RuntimeException) {
//            CrashLoggingUtils.logException(
//                runtimeException,
//                AppLog.T.UTILS,
//                "See issue #2858 / #3966"
//            )
//            AppLog.d(
//                AppLog.T.POSTS,
//                "See issue #2858 / #3966; notify failed with:$runtimeException"
//            )
            Log.d("Portkey", "See issue #2858 / #3966; notify failed with:$runtimeException")
        }
    }

    @Synchronized fun setTotalMediaItems(totalMediaItems: Int) {
        sNotificationData.mTotalMediaItems = totalMediaItems
    }

    @Synchronized fun removeMediaInfoFromForegroundNotification(idList: List<String>) {
        if (sNotificationData.mTotalMediaItems >= idList.size) {
            sNotificationData.mTotalMediaItems -= idList.size
            // update Notification now
            updateForegroundNotification(null)
        }
    }

    @Synchronized fun removeOneMediaItemInfoFromForegroundNotification() {
        if (sNotificationData.mTotalMediaItems >= 1) {
            sNotificationData.mTotalMediaItems--
            // update Notification now
            updateForegroundNotification(null)
        }
    }

    @Synchronized fun addStoryPageInfoToForegroundNotification(idList: List<String>, title: String) {
        sNotificationData.mTotalMediaItems += idList.size
        // setup progresses for each media item
        for (id in idList) {
            setProgressForMediaItem(id, 0.0f)
        }
        startOrUpdateForegroundNotification(title)
    }

    @Synchronized fun addStoryPageInfoToForegroundNotification(id: String, title: String) {
        sNotificationData.mTotalMediaItems++
        // setup progress for media item
        setProgressForMediaItem(id, 0.0f)
        startOrUpdateForegroundNotification(title)
    }

    // TODO change signature to receive a CPT (Post) as parameter instead of a plain String
    @Synchronized private fun startOrUpdateForegroundNotification(title: String?) {
        updateNotificationBuilder(title)
        if (sNotificationData.mNotificationId == 0) {
            sNotificationData.mNotificationId = Random().nextInt()
            service.startForeground(
                sNotificationData.mNotificationId,
                mNotificationBuilder.build()
            )
        } else {
            // service was already started, let's just modify the notification
            doNotify(sNotificationData.mNotificationId.toLong(), mNotificationBuilder.build())
        }
    }
}
