package com.automattic.portkey.compose.photopicker;

import android.Manifest.permission;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.automattic.portkey.R;
import com.automattic.portkey.compose.photopicker.PhotoPickerAdapter.PhotoPickerAdapterListener;
import com.automattic.portkey.compose.photopicker.utils.AniUtils;

import java.util.ArrayList;
import java.util.List;

public class PhotoPickerFragment extends Fragment {
    private static final String KEY_LAST_TAPPED_ICON = "last_tapped_icon";
    private static final String KEY_SELECTED_POSITIONS = "selected_positions";

    static final int NUM_COLUMNS = 3;
    public static final String ARG_BROWSER_TYPE = "browser_type";

    public enum PhotoPickerIcon {
        ANDROID_CHOOSE_PHOTO,
        ANDROID_CHOOSE_VIDEO,
        ANDROID_CAPTURE_PHOTO,
        ANDROID_CAPTURE_VIDEO,
        WP_MEDIA,
        STOCK_MEDIA,
        GIPHY
    }

    /*
     * parent activity must implement this listener
     */
    public interface PhotoPickerListener {
        void onPhotoPickerMediaChosen(@NonNull List<Uri> uriList);

        void onPhotoPickerIconClicked(@NonNull PhotoPickerIcon icon);
    }

    private EmptyViewRecyclerView mRecycler;
    private PhotoPickerAdapter mAdapter;
    private View mBottomBar;
    private ActionableEmptyView mSoftAskView;
    private ActionMode mActionMode;
    private GridLayoutManager mGridManager;
    private Parcelable mRestoreState;
    private PhotoPickerListener mListener;
    private PhotoPickerIcon mLastTappedIcon;
    private MediaBrowserType mBrowserType;
//    private SiteModel mSite;
    private ArrayList<Integer> mSelectedPositions;

//    public static PhotoPickerFragment newInstance(@NonNull PhotoPickerListener listener,
//                                                  @NonNull MediaBrowserType browserType,
//                                                  @Nullable SiteModel site) {
//        Bundle args = new Bundle();
//        args.putSerializable(ARG_BROWSER_TYPE, browserType);
//        if (site != null) {
//            args.putSerializable(WordPress.SITE, site);
//        }
//
//        PhotoPickerFragment fragment = new PhotoPickerFragment();
//        fragment.setPhotoPickerListener(listener);
//        fragment.setArguments(args);
//        return fragment;
//    }

    public static PhotoPickerFragment newInstance(@NonNull PhotoPickerListener listener,
                                                  @NonNull MediaBrowserType browserType) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_BROWSER_TYPE, browserType);
//        if (site != null) {
//            args.putSerializable(WordPress.SITE, site);
//        }

        PhotoPickerFragment fragment = new PhotoPickerFragment();
        fragment.setPhotoPickerListener(listener);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBrowserType = (MediaBrowserType) getArguments().getSerializable(ARG_BROWSER_TYPE);
//        mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);

        if (savedInstanceState != null) {
            String savedLastTappedIconName = savedInstanceState.getString(KEY_LAST_TAPPED_ICON);
            mLastTappedIcon = savedLastTappedIconName == null ? null : PhotoPickerIcon.valueOf(savedLastTappedIconName);
            if (savedInstanceState.containsKey(KEY_SELECTED_POSITIONS)) {
                mSelectedPositions = savedInstanceState.getIntegerArrayList(KEY_SELECTED_POSITIONS);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.photo_picker_fragment, container, false);

        mRecycler = view.findViewById(R.id.recycler);
        mRecycler.setEmptyView(view.findViewById(R.id.actionable_empty_view));
        mRecycler.setHasFixedSize(true);

        // disable thumbnail loading during a fling to conserve memory
        final int minDistance = ViewConfiguration.get(getActivity()).getScaledMaximumFlingVelocity() / 2;

        mRecycler.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (Math.abs(velocityY) > minDistance) {
                    getAdapter().setLoadThumbnails(false);
                }
                return false;
            }
        });
        mRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    getAdapter().setLoadThumbnails(true);
                }
            }
        });

        mBottomBar = view.findViewById(R.id.bottom_bar);

        if (!canShowBottomBar()) {
            mBottomBar.setVisibility(View.GONE);
        } else {
//            mBottomBar.findViewById(R.id.icon_camera).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (mBrowserType.isSingleImagePicker()) {
//                        doIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_PHOTO);
////                    } else {
////                        showCameraPopupMenu(v);
//                    }
//                }
//            });
            mBottomBar.findViewById(R.id.icon_picker).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBrowserType == MediaBrowserType.GRAVATAR_IMAGE_PICKER
                        || mBrowserType == MediaBrowserType.SITE_ICON_PICKER) {
                        doIconClicked(PhotoPickerIcon.ANDROID_CHOOSE_PHOTO);
                    } else {
                        showPickerPopupMenu(v);
                    }
                }
            });

