
package com.daasuu.mp4compose.composer;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Size;

import com.daasuu.mp4compose.FillMode;
import com.daasuu.mp4compose.FillModeCustomItem;
import com.daasuu.mp4compose.Rotation;
import com.daasuu.mp4compose.filter.GlFilter;
import com.daasuu.mp4compose.utils.BitmapEncodingUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;

// Refer: https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/ExtractDecodeEditEncodeMuxTest.java
// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/VideoTrackTranscoder.java
class VideoComposer {
    private static final String TAG = "VideoComposer";
    private static final int DRAIN_STATE_NONE = 0;
    private static final int DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 1;
    private static final int DRAIN_STATE_CONSUMED = 2;

    private final MediaExtractor mediaExtractor;
    private final int trackIndex;
    private final MediaFormat outputFormat;
    private final MuxRender muxRender;
    private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec decoder;
    private MediaCodec encoder;
    private ByteBuffer[] decoderInputBuffers;
    private ByteBuffer[] encoderOutputBuffers;
    private MediaFormat actualOutputFormat;
    private DecoderSurface decoderSurface;
    private EncoderSurface encoderSurface;
    private boolean isExtractorEOS;
    private boolean isDecoderEOS;
    private boolean isEncoderEOS;
    private boolean decoderStarted;
    private boolean encoderStarted;
    private long writtenPresentationTimeUs;
    private final int timeScale;
    private boolean useStaticBkg;
    //private Bitmap bkgBitmap;
    private static final long ONE_SEC = 1000000;
    private int addedFrameCount = 0;
    private byte[] bkgBitmapBytesNV12;
    private int lastBufferIdx = 0;

    VideoComposer(MediaExtractor mediaExtractor, int trackIndex,
                  MediaFormat outputFormat, MuxRender muxRender, int timeScale) {
        this.mediaExtractor = mediaExtractor;
        this.trackIndex = trackIndex;
        this.outputFormat = outputFormat;
        this.muxRender = muxRender;
        this.timeScale = timeScale;
    }

    // alternate constructor that doesn't have a MediaExtractor, since we won't be converting from video to another
    VideoComposer(Bitmap bkgBitmap, MediaFormat outputFormat, MuxRender muxRender, int timeScale) {
        this.outputFormat = outputFormat;
        this.muxRender = muxRender;
        this.timeScale = timeScale;
        this.useStaticBkg = true;

        this.bkgBitmapBytesNV12 = BitmapEncodingUtils.getNV12(bkgBitmap.getWidth(), bkgBitmap.getHeight(), bkgBitmap);
        bkgBitmap.recycle();

        this.mediaExtractor = null;
        this.trackIndex = -1;
    }

