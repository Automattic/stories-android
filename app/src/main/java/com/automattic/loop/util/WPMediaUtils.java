package com.automattic.loop.util;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class WPMediaUtils {
    public static List<Uri> retrieveMediaUris(Intent data) {
        ClipData clipData = data.getClipData();
        ArrayList<Uri> uriList = new ArrayList<>();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                uriList.add(item.getUri());
            }
        } else {
            uriList.add(data.getData());
        }
        return uriList;
    }
}
