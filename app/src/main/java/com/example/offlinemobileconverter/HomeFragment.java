package com.example.offlinemobileconverter;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView statusText;
    private Button selectFileButton;
    private Button convertButton;
    private ProgressBar progressBar;
    private TextView progressPercentage;

    private TextInputLayout formatInputLayout;
    private android.widget.AutoCompleteTextView formatAutoComplete;

    private TextInputLayout resolutionInputLayout;
    private android.widget.AutoCompleteTextView resolutionAutoComplete;
    private TextInputLayout qualityInputLayout;
    private android.widget.AutoCompleteTextView qualityAutoComplete;

    private LinearLayout trimLayout;
    private TextInputEditText startTimeInput;
    private TextInputEditText endTimeInput;

    private String currentSelectedFilePath = null;
    private String currentOriginalFileName = null;
    private String currentFullOriginalFileName = null;
    private static final int FILE_PICKER_REQUEST_CODE = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        statusText = view.findViewById(R.id.statusText);
        selectFileButton = view.findViewById(R.id.selectFileButton);
        convertButton = view.findViewById(R.id.convertButton);
        progressBar = view.findViewById(R.id.progressBar);
        progressPercentage = view.findViewById(R.id.progressPercentage);

        formatInputLayout = view.findViewById(R.id.formatInputLayout);
        formatAutoComplete = view.findViewById(R.id.formatAutoComplete);

        resolutionInputLayout = view.findViewById(R.id.resolutionInputLayout);
        resolutionAutoComplete = view.findViewById(R.id.resolutionAutoComplete);
        qualityInputLayout = view.findViewById(R.id.qualityInputLayout);
        qualityAutoComplete = view.findViewById(R.id.qualityAutoComplete);

        trimLayout = view.findViewById(R.id.trimLayout);
        startTimeInput = view.findViewById(R.id.startTimeInput);
        endTimeInput = view.findViewById(R.id.endTimeInput);

        selectFileButton.setOnClickListener(v -> openFilePicker());

        convertButton.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
                    return;
                }
            }

            if (currentSelectedFilePath != null && !formatAutoComplete.getText().toString().isEmpty()) {
                String targetFormat = formatAutoComplete.getText().toString();
                startConversion(currentSelectedFilePath, targetFormat);
            } else {
                Toast.makeText(requireContext(), "Lütfen bir format seçin!", Toast.LENGTH_SHORT).show();
            }
        });

        formatAutoComplete.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedFormat = parent.getItemAtPosition(position).toString();
            checkAndToggleAdvancedOptions(selectedFormat);
        });

        return view;
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
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
                setupAdvancedSpinners();

                formatInputLayout.setVisibility(View.VISIBLE);
                trimLayout.setVisibility(View.VISIBLE);
                convertButton.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                progressPercentage.setVisibility(View.GONE);

                if (!formatAutoComplete.getText().toString().isEmpty()) {
                    checkAndToggleAdvancedOptions(formatAutoComplete.getText().toString());
                }

                startTimeInput.setText("");
                endTimeInput.setText("");
            } else {
                statusText.setText("Hata: Dosya işlenemedi!");
            }
        }
    }

    private void setupFormatSpinner(String currentExtension) {
        List<String> availableFormats = new ArrayList<>();
        switch (currentExtension) {
            case "mp4": case "mkv": case "avi": case "mov":
                availableFormats.add("mp4"); availableFormats.add("mkv"); availableFormats.add("avi");
                availableFormats.add("mp3"); availableFormats.add("wav"); availableFormats.add("aac");
                break;
            case "mp3": case "wav": case "aac": case "flac": case "m4a":
                availableFormats.add("mp3"); availableFormats.add("wav");
                availableFormats.add("aac"); availableFormats.add("flac");
                break;
            default:
                availableFormats.add("mp4"); availableFormats.add("mp3"); availableFormats.add("wav");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, availableFormats);
        formatAutoComplete.setAdapter(adapter);

        if (!availableFormats.isEmpty()) {
            String defaultSelection = availableFormats.contains(currentExtension) ? currentExtension : availableFormats.get(0);
            formatAutoComplete.setText(defaultSelection, false);
        }
    }

    private void setupAdvancedSpinners() {
        String[] resolutions = {"Orijinal", "1080p", "720p", "480p"};
        ArrayAdapter<String> resAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, resolutions);
        resolutionAutoComplete.setAdapter(resAdapter);
        resolutionAutoComplete.setText("Orijinal", false);

        String[] qualities = {"Yüksek Kalite", "Orta Kalite (Önerilen)", "Düşük Kalite (Küçük Boyut)"};
        ArrayAdapter<String> qualAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, qualities);
        qualityAutoComplete.setAdapter(qualAdapter);
        qualityAutoComplete.setText("Orta Kalite (Önerilen)", false);
    }

    private void checkAndToggleAdvancedOptions(String targetFormat) {
        boolean isVideo = targetFormat.equals("mp4") || targetFormat.equals("mkv") || targetFormat.equals("avi") || targetFormat.equals("mov");
        if (isVideo) {
            resolutionInputLayout.setVisibility(View.VISIBLE);
            qualityInputLayout.setVisibility(View.VISIBLE);
        } else {
            resolutionInputLayout.setVisibility(View.GONE);
            qualityInputLayout.setVisibility(View.GONE);
        }
    }

    private int parseTimeToSeconds(String timeStr) {
        try {
            if (timeStr == null || timeStr.trim().isEmpty()) return 0;
            if (timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                int seconds = 0;
                int multiplier = 1;
                for (int i = parts.length - 1; i >= 0; i--) {
                    seconds += Integer.parseInt(parts[i]) * multiplier;
                    multiplier *= 60;
                }
                return seconds;
            } else {
                return Integer.parseInt(timeStr);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private void startConversion(String inputPath, String targetFormat) {
        final int originalDuration = getVideoDuration(inputPath) / 1000;
        final java.util.concurrent.atomic.AtomicBoolean isFinished = new java.util.concurrent.atomic.AtomicBoolean(false);

        String startTimeStr = startTimeInput.getText() != null ? startTimeInput.getText().toString().trim() : "";
        String endTimeStr = endTimeInput.getText() != null ? endTimeInput.getText().toString().trim() : "";

        int expectedDuration = originalDuration;
        int startSec = parseTimeToSeconds(startTimeStr);
        int endSec = parseTimeToSeconds(endTimeStr);

        if (startSec > 0) expectedDuration -= startSec;
        if (endSec > 0 && endSec > startSec) expectedDuration = endSec - startSec;
        if (expectedDuration <= 0) expectedDuration = 1; // Sıfıra bölme hatasını engelle

        final int finalExpectedDuration = expectedDuration;

        requireActivity().runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            progressPercentage.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            statusText.setText("Durum: İşleniyor...");

            convertButton.setEnabled(false); selectFileButton.setEnabled(false);
            formatInputLayout.setEnabled(false); resolutionInputLayout.setEnabled(false); qualityInputLayout.setEnabled(false);
            startTimeInput.setEnabled(false); endTimeInput.setEnabled(false);
        });

        String uniqueId = String.valueOf(System.currentTimeMillis() % 10000);
        String outFileName = currentOriginalFileName + "_converted_" + uniqueId + "." + targetFormat;
        String outputPath = new File(requireContext().getCacheDir(), outFileName).getAbsolutePath();

        StringBuilder commandBuilder = new StringBuilder();
        commandBuilder.append("-y -i \"").append(inputPath).append("\" ");

        if (!startTimeStr.isEmpty()) {
            commandBuilder.append("-ss ").append(startTimeStr).append(" ");
        }
        if (!endTimeStr.isEmpty()) {
            commandBuilder.append("-to ").append(endTimeStr).append(" ");
        }

        boolean isTargetVideo = targetFormat.equals("mp4") || targetFormat.equals("mkv") || targetFormat.equals("avi") || targetFormat.equals("mov");

        if (isTargetVideo) {
            String resolution = resolutionAutoComplete.getText().toString();
            String quality = qualityAutoComplete.getText().toString();

            if (resolution.equals("1080p")) {
                commandBuilder.append("-vf scale=-2:1080 ");
            } else if (resolution.equals("720p")) {
                commandBuilder.append("-vf scale=-2:720 ");
            } else if (resolution.equals("480p")) {
                commandBuilder.append("-vf scale=-2:480 ");
            }

            if (quality.contains("Yüksek")) {
                commandBuilder.append("-crf 18 ");
            } else if (quality.contains("Düşük")) {
                commandBuilder.append("-crf 32 ");
            } else {
                commandBuilder.append("-crf 26 ");
            }
        }

        commandBuilder.append("\"").append(outputPath).append("\"");
        String finalCommand = commandBuilder.toString();
        Log.d("FFMPEG_CMD", "Çalıştırılan Komut: " + finalCommand);

        FFmpegKit.executeAsync(finalCommand, session -> {
            isFinished.set(true);
            if (ReturnCode.isSuccess(session.getReturnCode())) {
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        boolean isSaved = saveToDownloads(outputPath, outFileName);

                        if (isSaved) {
                            statusText.setText("Başarılı! ✅\nKaydedildi: Download/" + outFileName);
                            progressBar.setProgress(100);
                            progressPercentage.setText("İşlem Tamamlandı! 🚀");
                        } else {
                            statusText.setText("Hata: Dosya kopyalanamadı!");
                            progressPercentage.setText("Başarısız ❌");
                        }

                        unlockUI();

                        new android.os.Handler().postDelayed(() -> {
                            progressBar.setVisibility(View.GONE); progressPercentage.setVisibility(View.GONE);
                            if (currentFullOriginalFileName != null) {
                                statusText.setText("📂 Seçili Dosya:\n" + currentFullOriginalFileName);
                            }
                        }, 3000);
                    });
                }
            } else {
                if (getActivity() != null) {
                    String errorMessage = session.getFailStackTrace();
                    if (errorMessage == null) errorMessage = session.getLogsAsString();
                    if (errorMessage == null) errorMessage = "Bilinmeyen FFmpeg hatası.";
                    final String safeErrorMessage = errorMessage;

                    requireActivity().runOnUiThread(() -> {
                        statusText.setText("Hata! Dönüştürülemedi.");
                        progressPercentage.setText("Başarısız ❌");
                        Log.e("FFMPEG", "Detay: " + safeErrorMessage);
                        unlockUI();

                        new android.os.Handler().postDelayed(() -> {
                            progressBar.setVisibility(View.GONE); progressPercentage.setVisibility(View.GONE);
                            if (currentFullOriginalFileName != null) {
                                statusText.setText("📂 Seçili Dosya:\n" + currentFullOriginalFileName);
                            }
                        }, 3000);
                    });
                }
            }
        }, log -> {}, statistics -> {
            if (isFinished.get()) return;
            float timeInMilliseconds = statistics.getTime();
            int currentSecond = (int) timeInMilliseconds / 1000;

            if (finalExpectedDuration > 0 && getActivity() != null) {
                int progress = (currentSecond * 100) / finalExpectedDuration;
                if (progress > 99) progress = 99;
                final int finalProgress = progress;
                requireActivity().runOnUiThread(() -> {
                    if (!isFinished.get()) {
                        progressBar.setProgress(finalProgress);
                        progressPercentage.setText("%" + finalProgress + " İşleniyor...");
                    }
                });
            }
        });
    }

    private boolean saveToDownloads(String sourceFilePath, String fileName) {
        try {
            File sourceFile = new File(sourceFilePath);
            if (!sourceFile.exists()) return false;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = requireContext().getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (java.io.OutputStream out = requireContext().getContentResolver().openOutputStream(uri);
                         java.io.FileInputStream in = new java.io.FileInputStream(sourceFile)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                    sourceFile.delete();
                    return true;
                }
            } else {
                File destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File destFile = new File(destDir, fileName);
                try (java.io.FileOutputStream out = new java.io.FileOutputStream(destFile);
                     java.io.FileInputStream in = new java.io.FileInputStream(sourceFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                sourceFile.delete();
                android.media.MediaScannerConnection.scanFile(requireContext(), new String[]{destFile.getAbsolutePath()}, null, null);
                return true;
            }
        } catch (Exception e) {
            Log.e("SAVE_FILE", "Dosya kaydedilemedi: " + e.getMessage());
        }
        return false;
    }

    private void unlockUI() {
        convertButton.setEnabled(true); selectFileButton.setEnabled(true);
        formatInputLayout.setEnabled(true); resolutionInputLayout.setEnabled(true); qualityInputLayout.setEnabled(true);
        startTimeInput.setEnabled(true); endTimeInput.setEnabled(true);
    }

    private int getVideoDuration(String path) {
        try {
            android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
            retriever.setDataSource(path);
            String time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            retriever.release();
            return (time != null) ? Integer.parseInt(time) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireActivity().getContentResolver().query(uri, null, null, null, null)) {
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
            File tempFile = File.createTempFile("temp_media", "." + extension, requireContext().getCacheDir());
            tempFile.deleteOnExit();

            java.io.InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush(); outputStream.close();
            if (inputStream != null) inputStream.close();

            filePath = tempFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("FILE_PICKER", "Dosya kopyalanırken hata: " + e.getMessage());
        }
        return filePath;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                convertButton.performClick();
            } else {
                Toast.makeText(requireContext(), "Dosyayı kaydetmek için bu izin şarttır!", Toast.LENGTH_LONG).show();
            }
        }
    }
}