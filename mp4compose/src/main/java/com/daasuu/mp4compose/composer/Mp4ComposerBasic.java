package com.daasuu.mp4compose.composer;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaMetadataRetriever;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.daasuu.mp4compose.FillMode;
import com.daasuu.mp4compose.FillModeCustomItem;
import com.daasuu.mp4compose.Rotation;
import com.daasuu.mp4compose.VideoFormatMimeType;
import com.daasuu.mp4compose.filter.GlFilter;
import com.daasuu.mp4compose.logger.AndroidLogger;
import com.daasuu.mp4compose.logger.Logger;
import com.daasuu.mp4compose.source.DataSource;
import com.daasuu.mp4compose.source.FilePathDataSource;

import java.io.FileDescriptor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sudamasayuki on 2017/11/15.
 */
@SuppressWarnings("MemberName")
public class Mp4ComposerBasic implements ComposerInterface {
    private static final String TAG = Mp4Composer.class.getSimpleName();

    private final DataSource srcDataSource;
    private final String destPath;
    private FileDescriptor destFileDescriptor;
    private GlFilter filter;
    private Size outputResolution;
    private int bitrate = -1;
    private int iFrameInterval = 1;
    private int audioBitRate = 128000;
    private int aacProfile = MediaCodecInfo.CodecProfileLevel.AACObjectELD;
    private boolean forceAudioEncoding = false;
    private boolean mute = false;
    private Rotation rotation = Rotation.NORMAL;
    private Listener listener;
    private FillMode fillMode = FillMode.PRESERVE_ASPECT_FIT;
    private FillModeCustomItem fillModeCustomItem;
    // TODO: currently we do not use the timeScale feature. Also the timeScale ends up
    // being converted into an int in the VideoComposer layer.
    // See https://github.com/Automattic/stories-android/issues/685 for more context.
    private float timeScale = 1f; // should be in range 0.125 (-8X) to 8.0 (8X)
    private boolean isPitchChanged = false;
    private boolean flipVertical = false;
    private boolean flipHorizontal = false;
    private long trimStartMs = 0;
    private long trimEndMs = -1;
    private VideoFormatMimeType videoFormatMimeType = VideoFormatMimeType.AUTO;

    private ExecutorService executorService;
    private Mp4ComposerEngineBasic engine;

    private Logger logger;

    private DataSource.Listener errorDataSource = new DataSource.Listener() {
        @Override
        public void onError(Exception e) {
            notifyListenerOfFailureAndShutdown(e);
        }
    };

    public Mp4ComposerBasic(@NonNull final String srcPath, @NonNull final String destPath) {
        this(srcPath, destPath, new AndroidLogger());
    }

    public Mp4ComposerBasic(
            @NonNull final String srcPath,
            @NonNull final String destPath,
            @NonNull final Logger logger
    ) {
        this.logger = logger;
        this.srcDataSource = new FilePathDataSource(srcPath, logger, errorDataSource);
        this.destPath = destPath;
    }

    public Mp4ComposerBasic filter(@NonNull GlFilter filter) {
        this.filter = filter;
        return this;
    }

    public Mp4ComposerBasic size(int width, int height) {
        this.outputResolution = new Size(width, height);
        return this;
    }

    @Override
    public Mp4ComposerBasic size(Size size) {
        this.outputResolution = size;
        return this;
    }

    public Mp4ComposerBasic videoBitrate(int bitrate) {
        this.bitrate = bitrate;
        return this;
    }

    public Mp4ComposerBasic iFrameInterval(int iFrameInterval) {
        this.iFrameInterval = iFrameInterval;
        return this;
    }

    public Mp4ComposerBasic audioBitRate(int audioBitRate) {
        this.audioBitRate = audioBitRate;
        return this;
    }

    public Mp4ComposerBasic aacProfile(int aacProfile) {
        this.aacProfile = aacProfile;
        return this;
    }

    public Mp4ComposerBasic forceAudioEncoding(boolean forceAudioEncoding) {
        this.forceAudioEncoding = forceAudioEncoding;
        return this;
    }

    public Mp4ComposerBasic mute(boolean mute) {
        this.mute = mute;
        return this;
    }

    public Mp4ComposerBasic flipVertical(boolean flipVertical) {
        this.flipVertical = flipVertical;
        return this;
    }

