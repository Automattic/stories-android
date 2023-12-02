package com.automattic.loop.photopicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.automattic.loop.R;
import com.automattic.loop.databinding.PhotoPickerThumbnailBinding;
import com.automattic.loop.photopicker.utils.AniUtils;
import com.automattic.loop.photopicker.utils.AniUtils.Duration;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static com.automattic.loop.photopicker.PhotoPickerFragment.NUM_COLUMNS;
import static com.wordpress.stories.util.DisplayUtilsKt.getDisplayPixelWidth;

public class PhotoPickerAdapter extends RecyclerView.Adapter<PhotoPickerAdapter.ThumbnailViewHolder> {
    private static final float SCALE_NORMAL = 1.0f;
    private static final float SCALE_SELECTED = .8f;
    private static final String TAG = "PhotoPickerAdapter";

    /*
     * used by this adapter to communicate with the owning fragment
     */
    interface PhotoPickerAdapterListener {
        void onSelectedCountChanged(int count);

        void onAdapterLoaded(boolean isEmpty);
    }

    private class PhotoPickerItem {
        private long mId;
        private Uri mUri;
        private boolean mIsVideo;
    }

    private final ArrayList<Integer> mSelectedPositions = new ArrayList<>();
    private static final Duration ANI_DURATION = AniUtils.Duration.SHORT;

    private final Context mContext;
    private RecyclerView mRecycler;
    private int mThumbWidth;
    private int mThumbHeight;

    private boolean mIsListTaskRunning;
    private boolean mLoadThumbnails = true;

    private final PhotoPickerAdapterListener mListener;
    private final LayoutInflater mInflater;
    private final MediaBrowserType mBrowserType;

    private final ArrayList<PhotoPickerItem> mMediaList = new ArrayList<>();

    // comes from  WPAndroid, not used in Loop demo app
//    protected final ImageManager mImageManager;

    PhotoPickerAdapter(Context context,
                       MediaBrowserType browserType,
                       PhotoPickerAdapterListener listener) {
        super();
        mContext = context;
        mListener = listener;
        mInflater = LayoutInflater.from(context);
        mBrowserType = browserType;
//        mImageManager = ImageManager.getInstance();

        setHasStableIds(true);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecycler = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mRecycler = null;
    }

    void refresh(boolean forceReload) {
        if (mIsListTaskRunning) {
//            AppLog.w(AppLog.T.MEDIA, "photo picker > build list task already running");
            return;
        }

        int displayWidth = getDisplayPixelWidth(mContext);
        int thumbWidth = displayWidth / NUM_COLUMNS;
        int thumbHeight = (int) (thumbWidth * 1.75f);
        boolean sizeChanged = thumbWidth != mThumbWidth || thumbHeight != mThumbHeight;

        // if thumb sizes have changed (due to device rotation, or never being set), we must
        // reload from scratch - otherwise we can do a refresh so the adapter is only loaded
        // if there are changes
        boolean mustReload;
        if (sizeChanged) {
            mThumbWidth = thumbWidth;
            mThumbHeight = thumbHeight;
            mustReload = true;
        } else {
            mustReload = forceReload;
        }

        new BuildDeviceMediaListTask(mustReload).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        return mMediaList.size();
    }

    @Override
    public long getItemId(int position) {
        if (isValidPosition(position)) {
            return getItemAtPosition(position).mId;
        } else {
            return NO_POSITION;
        }
    }

    private boolean isEmpty() {
        return mMediaList.size() == 0;
    }

    void setLoadThumbnails(boolean loadThumbnails) {
        if (loadThumbnails != mLoadThumbnails) {
            mLoadThumbnails = loadThumbnails;
//            AppLog.d(AppLog.T.MEDIA, "PhotoPickerAdapter > loadThumbnails = " + loadThumbnails);
            if (mLoadThumbnails) {
                notifyDataSetChanged();
            }
        }
    }

