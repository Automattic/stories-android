package com.wordpress.stories.compose.frame

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.wordpress.stories.R
import com.wordpress.stories.compose.frame.StoryNotificationType.STORY_SAVE_ERROR
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveSuccess
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryIndex
import com.wordpress.stories.util.KEY_STORY_SAVE_RESULT
import java.util.Random

class FrameSaveNotifier(private val context: Context, private val service: FrameSaveService) {
    private var notificationManager: NotificationManager
    private var notificationBuilder: NotificationCompat.Builder

    // used to hold notification data for everything (only one outstanding foreground notification
    // for the live FrameSaveService instance)
    private val notificationData: NotificationData

    private inner class NotificationData {
        internal var notificationId: Int = 0
        internal var totalMediaItems: Int = 0
        internal var currentMediaItem: Int = 0
        internal var currentStoriesToQtyUploadingMap = HashMap<StoryIndex, Int>() // keep amount of items being uploaded
                                                                            // for multiple concurrent stories
        internal val mediaItemToProgressMap = HashMap<String, Float>()
    }

    init {
        notificationData = NotificationData()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = NotificationCompat.Builder(
            context.applicationContext,
            context.getString(R.string.notification_channel_transient_id)
        ).apply {
                setSmallIcon(android.R.drawable.stat_sys_upload)
                color = context.resources.getColor(R.color.primary_50)
                setOnlyAlertOnce(true)
        }
    }

    private fun buildNotificationTitleForFrameSaveProcess(title: String): String {
        return String.format(context.getString(R.string.story_saving_title), title)
    }

    private fun buildNotificationSubtitleForFrameSaveProcess(): String {
        if (notificationData.totalMediaItems == 1) {
            return context.getString(R.string.story_saving_subtitle_frames_remaining_singular)
        } else {
            return String.format(context.getString(R.string.story_saving_subtitle_frames_remaining_plural),
                notificationData.totalMediaItems - getCurrentMediaItem())
        }
    }

    private fun updateForegroundNotification(title: String) {
        updateNotificationBuilder(title)
        updateNotificationProgress()
    }

    private fun updateNotificationBuilder(title: String) {
        // set the Notification's title and prepare the Notifications message text,
        // i.e. "Saving story... 3 frames remaining"
        if (notificationData.totalMediaItems > 0) {
            updateNotificationTitle(title)
            notificationBuilder.setContentText(buildNotificationSubtitleForFrameSaveProcess())
        }
    }

    private fun updateNotificationTitle(title: String) {
        // if there are frames of more than 1 concurrent Story being saved, show plural title
        if (notificationData.currentStoriesToQtyUploadingMap.size > 1) {
            notificationBuilder.setContentTitle(buildNotificationTitleForFrameSaveProcess(
                context.getString(R.string.story_saving_title_several))
            )
        } else {
            notificationBuilder.setContentTitle(buildNotificationTitleForFrameSaveProcess(title))
        }
    }

    private fun getCurrentMediaItem(): Int {
        return if (notificationData.currentMediaItem >= notificationData.totalMediaItems)
            notificationData.totalMediaItems - 1
        else
            notificationData.currentMediaItem
    }

    @Synchronized fun updateNotificationProgressForMedia(id: String, progress: Float) {
        if (notificationData.totalMediaItems == 0) {
            return
        }

        val currentProgress = notificationData.mediaItemToProgressMap.get(id)
        // also, only set updates in increments of 5% per media item to avoid lots of notification updates
        if (currentProgress != null && progress > currentProgress + 0.05f) {
            setProgressForMediaItem(id, progress)
            updateNotificationProgress()
        }
    }

    @Synchronized fun incrementUploadedMediaCountFromProgressNotification(
        storyIndex: StoryIndex,
        title: String,
        id: String,
        success: Boolean = false
    ) {
        decrementCurrentUploadingItemQtyForStory(storyIndex)
        notificationData.currentMediaItem++
        if (success) {
            setProgressForMediaItem(id, 1f)
        }
        if (!removeNotificationAndStopForegroundServiceIfNoItemsInQueue()) {
            // update Notification now
            updateForegroundNotification(title)
        }
    }

