package com.downloader.utils;

import android.content.Context;
import android.os.Environment;

import com.downloader.Constants;
import com.downloader.core.Core;
import com.downloader.database.DownloadModel;
import com.downloader.httpclient.HttpClient;
import com.downloader.internal.ComponentHolder;
import com.downloader.request.DownloadRequest;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

/**
 * Created by amitshekhar on 13/11/17.
 */

public final class Utils {

    private final static int MAX_REDIRECTION = 10;

    private Utils() {
        // no instance
    }

    public static boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean isExternalStorageReadWrite() {
        String state = Environment.getExternalStorageState();
        return !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static String getPath(String dirPath, String fileName) {
        return dirPath + File.separator + fileName;
    }

    private static String generateTempFileUUID(String id, String dirPath, int fileName) {
        String input = dirPath + "|" + fileName + "|" + id;
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8)).toString();
    }

//    public static String getTempPath(String dirPath, String fileName) {
//        return getPath(dirPath, fileName) + ".temp";
//    }

    public static String getTempPath(int id, String fileName) {
        String downloadPath = ComponentHolder.getInstance().getTempFilePath();
        String uuid = generateTempFileUUID(downloadPath, fileName, id);

        String folderPath = downloadPath + File.separator + uuid;

        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folderPath + File.separator + fileName + ".temp";
    }

//    public static void renameFileName(String oldPath, String newPath) throws IOException {
//        final File oldFile = new File(oldPath);
//        try {
//            final File newFile = new File(newPath);
//            if (newFile.exists()) {
//                if (!newFile.delete()) {
//                    throw new IOException("Deletion Failed");
//                }
//            }
//            if (!oldFile.renameTo(newFile)) {
//                throw new IOException("Rename Failed");
//            }
//        } finally {
//            if (oldFile.exists()) {
//                //noinspection ResultOfMethodCallIgnored
//                oldFile.delete();
//            }
//        }
//    }


    public static void renameFileName(Context context, String oldPath, String newDirPath, String newFileName) throws IOException {
        final File oldFile = new File(oldPath);

        if (!oldFile.exists()) {
            throw new IOException("oldFile not exist：" + oldPath);
        }

        File newDir = new File(newDirPath);
        if (!newDir.exists() && !newDir.mkdirs()) {
            throw new IOException("create target file failure：" + newDirPath);
        }

        String finalFileName = generateUniqueFileName(newDir, newFileName);

        if (ScopedStorageHelper.isScopedStorageRequired(context,newDirPath)) {
            ScopedStorageHelper.moveFileScopedStorage(context, oldFile, newDirPath, finalFileName);
        } else {
            File newFile = new File(newDir, finalFileName);
            if (newFile.exists() && !newFile.delete()) {
                throw new IOException("target file delete failure：" + newFile.getPath());
            }
            if (!oldFile.renameTo(newFile)) {
                throw new IOException("file move failure：" + oldFile.getPath() + " -> " + newFile.getPath());
            }
        }
    }

    private static String generateUniqueFileName(File targetDir, String fileName) {
        String name = fileName;
        String extension = "";

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            name = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        File file = new File(targetDir, fileName);
        int count = 1;
        while (file.exists()) {
            file = new File(targetDir, name + "(" + count + ")" + extension);
            count++;
        }
        return file.getName();
    }

    public static void deleteTempFileAndDatabaseEntryInBackground(final String path, final int downloadId) {
        Core.getInstance().getExecutorSupplier().forBackgroundTasks()
                .execute(new Runnable() {
                    @Override
                    public void run() {
                        ComponentHolder.getInstance().getDbHelper().remove(downloadId);
                        File file = new File(path);
                        if (file.exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            file.delete();
                        }
                    }
                });
    }

    public static void deleteUnwantedModelsAndTempFiles(final int days) {
        Core.getInstance().getExecutorSupplier().forBackgroundTasks()
                .execute(new Runnable() {
                    @Override
                    public void run() {
                        List<DownloadModel> models = ComponentHolder.getInstance()
                                .getDbHelper()
                                .getUnwantedModels(days);
                        if (models != null) {
                            for (DownloadModel model : models) {
                                final String tempPath = getTempPath(model.getId(), model.getFileName());
                                ComponentHolder.getInstance().getDbHelper().remove(model.getId());
                                File file = new File(tempPath);
                                if (file.exists()) {
                                    //noinspection ResultOfMethodCallIgnored
                                    file.delete();
                                }
                            }
                        }
                    }
                });
    }

    public static int getUniqueId(String url, String dirPath, String fileName) {

        String string = url + File.separator + dirPath + File.separator + fileName;

        byte[] hash;

        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UnsupportedEncodingException", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);

        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }

        return hex.toString().hashCode();

    }

    public static HttpClient getRedirectedConnectionIfAny(HttpClient httpClient,
                                                          DownloadRequest request)
            throws IOException, IllegalAccessException {
        int redirectTimes = 0;
        int code = httpClient.getResponseCode();
        String location = httpClient.getResponseHeader("Location");

        while (isRedirection(code)) {
            if (location == null) {
                throw new IllegalAccessException("Location is null");
            }
            httpClient.close();

            request.setUrl(location);
            httpClient = ComponentHolder.getInstance().getHttpClient();
            httpClient.connect(request);
            code = httpClient.getResponseCode();
            location = httpClient.getResponseHeader("Location");
            redirectTimes++;
            if (redirectTimes >= MAX_REDIRECTION) {
                throw new IllegalAccessException("Max redirection done");
            }
        }

        return httpClient;
    }

    private static boolean isRedirection(int code) {
        return code == HttpURLConnection.HTTP_MOVED_PERM
                || code == HttpURLConnection.HTTP_MOVED_TEMP
                || code == HttpURLConnection.HTTP_SEE_OTHER
                || code == HttpURLConnection.HTTP_MULT_CHOICE
                || code == Constants.HTTP_TEMPORARY_REDIRECT
                || code == Constants.HTTP_PERMANENT_REDIRECT;
    }

}