    @Override
    public ThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ThumbnailViewHolder(
            PhotoPickerThumbnailBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
        );
    }

    private void updateSelectionCountForPosition(int position,
                                                 boolean isSelected,
                                                 @NonNull TextView txtSelectionCount) {
        if (canMultiselect() && isSelected) {
            int count = mSelectedPositions.indexOf(position) + 1;
            txtSelectionCount.setText(String.format(Locale.getDefault(), "%d", count));
        } else {
            txtSelectionCount.setText(null);
        }
    }

    @Override
    public void onBindViewHolder(ThumbnailViewHolder holder, int position) {
        PhotoPickerItem item = getItemAtPosition(position);
        if (item == null) {
            return;
        }

        boolean isSelected = isItemSelected(position);
        holder.mBinding.textSelectionCount.setSelected(isSelected);
        holder.mBinding.textSelectionCount.setVisibility(isSelected || canMultiselect() ? View.VISIBLE : View.GONE);
        updateSelectionCountForPosition(position, isSelected, holder.mBinding.textSelectionCount);

        float scale = isSelected ? SCALE_SELECTED : SCALE_NORMAL;
        if (holder.mBinding.imageThumbnail.getScaleX() != scale) {
            holder.mBinding.imageThumbnail.setScaleX(scale);
            holder.mBinding.imageThumbnail.setScaleY(scale);
        }

//        holder.mVideoOverlay.setVisibility(item.mIsVideo ? View.VISIBLE : View.GONE);
        holder.mBinding.textVideoDuration.setVisibility(item.mIsVideo ? View.VISIBLE : View.GONE);

        if (mLoadThumbnails) {
            // mImageManager.load(holder.mImgThumbnail, ImageType.PHOTO, item.mUri.toString(), ScaleType.FIT_CENTER);
            Glide.with(mContext)
                 .load(item.mUri.toString())
                 .fitCenter()
                 .into(holder.mBinding.imageThumbnail);

            if (item.mIsVideo) {
//                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
//                String duration = sdf.format(new Date(getVideoDuration(item.mUri)));
                long milliseconds = getVideoDuration(item.mUri);
                int seconds = (int) (milliseconds / 1000) % 60;
                int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
                int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);
                String duration = "";
                if (hours > 0) {
                    duration = duration + String.format(Locale.US, "%02d", hours) + ":";
                }
                if (minutes > 0) {
                    duration = duration + String.format(Locale.US, "%02d", minutes) + ":";
                } else {
                    duration = duration + "00:";
                }
                if (seconds > 0) {
                    duration = duration + String.format(Locale.US, "%02d", seconds);
                } else {
                    duration = duration + "01"; // default to 1 second if even less than a second
                }
                holder.mBinding.textVideoDuration.setText(duration);
            } else {
                holder.mBinding.textVideoDuration.setText("");
            }
        } else {
            Glide.with(holder.mBinding.imageThumbnail.getContext()).clear(holder.mBinding.imageThumbnail);
        }
    }

    private PhotoPickerItem getItemAtPosition(int position) {
        if (!isValidPosition(position)) {
//            AppLog.w(AppLog.T.POSTS, "photo picker > invalid position in getItemAtPosition");
            return null;
        }
        return mMediaList.get(position);
    }

    public boolean isSelectedSingleItemVideo() {
        PhotoPickerItem item = getItemAtPosition(getSelectedPositions().get(0));
        if (item != null) {
            return item.mIsVideo;
        } else {
            return false;
        }
    }

    private boolean isValidPosition(int position) {
        return position >= 0 && position < mMediaList.size();
    }

    private boolean isItemSelected(int position) {
        return mSelectedPositions.contains(position);
    }

    private void toggleItemSelected(int position) {
        setItemSelected(position, !isItemSelected(position));
    }

    private void setItemSelected(int position, boolean isSelected) {
        setItemSelected(position, isSelected, true);
    }

    private void setItemSelected(int position, boolean isSelected, boolean updateAfter) {
        PhotoPickerItem item = getItemAtPosition(position);
        if (item == null) {
            return;
        }

        // if an item is already selected and multiselect isn't allowed, deselect the previous selection
        if (isSelected && !canMultiselect() && !mSelectedPositions.isEmpty()) {
            setItemSelected(mSelectedPositions.get(0), false, false);
        }

        if (isSelected) {
            mSelectedPositions.add(position);
        } else {
            int selectedIndex = mSelectedPositions.indexOf(position);
            if (selectedIndex > -1) {
                mSelectedPositions.remove(selectedIndex);
            }
        }

        ThumbnailViewHolder holder = getViewHolderAtPosition(position);
        if (holder != null) {
            holder.mBinding.textSelectionCount.setSelected(isSelected);
            updateSelectionCountForPosition(position, isSelected, holder.mBinding.textSelectionCount);

            if (isSelected) {
                AniUtils.scale(holder.mBinding.imageThumbnail, SCALE_NORMAL, SCALE_SELECTED, ANI_DURATION);
            } else {
                AniUtils.scale(holder.mBinding.imageThumbnail, SCALE_SELECTED, SCALE_NORMAL, ANI_DURATION);
            }

            if (canMultiselect()) {
                AniUtils.startAnimation(holder.mBinding.textSelectionCount, R.anim.pop);
            } else if (isSelected) {
                AniUtils.fadeIn(holder.mBinding.textSelectionCount, ANI_DURATION);
            } else {
                AniUtils.fadeOut(holder.mBinding.textSelectionCount, ANI_DURATION);
            }
        }

        if (updateAfter) {
            notifySelectionCountChanged();
            // redraw the grid after the scale animation completes
            long delayMs = ANI_DURATION.toMillis(mContext);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            }, delayMs);
        }
    }

    private ThumbnailViewHolder getViewHolderAtPosition(int position) {
        if (mRecycler == null || !isValidPosition(position)) {
            return null;
        }
        return (ThumbnailViewHolder) mRecycler.findViewHolderForAdapterPosition(position);
    }

    @NonNull
    ArrayList<Uri> getSelectedURIs() {
        ArrayList<Uri> uriList = new ArrayList<>();
        for (Integer position : mSelectedPositions) {
            PhotoPickerItem item = getItemAtPosition(position);
            if (item != null) {
                uriList.add(item.mUri);
            }
        }
        return uriList;
    }

    ArrayList<Integer> getSelectedPositions() {
        return mSelectedPositions;
    }

    void setSelectedPositions(@NonNull ArrayList<Integer> selectedPositions) {
        mSelectedPositions.clear();
        mSelectedPositions.addAll(selectedPositions);
        notifyDataSetChanged();
        notifySelectionCountChanged();
    }

    void clearSelection() {
        if (mSelectedPositions.size() > 0) {
            mSelectedPositions.clear();
            notifyDataSetChanged();
        }
    }

    private boolean canMultiselect() {
        return mBrowserType.canMultiselect();
    }

    int getNumSelected() {
        return mSelectedPositions.size();
    }

    private void notifySelectionCountChanged() {
        if (mListener != null) {
            mListener.onSelectedCountChanged(getNumSelected());
        }
    }

    /*
     * ViewHolder containing a device thumbnail
     */
    class ThumbnailViewHolder extends RecyclerView.ViewHolder {
        PhotoPickerThumbnailBinding mBinding;

        ThumbnailViewHolder(PhotoPickerThumbnailBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;
            mBinding.imageThumbnail.getLayoutParams().width = mThumbWidth;
            mBinding.imageThumbnail.getLayoutParams().height = mThumbHeight;

            if (!canMultiselect()) {
                mBinding.textSelectionCount.setBackgroundResource(R.drawable.photo_picker_circle_pressed);
            }

            mBinding.imageThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position)) {
                        toggleItemSelected(position);
                    }
                }
            });