    @Synchronized private fun removeNotificationAndStopForegroundServiceIfNoItemsInQueue(): Boolean {
        if (notificationData.currentMediaItem == notificationData.totalMediaItems) {
            notificationManager.cancel(notificationData.notificationId)
            // reset the notification id so a new one is generated next time the service is started
            notificationData.notificationId = 0
            resetNotificationCounters()
            service.stopForeground(true)
            return true
        }
        return false
    }

    @Synchronized private fun resetNotificationCounters() {
        notificationData.currentMediaItem = 0
        notificationData.totalMediaItems = 0
        notificationData.mediaItemToProgressMap.clear()
    }

    private fun updateNotificationProgress() {
        if (notificationData.totalMediaItems == 0) {
            return
        }

        notificationBuilder.setProgress(
            100,
            Math.ceil((getCurrentOverallProgress() * 100).toDouble()).toInt(),
            false
        )
        doNotify(notificationData.notificationId, notificationBuilder.build(), null)
    }

    @Synchronized private fun setProgressForMediaItem(id: String, progress: Float) {
        notificationData.mediaItemToProgressMap.put(id, progress)
    }

    private fun getCurrentOverallProgress(): Float {
        val currentMediaProgress = getCurrentMediaProgress()
        var overAllProgress: Float
        overAllProgress = (if (notificationData.totalMediaItems > 0) {
            notificationData.currentMediaItem / notificationData.totalMediaItems
        } else {
            0
        }).toFloat()
        overAllProgress += currentMediaProgress
        return overAllProgress
    }

    private fun getCurrentMediaProgress(): Float {
        var currentMediaProgress = 0.0f
        val size = notificationData.mediaItemToProgressMap.size
        for (oneItemProgess in notificationData.mediaItemToProgressMap.values) {
            currentMediaProgress += oneItemProgess / size
        }
        return currentMediaProgress
    }

