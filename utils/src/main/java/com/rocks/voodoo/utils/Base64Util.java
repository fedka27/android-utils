package com.rocks.voodoo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Base64Util {
    /**
     * Encode image to base64
     *
     * @return String base64
     */
    public static String bitmapToBase64(Bitmap bitmap) {

        Bitmap copy = bitmap.copy(bitmap.getConfig(), false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        copy.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);

        byte[] bytes = byteArrayOutputStream.toByteArray();

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(Base64.encodeToString(bytes, Base64.DEFAULT));

        return stringBuilder.toString();
    }

    /**
     * Decode base64 to bitmap
     *
     * @return Bitmap image
     */
    public static Bitmap base64ToBitmap(String b64) {
        byte[] imageAsBytes;
        try {
            imageAsBytes = Base64.decode(b64.getBytes(), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap base64ToBitmap(InputStream inputStream) {
        try {
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    public static String fileToBase64(Context context, Uri uri, int aDefault) throws IOException {

        String name = uri.getLastPathSegment();
        File file = new File(context.getCacheDir(), name);

        int maxBufferSize = 1024 * 1024;

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);

            int bytesAvailable = inputStream.available();

            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            final byte[] buffers = new byte[bufferSize];

            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            inputStream.close();
            outputStream.close();

            file.delete();

            return Base64.encodeToString(buffers, aDefault);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
