package com.banglatext.topdf;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.media3.common.Format;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DecoderReuseEvaluation;
import androidx.media3.exoplayer.analytics.AnalyticsListener;

@UnstableApi
public class PlayerDiagnosticsListener implements AnalyticsListener {

    private static final String TAG = "VPPlayerDiagnostics";

    @Override
    public void onVideoDecoderInitialized(@NonNull EventTime eventTime, @NonNull String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        Log.d(TAG, "Video Decoder Initialized: " + decoderName);
        if (decoderName.toLowerCase().contains("google") || decoderName.toLowerCase().contains("android.soft")) {
            Log.w(TAG, "WARNING: Software decoder in use: " + decoderName);
        } else {
            Log.i(TAG, "Hardware decoder in use: " + decoderName);
        }
    }

    @Override
    public void onDroppedVideoFrames(@NonNull EventTime eventTime, int droppedFrames, long elapsedMs) {
        if (droppedFrames > 0) {
            Log.w(TAG, "Dropped Frames: " + droppedFrames + " over " + elapsedMs + "ms");
        }
    }

    @Override
    public void onVideoSizeChanged(@NonNull EventTime eventTime, @NonNull VideoSize videoSize) {
        Log.d(TAG, "Video Resolution: " + videoSize.width + "x" + videoSize.height);
    }

    @Override
    public void onVideoInputFormatChanged(@NonNull EventTime eventTime, @NonNull Format format, DecoderReuseEvaluation decoderReuseEvaluation) {
        Log.d(TAG, "Video Format: " + format.sampleMimeType + ", Bitrate: " + format.bitrate + ", FPS: " + format.frameRate);
    }

    @Override
    public void onPlayerError(@NonNull EventTime eventTime, @NonNull PlaybackException error) {
        Log.e(TAG, "Player Error: " + error.getMessage(), error);
    }
}