//            mImgThumbnail.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    int position = getAdapterPosition();
//                    Log.d(TAG, "should select picture: " + position);
//                    return true;
//                }
//            });

            mBinding.imageVideoOverlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    Log.d(TAG, "should select video: " + position);
                }
            });

            // TODO: re-add this depending on the UI needed
//            ViewUtils.addCircularShadowOutline(mTxtSelectionCount);
        }
    }

//    private void showPreview(int position) {
//        PhotoPickerItem item = getItemAtPosition(position);
//        if (item != null) {
//            trackOpenPreviewScreenEvent(item);
//            MediaPreviewActivity.showPreview(
//                    mContext,
//                    null,
//                    item.mUri.toString());
//        }
//    }

    private void trackOpenPreviewScreenEvent(final PhotoPickerItem item) {
        if (item == null) {
            return;
        }

//        new Thread(new Runnable() {
//            public void run() {
//                Map<String, Object> properties =
//                        AnalyticsUtils.getMediaProperties(mContext, item.mIsVideo, item.mUri, null);
//                properties.put("is_video", item.mIsVideo);
//                AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PICKER_PREVIEW_OPENED, properties);
//            }
//        }).start();
    }

    private long getVideoDuration(Uri videoUri) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        long durationUs = 0;
        try {
            mediaMetadataRetriever.setDataSource(mContext, videoUri);
            durationUs = Long.parseLong(
                mediaMetadataRetriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (NumberFormatException e) {
            durationUs = -1;
        } catch (Exception ex) {
            // catch every other exception... we don't want to crash for this for god's sake
        } finally {
            try {
                mediaMetadataRetriever.release();
            } catch (Exception e) {
                Log.e(TAG, "Failed to release mediaMetadataRetriever." + e.getMessage());
            }
        }

        return durationUs;
    }

    /*
     * builds the list of media items from the device
     */
    @SuppressLint("StaticFieldLeak")
    private class BuildDeviceMediaListTask extends AsyncTask<Void, Void, Boolean> {
        private final ArrayList<PhotoPickerItem> mTmpList = new ArrayList<>();
        private final boolean mReload;
        private static final String ID_COL = MediaStore.Images.Media._ID;

        BuildDeviceMediaListTask(boolean mustReload) {
            super();
            mReload = mustReload;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // images
            addMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false);

            // videos
            addMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true);

            // sort by id in reverse (newest first)
            Collections.sort(mTmpList, new Comparator<PhotoPickerItem>() {
                @Override
                public int compare(PhotoPickerItem o1, PhotoPickerItem o2) {
                    long id1 = o1.mId;
                    long id2 = o2.mId;
                    return (id2 < id1) ? -1 : ((id1 == id2) ? 0 : 1);
                }
            });

            // if we're reloading then return true so the adapter is updated, otherwise only
            // return true if changes are detected
            return mReload || !isSameMediaList();
        }

        private void addMedia(Uri baseUri, boolean isVideo) {
            String[] projection = {ID_COL};
            Cursor cursor = null;
            try {
                cursor = mContext.getContentResolver().query(
                    baseUri,
                    projection,
                    null,
                    null,
                    null);
            } catch (SecurityException e) {
                Log.e(TAG, e.getMessage());
            }

            if (cursor == null) {
                return;
            }

            try {
                int idIndex = cursor.getColumnIndexOrThrow(ID_COL);
                while (cursor.moveToNext()) {
                    PhotoPickerItem item = new PhotoPickerItem();
                    item.mId = cursor.getLong(idIndex);
                    item.mUri = Uri.withAppendedPath(baseUri, "" + item.mId);
                    item.mIsVideo = isVideo;
                    mTmpList.add(item);
                }
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }

        // returns true if the media list built here is the same as the existing one
        private boolean isSameMediaList() {
            if (mTmpList.size() != mMediaList.size()) {
                return false;
            }
            for (int i = 0; i < mTmpList.size(); i++) {
                if (!isValidPosition(i)) {
                    return false;
                }
                if (mTmpList.get(i).mId != mMediaList.get(i).mId) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsListTaskRunning = true;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mIsListTaskRunning = false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mMediaList.clear();
                mMediaList.addAll(mTmpList);
                notifyDataSetChanged();
            }
            if (mListener != null) {
                mListener.onAdapterLoaded(isEmpty());
            }
            mIsListTaskRunning = false;
        }
    }
}
