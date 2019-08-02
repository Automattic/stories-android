package com.daasuu.mp4compose.filter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Created by sudamasayuki2 on 2018/01/27.
 */

public class GlWatermarkFilter extends GlOverlayFilter {

    private Bitmap bitmap;
    private Position position = Position.LEFT_TOP;
    private ViewPositionInfo positionInfo;
    private Matrix matrixF;

    public GlWatermarkFilter(Bitmap bitmap) {
        this.bitmap = bitmap;
    }


    public GlWatermarkFilter(Bitmap bitmap, Position position) {
        this.bitmap = bitmap;
        this.position = position;
    }

    public GlWatermarkFilter(Bitmap bitmap, ViewPositionInfo position) {
        this.bitmap = bitmap;
        this.positionInfo = position;
    }

    @Override
    protected void drawCanvas(Canvas canvas, long presentationTime) {
        if (bitmap != null && !bitmap.isRecycled()) {
            if (positionInfo != null) {
                // calculate once
                if (matrixF == null) {
                    // transform coordinates from original parent View coordinate system to this video Canvas
                    // coordinate system
                    float newScaleY = (float) canvas.getHeight() / (float) positionInfo.getParentViewHeight();
                    float newScaleX = (float) canvas.getWidth() / (float) positionInfo.getParentViewWidth();

                    float quadrant1XOffset = positionInfo.getParentViewWidth() / 2;
                    float quadrant1YOffset = positionInfo.getParentViewHeight() / 2;
                    float newXcoord = (quadrant1XOffset - (positionInfo.getWidth() / 2)) * newScaleX;
                    float newYcoord = (quadrant1YOffset - (positionInfo.getHeight() / 2)) * newScaleY;

                    // deep copy the Matrix from original pinched/dragged view, re-scale with new destination surface scale
                    // and translate to new coordinate system
                    matrixF = new Matrix(positionInfo.getMatrix());
                    matrixF.postScale(newScaleX, newScaleX);
                    matrixF.postTranslate(newXcoord, newYcoord);
                }
                Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
                canvas.drawBitmap(bitmap, matrixF, p);
            } else {
                switch (position) {
                    case LEFT_TOP:
                        canvas.drawBitmap(bitmap, 0, 0, null);
                        break;
                    case LEFT_BOTTOM:
                        canvas.drawBitmap(bitmap, 0, canvas.getHeight() - bitmap.getHeight(), null);
                        break;
                    case RIGHT_TOP:
                        canvas.drawBitmap(bitmap, canvas.getWidth() - bitmap.getWidth(), 0, null);
                        break;
                    case RIGHT_BOTTOM:
                        canvas.drawBitmap(bitmap, canvas.getWidth() - bitmap.getWidth(), canvas.getHeight() - bitmap.getHeight(), null);
                        break;
                }
            }
        }
    }

    public enum Position {
        LEFT_TOP,
        LEFT_BOTTOM,
        RIGHT_TOP,
        RIGHT_BOTTOM
    }
}