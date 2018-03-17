package com.github.fedka27.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.junit.Assert;
import org.junit.Test;

public class Bitmap64UtilTest {
    private static final String TAG = Bitmap64UtilTest.class.getSimpleName();

    @Test
    public void bitmapToBase64() throws Exception {
        String testBitmapEncoded = "gf45vsdq4rfsdg4t54";

        Bitmap bitmap1 = BitmapFactory.decodeByteArray(testBitmapEncoded.getBytes(),

                0, testBitmapEncoded.getBytes().length);

        String base64Bitmap1 = Bitmap64Util.bitmapToBase64(bitmap1);

        System.out.println(TAG + "base64Bitmap1 length: " + base64Bitmap1.length());

        Bitmap bitmap2 = Bitmap64Util.base64ToBitmap(base64Bitmap1);

        boolean isEqualsBitmaps = bitmap1.equals(bitmap2);

        System.out.println(TAG + "is equals bitmaps of base64: " + isEqualsBitmaps);

        Assert.assertEquals(true, isEqualsBitmaps);
    }

}