//            // choosing from WP media requires a site
//            View wpMedia = mBottomBar.findViewById(R.id.icon_wpmedia);
//            if (mSite == null) {
//                wpMedia.setVisibility(View.GONE);
//            } else {
//                wpMedia.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        doIconClicked(PhotoPickerIcon.WP_MEDIA);
//                    }
//                });
//            }
        }

        mSoftAskView = view.findViewById(R.id.soft_ask_view);

        return view;
    }

    private boolean canShowBottomBar() {
//        if (mBrowserType == MediaBrowserType.AZTEC_EDITOR_PICKER && DisplayUtils.isLandscape(getActivity())) {
//            return true;
//        } else if (mBrowserType == MediaBrowserType.AZTEC_EDITOR_PICKER) {
//            return false;
//        }
//
//        return true;

        // Portkey: we're always portrait ;)
        return false;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_LAST_TAPPED_ICON, mLastTappedIcon == null ? null : mLastTappedIcon.name());

        if (hasAdapter() && getAdapter().getNumSelected() > 0) {
            ArrayList<Integer> selectedItems = getAdapter().getSelectedPositions();
            outState.putIntegerArrayList(KEY_SELECTED_POSITIONS, selectedItems);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkStoragePermission();
    }

    public void doIconClicked(@NonNull PhotoPickerIcon icon) {
        mLastTappedIcon = icon;

        if (icon == PhotoPickerIcon.ANDROID_CAPTURE_PHOTO || icon == PhotoPickerIcon.ANDROID_CAPTURE_VIDEO) {
            if (ContextCompat.checkSelfPermission(
                    getActivity(), permission.CAMERA) != PackageManager.PERMISSION_GRANTED || !hasStoragePermission()) {
//                requestCameraPermission();
                Toast.makeText(getActivity(), "Need permissions", Toast.LENGTH_SHORT).show();
                return;
            }
        }

//        switch (icon) {
//            case ANDROID_CAPTURE_PHOTO:
//                trackSelectedOtherSourceEvents(AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_CAPTURE_MEDIA, false);
//                break;
//            case ANDROID_CAPTURE_VIDEO:
//                trackSelectedOtherSourceEvents(AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_CAPTURE_MEDIA, true);
//                break;
//            case ANDROID_CHOOSE_PHOTO:
//                trackSelectedOtherSourceEvents(AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_DEVICE_LIBRARY, false);
//                break;
//            case ANDROID_CHOOSE_VIDEO:
//                trackSelectedOtherSourceEvents(AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_DEVICE_LIBRARY, true);
//                break;
//            case WP_MEDIA:
//                AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_WP_MEDIA);
//                break;
//            case STOCK_MEDIA:
//                break;
//            case GIPHY:
//                break;
//        }

        if (mListener != null) {
            mListener.onPhotoPickerIconClicked(icon);
        }
    }

    public void showPickerPopupMenu(@NonNull View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);

        MenuItem itemPhoto = popup.getMenu().add(R.string.photo_picker_choose_photo);
        itemPhoto.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                doIconClicked(PhotoPickerIcon.ANDROID_CHOOSE_PHOTO);
                return true;
            }
        });

        MenuItem itemVideo = popup.getMenu().add(R.string.photo_picker_choose_video);
        itemVideo.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                doIconClicked(PhotoPickerIcon.ANDROID_CHOOSE_VIDEO);
                return true;
            }
        });

