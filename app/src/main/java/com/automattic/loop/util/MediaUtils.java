package com.automattic.loop.util;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaUtils {
    private static final int DEFAULT_MAX_IMAGE_WIDTH = 1024;
    private static final Pattern FILE_EXISTS_PATTERN = Pattern.compile("(.*?)(-([0-9]+))?(\\..*$)?");

    /**
     * Get image max size setting from the image max size setting string. This string can be an int, in this case it's
     * the maximum image width defined by the site.
     * Examples:
     * "1000" will return 1000
     * "Original Size" will return Integer.MAX_VALUE
     * "Largeur originale" will return Integer.MAX_VALUE
     * null will return Integer.MAX_VALUE
     * @param imageMaxSizeSiteSettingString Image max size site setting string
     * @return Integer.MAX_VALUE if image width is not defined or invalid, maximum image width in other cases.
     */
    public static int getImageMaxSizeSettingFromString(String imageMaxSizeSiteSettingString) {
        if (imageMaxSizeSiteSettingString == null) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.valueOf(imageMaxSizeSiteSettingString);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Calculate and return the maximum allowed image width by comparing the width of the image at its full size with
     * the maximum upload width set in the blog settings
     * @param imageSize the image's natural (full) width
     * @param imageMaxSizeSiteSettingString the maximum upload width set in the site settings
     * @return maximum allowed image width
     */
    public static int getMaximumImageSize(int imageSize, String imageMaxSizeSiteSettingString) {
        int imageMaxSizeBlogSetting = getImageMaxSizeSettingFromString(imageMaxSizeSiteSettingString);
        int imageWidthPictureSetting = imageSize == 0 ? Integer.MAX_VALUE : imageSize;

        if (Math.min(imageWidthPictureSetting, imageMaxSizeBlogSetting) == Integer.MAX_VALUE) {
            // Default value in case of errors reading the picture size or the blog settings is set to Original size
            return DEFAULT_MAX_IMAGE_WIDTH;
        } else {
            return Math.min(imageWidthPictureSetting, imageMaxSizeBlogSetting);
        }
    }

    public static boolean isInMediaStore(Uri mediaUri) {
        // Check if the image is externally hosted (Picasa/Google Photos for example)
        return mediaUri != null && mediaUri.toString().startsWith("content://media/");
    }

    public static @Nullable String getFilenameFromURI(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null);
        try {
            String result = null;
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndexDisplayName = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (columnIndexDisplayName == -1) {
                    return null;
                }
                result = cursor.getString(columnIndexDisplayName);
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /*
     * Some media providers (eg. Google Photos) give us a limited access to media files just so we can copy them and
     * then they revoke the access. Copying these files must be performed on the UI thread, otherwise the access might
     * be revoked before the action completes. See https://github.com/wordpress-mobile/WordPress-Android/issues/5818
     */
    public static Uri downloadExternalMedia(Context context, Uri imageUri) {
        if (context == null || imageUri == null) {
            return null;
        }
        String mimeType = getUrlMimeType(imageUri.toString());
        File cacheDir = context.getCacheDir();

        if (cacheDir != null && !cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        try {
            InputStream input;
            // Download the file
            if (imageUri.toString().startsWith("content://")) {
                input = context.getContentResolver().openInputStream(imageUri);
                if (input == null) {
                    Log.e("UTILS", "openInputStream returned null");
                    return null;
                }
            } else {
                input = new URL(imageUri.toString()).openStream();
            }

            String fileName = getFilenameFromURI(context, imageUri);
            if (TextUtils.isEmpty(fileName)) {
                fileName = generateTimeStampedFileName(mimeType);
            }

            File f = getUniqueCacheFileForName(fileName, cacheDir, mimeType);

            OutputStream output = new FileOutputStream(f);

            byte[] data = new byte[1024];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            return Uri.fromFile(f);
        } catch (IOException e) {
            Log.e("UTILS", e.getMessage());
        }

        return null;
    }

    private static File getUniqueCacheFileForName(String fileName, File cacheDir, String mimeType) {
        File file = new File(cacheDir, fileName);

        while (file.exists()) {
            Matcher matcher = FILE_EXISTS_PATTERN.matcher(fileName);
            if (matcher.matches()) {
                String baseFileName = matcher.group(1);
                String existingDuplicationNumber = matcher.group(3);
                String fileType = StringUtils.notNullStr(matcher.group(4));

                if (existingDuplicationNumber == null) {
                    // Not a copy already
                    fileName = baseFileName + "-1" + fileType;
                } else {
                    fileName = baseFileName + "-" + (StringUtils.stringToInt(existingDuplicationNumber) + 1) + fileType;
                }
            } else {
                // Shouldn't happen, but in case our match fails fall back to timestamped file name
                fileName = generateTimeStampedFileName(mimeType);
            }
            file = new File(cacheDir, fileName);
        }
        return file;
    }

    public static String generateTimeStampedFileName(String mimeType) {
        return "wp-" + System.currentTimeMillis() + "." + getExtensionForMimeType(mimeType);
    }

    public static String getMimeTypeOfInputStream(InputStream stream) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, options);
        return options.outMimeType;
    }

    /**
     * see http://stackoverflow.com/a/8591230/1673548
     */
    public static String getUrlMimeType(final String urlString) {
        if (urlString == null) {
            return null;
        }

        String extension = MimeTypeMap.getFileExtensionFromUrl(urlString);
        if (extension == null) {
            return null;
        }

        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String mimeType = mime.getMimeTypeFromExtension(extension);
        if (mimeType == null) {
            return null;
        }

        return mimeType;
    }

    public static String getExtensionForMimeType(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return "";
        }

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String fileExtensionFromMimeType = mimeTypeMap.getExtensionFromMimeType(mimeType);
        if (TextUtils.isEmpty(fileExtensionFromMimeType)) {
            // We're still without an extension - split the mime type and retrieve it
            String[] split = mimeType.split("/");
            fileExtensionFromMimeType = split.length > 1 ? split[1] : split[0];
        }

        return fileExtensionFromMimeType.toLowerCase(Locale.ROOT);
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * Based on paulburke's solution for aFileChooser - https://github.com/iPaulPro/aFileChooser
     *
     * @param context The context.
     * @param uri The Uri to query.
     */
    private static String getPath(final Context context, final Uri uri) {
        String path = getDocumentProviderPathKitkatOrHigher(context, uri);

        if (path != null) {
            return path;
        }

        // MediaStore (and general)
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) { // File
            return uri.getPath();
        }

        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getDocumentProviderPathKitkatOrHigher(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                String id = DocumentsContract.getDocumentId(uri);

                if (id != null && id.startsWith("raw:")) {
                    return id.substring(4);
                }

                // https://github.com/Javernaut/WhatTheCodec/issues/2
                if (id != null && id.startsWith("msf:")) {
                    id = id.substring(4);
                }

                String[] contentUriPrefixesToTry = new String[]{
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads",
                        "content://downloads/all_downloads"
                };

                for (String contentUriPrefix : contentUriPrefixesToTry) {
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));
                    try {
                        String path = getDataColumn(context, contentUri, null, null);
                        if (path != null) {
                            return path;
                        }
                    } catch (Exception e) {
                        Log.e("UTILS", "Error reading _data column for URI: " + contentUri, e);
                    }
                }
                return downloadExternalMedia(context, uri).getPath();
            } else if (isMediaDocument(uri)) { // MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;

                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = MediaStore.MediaColumns._ID + "=?";

                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        final String column = MediaStore.MediaColumns.DATA;

        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndex(column);
                if (columnIndex != -1) {
                    return cursor.getString(columnIndex);
                }
            }
        } catch (SecurityException errReadingContentResolver) {
            Log.e("UTILS", "Error reading _data column for URI: " + uri, errReadingContentResolver);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
