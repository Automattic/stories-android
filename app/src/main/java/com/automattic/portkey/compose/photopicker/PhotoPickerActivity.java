package com.automattic.portkey.compose.photopicker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.transition.Slide;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.automattic.portkey.R;
import com.automattic.portkey.compose.photopicker.utils.CameraIntentUtils;

import java.io.File;
import java.util.List;


public class PhotoPickerActivity extends AppCompatActivity
        implements PhotoPickerFragment.PhotoPickerListener {
    private static final String PICKER_FRAGMENT_TAG = "picker_fragment_tag";
    private static final String KEY_MEDIA_CAPTURE_PATH = "media_capture_path";

    public static final String EXTRA_MEDIA_URI = "media_uri";
    public static final String EXTRA_MEDIA_ID = "media_id";
    public static final String EXTRA_MEDIA_QUEUED = "media_queued";

    // the enum name of the source will be returned as a string in EXTRA_MEDIA_SOURCE
    public static final String EXTRA_MEDIA_SOURCE = "media_source";

    public static final String LOCAL_POST_ID = "local_post_id";

    private String mMediaCapturePath;
    private MediaBrowserType mBrowserType;

    // note that the local post id isn't required (default value is EMPTY_LOCAL_POST_ID)
//    private Integer mLocalPostId;

    private GestureDetectorCompat mSwipeDetector;

    public enum PhotoPickerMediaSource {
        ANDROID_CAMERA,
        ANDROID_PICKER,
        APP_PICKER,
        WP_MEDIA_PICKER,
        STOCK_MEDIA_PICKER;

        public static PhotoPickerMediaSource fromString(String strSource) {
            if (strSource != null) {
                for (PhotoPickerMediaSource source : PhotoPickerMediaSource.values()) {
                    if (source.name().equalsIgnoreCase(strSource)) {
                        return source;
                    }
                }
            }
            return null;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // inside your activity (if you did not enable transitions in your theme)
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        // set an enter transition (slide up from bottom)
        getWindow().setEnterTransition(new Slide(Gravity.BOTTOM));
        // set exit transition (slide down from top)
        getWindow().setExitTransition(new Slide(Gravity.TOP));

        setContentView(R.layout.photo_picker_activity);

        mSwipeDetector = new GestureDetectorCompat(this, new FlingGestureListener());

        Toolbar toolbar = findViewById(R.id.toolbar);
        // toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        if (savedInstanceState == null) {
            mBrowserType = (MediaBrowserType) getIntent().getSerializableExtra(PhotoPickerFragment.ARG_BROWSER_TYPE);
//            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
//            mLocalPostId = getIntent().getIntExtra(LOCAL_POST_ID, EMPTY_LOCAL_POST_ID);
        } else {
            mBrowserType = (MediaBrowserType) savedInstanceState.getSerializable(PhotoPickerFragment.ARG_BROWSER_TYPE);
//            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
//            mLocalPostId = savedInstanceState.getInt(LOCAL_POST_ID, EMPTY_LOCAL_POST_ID);
        }

        PhotoPickerFragment fragment = getPickerFragment();
        if (fragment == null) {
//            fragment = PhotoPickerFragment.newInstance(this, mBrowserType, mSite);
            fragment = PhotoPickerFragment.newInstance(this, mBrowserType);
            getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, fragment, PICKER_FRAGMENT_TAG)
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                .commitAllowingStateLoss();
        } else {
            fragment.setPhotoPickerListener(this);
        }
    }

    private PhotoPickerFragment getPickerFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(PICKER_FRAGMENT_TAG);
        if (fragment != null) {
            return (PhotoPickerFragment) fragment;
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(PhotoPickerFragment.ARG_BROWSER_TYPE, mBrowserType);
//        outState.putInt(LOCAL_POST_ID, mLocalPostId);
//        if (mSite != null) {
//            outState.putSerializable(WordPress.SITE, mSite);
//        }
        if (!TextUtils.isEmpty(mMediaCapturePath)) {
            outState.putString(KEY_MEDIA_CAPTURE_PATH, mMediaCapturePath);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mMediaCapturePath = savedInstanceState.getString(KEY_MEDIA_CAPTURE_PATH);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mSwipeDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            // user chose a photo from the device library
            case RequestCodes.PICTURE_LIBRARY:
                if (data != null) {
                    Uri imageUri = data.getData();
                    if (imageUri != null) {
                        doMediaUriSelected(imageUri, PhotoPickerMediaSource.ANDROID_PICKER);
                    }
                }
                break;
            // user took a photo with the device camera
            case RequestCodes.TAKE_PHOTO:
                try {
                    CameraIntentUtils.scanMediaFile(this, mMediaCapturePath);
                    File f = new File(mMediaCapturePath);
                    Uri capturedImageUri = Uri.fromFile(f);
                    doMediaUriSelected(capturedImageUri, PhotoPickerMediaSource.ANDROID_CAMERA);
                } catch (RuntimeException e) {
                    Log.e(PhotoPickerActivity.class.getName(), e.getMessage());
                }
                break;
//            // user selected from WP media library, extract the media ID and pass to caller
//            case RequestCodes.SINGLE_SELECT_MEDIA_PICKER:
//                if (data.hasExtra(MediaBrowserActivity.RESULT_IDS)) {
//                    ArrayList<Long> ids =
//                            ListUtils.fromLongArray(data.getLongArrayExtra(MediaBrowserActivity.RESULT_IDS));
//                    if (ids != null && ids.size() == 1) {
//                        doMediaIdSelected(ids.get(0), PhotoPickerMediaSource.WP_MEDIA_PICKER);
//                    }
//                }
//                break;
//            // user selected a stock photo
//            case RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT:
//                if (data != null && data.hasExtra(EXTRA_MEDIA_ID)) {
//                    long mediaId = data.getLongExtra(EXTRA_MEDIA_ID, 0);
//                    doMediaIdSelected(mediaId, PhotoPickerMediaSource.STOCK_MEDIA_PICKER);
//                }
//                break;
        }
    }

    private void launchCamera() {
        CameraIntentUtils.launchCamera(this, getApplicationContext().getPackageName(),
                                  new CameraIntentUtils.LaunchCameraCallback() {
                                      @Override
                                      public void onMediaCapturePathReady(String mediaCapturePath) {
                                          mMediaCapturePath = mediaCapturePath;
                                      }
                                  });
    }

    private void launchPictureLibrary() {
//        WPMediaUtils.launchPictureLibrary(this, false);
        startActivityForResult(
                preparePictureLibraryIntent(getString(R.string.pick_photo), false),
                RequestCodes.PICTURE_LIBRARY);
    }

    private static Intent preparePictureLibraryIntent(String title, boolean multiSelect) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (multiSelect) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        return Intent.createChooser(intent, title);
    }

    private static Intent prepareGalleryIntent(String title) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        return Intent.createChooser(intent, title);
    }

