package com.example.offlinemobileconverter;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private Button selectFileButton;
    private Button convertButton;
    private Spinner formatSpinner;
    private ProgressBar progressBar;
    private TextView progressPercentage;

    private String currentSelectedFilePath = null;
    private String currentOriginalFileName = null;
    private String currentFullOriginalFileName = null;
    private static final int FILE_PICKER_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        selectFileButton = findViewById(R.id.selectFileButton);
        convertButton = findViewById(R.id.convertButton);
        formatSpinner = findViewById(R.id.formatSpinner);
        progressBar = findViewById(R.id.progressBar);
        progressPercentage = findViewById(R.id.progressPercentage);

        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        selectFileButton.setOnClickListener(v -> {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                statusText.setText("Hata: Önce dosya izni vermelisiniz!");
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;
            }
            openFilePicker();
        });

        convertButton.setOnClickListener(v -> {
            if (currentSelectedFilePath != null && formatSpinner.getSelectedItem() != null) {
                String targetFormat = formatSpinner.getSelectedItem().toString();
                startConversion(currentSelectedFilePath, targetFormat);
            } else {
                Toast.makeText(this, "Lütfen bir format seçin!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimetypes = {"video/*", "audio/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Bir Medya Dosyası Seçin"), FILE_PICKER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();

            String originalFileName = getFileNameFromUri(selectedUri);
            currentFullOriginalFileName = originalFileName;

            String extension = "mp4";
            currentOriginalFileName = "Medya";

            if (originalFileName != null && originalFileName.contains(".")) {
                int dotIndex = originalFileName.lastIndexOf(".");
                extension = originalFileName.substring(dotIndex + 1).toLowerCase();
                currentOriginalFileName = originalFileName.substring(0, dotIndex);
            } else if (originalFileName != null) {
                currentOriginalFileName = originalFileName;
            }

            currentOriginalFileName = currentOriginalFileName.replaceAll("\\s+", "_");
            String realPath = getPathFromUri(selectedUri, extension);

            if (realPath != null) {
                currentSelectedFilePath = realPath;

                statusText.setText("📂 Seçili Dosya:\n" + currentFullOriginalFileName);
                selectFileButton.setText("Başka Bir Dosya Seç");

                setupFormatSpinner(extension);

                formatSpinner.setVisibility(View.VISIBLE);
                convertButton.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                progressPercentage.setVisibility(View.GONE);

            } else {
                statusText.setText("Hata: Dosya işlenemedi!");
            }
        }
    }

    private void setupFormatSpinner(String currentExtension) {
        List<String> availableFormats = new ArrayList<>();

        switch (currentExtension) {
            case "mp4":
            case "mkv":
            case "avi":
            case "mov":
                availableFormats.add("mp3");
                availableFormats.add("wav");
                availableFormats.add("aac");
                availableFormats.add("avi");
                availableFormats.add("mkv");
                break;
            case "mp3":
            case "wav":
            case "aac":
            case "flac":
            case "m4a":
                availableFormats.add("mp3");
                availableFormats.add("wav");
                availableFormats.add("aac");
                availableFormats.add("flac");
                break;
            default:
                availableFormats.add("mp3");
                availableFormats.add("mp4");
                availableFormats.add("wav");
        }

        availableFormats.remove(currentExtension);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableFormats);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        formatSpinner.setAdapter(adapter);
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private String getPathFromUri(Uri uri, String extension) {
        String filePath = null;
        try {
            File tempFile = File.createTempFile("temp_media", "." + extension, getCacheDir());
            tempFile.deleteOnExit();

            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            filePath = tempFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e("FILE_PICKER", "Dosya kopyalanırken hata oluştu: " + e.getMessage());
        }
        return filePath;
    }

    private void startConversion(String inputPath, String targetFormat) {
        final int totalDuration = getVideoDuration(inputPath) / 1000;

        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            progressPercentage.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            statusText.setText("Durum: " + targetFormat.toUpperCase() + " formatına dönüştürülüyor...");

            convertButton.setEnabled(false);
            selectFileButton.setEnabled(false);
            formatSpinner.setEnabled(false);
        });

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        String outFileName = currentOriginalFileName + "_converted." + targetFormat;

        String outputPath = new File(downloadDir, outFileName).getAbsolutePath();

        String command = "-y -i \"" + inputPath + "\" \"" + outputPath + "\"";

        FFmpegKit.executeAsync(command, session -> {
            if (ReturnCode.isSuccess(session.getReturnCode())) {
                runOnUiThread(() -> {
                    statusText.setText("Başarılı! ✅\nKaydedildi: Download/" + outFileName);
                    progressBar.setProgress(100);
                    progressPercentage.setText("İşlem Tamamlandı! 🚀");
                    unlockUI();

                    new android.os.Handler().postDelayed(() -> {
                        progressBar.setVisibility(View.GONE);
                        progressPercentage.setVisibility(View.GONE);

                        if (currentFullOriginalFileName != null) {
                            statusText.setText("📂 Seçili Dosya:\n" + currentFullOriginalFileName);
                        }
                    }, 3000);
                });
            } else {
                runOnUiThread(() -> {
                    statusText.setText("Hata! Dönüştürülemedi.");
                    progressPercentage.setText("Başarısız ❌");
                    Log.e("FFMPEG", session.getFailStackTrace());
                    unlockUI();

                    new android.os.Handler().postDelayed(() -> {
                        progressBar.setVisibility(View.GONE);
                        progressPercentage.setVisibility(View.GONE);
                        if (currentFullOriginalFileName != null) {
                            statusText.setText("📂 Seçili Dosya:\n" + currentFullOriginalFileName);
                        }
                    }, 3000);
                });
            }
        }, log -> {
        }, statistics -> {
            float timeInMilliseconds = statistics.getTime();
            int currentSecond = (int) timeInMilliseconds / 1000;

            if (totalDuration > 0) {
                int progress = (currentSecond * 100) / totalDuration;

                if (progress > 99) progress = 99;

                final int finalProgress = progress;
                runOnUiThread(() -> {
                    progressBar.setProgress(finalProgress);
                    progressPercentage.setText("%" + finalProgress + " Dönüştürülüyor...");
                });
            }
        });
    }

    private void unlockUI() {
        convertButton.setEnabled(true);
        selectFileButton.setEnabled(true);
        formatSpinner.setEnabled(true);
    }

    private int getVideoDuration(String path) {
        try {
            android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
            retriever.setDataSource(path);
            String time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();
            return (time != null) ? Integer.parseInt(time) : 0;
        } catch (Exception e) {
            Log.e("DURATION", "Süre alınamadı: " + e.getMessage());
            return 0;
        }
    }
}