package com.daasuu.mp4compose.composer;

import android.graphics.Bitmap;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.util.Log;
import android.util.Size;

import com.daasuu.mp4compose.FillMode;
import com.daasuu.mp4compose.FillModeCustomItem;
import com.daasuu.mp4compose.Rotation;
import com.daasuu.mp4compose.filter.GlFilter;

import java.io.FileInputStream;
import java.io.IOException;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;

// Refer: https://github.com/ypresto/android-transcoder/blob/master/lib/src/main/java/net/ypresto/androidtranscoder/engine/MediaTranscoderEngine.java

/**
 * Internal engine, do not use this directly.
 */
class Mp4ComposerEngine {
    private static final String TAG = "Mp4ComposerEngine";
    private static final double PROGRESS_UNKNOWN = -1.0;
    private static final long SLEEP_TO_WAIT_TRACK_TRANSCODERS = 10;
    private static final long PROGRESS_INTERVAL_STEPS = 10;
    private FileInputStream isSourceInputStream;
    private VideoComposer videoComposer;
    private IAudioComposer audioComposer;
    private MediaExtractor mediaExtractor;
    private MediaMuxer mediaMuxer;
    private ProgressCallback progressCallback;
    private long durationUs;
    private MediaMetadataRetriever mediaMetadataRetriever;

    private boolean useStaticBkg;
    private Bitmap bkgBitmap;

    private static final int BIT_RATE = 2000000;
    private static final int FRAME_RATE = 20;
    private static final int I_FRAME_INTERVAL = 5;

    void setDataSource(FileInputStream sourceIS) throws IOException {
        isSourceInputStream = sourceIS;
        // make this call to make sure a file descriptor can be obtained
        isSourceInputStream.getFD();
    }

    void setProgressCallback(ProgressCallback progressCallback) {
        this.progressCallback = progressCallback;
    }


    void composeFromVideoSource(
            final String destPath,
            final Size outputResolution,
            final GlFilter filter,
            final int bitrate,
            final boolean mute,
            final Rotation rotation,
            final Size inputResolution,
            final FillMode fillMode,
            final FillModeCustomItem fillModeCustomItem,
            final int timeScale,
            final boolean flipVertical,
            final boolean flipHorizontal
                ) throws IOException {
        this.useStaticBkg = false;
        compose(destPath, outputResolution, filter, bitrate, mute, rotation, inputResolution, fillMode,
                fillModeCustomItem, timeScale, flipVertical, flipHorizontal);
    }

    void composeFromStaticImageSource(
            final Bitmap bkgBitmap,
            final String destPath,
            final Size outputResolution,
            final GlFilter filter,
            final int bitrate,
            final boolean mute,
            final Rotation rotation,
            final Size inputResolution,
            final FillMode fillMode,
            final FillModeCustomItem fillModeCustomItem,
            final int timeScale,
            final boolean flipVertical,
            final boolean flipHorizontal
                                     ) throws IOException {
        this.useStaticBkg = true;
        this.bkgBitmap = bkgBitmap;
        compose(destPath, outputResolution, filter, bitrate, mute, rotation, inputResolution, fillMode,
                fillModeCustomItem, timeScale, flipVertical, flipHorizontal);
    }