//    private void launchWPMediaLibrary() {
//        if (mSite != null) {
//            ActivityLauncher.viewMediaPickerForResult(this, mSite, mBrowserType);
//        } else {
//            ToastUtils.showToast(this, R.string.blog_not_found);
//        }
//    }

//    private void launchStockMediaPicker() {
//        if (mSite != null) {
//            ActivityLauncher.showStockMediaPickerForResult(this,
//                    mSite, RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT);
//        } else {
//            ToastUtils.showToast(this, R.string.blog_not_found);
//        }
//    }

    private void doMediaUriSelected(@NonNull Uri mediaUri, @NonNull PhotoPickerMediaSource source) {
        // if user chose a featured image, we need to upload it and return the uploaded media object
//        if (mBrowserType == MediaBrowserType.FEATURED_IMAGE_PICKER) {
//            final String mimeType = getContentResolver().getType(mediaUri);
//            WPMediaUtils.fetchMediaAndDoNext(this, mediaUri,
//                                             new WPMediaUtils.MediaFetchDoNext() {
//                                                 @Override
//                                                 public void doNext(Uri uri) {
//                                                     mFeaturedImageHelper
//                                                             .queueFeaturedImageForUpload(PhotoPickerActivity.this,
//                                                                     mLocalPostId, mSite, uri, mimeType);
//                                                     Intent intent = new Intent()
//                                                             .putExtra(EXTRA_MEDIA_QUEUED, true);
//                                                     setResult(RESULT_OK, intent);
//                                                     finish();
//                                                 }
//                                             });
//        } else {
            Intent intent = new Intent()
                    .putExtra(EXTRA_MEDIA_URI, mediaUri.toString())
                    .putExtra(EXTRA_MEDIA_SOURCE, source.name());
            setResult(RESULT_OK, intent);
            finish();
//        }
    }

    private void doMediaIdSelected(long mediaId, @NonNull PhotoPickerMediaSource source) {
        Intent data = new Intent()
                .putExtra(EXTRA_MEDIA_ID, mediaId)
                .putExtra(EXTRA_MEDIA_SOURCE, source.name());
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onPhotoPickerMediaChosen(@NonNull List<Uri> uriList) {
        if (uriList.size() > 0) {
            doMediaUriSelected(uriList.get(0), PhotoPickerMediaSource.APP_PICKER);
        }
    }

    @Override
    public void onPhotoPickerIconClicked(@NonNull PhotoPickerFragment.PhotoPickerIcon icon) {
        switch (icon) {
            case ANDROID_CAPTURE_PHOTO:
                launchCamera();
                break;
            case ANDROID_CHOOSE_PHOTO:
                launchPictureLibrary();
                break;
            case WP_MEDIA:
//                launchWPMediaLibrary();
                break;
            case STOCK_MEDIA:
//                launchStockMediaPicker();
                break;
            case ANDROID_CHOOSE_VIDEO:
                break;
            case ANDROID_CAPTURE_VIDEO:
                break;
            case GIPHY:
                break;
        }
    }

    private class FlingGestureListener extends SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 != null && e2 != null) {
                if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    // Top to bottom
                    finish();
                    return false;
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }
}
