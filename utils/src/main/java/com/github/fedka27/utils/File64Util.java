package com.github.fedka27.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class File64Util {

    /**
     * @return base64 of the file
     */
    @Nullable
    public static String fileToBase64(@NonNull Context context,
                                      Uri uriFile) throws IOException {
        return fileToBase64(context.getContentResolver(),
                context.getCacheDir().getPath(),
                uriFile,
                Base64.DEFAULT);
    }

    /**
     * @param encodeFlag controls certain features of the encoded output.
     *                   Passing {@code DEFAULT} results in output that
     *                   adheres to RFC 2045.
     * @return base64 of the file
     */
    @Nullable
    public static String fileToBase64(@NonNull Context context,
                                      Uri uriFile,
                                      int encodeFlag) throws IOException {
        return fileToBase64(context.getContentResolver(),
                context.getCacheDir().getPath(),
                uriFile,
                encodeFlag);
    }

    /**
     * @return base64 of the file
     */
    @Nullable
    public static String fileToBase64(@NonNull ContentResolver contentResolver,
                                      String cacheDir,
                                      Uri uriFile) throws IOException {
        return fileToBase64(contentResolver, cacheDir, uriFile, Base64.DEFAULT);
    }

    /**
     * @param encodeFlag controls certain features of the encoded output.
     *                   Passing {@code DEFAULT} results in output that
     *                   adheres to RFC 2045.
     * @return base64 of the file
     */
    @Nullable
    public static String fileToBase64(@NonNull ContentResolver contentResolver,
                                      String cacheDir,
                                      Uri uriFile,
                                      int encodeFlag) throws IOException {

        String name = uriFile.getLastPathSegment();
        File file = new File(cacheDir, name);

        int maxBufferSize = 1024 * 1024;

        try {
            InputStream inputStream = contentResolver.openInputStream(uriFile);

            int bytesAvailable = inputStream.available();

            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            final byte[] buffers = new byte[bufferSize];

            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            Log.e("File Size", "Size " + file.length());
            inputStream.close();
            outputStream.close();

            Log.e("File Path", "Path " + file.getPath());

            Log.e("File Size", "Size " + file.length());

            file.delete();

            return Base64.encodeToString(buffers, encodeFlag);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
