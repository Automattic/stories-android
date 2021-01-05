package com.automattic.loop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.automattic.loop.StoryComposerActivity.Companion.KEY_EXAMPLE_METADATA
import com.automattic.loop.StoryComposerActivity.Companion.KEY_STORY_INDEX
import com.automattic.loop.databinding.ActivityMainBinding
import com.automattic.loop.intro.IntroActivity
import com.automattic.loop.photopicker.PhotoPickerActivity
import com.automattic.photoeditor.util.PermissionUtils
import com.google.android.material.snackbar.Snackbar
import com.wordpress.stories.compose.frame.FrameSaveNotifier
import com.wordpress.stories.compose.frame.FrameSaveNotifier.Companion.getNotificationIdForError
import com.wordpress.stories.compose.frame.StorySaveEvents.SaveResultReason.SaveError
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveProcessStart
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import com.wordpress.stories.compose.story.StoryRepository
import com.wordpress.stories.util.KEY_STORY_SAVE_RESULT
import com.wordpress.stories.viewBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity(), MainFragment.OnFragmentInteractionListener {
    private val binding by viewBinding(ActivityMainBinding::inflate)

    override fun onFragmentInteraction(uri: Uri) {
        // TODO: change OnFragmentInteractionListener for something relevant to our needs
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        EventBus.getDefault().register(this)

        binding.root.setOnApplyWindowInsetsListener { view, insets ->
            // remember the insetTop as margin to all controls appearing at the top of the screen for full screen
            // screens (i.e. ComposeLoopFrameActivity)
            (application as Loop).setStatusBarHeight(insets.systemWindowInsetTop)
            view.onApplyWindowInsets(insets)
        }

        if (AppPrefs.isIntroRequired() || !PermissionUtils.allRequiredPermissionsGranted(this)) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        }

        binding.fab.run {
            setOnClickListener {
                isEnabled = false
                // NOTE: we want to start with camera capture mode in this demo app, so we pass the
                // bundle with the corresponding parameter.
                // If we had URIs to start the composer already populated with them, we'd use
                // EXTRA_MEDIA_URIS and a list of URIs for media items we want to use as Story frames.
                val bundle = Bundle()
                bundle.putBoolean(PhotoPickerActivity.EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED, true)
                Navigation.findNavController(this@MainActivity, R.id.nav_host_fragment)
                        .navigate(R.id.action_mainFragment_to_composeLoopFrameActivity, bundle)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.fab.isEnabled = true
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

        // check the metadata we've put is effectively there in the StorySaveResult event
        event.metadata?.let {
            val payloadString = it.getString(KEY_EXAMPLE_METADATA)
            val storyIndex = it.getInt(KEY_STORY_INDEX)
            Toast.makeText(
                    this, "Payload is: $payloadString - index: $storyIndex",
                    Toast.LENGTH_SHORT
            )
                    .show()
        }

        if (event.isSuccess()) {
            val text = String.format(
                    getString(R.string.story_saving_snackbar_finished_successfully),
                    StoryRepository.getStoryAtIndex(event.storyIndex).title
            )
            Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_SHORT).show()
        } else {
            // show snackbar and add the PendingIntent with the StorySaveResult as a Serialized object if errors
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
                // here go to the StoryComposerActivity, passing the SaveResult
                val intent = Intent(this@MainActivity, StoryComposerActivity::class.java)
                intent.putExtra(KEY_STORY_SAVE_RESULT, event)
                // TODO add SITE param later when integrating with WPAndroid
                // notificationIntent.putExtra(WordPress.SITE, site)

                // we need to have a way to cancel the related error notification when the user comes
                // from tapping on MANAGE on the snackbar (otherwise they'll be able to discard the
                // errored story but the error notification will remain existing in the system dashboard)
                intent.action = getNotificationIdForError(
                        StoryComposerActivity.BASE_FRAME_MEDIA_ERROR_NOTIFICATION_ID, event.storyIndex
                ).toString() + ""

                startActivity(intent)
            }
            snackbar.show()
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onStorySaveStart(event: StorySaveProcessStart) {
        EventBus.getDefault().removeStickyEvent(event)
        val text = String.format(
                getString(R.string.story_saving_snackbar_started),
                StoryRepository.getStoryAtIndex(event.storyIndex).title
        )
        Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_SHORT).show()
    }
}