    private void compose(
            final String destPath,
            final Size outputResolution,
            final GlFilter filter,
            final int bitrate,
            final boolean mute,
            final Rotation rotation,
            final Size inputResolution,
            final FillMode fillMode,
            final FillModeCustomItem fillModeCustomItem,
            final int timeScale,
            final boolean flipVertical,
            final boolean flipHorizontal
    ) throws IOException {
        try {

            mediaMuxer = new MediaMuxer(destPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            MediaFormat videoOutputFormat = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, outputResolution.getWidth(), outputResolution.getHeight());
            MuxRender muxRender = new MuxRender(mediaMuxer);

            if (!useStaticBkg) {
                mediaExtractor = new MediaExtractor();
                mediaExtractor.setDataSource(isSourceInputStream.getFD());
                mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(isSourceInputStream.getFD());
                try {
                    durationUs = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
                } catch (NumberFormatException e) {
                    durationUs = -1;
                }
                Log.d(TAG, "Duration (us): " + durationUs);

                videoOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
                videoOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
                videoOutputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                videoOutputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

                // identify track indices
                MediaFormat format = mediaExtractor.getTrackFormat(0);
                String mime = format.getString(MediaFormat.KEY_MIME);

                final int videoTrackIndex;
                final int audioTrackIndex;

                if (mime.startsWith("video/")) {
                    videoTrackIndex = 0;
                    audioTrackIndex = 1;
                } else {
                    videoTrackIndex = 1;
                    audioTrackIndex = 0;
                }

                // setup video composer
                videoComposer = new VideoComposer(mediaExtractor, videoTrackIndex, videoOutputFormat, muxRender, timeScale);
                videoComposer.setUp(filter, rotation, outputResolution, inputResolution, fillMode, fillModeCustomItem, flipVertical, flipHorizontal);
                mediaExtractor.selectTrack(videoTrackIndex);
                // setup audio if present and not muted
                if (mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null && !mute) {
                    // has Audio video

                    if (timeScale < 2) {
                        audioComposer = new AudioComposer(mediaExtractor, audioTrackIndex, muxRender);
                    } else {
                        audioComposer = new RemixAudioComposer(mediaExtractor, audioTrackIndex, mediaExtractor.getTrackFormat(audioTrackIndex), muxRender, timeScale);
                    }

                    audioComposer.setup();

                    mediaExtractor.selectTrack(audioTrackIndex);

                    runPipelines();
                } else {
                    // no audio video
                    runPipelinesNoAudio();
                }
            } else {
                videoOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
                videoOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
                videoOutputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
                videoOutputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420SemiPlanar);

                // setup video composer for static background image
                videoComposer = new VideoComposer(bkgBitmap, videoOutputFormat, muxRender, timeScale);
                videoComposer.setUp(filter, rotation, outputResolution, inputResolution, fillMode, fillModeCustomItem, flipVertical, flipHorizontal);
                runPipelinesNoAudioForStaticBackground();
            }

            mediaMuxer.stop();
        } finally {
            try {
                if (videoComposer != null) {
                    videoComposer.release();
                    videoComposer = null;
                }
                if (audioComposer != null) {
                    audioComposer.release();
                    audioComposer = null;
                }
                if (mediaExtractor != null) {
                    mediaExtractor.release();
                    mediaExtractor = null;
                }
            } catch (RuntimeException e) {
                // Too fatal to make alive the app, because it may leak native resources.
                //noinspection ThrowFromFinallyBlock
                throw new Error("Could not shutdown mediaExtractor, codecs and mediaMuxer pipeline.", e);
            }
            try {
                if (mediaMuxer != null) {
                    mediaMuxer.release();
                    mediaMuxer = null;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to release mediaMuxer.", e);
            }
            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever.release();
                    mediaMetadataRetriever = null;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to release mediaMetadataRetriever.", e);
            }
        }


    }


    private void runPipelines() {
        long loopCount = 0;
        if (durationUs <= 0) {
            if (progressCallback != null) {
                progressCallback.onProgress(PROGRESS_UNKNOWN);
            }// unknown
        }
        while (!(videoComposer.isFinished() && audioComposer.isFinished())) {
            boolean stepped = videoComposer.stepPipeline()
                    || audioComposer.stepPipeline();
            loopCount++;
            if (durationUs > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0) {
                double videoProgress = videoComposer.isFinished() ? 1.0 : Math.min(1.0, (double) videoComposer.getWrittenPresentationTimeUs() / durationUs);
                double audioProgress = audioComposer.isFinished() ? 1.0 : Math.min(1.0, (double) audioComposer.getWrittenPresentationTimeUs() / durationUs);
                double progress = (videoProgress + audioProgress) / 2.0;
                if (progressCallback != null) {
                    progressCallback.onProgress(progress);
                }
            }
            if (!stepped) {
                try {
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS);
                } catch (InterruptedException e) {
                    // nothing to do
                }
            }
        }
    }

    private void runPipelinesNoAudio() {
        long loopCount = 0;
        if (durationUs <= 0) {
            if (progressCallback != null) {
                progressCallback.onProgress(PROGRESS_UNKNOWN);
            } // unknown
        }
        while (!videoComposer.isFinished()) {
            boolean stepped = videoComposer.stepPipeline();
            loopCount++;
            if (durationUs > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0) {
                double videoProgress = videoComposer.isFinished() ? 1.0 : Math.min(1.0, (double) videoComposer.getWrittenPresentationTimeUs() / durationUs);
                if (progressCallback != null) {
                    progressCallback.onProgress(videoProgress);
                }
            }
            if (!stepped) {
                try {
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS);
                } catch (InterruptedException e) {
                    // nothing to do
                }
            }
        }
    }

    private void runPipelinesNoAudioForStaticBackground() {
        long loopCount = 0;
        if (durationUs <= 0) {
            if (progressCallback != null) {
                progressCallback.onProgress(PROGRESS_UNKNOWN);
            } // unknown
        }
        while (!videoComposer.isFinished()) {
            boolean stepped = videoComposer.stepPipelineStaticImageBackground();
            loopCount++;
            if (durationUs > 0 && loopCount % PROGRESS_INTERVAL_STEPS == 0) {
                double videoProgress = videoComposer.isFinished() ? 1.0 : Math.min(1.0, (double) videoComposer.getWrittenPresentationTimeUs() / durationUs);
                if (progressCallback != null) {
                    progressCallback.onProgress(videoProgress);
                }
            }
            if (!stepped) {
                try {
                    Thread.sleep(SLEEP_TO_WAIT_TRACK_TRANSCODERS);
                } catch (InterruptedException e) {
                    // nothing to do
                }
            }
        }


    }


    interface ProgressCallback {
        /**
         * Called to notify progress. Same thread which initiated transcode is used.
         *
         * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
         */
        void onProgress(double progress);
    }
}
