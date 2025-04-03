package com.ims.bpcluat.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class Cache {

    public static void listCacheFiles(Context context) {
        File cacheDir = context.getCacheDir();
        File[] cacheFiles = cacheDir.listFiles();
        if (cacheFiles != null && cacheFiles.length > 0) {
            for (File file : cacheFiles) {
                Log.d("CacheFile", "File: " + file.getName());
            }
        } else {
            Log.d("CacheFile", "No cache files found!");
        }
    }
}