    // TODO: uncomment these lines when migrating code to WPAndroid
    @Synchronized private fun doNotify(
        id: Int,
        notification: Notification,
        notificationType: StoryNotificationType?
    ) {
        try {
            notificationManager.notify(id, notification)
            notificationType?.let {
                service.getNotificationTrackerProvider()?.trackShownNotification(it)
            }
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
        notificationData.totalMediaItems = totalMediaItems
    }

    @Synchronized fun addStoryPageInfoToForegroundNotification(
        storyIndex: StoryIndex,
        idList: List<String>,
        title: String
    ) {
        // keep our story current uploads quantity map updated
        incrementCurrentUploadingItemQtyForStory(storyIndex)
        notificationData.totalMediaItems += idList.size
        // setup progresses for each media item
        for (id in idList) {
            setProgressForMediaItem(id, 0.0f)
        }
        startOrUpdateForegroundNotification(title)
    }

    @Synchronized fun addStoryPageInfoToForegroundNotification(storyIndex: StoryIndex, id: String, title: String) {
        // keep our story current uploads quantity map updated
        incrementCurrentUploadingItemQtyForStory(storyIndex)
        notificationData.totalMediaItems++
        // setup progress for media item
        setProgressForMediaItem(id, 0.0f)
        startOrUpdateForegroundNotification(title)
    }

    @Synchronized fun incrementCurrentUploadingItemQtyForStory(storyIndex: StoryIndex) {
        val currentAmount = getCurrentUploadingItemQtyForStory(storyIndex)
        notificationData.currentStoriesToQtyUploadingMap.put(storyIndex, currentAmount + 1)
    }

    @Synchronized fun decrementCurrentUploadingItemQtyForStory(storyIndex: StoryIndex) {
        var currentAmount = getCurrentUploadingItemQtyForStory(storyIndex)
        if (currentAmount > 0) {
            currentAmount--
        }
        if (currentAmount == 0) {
            // remove the entry altogether
            notificationData.currentStoriesToQtyUploadingMap.remove(storyIndex)
        } else {
            // otherwise update the value
            notificationData.currentStoriesToQtyUploadingMap.put(storyIndex, currentAmount)
        }
    }

    @Synchronized fun getCurrentUploadingItemQtyForStory(storyIndex: StoryIndex): Int {
        val currentUploadingQtyForStory = notificationData.currentStoriesToQtyUploadingMap.get(storyIndex)
        if (currentUploadingQtyForStory != null) {
            return currentUploadingQtyForStory
        }
        return 0
    }

    @Synchronized private fun startOrUpdateForegroundNotification(title: String) {
        updateNotificationBuilder(title)
        if (notificationData.notificationId == 0) {
            notificationData.notificationId = Random().nextInt()
            service.startForeground(
                notificationData.notificationId,
                notificationBuilder.build()
            )
        } else {
            // service was already started, let's just modify the notification
            doNotify(notificationData.notificationId, notificationBuilder.build(), null)
        }
    }

    fun updateNotificationErrorForStoryFramesSave(
        // mediaList: List<MediaModel>,
        // site: SiteModel,
        storyTitle: String?,
        storySaveResult: StorySaveResult
    ) {
        // AppLog.d(AppLog.T.MEDIA, "updateNotificationErrorForStoryFramesSave: $errorMessage")

        val notificationBuilder = NotificationCompat.Builder(
            context.getApplicationContext(),
            context.getString(R.string.notification_channel_normal_id)
        )

        // val notificationId = getNotificationIdForMedia(site)
        val notificationId = getNotificationIdForError(service.getNotificationErrorBaseId(), storySaveResult.storyIndex)
        // Tap notification intent (open the media browser)
        val notificationIntent = service.getNotificationIntent()
        notificationIntent.putExtra(KEY_STORY_SAVE_RESULT, storySaveResult)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        notificationIntent.setAction(notificationId.toString())

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            notificationIntent,
            PendingIntent.FLAG_ONE_SHOT
        )

        notificationBuilder.setSmallIcon(android.R.drawable.stat_notify_error)

        val notificationTitle =
            String.format(context.getString(R.string.story_saving_failed_title),
                storyTitle ?: context.getString(R.string.story_saving_untitled))

        val newErrorMessage = buildErrorMessageForMedia(context,
            storySaveResult.frameSaveResult
                .filterNot { it.resultReason == SaveSuccess }
                .size
        )

        notificationBuilder.setContentTitle(notificationTitle)
        notificationBuilder.setContentText(newErrorMessage)
        notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(newErrorMessage))
        notificationBuilder.setContentIntent(pendingIntent)
        notificationBuilder.setAutoCancel(true)
        notificationBuilder.setOnlyAlertOnce(true)
        service.getDeleteNotificationPendingIntent()?.let {
            notificationBuilder.setDeleteIntent(service.getDeleteNotificationPendingIntent())
        }

        // Add MANAGE action and default action
        notificationBuilder.addAction(
            0, context.getString(R.string.story_saving_failed_quick_action_manage),
            pendingIntent
        ).color = context.resources.getColor(R.color.colorAccent)

        doNotify(notificationId, notificationBuilder.build(), STORY_SAVE_ERROR)
    }

    companion object {
        fun buildErrorMessageForMedia(context: Context, mediaItemsNotUploaded: Int) = if (mediaItemsNotUploaded == 1) {
        context.getString(R.string.story_saving_failed_message_singular)
        } else {
            String.format(
                context.getString(R.string.story_saving_failed_message_plural),
                mediaItemsNotUploaded
            )
        }

        @JvmStatic fun getNotificationIdForError(baseId: Int, storyIndex: StoryIndex): Int {
            return baseId + storyIndex
        }

        @JvmStatic fun buildSnackbarErrorMessage(
            context: Context,
            mediaItemsNotUploaded: Int,
            errorMessage: String
        ) = errorMessage + "\n" + buildErrorMessageForMedia(context, mediaItemsNotUploaded)
    }
}
