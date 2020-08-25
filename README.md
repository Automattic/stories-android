# stories-android
Stories concept app

WordPress stories library

# Code style

Our code style guidelines are based on the [Android Code Style Guidelines for Contributors](https://source.android.com/source/code-style.html). We only changed a few rules:

* Line length is 120 characters
* FIXME must not be committed in the repository use TODO instead. FIXME can be used in your own local repository only.

On top of the Android linter rules (best run for this project using `./gradlew lintRelease`), we use two linters: [Checkstyle](http://checkstyle.sourceforge.net/) (for Java and some language-independent custom project rules), and [ktlint](https://github.com/pinterest/ktlint) (for Kotlin).

## Checkstyle

You can run checkstyle via a gradle command:

```
$ ./gradlew checkstyle
```

It generates an HTML report in `app/build/reports/checkstyle/checkstyle.html`.

You can also view errors and warnings in realtime with the Checkstyle plugin.  When importing the project into Android Studio, Checkstyle should be set up automatically.  If it is not, follow the steps below.

You can install the CheckStyle-IDEA plugin in Android Studio here:

`Android Studio > Preferences... > Plugins > CheckStyle-IDEA`

Once installed, you can configure the plugin here:

`Android Studio > Preferences... > Other Settings > Checkstyle`

From there, add and enable the custom configuration file, located at [config/checkstyle.xml](https://github.com/automattic/stories-android/blob/develop/config/checkstyle.xml).

## ktlint

You can run ktlint using `./gradlew ktlint`, and you can also run `./gradlew ktlintFormat` for auto-formatting. There is no IDEA plugin (like Checkstyle's) at this time.

## The Stories library

In order to integrate the stories library, you must include the following in your project:
```
    implementation project(path: ':photoeditor')
    implementation project(path: ':stories')
```

Implement these:
- SnackbarProvider
- call `setSnackbarProvider()` as in the example Stories demo app.

- MediaPickerProvider
- call `setMediaPickerProvider()` as in the example Stories demo app.
- remember to override `setupRequestCodes()` and set the right request codes as per the host app so it works seamlessly and media can be fed into the Composer by the externally provided MediaPicker.

### Entry points
The ComposeLoopFrameActivity super class will attempt to capture different Activity entry points such as `onCreate()` (which is handled in the `onLoadFromIntent()` override) or `onActivityResult()`. As such, you'll be able to use the behavior by passing requestCode `requestCodes.PHOTO_PICKER` and a combination of one of the following extras to trigger each of the modes the composer can be presented in:
- EXTRA_MEDIA_URIS: if `providerHandlesMediaPickerResult()`  returns false, these extra will consist of a list of media Uris that will be added each one as a new Story slide to the composer.
- EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED: if the previous extra is not present and this extra is passed, then the camera capture mode will be presented, and the user will then have the ability to capture a still image or a video, which will be added to their Story as a new slide.

In the Loop demo app, we initially trigger the camera capture mode for a new Story (you can try this by tapping on the FAB), and then rely on the PhotoPicker implementation to allow the user to keep adding pictures (note they can also trigger the capture mode from there as well). For an example of a using a different entry point for its initial state, you can check WordPress Android's [StoryComposerActivity](https://github.com/wordpress-mobile/WordPress-Android/blob/develop/WordPress/src/main/java/org/wordpress/android/ui/stories/StoryComposerActivity.kt)'s `handleMediaPickerIntentData()` method, given in this case the Media picker is presented first, and the Story composer is initialized later with the selected background media constituting one Story slide each.

## Build Instructions ##

1. Make sure you've installed [Android Studio](https://developer.android.com/studio/index.html).
1. `git clone git@github.com:Automattic/stories-android.git` in the folder of your preference.
1. `cd stories-android` to enter the working directory.
1. `cp gradle.properties-example gradle.properties` to set up the sample app properties file. Specifically, you can use `stories.use.cameraX = true` to use the CameraX underlying implementation, or `false` to use the Camera2 implementation
1. In Android Studio, open the project from the local repository. This will auto-generate `local.properties` with the SDK location.
1. Go to Tools → AVD Manager and create an emulated device.
1. Run.

## License ##

The stories module and the Loop concept app are Open Source projects covered by the
[GNU General Public License version 2](LICENSE.md). Note: code in the mp4compose and photoeditor directories are covered by the MIT license.
