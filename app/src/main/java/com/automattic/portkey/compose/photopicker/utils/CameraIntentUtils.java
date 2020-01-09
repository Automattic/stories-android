package com.automattic.portkey.compose.photopicker.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.automattic.portkey.R;
import com.automattic.portkey.compose.photopicker.RequestCodes;

import java.io.File;
import java.io.IOException;

public class CameraIntentUtils {
    public interface LaunchCameraCallback {
        void onMediaCapturePathReady(String mediaCapturePath);
    }


    private static Intent prepareLaunchCamera(Context context, String applicationId, LaunchCameraCallback callback) {
        String state = android.os.Environment.getExternalStorageState();
        if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            showSDCardRequiredDialog(context);
            return null;
        } else {
            try {
                return getLaunchCameraIntent(context, applicationId, callback);
            } catch (IOException e) {
                // No need to write log here
                return null;
            }
        }
    }

    private static Intent getLaunchCameraIntent(Context context, String applicationId, LaunchCameraCallback callback)
            throws IOException {
        File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        String mediaCapturePath =
                externalStoragePublicDirectory + File.separator + "Camera" + File.separator + "wp-" + System
                        .currentTimeMillis() + ".jpg";

        // make sure the directory we plan to store the recording in exists
        File directory = new File(mediaCapturePath).getParentFile();
        if (directory == null || (!directory.exists() && !directory.mkdirs())) {
            try {
                throw new IOException("Path to file could not be created: " + mediaCapturePath);
            } catch (IOException e) {
                Log.e(getTag(), e.getMessage());
                throw e;
            }
        }

        Uri fileUri;
        try {
            fileUri = FileProvider
                    .getUriForFile(context, applicationId + ".provider", new File(mediaCapturePath));
        } catch (IllegalArgumentException e) {
            Log.e(getTag(), "Cannot access the file planned to store the new media", e);
            throw new IOException("Cannot access the file planned to store the new media");
        } catch (NullPointerException e) {
            Log.e(getTag(), "Cannot access the file planned to store the new media - "
                              + "FileProvider.getUriForFile cannot find a valid provider for the authority: "
                              + applicationId + ".provider", e);
            throw new IOException("Cannot access the file planned to store the new media");
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, fileUri);

        if (callback != null) {
            callback.onMediaCapturePathReady(mediaCapturePath);
        }
        return intent;
    }

    public static void launchCamera(Activity activity, String applicationId, LaunchCameraCallback callback) {
        Intent intent = prepareLaunchCamera(activity, applicationId, callback);
        if (intent != null) {
            activity.startActivityForResult(intent, RequestCodes.TAKE_PHOTO);
        }
    }

    private static void showSDCardRequiredDialog(Context context) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
        dialogBuilder.setTitle(context.getResources().getText(R.string.sdcard_title));
        dialogBuilder.setMessage(context.getResources().getText(R.string.sdcard_message));
        dialogBuilder.setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    private static String getTag() {
        return CameraIntentUtils.class.toString();
    }

    /*
     * passes a newly-created media file to the media scanner service so it's available to
     * the media content provider - use this after capturing or downloading media to ensure
     * that it appears in the stock Gallery app
     */
    public static void scanMediaFile(@NonNull Context context, @NonNull String localMediaPath) {
        MediaScannerConnection.scanFile(context,
                new String[]{localMediaPath}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.d(getTag(), "Media scanner finished scanning " + path);
                    }
                });
    }
}
