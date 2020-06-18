package com.automattic.portkey

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.automattic.photoeditor.util.PermissionUtils
import com.automattic.portkey.intro.IntroActivity
import com.google.android.material.snackbar.Snackbar
import com.wordpress.stories.compose.ComposeLoopFrameActivity
import com.wordpress.stories.compose.frame.FrameSaveNotifier
import com.wordpress.stories.compose.frame.FrameSaveNotifier.Companion.getNotificationIdForError
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveError
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveProcessStart
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryRepository
import com.wordpress.stories.util.KEY_STORY_SAVE_RESULT
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity(), MainFragment.OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {
        // TODO: change OnFragmentInteractionListener for something relevant to our needs
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        EventBus.getDefault().register(this)

        activity_main.setOnApplyWindowInsetsListener { view, insets ->
            // remember the insetTop as margin to all controls appearing at the top of the screen for full screen
            // screens (i.e. ComposeLoopFrameActivity)
            (application as Portkey).setStatusBarHeight(insets.systemWindowInsetTop)
            view.onApplyWindowInsets(insets)
        }

        if (AppPrefs.isIntroRequired() || !PermissionUtils.allRequiredPermissionsGranted(this)) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        }

        fab.setOnClickListener { view ->
            fab.isEnabled = false
            Navigation.findNavController(this, R.id.nav_host_fragment)
                .navigate(R.id.action_mainFragment_to_composeLoopFrameActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        fab.isEnabled = true
    }

    override fun onSupportNavigateUp() =
        Navigation.findNavController(this, R.id.nav_host_fragment).navigateUp()

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onStorySaveResult(event: StorySaveResult) {
        EventBus.getDefault().removeStickyEvent(event)
        if (event.isSuccess()) {
            // TODO will remove this snackbar when integrating to WPAndroid as at this successful saving point we''l
            // want to enqueue the Story post and media to be uploaded to the user's site.
            val text = String.format(
                getString(R.string.story_saving_snackbar_finished_successfully),
                StoryRepository.getStoryAtIndex(event.storyIndex).title
            )
            Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_SHORT).show()
        } else {
            // TODO show snackbar and add the PendingIntent with the StorySaveResult as a Serialized object if errors
            // TODO replace this with calls to snackbarSequencer.enqueue() when integrating code in WPAndroid

            val errorText = String.format(
                getString(R.string.story_saving_snackbar_finished_with_error),
                StoryRepository.getStoryAtIndex(event.storyIndex).title
            )
            val snackbarMessage = FrameSaveNotifier.buildSnackbarErrorMessage(
                this,
                event.frameSaveResult.count { it.resultReason is SaveError },
                errorText
            )
            val snackbar = Snackbar.make(findViewById(android.R.id.content), snackbarMessage, Snackbar.LENGTH_LONG)
            snackbar.setAction(R.string.story_saving_failed_quick_action_manage) { view ->
                // here go to the ComposeActivity, passing the SaveResult
                val intent = Intent(this@MainActivity, ComposeLoopFrameActivity::class.java)
                intent.putExtra(KEY_STORY_SAVE_RESULT, event)
                // TODO add SITE param later when integrating with WPAndroid
                // notificationIntent.putExtra(WordPress.SITE, site)

                // we need to have a way to cancel the related error notification when the user comes
                // from tapping on MANAGE on the snackbar (otherwise they'll be able to discard the
                // errored story but the error notification will remain existing in the system dashboard)
                intent.action = getNotificationIdForError(event.storyIndex).toString() + ""

                // TODO add NotificationType.MEDIA_SAVE_ERROR param later when integrating with WPAndroid
                //        val notificationType = NotificationType.MEDIA_SAVE_ERROR
                //        notificationIntent.putExtra(ARG_NOTIFICATION_TYPE, notificationType)
                startActivity(intent)
            }
            snackbar.show()
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onStorySaveStart(event: StorySaveProcessStart) {
        EventBus.getDefault().removeStickyEvent(event)
        // TODO replace this with calls to snackbarSequencer.enqueue() when integrating code in WPAndroid
        val text = String.format(
            getString(R.string.story_saving_snackbar_started),
            StoryRepository.getStoryAtIndex(event.storyIndex).title
        )
        Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_SHORT).show()
    }
}
