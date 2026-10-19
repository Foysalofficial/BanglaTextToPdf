package com.banglatext.topdf;

import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;
import com.google.android.material.button.MaterialButton;

@UnstableApi
public class PlayingSelected extends AppCompatActivity {

    private VPPlayerManager playerManager;
    private PlayerView playerView;
    private ActivityResultLauncher<String> pickVideoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.player_view);
        MaterialButton btnPickVideo = findViewById(R.id.btn_pick_video);

        playerManager = new VPPlayerManager(this);
        playerView.setPlayer(playerManager.getPlayer());

        // Register system file picker for selecting video from storage
        pickVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        playerManager.play(uri);
                        Toast.makeText(this, "Playing selected video", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No video selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Pick video button click listener
        btnPickVideo.setOnClickListener(v -> pickVideoLauncher.launch("video/*"));

        // Automatically launch video picker when activity starts
        pickVideoLauncher.launch("video/*");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        playerManager.releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerManager.releasePlayer();
    }
}