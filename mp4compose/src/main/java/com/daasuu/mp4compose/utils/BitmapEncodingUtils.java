package com.daasuu.mp4compose.utils;

import android.graphics.Bitmap;

public class BitmapEncodingUtils {
    // read more on https://wiki.videolan.org/YUV
    public static byte[] getNV12(int inputWidth, int inputHeight, Bitmap scaled) {
        int[] argb = new int[(inputWidth * inputHeight)];
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);
        byte[] yuv = new byte[(((inputWidth * inputHeight) * 3) / 2)];
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);
        scaled.recycle();
        return yuv;
    }

    // encodes bitmap in YUV 4:2:0 format
    public static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        int yIndex = 0;
        int uvIndex = width * height;
        int index = 0;
        int j = 0;
        while (j < height) {
            int uvIndex2;
            int yIndex2;
            int i = 0;
            while (true) {
                uvIndex2 = uvIndex;
                yIndex2 = yIndex;
                if (i >= width) {
                    break;
                }
                int R = (argb[index] & 0xFF0000) >> 16;
                int G = (argb[index] & 0xFF00) >> 8;
                int B = (argb[index] & 0x0000FF) >> 0;
                int Y = ((((R * 77) + (G * 150)) + (B * 29)) + 128) >> 8;
                int V = (((((R * -43) - (G * 84)) + (B * 127)) + 128) >> 8) + 128;
                int U = (((((R * 127) - (G * 106)) - (B * 21)) + 128) >> 8) + 128;
                yIndex = yIndex2 + 1;
                if (Y < 0) {
                    Y = 0;
                } else if (Y > 255) {
                    Y = 255;
                }
                yuv420sp[yIndex2] = (byte) Y;
                if (j % 2 == 0 && index % 2 == 0) {
                    uvIndex = uvIndex2 + 1;
                    if (V < 0) {
                        V = 0;
                    } else if (V > 255) {
                        V = 255;
                    }
                    yuv420sp[uvIndex2] = (byte) V;
                    uvIndex2 = uvIndex + 1;
                    if (U < 0) {
                        U = 0;
                    } else if (U > 255) {
                        U = 255;
                    }
                    yuv420sp[uvIndex] = (byte) U;
                }
                uvIndex = uvIndex2;
                index++;
                i++;
            }
            j++;
            uvIndex = uvIndex2;
            yIndex = yIndex2;
        }
    }
}