    public Mp4ComposerBasic flipHorizontal(boolean flipHorizontal) {
        this.flipHorizontal = flipHorizontal;
        return this;
    }

    public Mp4ComposerBasic rotation(@NonNull Rotation rotation) {
        this.rotation = rotation;
        return this;
    }

    public Mp4ComposerBasic fillMode(@NonNull FillMode fillMode) {
        this.fillMode = fillMode;
        return this;
    }

    public Mp4ComposerBasic customFillMode(@NonNull FillModeCustomItem fillModeCustomItem) {
        this.fillModeCustomItem = fillModeCustomItem;
        this.fillMode = FillMode.CUSTOM;
        return this;
    }

    public Mp4ComposerBasic listener(@NonNull Listener listener) {
        this.listener = listener;
        return this;
    }

    public Mp4ComposerBasic timeScale(final float timeScale) {
        this.timeScale = timeScale;
        return this;
    }

    public Mp4ComposerBasic changePitch(final boolean isPitchChanged) {
        this.isPitchChanged = isPitchChanged;
        return this;
    }

    public Mp4ComposerBasic videoFormatMimeType(@NonNull VideoFormatMimeType videoFormatMimeType) {
        this.videoFormatMimeType = videoFormatMimeType;
        return this;
    }

    /**
     * Set the {@link Logger} that should be used. Defaults to {@link AndroidLogger} if none is set.
     *
     * @param logger The logger that should be used to log.
     * @return The composer instance.
     */
    public Mp4ComposerBasic logger(@NonNull final Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Trim the video to the provided times. By default the video will not be trimmed.
     *
     * @param trimStartMs The start time of the trim in milliseconds.
     * @param trimEndMs   The end time of the trim in milliseconds, -1 for no end.
     * @return The composer instance.
     */
    public Mp4ComposerBasic trim(final long trimStartMs, final long trimEndMs) {
        this.trimStartMs = trimStartMs;
        this.trimEndMs = trimEndMs;
        return this;
    }

    @Nullable
    public Size getSrcVideoResolution() {
        return getVideoResolution(srcDataSource);
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
        return executorService;
    }


    public Mp4ComposerBasic start() {
        // if we're already composing, calling this should do nothing
        if (engine != null) {
            return this;
        }

        getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                if (logger == null) {
                    logger = new AndroidLogger();
                }
                engine = new Mp4ComposerEngineBasic(logger);

                engine.setProgressCallback(new Mp4ComposerEngineBasic.ProgressCallback() {
                    @Override
                    public void onProgress(final double progress) {
                        if (listener != null) {
                            listener.onProgress(progress);
                        }
                    }
                });

                final Integer videoRotate = getVideoRotation(srcDataSource);
                final Size srcVideoResolution = getVideoResolution(srcDataSource);

                if (srcVideoResolution == null || videoRotate == null) {
                    notifyListenerOfFailureAndShutdown(
                            new UnsupportedOperationException("File type unsupported, path: " + srcDataSource)
                    );
                    return;
                }

                if (filter == null) {
                    filter = new GlFilter();
                }

                if (fillMode == null) {
                    fillMode = FillMode.PRESERVE_ASPECT_FIT;
                }
                if (fillMode == FillMode.CUSTOM && fillModeCustomItem == null) {
                    notifyListenerOfFailureAndShutdown(
                            new IllegalAccessException("FillMode.CUSTOM must need fillModeCustomItem.")
                    );
                    return;
                }

                if (fillModeCustomItem != null) {
                    fillMode = FillMode.CUSTOM;
                }

                if (outputResolution == null) {
                    if (fillMode == FillMode.CUSTOM) {
                        outputResolution = srcVideoResolution;
                    } else {
                        Rotation rotate = Rotation.fromInt(rotation.getRotation() + videoRotate);
                        if (rotate == Rotation.ROTATION_90 || rotate == Rotation.ROTATION_270) {
                            outputResolution = new Size(srcVideoResolution.getHeight(), srcVideoResolution.getWidth());
                        } else {
                            outputResolution = srcVideoResolution;
                        }
                    }
                }

                if (timeScale < 0.125f) {
                    timeScale = 0.125f;
                } else if (timeScale > 8f) {
                    timeScale = 8f;
                }

                logger.debug(TAG, "rotation = " + (rotation.getRotation() + videoRotate));
                logger.debug(TAG, "rotation = " + Rotation.fromInt(rotation.getRotation() + videoRotate));
                logger.debug(
                        TAG,
                        "inputResolution width = " + srcVideoResolution.getWidth() + " height = "
                        + srcVideoResolution.getHeight()
                );
                logger.debug(
                        TAG,
                        "outputResolution width = " + outputResolution.getWidth() + " height = "
                        + outputResolution.getHeight()
                );
                logger.debug(TAG, "fillMode = " + fillMode);

                try {
                    if (bitrate < 0) {
                        bitrate = calcBitRate(outputResolution.getWidth(), outputResolution.getHeight());
                    }

                    if (listener != null) {
                        listener.onStart();
                    }

                    engine.compose(
                            srcDataSource,
                            destPath,
                            destFileDescriptor,
                            outputResolution,
                            filter,
                            bitrate,
                            mute,
                            Rotation.fromInt(rotation.getRotation() + videoRotate),
                            srcVideoResolution,
                            fillMode,
                            fillModeCustomItem,
                            timeScale,
                            isPitchChanged,
                            flipVertical,
                            flipHorizontal,
                            trimStartMs,
                            trimEndMs,
                            videoFormatMimeType,
                            iFrameInterval,
                            audioBitRate,
                            aacProfile,
                            forceAudioEncoding
                    );
                } catch (Exception e) {
                    if (e instanceof MediaCodec.CodecException) {
                        logger.error(
                                TAG,
                                "This devicel cannot codec with that setting. Check width, height, "
                                + "bitrate and video format.", e
                        );
                        notifyListenerOfFailureAndShutdown(e);
                        return;
                    }

                    logger.error(TAG, "Unable to compose the engine", e);
                    notifyListenerOfFailureAndShutdown(e);
                    return;
                }

                if (listener != null) {
                    if (engine.isCanceled()) {
                        listener.onCanceled();
                    } else {
                        listener.onCompleted();
                    }
                }
                executorService.shutdown();
                engine = null;
            }
        });

        return this;
    }

    private void notifyListenerOfFailureAndShutdown(final Exception failure) {
        if (listener != null) {
            listener.onFailed(failure);
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    public void cancel() {
        if (engine != null) {
            engine.cancel();
        }
    }

    @Nullable
    private Integer getVideoRotation(DataSource dataSource) {
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(dataSource.getFileDescriptor());
            final String orientation = mediaMetadataRetriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            );
            if (orientation == null) {
                return null;
            }
            return Integer.valueOf(orientation);
        } catch (IllegalArgumentException e) {
            logger.error("MediaMetadataRetriever", "getVideoRotation IllegalArgumentException", e);
            return 0;
        } catch (RuntimeException e) {
            logger.error("MediaMetadataRetriever", "getVideoRotation RuntimeException", e);
            return 0;
        } catch (Exception e) {
            logger.error("MediaMetadataRetriever", "getVideoRotation Exception", e);
            return 0;
        } finally {
            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever.release();
                }
            } catch (RuntimeException e) {
                logger.error(TAG, "Failed to release mediaMetadataRetriever.", e);
            }
        }
    }

    private int calcBitRate(int width, int height) {
        final int bitrate = (int) (0.25 * 30 * width * height);
        logger.debug(TAG, "bitrate=" + bitrate);
        return bitrate;
    }

    /**
     * Extract the resolution of the video at the provided path, or null if the format is
     * unsupported.
     */
    @Nullable
    private Size getVideoResolution(DataSource dataSource) {
        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(dataSource.getFileDescriptor());
            final String rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            final String rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (rawWidth == null || rawHeight == null) {
                return null;
            }
            final int width = Integer.parseInt(rawWidth);
            final int height = Integer.parseInt(rawHeight);

            return new Size(width, height);
        } catch (IllegalArgumentException e) {
            logger.error("MediaMetadataRetriever", "getVideoResolution IllegalArgumentException", e);
            return null;
        } catch (RuntimeException e) {
            logger.error("MediaMetadataRetriever", "getVideoResolution RuntimeException", e);
            return null;
        } catch (Exception e) {
            logger.error("MediaMetadataRetriever", "getVideoResolution Exception", e);
            return null;
        } finally {
            try {
                if (retriever != null) {
                    retriever.release();
                }
            } catch (RuntimeException e) {
                logger.error(TAG, "Failed to release mediaMetadataRetriever.", e);
            }
        }
    }
}