    void setUp(GlFilter filter,
               Rotation rotation,
               Size outputResolution,
               Size inputResolution,
               FillMode fillMode,
               FillModeCustomItem fillModeCustomItem,
               final boolean flipVertical,
               final boolean flipHorizontal) {
        try {
            encoder = MediaCodec.createEncoderByType(outputFormat.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        if (!useStaticBkg) {
            encoderSurface = new EncoderSurface(encoder.createInputSurface());
            encoderSurface.makeCurrent();
        }
        encoder.start();
        encoderStarted = true;
        encoderOutputBuffers = encoder.getOutputBuffers();

        if (!useStaticBkg) {
            // only set up mediaExtractor if source is a video we can decode :)
            mediaExtractor.selectTrack(trackIndex);
            MediaFormat inputFormat = mediaExtractor.getTrackFormat(trackIndex);
            if (inputFormat.containsKey("rotation-degrees")) {
                // Decoded video is rotated automatically in Android 5.0 lollipop.
                // Turn off here because we don't want to encode rotated one.
                // refer: https://android.googlesource.com/platform/frameworks/av/+blame/lollipop-release/media/libstagefright/Utils.cpp
                inputFormat.setInteger("rotation-degrees", 0);
            }

            decoderSurface = new DecoderSurface(filter);
            decoderSurface.setRotation(rotation);
            decoderSurface.setOutputResolution(outputResolution);
            decoderSurface.setInputResolution(inputResolution);
            decoderSurface.setFillMode(fillMode);
            decoderSurface.setFillModeCustomItem(fillModeCustomItem);
            decoderSurface.setFlipHorizontal(flipHorizontal);
            decoderSurface.setFlipVertical(flipVertical);
            decoderSurface.completeParams();

            try {
                decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            decoder.configure(inputFormat, decoderSurface.getSurface(), null, 0);
            decoder.start();
            decoderStarted = true;
            decoderInputBuffers = decoder.getInputBuffers();
        }
    }

    boolean stepPipeline() {
        boolean busy = false;

        int status;
        while (drainEncoder() != DRAIN_STATE_NONE) {
            busy = true;
        }
        do {
            status = drainDecoder();
            if (status != DRAIN_STATE_NONE) {
                busy = true;
            }
            // NOTE: not repeating to keep from deadlock when encoder is full.
        } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY);
        while (drainExtractor() != DRAIN_STATE_NONE) {
            busy = true;
        }

        return busy;
    }

    boolean stepPipelineStaticImageBackground() {
        boolean busy = false;
        int status;

        int inputBufIdx = encoder.dequeueInputBuffer(0);
        // inject the image
        if (inputBufIdx >= 0) {
            // TODO figure out here when we have run out of bitmaps OR when it's the end time for the video?
            // FIXME hardcoded 5 seconds length
            if (writtenPresentationTimeUs > 5000000) { // fixed at 5 seconds
                isExtractorEOS = true;
                encoder.queueInputBuffer(inputBufIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                status = DRAIN_STATE_NONE;
            } else {
                // add bitmap into decoder.inputBuffer
                //byte[] input = BitmapEncodingUtils.getNV12(bkgBitmap.getWidth(), bkgBitmap.getHeight(), bkgBitmap);
                ByteBuffer inputBuffer = encoder.getInputBuffer(inputBufIdx);
                inputBuffer.clear();
                inputBuffer.put(bkgBitmapBytesNV12);
                encoder.queueInputBuffer(inputBufIdx, 0, bkgBitmapBytesNV12.length,
                        getPresentationTimeUsec(addedFrameCount), 0);
                addedFrameCount++;
            }
        }

        while (drainEncoder() != DRAIN_STATE_NONE) {
            busy = true;
        }
//        do {
//            status = drainDecoder();
//            if (status != DRAIN_STATE_NONE) {
//                busy = true;
//            }
//            // NOTE: not repeating to keep from deadlock when encoder is full.
//        } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY);
//        while (drainExtractor() != DRAIN_STATE_NONE) {
//            busy = true;
//        }

        return busy;
    }


    long getWrittenPresentationTimeUs() {
        return writtenPresentationTimeUs;
    }


    boolean isFinished() {
        return isEncoderEOS;
    }


    void release() {
        if (decoderSurface != null) {
            decoderSurface.release();
            decoderSurface = null;
        }
        if (encoderSurface != null) {
            encoderSurface.release();
            encoderSurface = null;
        }
        if (decoder != null) {
            if (decoderStarted) decoder.stop();
            decoder.release();
            decoder = null;
        }
        if (encoder != null) {
            if (encoderStarted) encoder.stop();
            encoder.release();
            encoder = null;
        }
    }

    private int drainExtractor() {
        if (isExtractorEOS) return DRAIN_STATE_NONE;

        int trackIndex = mediaExtractor.getSampleTrackIndex();
        if (trackIndex >= 0 && trackIndex != this.trackIndex) {
            return DRAIN_STATE_NONE;
        }
        int result = decoder.dequeueInputBuffer(0);
        if (result < 0) return DRAIN_STATE_NONE;
        if (trackIndex < 0) {
            isExtractorEOS = true;
            decoder.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return DRAIN_STATE_NONE;
        }
        int sampleSize = mediaExtractor.readSampleData(decoderInputBuffers[result], 0);
        boolean isKeyFrame = (mediaExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        decoder.queueInputBuffer(result, 0, sampleSize, mediaExtractor.getSampleTime() / timeScale, isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0);
        mediaExtractor.advance();
        return DRAIN_STATE_CONSUMED;
    }


    private static long getPresentationTimeUsec(int frameIndex) {
        return (((long) frameIndex) * ONE_SEC) / 20;
    }

    private int drainDecoder() {
        if (isDecoderEOS) return DRAIN_STATE_NONE;
        int result = decoder.dequeueOutputBuffer(bufferInfo, 0);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            encoder.signalEndOfInputStream();
            isDecoderEOS = true;
            bufferInfo.size = 0;
        }
        boolean doRender = (bufferInfo.size > 0);
        // NOTE: doRender will block if buffer (of encoder) is full.
        // Refer: http://bigflake.com/mediacodec/CameraToMpegTest.java.txt
        decoder.releaseOutputBuffer(result, doRender);
        if (doRender) {
            decoderSurface.awaitNewImage();
            decoderSurface.drawImage(bufferInfo.presentationTimeUs);
            encoderSurface.setPresentationTime(bufferInfo.presentationTimeUs * 1000);
            encoderSurface.swapBuffers();
        }
        return DRAIN_STATE_CONSUMED;
    }

    private int drainEncoder() {
        if (isEncoderEOS) return DRAIN_STATE_NONE;
        int result = encoder.dequeueOutputBuffer(bufferInfo, 0);
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                if (actualOutputFormat != null) {
                    throw new RuntimeException("Video output format changed twice.");
                }
                actualOutputFormat = encoder.getOutputFormat();
                muxRender.setOutputFormat(MuxRender.SampleType.VIDEO, actualOutputFormat);
                muxRender.onSetOutputFormat();
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                encoderOutputBuffers = encoder.getOutputBuffers();
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }
        if (actualOutputFormat == null) {
            throw new RuntimeException("Could not determine actual output format.");
        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            isEncoderEOS = true;
            bufferInfo.set(0, 0, 0, bufferInfo.flags);
        }
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // SPS or PPS, which should be passed by MediaFormat.
            encoder.releaseOutputBuffer(result, false);
            return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }

        if (useStaticBkg) {
            ByteBuffer encodedData = encoder.getOutputBuffer(result);
            if (encodedData != null) {
                encodedData.position(bufferInfo.offset);
                encodedData.limit(bufferInfo.offset + bufferInfo.size);
                muxRender.writeSampleData(MuxRender.SampleType.VIDEO, encodedData, bufferInfo);
                writtenPresentationTimeUs = bufferInfo.presentationTimeUs;
                encoder.releaseOutputBuffer(result, false);
                //encodedFrameCount++;
            }
        } else {
            muxRender.writeSampleData(MuxRender.SampleType.VIDEO, encoderOutputBuffers[result], bufferInfo);
            writtenPresentationTimeUs = bufferInfo.presentationTimeUs;
            encoder.releaseOutputBuffer(result, false);
        }
        return DRAIN_STATE_CONSUMED;
    }
}