//        if (mSite != null) {
//            MenuItem itemStock = popup.getMenu().add(R.string.photo_picker_stock_media);
//            itemStock.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//                @Override
//                public boolean onMenuItemClick(MenuItem item) {
//                    doIconClicked(PhotoPickerIcon.STOCK_MEDIA);
//                    return true;
//                }
//            });
//
//            MenuItem itemGiphy = popup.getMenu().add(R.string.photo_picker_giphy);
//            itemGiphy.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//                @Override public boolean onMenuItemClick(MenuItem item) {
//                    doIconClicked(PhotoPickerIcon.GIPHY);
//                    return true;
//                }
//            });
//        }

        popup.show();
    }

//    public void showCameraPopupMenu(@NonNull View view) {
//        PopupMenu popup = new PopupMenu(getActivity(), view);
//
//        MenuItem itemPhoto = popup.getMenu().add(R.string.photo_picker_capture_photo);
//        itemPhoto.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                doIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_PHOTO);
//                return true;
//            }
//        });
//
//        MenuItem itemVideo = popup.getMenu().add(R.string.photo_picker_capture_video);
//        itemVideo.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                doIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_VIDEO);
//                return true;
//            }
//        });
//
//        popup.show();
//    }

    public void setPhotoPickerListener(PhotoPickerListener listener) {
        mListener = listener;
    }

    private void showBottomBar() {
        if (!isBottomBarShowing() && canShowBottomBar()) {
//            AniUtils.animateBottomBar(mBottomBar, true);
        }
    }

    private void hideBottomBar() {
        if (isBottomBarShowing() && canShowBottomBar()) {
//            AniUtils.animateBottomBar(mBottomBar, false);
        }
    }

    private boolean isBottomBarShowing() {
        return mBottomBar.getVisibility() == View.VISIBLE;
    }

    private final PhotoPickerAdapterListener mAdapterListener = new PhotoPickerAdapterListener() {
        @Override
        public void onSelectedCountChanged(int count) {
            if (count == 0) {
                finishActionMode();
            } else {
                if (mActionMode == null) {
                    ((AppCompatActivity) getActivity()).startSupportActionMode(new ActionModeCallback());
                }
                updateActionModeTitle(mAdapter.isSelectedSingleItemVideo());
            }
        }

        @Override
        public void onAdapterLoaded(boolean isEmpty) {
            // restore previous selection
            if (mSelectedPositions != null) {
                getAdapter().setSelectedPositions(mSelectedPositions);
                mSelectedPositions = null;
            }
            // restore previous state
            if (mRestoreState != null) {
                mGridManager.onRestoreInstanceState(mRestoreState);
                mRestoreState = null;
            }
        }
    };

    private boolean hasAdapter() {
        return mAdapter != null;
    }

    private PhotoPickerAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new PhotoPickerAdapter(getActivity(), mBrowserType, mAdapterListener);
        }
        return mAdapter;
    }

    /*
     * populates the adapter with media stored on the device
     */
    public void reload() {
        if (!isAdded()) {
//            AppLog.w(AppLog.T.POSTS, "Photo picker > can't reload when not added");
            return;
        }

        if (!hasStoragePermission()) {
            return;
        }

        // save the current state so we can restore it after loading
        if (mGridManager != null) {
            mRestoreState = mGridManager.onSaveInstanceState();
        }

        mGridManager = new GridLayoutManager(getActivity(), NUM_COLUMNS);
        mRecycler.setLayoutManager(mGridManager);
        mRecycler.setAdapter(getAdapter());
        getAdapter().refresh(true);
    }

    /*
     * similar to the above but only repopulates if changes are detected
     */
    public void refresh() {
        if (!isAdded()) {
//            AppLog.w(AppLog.T.POSTS, "Photo picker > can't refresh when not added");
            return;
        }

        if (!hasStoragePermission()) {
            return;
        }

        if (mGridManager == null || mAdapter == null) {
            reload();
        } else {
            getAdapter().refresh(false);
        }
    }

    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    private void updateActionModeTitle(boolean selectedItemIsVideo) {
        if (mActionMode == null) {
            return;
        }
        String title;
        if (mBrowserType.isSingleMediaItemPicker()) {
            if (selectedItemIsVideo) {
                mActionMode.setTitle(R.string.photo_picker_use_video);
            } else {
                mActionMode.setTitle(R.string.photo_picker_use_photo);
            }
        } else {
            int numSelected = getAdapter().getNumSelected();
            title = String.format(getString(R.string.cab_selected), numSelected);
            mActionMode.setTitle(title);
        }
    }

    private final class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;
