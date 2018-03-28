package com.github.fedka27.utils.encoding;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Bitmap64Util {
    /**
     * Encode image to base64
     *
     * Not recommended use in Main thread
     *
     * @return String base64
     */
    public static String bitmapToBase64(Bitmap bitmap) {


        Bitmap copy = bitmap.copy(bitmap.getConfig(), false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        copy.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);

        byte[] bytes = Base64.encode(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);

        StringBuilder stringBuffer = new StringBuilder();

        InputStreamReader inputStreamReader = new InputStreamReader(new ByteArrayInputStream(bytes));
        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];

        while (true) {
            try {

                int read = inputStreamReader.read(buffer, 0, bufferSize);
                if (read == -1) {
                    break;
                }
                String line = new String(buffer, 0, read);

                stringBuffer.append(line);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return stringBuffer.toString();

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
}
