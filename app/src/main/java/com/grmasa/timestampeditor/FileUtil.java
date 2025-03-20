package com.grmasa.timestampeditor;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

public class FileUtil {
    public static String getFullPathFromTreeUri(Uri uri, Context context) {
        String docId = DocumentsContract.getDocumentId(uri);
        String[] split = docId.split(":");
        String type = split[0];
        String path = split.length > 1 ? split[1] : "";
        if ("primary".equalsIgnoreCase(type)) {
            return "/storage/emulated/0/" + path;
        }
        return null;
    }
}
