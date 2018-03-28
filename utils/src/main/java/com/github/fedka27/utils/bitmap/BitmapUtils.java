package com.github.fedka27.utils.bitmap;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.support.media.ExifInterface;

import java.io.IOException;

public class BitmapUtils {

    public static Bitmap toPortraitBitmap(ContentResolver contentResolver, Bitmap originalBitmap, Uri uri) {
        try {
            Bitmap rotatedBitmap;

            ExifInterface exifInterface = new ExifInterface(contentResolver.openInputStream(uri));

            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotate(originalBitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotate(originalBitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotate(originalBitmap, 270);
                    break;
                default:
                    rotatedBitmap = originalBitmap;
            }

            return rotatedBitmap;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return originalBitmap;
    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
    }
}
