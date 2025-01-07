package com.downloader.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class ScopedStorageHelper {

    private static final List<String> ALLOWED_PUBLIC_DIRECTORIES = Arrays.asList(
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_DOCUMENTS,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_MUSIC,
            Environment.DIRECTORY_MOVIES
    );

    public static boolean validateInputPath(Context context, String path) {
        if (path == null || path.isEmpty()) {
            System.out.println("path empty:" + path);
            return false;
        }

        File file = new File(path);

        if (!checkBasicFileProperties(file)) {
            return false;
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && !isInAppPrivateDirectory(context, file) && !isInAllowedPublicDirectory(file)) {
            return false;
        }
        return true;
    }

    private static boolean checkBasicFileProperties(File file) {
        try {
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return false;
                }
            }

            if (!file.canRead()) {
                return false;
            }

            if (!file.canWrite()) {
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean isScopedStorageRequired(Context context, String path) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            return false;
        }

        File file = new File(path);

        if (isInAppPrivateDirectory(context, file)) {
            return false;
        }

        return isInAllowedPublicDirectory(file);
    }

    private static boolean isInAppPrivateDirectory(Context context, File file) {
        try {
            String appPrivateDir = context.getExternalFilesDir(null).getAbsolutePath();
            String appPrivateCacheDir = context.getExternalCacheDir().getAbsolutePath();

            String filePath = file.getAbsolutePath();

            return filePath.startsWith(appPrivateDir) || filePath.startsWith(appPrivateCacheDir);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isInAllowedPublicDirectory(File file) {
        try {
            String filePath = file.getAbsolutePath();

            for (String publicDir : ALLOWED_PUBLIC_DIRECTORIES) {
                File publicDirectory = Environment.getExternalStoragePublicDirectory(publicDir);
                if (filePath.startsWith(publicDirectory.getAbsolutePath())) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void moveFileScopedStorage(Context context, File oldFile, String newDirPath, String newFileName) throws IOException {
        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, validateAndExtractRelativePath(newDirPath));
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");

        Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);
        if (uri == null) {
            throw new IOException("create target file failure：" + newDirPath + "/" + newFileName);
        }

        try (InputStream in = resolver.openInputStream(Uri.fromFile(oldFile));
             OutputStream out = resolver.openOutputStream(uri)) {
            if (in == null || out == null) {
                throw new IOException("can not open the input stream");
            }

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            if (!oldFile.delete()) {
                throw new IOException("oldFile delete failure：" + oldFile.getPath());
            }
        }
    }

    private static String validateAndExtractRelativePath(String newDirPath) throws IllegalArgumentException {
        if (newDirPath == null || newDirPath.isEmpty()) {
            throw new IllegalArgumentException("Path is null or empty");
        }

        String externalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();

        if (newDirPath.startsWith(externalStoragePath)) {
            String relativePath = newDirPath.substring(externalStoragePath.length() + 1);
            for (String allowedDir : ALLOWED_PUBLIC_DIRECTORIES) {
                if (relativePath.startsWith(allowedDir)) {
                    return relativePath;
                }
            }
            throw new IllegalArgumentException("Invalid path: " + newDirPath + ". Allowed paths are: " + ALLOWED_PUBLIC_DIRECTORIES + " and relativePath:" + relativePath);
        } else {
            throw new IllegalArgumentException("Invalid path: " + newDirPath);
        }
    }
}

