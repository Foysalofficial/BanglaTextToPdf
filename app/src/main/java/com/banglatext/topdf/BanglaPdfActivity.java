package com.banglatext.topdf;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class BanglaPdfActivity extends AppCompatActivity {
    private static final String TAG = "BanglaPdfActivity";
    private TextInputEditText etHeadline, etBanglaText;
    private MaterialCardView cardResult;
    private TextView tvPdfInfo;

    private File generatedPdfFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bangla_pdf);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        etHeadline = findViewById(R.id.etHeadline);
        etBanglaText = findViewById(R.id.etBanglaText);
        MaterialButton btnGeneratePdf = findViewById(R.id.btnGeneratePdf);
        cardResult = findViewById(R.id.cardResult);
        tvPdfInfo = findViewById(R.id.tvPdfInfo);
        MaterialButton btnViewPdf = findViewById(R.id.btnViewPdf);
        MaterialButton btnSharePdf = findViewById(R.id.btnSharePdf);

        btnGeneratePdf.setOnClickListener(v -> {
            String headline = etHeadline.getText() != null ? etHeadline.getText().toString().trim() : "";
            String bodyText = etBanglaText.getText() != null ? etBanglaText.getText().toString().trim() : "";

            if (TextUtils.isEmpty(bodyText)) {
                Toast.makeText(BanglaPdfActivity.this, "অনুগ্রহ করে মূল বাংলা টেক্সট লিখুন", Toast.LENGTH_SHORT).show();
                return;
            }
            createBanglaPdf(headline, bodyText);
        });

        btnViewPdf.setOnClickListener(v -> viewGeneratedPdf());

        btnSharePdf.setOnClickListener(v -> shareGeneratedPdf());
    }

    /**
     * Converts input Bangla headline and body text to a multi-page PDF document.
     * The headline appears ONLY on Page 1.
     * Uses StaticLayout to ensure Bangla glyphs/conjuncts (যুক্তবর্ণ) are perfectly preserved.
     */
    private void createBanglaPdf(String headline, String bodyText) {
        // A4 page dimensions in points (1 point = 1/72 inch)
        final int pageWidth = 595;  // A4 Width
        final int pageHeight = 842; // A4 Height

        final int marginLeft = 40;
        final int marginRight = 40;
        final int marginTop = 45;
        final int marginBottom = 45;
        final int footerHeight = 30;

        final int contentWidth = pageWidth - marginLeft - marginRight;

        // 1. Build StaticLayout for Headline (if provided) - ONLY drawn on Page 1
        StaticLayout headlineLayout = null;
        int headlineHeight = 0;

        if (!TextUtils.isEmpty(headline)) {
            TextPaint headlinePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            headlinePaint.setTextSize(20f);
            headlinePaint.setColor(Color.parseColor("#1B5E20")); // Dark green
            headlinePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                headlineLayout = StaticLayout.Builder.obtain(headline, 0, headline.length(), headlinePaint, contentWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(2f, 1.1f)
                        .setIncludePad(false)
                        .build();
            } else {
                @SuppressWarnings("deprecation")
                StaticLayout legacyHeadline = new StaticLayout(headline, headlinePaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.1f, 2f, false);
                headlineLayout = legacyHeadline;
            }
            // Height of headline layout plus spacing and line divider
            headlineHeight = headlineLayout.getHeight() + 20;
        }

        // 2. Build StaticLayout for Body Text
        TextPaint bodyTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        bodyTextPaint.setTextSize(14f);
        bodyTextPaint.setColor(Color.parseColor("#111111"));
        bodyTextPaint.setTypeface(Typeface.DEFAULT);

        StaticLayout bodyLayout;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bodyLayout = StaticLayout.Builder.obtain(bodyText, 0, bodyText.length(), bodyTextPaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(4f, 1.15f)
                    .setIncludePad(false)
                    .build();
        } else {
            @SuppressWarnings("deprecation")
            StaticLayout legacyBody = new StaticLayout(bodyText, bodyTextPaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.15f, 4f, false);
            bodyLayout = legacyBody;
        }

        int totalLines = bodyLayout.getLineCount();
        if (totalLines == 0) {
            Toast.makeText(this, "টেক্সট রূপান্তর করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate available printable height for Page 1 vs Page 2+
        final int availableHeightPage1 = pageHeight - marginTop - marginBottom - headlineHeight - footerHeight;
        final int availableHeightPage2Plus = pageHeight - marginTop - marginBottom - footerHeight;

        // Calculate line distribution per page based on variable printable heights
        List<int[]> pageLineRanges = new ArrayList<>();
        int currentStartLine = 0;
        int pageIndex = 0;

        while (currentStartLine < totalLines) {
            int currentEndLine = currentStartLine;
            float startTop = bodyLayout.getLineTop(currentStartLine);
            float availableHeightForThisPage = (pageIndex == 0) ? availableHeightPage1 : availableHeightPage2Plus;

            while (currentEndLine < totalLines) {
                float currentBottom = bodyLayout.getLineBottom(currentEndLine);
                float pageHeightUsed = currentBottom - startTop;

                if (pageHeightUsed > availableHeightForThisPage) {
                    break;
                }
                currentEndLine++;
            }

            if (currentEndLine == currentStartLine) {
                currentEndLine = currentStartLine + 1; // Ensure at least 1 line per page
            }

            pageLineRanges.add(new int[]{currentStartLine, currentEndLine - 1});
            currentStartLine = currentEndLine;
            pageIndex++;
        }

        int totalPages = pageLineRanges.size();
        PdfDocument pdfDocument = new PdfDocument();

        // Render each calculated page
        for (int pIdx = 0; pIdx < totalPages; pIdx++) {
            int[] range = pageLineRanges.get(pIdx);
            int startLine = range[0];
            int endLine = range[1];

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pIdx + 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            float bodyStartY;

            // PAGE 1 ONLY: Draw Headline at the top
            if (pIdx == 0 && headlineLayout != null) {
                canvas.save();
                canvas.translate(marginLeft, marginTop);
                headlineLayout.draw(canvas);
                canvas.restore();

                // Draw horizontal line below headline
                Paint linePaint = new Paint();
                linePaint.setColor(Color.parseColor("#CCCCCC"));
                linePaint.setStrokeWidth(1f);
                float lineY = marginTop + headlineLayout.getHeight() + 8;
                canvas.drawLine(marginLeft, lineY, pageWidth - marginRight, lineY, linePaint);

                bodyStartY = marginTop + headlineHeight;
            } else {
                // PAGE 2+: NO Headline at top
                bodyStartY = marginTop;
            }

            // Draw Body Text lines for current page
            float lineTopForStart = bodyLayout.getLineTop(startLine);
            float lineBottomForEnd = bodyLayout.getLineBottom(endLine);

            canvas.save();
            canvas.translate(marginLeft, bodyStartY - lineTopForStart);
            canvas.clipRect(0, lineTopForStart, contentWidth, lineBottomForEnd + 2);
            bodyLayout.draw(canvas);
            canvas.restore();

            // Draw Footer at bottom of every page
            float footerY = pageHeight - marginBottom;
            Paint linePaint = new Paint();
            linePaint.setColor(Color.parseColor("#CCCCCC"));
            linePaint.setStrokeWidth(1f);
            canvas.drawLine(marginLeft, footerY - 12, pageWidth - marginRight, footerY - 12, linePaint);

            Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            footerPaint.setTextSize(10f);
            footerPaint.setColor(Color.parseColor("#666666"));

            String pageText = "পৃষ্ঠা " + toBanglaNumerals(pIdx + 1) + " / " + toBanglaNumerals(totalPages);
            float pageTextWidth = footerPaint.measureText(pageText);
            canvas.drawText(pageText, pageWidth - marginRight - pageTextWidth, footerY, footerPaint);

            pdfDocument.finishPage(page);
        }

        // Save PDF to Downloads Directory and Cache
        String fileName = "Bangla_Document_" + System.currentTimeMillis() + ".pdf";
        savePdfToDownloadsAndCache(pdfDocument, fileName, totalPages);

        pdfDocument.close();
    }

    /**
     * Saves the PDF to public Downloads directory (so it appears in Downloads manager/files)
     * and also keeps a copy in local cache for immediate FileProvider viewing/sharing.
     */
    private void savePdfToDownloadsAndCache(PdfDocument pdfDocument, String fileName, int totalPages) {
        boolean savedToDownloads = false;

        // 1. Save to Public Downloads Directory
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        if (os != null) {
                            pdfDocument.writeTo(os);
                            savedToDownloads = true;
                        }
                    }
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    if (!downloadsDir.mkdirs()) {
                        Log.w(TAG, "Could not create Downloads directory");
                    }
                }
                File pubFile = new File(downloadsDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(pubFile)) {
                    pdfDocument.writeTo(fos);
                    savedToDownloads = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving PDF to public Downloads directory", e);
        }

        // 2. Save a copy to Cache for internal FileProvider viewing/sharing
        try {
            File pdfDir = new File(getCacheDir(), "pdfs");
            if (!pdfDir.exists()) {
                if (!pdfDir.mkdirs()) {
                    Log.w(TAG, "Could not create cache pdfs directory");
                }
            }
            File cacheFile = new File(pdfDir, "bangla_document.pdf");
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                pdfDocument.writeTo(fos);
            }

            generatedPdfFile = cacheFile;

            long fileSizeBytes = cacheFile.length();
            long fileSizeKb = fileSizeBytes / 1024;

            String info = "মোট পৃষ্ঠা: " + toBanglaNumerals(totalPages) + " টি | ফাইল সাইজ: " + toBanglaNumerals((int) fileSizeKb) + " KB\nস্থান: Download ফোল্ডার (" + fileName + ")";
            tvPdfInfo.setText(info);
            cardResult.setVisibility(View.VISIBLE);

            if (savedToDownloads) {
                Toast.makeText(this, "পিডিএফ ফাইলটি Download ফোল্ডারে সেভ হয়েছে!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "পিডিএফ সফলভাবে তৈরি হয়েছে!", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            Log.e(TAG, "Error saving PDF to cache", e);
            Toast.makeText(this, "পিডিএফ ফাইল সেভ করতে ব্যর্থ হয়েছে: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void viewGeneratedPdf() {
        if (generatedPdfFile == null || !generatedPdfFile.exists()) {
            Toast.makeText(this, "পিডিএফ ফাইলটি পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    generatedPdfFile
            );

            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(contentUri, "application/pdf");
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(viewIntent, "পিডিএফ ওপেন করুন"));
        } catch (Exception e) {
            Log.e(TAG, "Error opening PDF", e);
            Toast.makeText(this, "পিডিএফ ওপেন করার মতো কোনো অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareGeneratedPdf() {
        if (generatedPdfFile == null || !generatedPdfFile.exists()) {
            Toast.makeText(this, "পিডিএফ ফাইলটি পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    generatedPdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "পিডিএফ শেয়ার করুন"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing PDF", e);
            Toast.makeText(this, "শেয়ার করতে ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Converts numbers to Bengali numerals (০, ১, ২, ৩, ৪, ৫, ৬, ৭, ৮, ৯).
     */
    private String toBanglaNumerals(int number) {
        String eng = String.valueOf(number);
        StringBuilder bangla = new StringBuilder();
        for (char c : eng.toCharArray()) {
            if (c >= '0' && c <= '9') {
                bangla.append((char) ('০' + (c - '0')));
            } else {
                bangla.append(c);
            }
        }
        return bangla.toString();
    }
}