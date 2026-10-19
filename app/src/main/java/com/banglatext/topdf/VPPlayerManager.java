package com.banglatext.topdf;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UnstableApi
public class VPPlayerManager {

    private ExoPlayer player;
    private final Context context;

    public VPPlayerManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public ExoPlayer getPlayer() {
        if (player == null) {
            initializePlayer();
        }
        return player;
    }

    private void initializePlayer() {
        // Custom MediaCodecSelector to prioritize hardware decoders
        MediaCodecSelector hardwarePrioritySelector = (mimeType, requiresSecureDecoder, requiresTunnelingDecoder) -> {
            List<MediaCodecInfo> decoderInfos = MediaCodecSelector.DEFAULT.getDecoderInfos(
                    mimeType, requiresSecureDecoder, requiresTunnelingDecoder);
            
            if (decoderInfos.isEmpty()) {
                return Collections.emptyList();
            }

            List<MediaCodecInfo> hardwareDecoders = new ArrayList<>();
            List<MediaCodecInfo> softwareDecoders = new ArrayList<>();

            for (MediaCodecInfo info : decoderInfos) {
                if (info.hardwareAccelerated) {
                    hardwareDecoders.add(info);
                } else {
                    softwareDecoders.add(info);
                }
            }

            // Return hardware decoders first, then software as fallback
            List<MediaCodecInfo> result = new ArrayList<>(hardwareDecoders);
            result.addAll(softwareDecoders);
            return result;
        };

        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setMediaCodecSelector(hardwarePrioritySelector);
        
        // Note: Default MediaCodecSelector already tries to find the best decoder.
        // For strict hardware priority, one could implement a custom selector, 
        // but typically Android prioritizes HW decoders by default unless they are blacklisted.

        // 2. Buffering optimization
        // Increase buffer to prevent stuttering on high-bitrate files.
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        50_000,  // Min buffer
                        100_000, // Max buffer
                        2_500,   // Buffer for playback
                        5_000    // Buffer for rebuffer
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        player = new ExoPlayer.Builder(context, renderersFactory)
                .setLoadControl(loadControl)
                .setHandleAudioBecomingNoisy(true)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build();

        player.addAnalyticsListener(new PlayerDiagnosticsListener());
    }

    public void play(Uri uri) {
        if (uri == null) return;
        logCodecCapabilities();
        MediaItem mediaItem = MediaItem.fromUri(uri);
        getPlayer().setMediaItem(mediaItem);
        getPlayer().prepare();
        getPlayer().play();
    }

    public void play(String videoUrl) {
        if (videoUrl != null) {
            play(Uri.parse(videoUrl));
        }
    }

    private void logCodecCapabilities() {
        try {
            boolean hevcSupport = !MediaCodecUtil.getDecoderInfos(MimeTypes.VIDEO_H265, false, false).isEmpty();
            boolean av1Support = !MediaCodecUtil.getDecoderInfos(MimeTypes.VIDEO_AV1, false, false).isEmpty();
            Log.i("VPPlayerManager", "HEVC Support: " + hevcSupport);
            Log.i("VPPlayerManager", "AV1 Support: " + av1Support);
        } catch (MediaCodecUtil.DecoderQueryException e) {
            Log.e("VPPlayerManager", "Error querying codecs", e);
        }
    }

    public void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}