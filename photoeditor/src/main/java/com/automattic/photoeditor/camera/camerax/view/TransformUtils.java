package com.automattic.photoeditor.camera.camerax.view;

/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.graphics.RectF;
import android.util.Size;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
/**
 * Utility class for transform.
 *
 * <p> The vertices representation uses a float array to represent a rectangle with arbitrary
 * rotation and rotation-direction. It could be otherwise represented by a triple of a
 * {@link RectF}, a rotation degrees integer and a boolean flag for the rotation-direction
 * (clockwise v.s. counter-clockwise).
 *
 * TODO(b/179827713): merge this with {@link androidx.camera.core.internal.utils.ImageUtil}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TransformUtils {
    // Each vertex is represented by a pair of (x, y) which is 2 slots in a float array.
    private static final int FLOAT_NUMBER_PER_VERTEX = 2;
    private TransformUtils() {
    }
    /**
     * Creates a new quad by rotating {@code original}'s vertices {@code rotationDegrees} clockwise.
     *
     * <pre>
     *  a----b
     *  |    |
     *  d----c  vertices = {a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y}
     *
     * After 90° rotation:
     *
     *  d----a
     *  |    |
     *  c----b  vertices = {d.x, d.y, a.x, a.y, b.x, b.y, c.x, c.y}
     * </pre>
     *
     * @param rotationDegrees multiple of 90.
     */
    @NonNull
    public static float[] createRotatedVertices(@NonNull float[] original, int rotationDegrees) {
        float[] rotated = new float[original.length];
        int offset = -rotationDegrees / 90 * FLOAT_NUMBER_PER_VERTEX;
        for (int originalIndex = 0; originalIndex < original.length; originalIndex++) {
            int rotatedIndex = (originalIndex + offset) % original.length;
            rotatedIndex = rotatedIndex < 0 ? rotatedIndex + original.length : rotatedIndex;
            rotated[rotatedIndex] = original[originalIndex];
        }
        return rotated;
    }
    /**
     * Converts an array of vertices to a {@link RectF}.
     */
    @NonNull
    public static RectF verticesToRect(@NonNull float[] vertices) {
        return new RectF(
                min(vertices[0], vertices[2], vertices[4], vertices[6]),
                min(vertices[1], vertices[3], vertices[5], vertices[7]),
                max(vertices[0], vertices[2], vertices[4], vertices[6]),
                max(vertices[1], vertices[3], vertices[5], vertices[7])
        );
    }
    /**
     * Returns the max value.
     */
    public static float max(float value1, float value2, float value3, float value4) {
        return Math.max(Math.max(value1, value2), Math.max(value3, value4));
    }
    /**
     * Returns the min value.
     */
    public static float min(float value1, float value2, float value3, float value4) {
        return Math.min(Math.min(value1, value2), Math.min(value3, value4));
    }
    /**
     * Converts {@link Surface} rotation to rotation degrees: 90, 180, 270 or 0.
     */
    public static int surfaceRotationToRotationDegrees(int rotationValue) {
        switch (rotationValue) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                throw new IllegalStateException("Unexpected rotation value " + rotationValue);
        }
    }
    /**
     * Returns true if the rotation degrees is 90 or 270.
     */
    public static boolean is90or270(int rotationDegrees) {
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            return true;
        }
        if (rotationDegrees == 0 || rotationDegrees == 180) {
            return false;
        }
        throw new IllegalArgumentException("Invalid rotation degrees: " + rotationDegrees);
    }
    /**
     * Converts a {@link Size} to a float array of vertices.
     */
    @NonNull
    public static float[] sizeToVertices(@NonNull Size size) {
        return new float[]{0, 0, size.getWidth(), 0, size.getWidth(), size.getHeight(), 0,
                size.getHeight()};
    }
    /**
     * Converts a {@link RectF} defined by top, left, right and bottom to an array of vertices.
     */
    @NonNull
    public static float[] rectToVertices(@NonNull RectF rectF) {
        return new float[]{rectF.left, rectF.top, rectF.right, rectF.top, rectF.right, rectF.bottom,
                rectF.left, rectF.bottom};
    }
    /**
     * Checks if aspect ratio matches while tolerating rounding error.
     *
     * <p> One example of the usage is comparing the viewport-based crop rect from different use
     * cases. The crop rect is rounded because pixels are integers, which may introduce an error
     * when we check if the aspect ratio matches. For example, when {@link PreviewView}'s
     * width/height are prime numbers 601x797, the crop rect from other use cases cannot have a
     * matching aspect ratio even if they are based on the same viewport. This method checks the
     * aspect ratio while tolerating a rounding error.
     *
     * @param size1       the rounded size1
     * @param isAccurate1 if size1 is accurate. e.g. it's true if it's the PreviewView's
     *                    dimension which viewport is based on
     * @param size2       the rounded size2
     * @param isAccurate2 if size2 is accurate.
     */
    public static boolean isAspectRatioMatchingWithRoundingError(
            @NonNull Size size1, boolean isAccurate1, @NonNull Size size2, boolean isAccurate2) {
        // The input width/height are rounded values, so they are at most .5 away from their
        // true values.
        // First figure out the possible range of the aspect ratio's ture value.
        float ratio1UpperBound;
        float ratio1LowerBound;
        if (isAccurate1) {
            ratio1UpperBound = (float) size1.getWidth() / size1.getHeight();
            ratio1LowerBound = ratio1UpperBound;
        } else {
            ratio1UpperBound = (size1.getWidth() + .5F) / (size1.getHeight() - .5F);
            ratio1LowerBound = (size1.getWidth() - .5F) / (size1.getHeight() + .5F);
        }
        float ratio2UpperBound;
        float ratio2LowerBound;
        if (isAccurate2) {
            ratio2UpperBound = (float) size2.getWidth() / size2.getHeight();
            ratio2LowerBound = ratio2UpperBound;
        } else {
            ratio2UpperBound = (size2.getWidth() + .5F) / (size2.getHeight() - .5F);
            ratio2LowerBound = (size2.getWidth() - .5F) / (size2.getHeight() + .5F);
        }
        // Then we check if the true value range overlaps.
        return ratio1UpperBound >= ratio2LowerBound && ratio2UpperBound >= ratio1LowerBound;
    }
}
