package com.banglatext.topdf;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ImportPlaylistActivity extends AppCompatActivity {

    private EditText etSportsCode, etTvCode, etMovieCode;
    private TextView tvSportsDomain, tvTvDomain, tvMovieDomain;
    private MaterialButton btnImport;
    private LinearLayout layoutImportForm, layoutMoreOptions;
    private final String[] domainOptions = {".live", ".com", ".top", ".site", ".fit", ".ink", ".m3u8"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_playlist);

        // Initialize views
        etSportsCode = findViewById(R.id.etSportsCode);
        etTvCode = findViewById(R.id.etTvCode);
        etMovieCode = findViewById(R.id.etMovieCode);

        tvSportsDomain = findViewById(R.id.tvSportsDomain);
        tvTvDomain = findViewById(R.id.tvTvDomain);
        tvMovieDomain = findViewById(R.id.tvMovieDomain);

        TextView tvMoreOptions = findViewById(R.id.tvMoreOptions);
        btnImport = findViewById(R.id.btnImport);

        layoutImportForm = findViewById(R.id.layoutImportForm);
        layoutMoreOptions = findViewById(R.id.layoutMoreOptions);
        TextView tvCancelMoreOptions = findViewById(R.id.tvCancelMoreOptions);

        // Setup domain selector dropdowns dynamically
        tvSportsDomain.setOnClickListener(v -> showDomainPopupMenu(v, tvSportsDomain));
        tvTvDomain.setOnClickListener(v -> showDomainPopupMenu(v, tvTvDomain));
        tvMovieDomain.setOnClickListener(v -> showDomainPopupMenu(v, tvMovieDomain));

        // More options toggle
        tvMoreOptions.setOnClickListener(v -> {
            layoutImportForm.setVisibility(View.GONE);
            layoutMoreOptions.setVisibility(View.VISIBLE);
        });

        tvCancelMoreOptions.setOnClickListener(v -> {
            layoutMoreOptions.setVisibility(View.GONE);
            layoutImportForm.setVisibility(View.VISIBLE);
        });

        // More options sub-section clicks
        findViewById(R.id.optionAddMovie).setOnClickListener(v -> {
            Toast.makeText(this, "Adding Movie Playlist...", Toast.LENGTH_SHORT).show();
            layoutMoreOptions.setVisibility(View.GONE);
            layoutImportForm.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.optionAddTv).setOnClickListener(v -> {
            Toast.makeText(this, "Adding TV Playlist...", Toast.LENGTH_SHORT).show();
            layoutMoreOptions.setVisibility(View.GONE);
            layoutImportForm.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.optionAddSports).setOnClickListener(v -> {
            Toast.makeText(this, "Adding Sports Playlist...", Toast.LENGTH_SHORT).show();
            layoutMoreOptions.setVisibility(View.GONE);
            layoutImportForm.setVisibility(View.VISIBLE);
        });

        // Import button click (Backend URL builder & validation)
        btnImport.setOnClickListener(v -> handleImportPlaylists());

        // Bottom Navigation setup
        setupBottomNavigation();
    }

    private void showDomainPopupMenu(View anchor, TextView domainTextView) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        for (int i = 0; i < domainOptions.length; i++) {
            popupMenu.getMenu().add(0, i, i, domainOptions[i]);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            String chosenDomain = domainOptions[item.getItemId()];
            domainTextView.setText(chosenDomain);
            return true;
        });

        popupMenu.show();
    }

    private void handleImportPlaylists() {
        String sportsCode = etSportsCode.getText() != null ? etSportsCode.getText().toString().trim() : "";
        String tvCode = etTvCode.getText() != null ? etTvCode.getText().toString().trim() : "";
        String movieCode = etMovieCode.getText() != null ? etMovieCode.getText().toString().trim() : "";

        String inputCode = "";
        if (!TextUtils.isEmpty(sportsCode)) {
            inputCode = sportsCode;
        } else if (!TextUtils.isEmpty(tvCode)) {
            inputCode = tvCode;
        } else if (!TextUtils.isEmpty(movieCode)) {
            inputCode = movieCode;
        }

        if (TextUtils.isEmpty(inputCode)) {
            Toast.makeText(this, "Please enter at least one playlist code", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchAndDecodePlaylistUrl(inputCode);
    }

    private void fetchAndDecodePlaylistUrl(String code) {
        Toast.makeText(this, "Fetching URL for code: " + code, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2/url-manager/api.php?code=" + code);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    String status = jsonObject.optString("status");
                    if ("success".equals(status)) {
                        String encodedUrl = jsonObject.optString("encoded_url");
                        byte[] decodedBytes = Base64.decode(encodedUrl, Base64.DEFAULT);
                        String decodedUrl = new String(decodedBytes, StandardCharsets.UTF_8);

                        runOnUiThread(() -> {
                            Toast.makeText(ImportPlaylistActivity.this, "Decoded URL: " + decodedUrl, Toast.LENGTH_LONG).show();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(ImportPlaylistActivity.this, "API returned status: " + status, Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ImportPlaylistActivity.this, "Server error: " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(ImportPlaylistActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navMovie).setOnClickListener(v -> Toast.makeText(this, "Movie tab clicked", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navTv).setOnClickListener(v -> Toast.makeText(this, "TV tab clicked", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navSports).setOnClickListener(v -> Toast.makeText(this, "Sports tab clicked", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navMyList).setOnClickListener(v -> Toast.makeText(this, "My List tab clicked", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navSetting).setOnClickListener(v -> Toast.makeText(this, "Setting tab clicked", Toast.LENGTH_SHORT).show());
    }
}