//            WPActivityUtils.setStatusBarColor(getActivity().getWindow(), R.color.neutral_60);
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.photo_picker_action_mode, menu);
            hideBottomBar();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.mnu_confirm_selection && mListener != null) {
                ArrayList<Uri> uriList = getAdapter().getSelectedURIs();
                mListener.onPhotoPickerMediaChosen(uriList);
//                trackAddRecentMediaEvent(uriList);
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
//            WPActivityUtils.setStatusBarColor(getActivity().getWindow(), R.color.status_bar);
            mActionMode = null;
            showBottomBar();
            getAdapter().clearSelection();
        }
    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(
                getActivity(), permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

///*
//    private boolean isStoragePermissionAlwaysDenied() {
//        return WPPermissionUtils.isPermissionAlwaysDenied(
//                getActivity(), permission.WRITE_EXTERNAL_STORAGE);
//    }
//*/

    /*
     * load the photos if we have the necessary permission, otherwise show the "soft ask" view
     * which asks the user to allow the permission
     */
    private void checkStoragePermission() {
        if (!isAdded()) {
            return;
        }

        if (hasStoragePermission()) {
            showSoftAskView(false);
            if (!hasAdapter()) {
                reload();
            }
        } else {
            showSoftAskView(true);
        }
    }

//    private void requestStoragePermission() {
//        String[] permissions = new String[]{permission.WRITE_EXTERNAL_STORAGE};
//        requestPermissions(
//                permissions, WPPermissionUtils.PHOTO_PICKER_STORAGE_PERMISSION_REQUEST_CODE);
//    }
//
//    private void requestCameraPermission() {
//        // in addition to CAMERA permission we also need a storage permission, to store media from the camera
//        String[] permissions = new String[]{permission.CAMERA, permission.WRITE_EXTERNAL_STORAGE};
//        requestPermissions(permissions, WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE);
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
//        boolean checkForAlwaysDenied =
//                requestCode == WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE;
//        boolean allGranted = WPPermissionUtils.setPermissionListAsked(
//                getActivity(), requestCode, permissions, grantResults, checkForAlwaysDenied);
//
//        switch (requestCode) {
//            case WPPermissionUtils.PHOTO_PICKER_STORAGE_PERMISSION_REQUEST_CODE:
//                checkStoragePermission();
//                break;
//            case WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE:
//                if (allGranted) {
//                    doIconClicked(mLastTappedIcon);
//                }
//                break;
//        }
    }

    /*
     * shows the "soft ask" view which should appear when storage permission hasn't been granted
     */
    private void showSoftAskView(boolean show) {
        if (!isAdded()) {
            return;
        }

//        boolean isAlwaysDenied = isStoragePermissionAlwaysDenied();
//
        if (show) {
//            String appName = "<strong>" + getString(R.string.app_name) + "</strong>";
//            String label;
//
//            if (isAlwaysDenied) {
//                String permissionName = "<strong>"
//                                        + WPPermissionUtils.getPermissionName(getActivity(),
//                                                permission.WRITE_EXTERNAL_STORAGE)
//                                        + "</strong>";
//                label = String.format(getString(R.string.photo_picker_soft_ask_permissions_denied), appName,
//                        permissionName);
//            } else {
//                label = String.format(getString(R.string.photo_picker_soft_ask_label), appName);
//            }
//
//            mSoftAskView.title.setText(Html.fromHtml(label));
//
//            // when the user taps Allow, request the required permissions unless the user already
//            // denied them permanently, in which case take them to the device settings for this
//            // app so the user can change permissions there
//            int allowId = isAlwaysDenied ? R.string.button_edit_permissions : R.string.photo_picker_soft_ask_allow;
//            mSoftAskView.button.setText(allowId);
//            mSoftAskView.button.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (isStoragePermissionAlwaysDenied()) {
//                        WPPermissionUtils.showAppSettings(getActivity());
//                    } else {
//                        requestStoragePermission();
//                    }
//                }
//            });
//
            mSoftAskView.setVisibility(View.VISIBLE);
//            hideBottomBar();
        } else if (mSoftAskView.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(mSoftAskView, AniUtils.Duration.MEDIUM);
//            showBottomBar();
        }
    }
}
