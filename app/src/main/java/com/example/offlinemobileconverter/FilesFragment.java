package com.example.offlinemobileconverter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilesFragment extends Fragment {

    private RecyclerView filesRecyclerView;
    private TextView emptyViewText;
    private android.widget.AutoCompleteTextView sortAutoComplete;
    private FilesAdapter adapter;
    private List<File> convertedFilesList = new ArrayList<>();
    private File[] allFilesRaw;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);

        filesRecyclerView = view.findViewById(R.id.filesRecyclerView);
        emptyViewText = view.findViewById(R.id.emptyViewText);
        sortAutoComplete = view.findViewById(R.id.sortAutoComplete);

        filesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new FilesAdapter(convertedFilesList, new FilesAdapter.OnFileClickListener() {
            @Override
            public void onPlayClick(File file) { openMediaFile(file); }
            @Override
            public void onFolderClick() { openDownloadsFolder(); }
        });
        filesRecyclerView.setAdapter(adapter);

        setupSortingSpinner();
        loadConvertedFilesRaw();
        sortFiles(0);

        return view;
    }

    private void setupSortingSpinner() {
        String[] sortOptions = {"Tarih (En Yeni)", "Tarih (En Eski)", "Boyut (Büyükten Küçüğe)", "Boyut (Küçükten Büyüğe)", "İsim (A-Z)"};

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sortOptions);
        sortAutoComplete.setAdapter(spinnerAdapter);

        sortAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            sortFiles(position);
        });
    }

    private void loadConvertedFilesRaw() {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir.exists() && downloadDir.isDirectory()) {
            allFilesRaw = downloadDir.listFiles();
        }
    }

    private void sortFiles(int sortType) {
        convertedFilesList.clear();

        if (allFilesRaw != null) {
            for (File file : allFilesRaw) {
                if (file.isFile() && file.getName().contains("_converted")) {
                    convertedFilesList.add(file);
                }
            }
        }

        switch (sortType) {
            case 0: // Tarih (En Yeni)
                Collections.sort(convertedFilesList, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                break;
            case 1: // Tarih (En Eski)
                Collections.sort(convertedFilesList, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
                break;
            case 2: // Boyut (Büyükten Küçüğe)
                Collections.sort(convertedFilesList, (f1, f2) -> Long.compare(f2.length(), f1.length()));
                break;
            case 3: // Boyut (Küçükten Büyüğe)
                Collections.sort(convertedFilesList, (f1, f2) -> Long.compare(f1.length(), f2.length()));
                break;
            case 4: // İsim (A-Z)
                Collections.sort(convertedFilesList, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                break;
        }

        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (convertedFilesList.isEmpty()) {
            emptyViewText.setVisibility(View.VISIBLE);
            filesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyViewText.setVisibility(View.GONE);
            filesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void openMediaFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);

            String mimeType = "video/*";
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".aac") || fileName.endsWith(".flac")) {
                mimeType = "audio/*";
            }

            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (IllegalArgumentException e) {
            android.util.Log.e("FILE_OPEN", "FileProvider Hatası: " + e.getMessage());
            Toast.makeText(requireContext(), "Güvenlik yolu hatası. Lütfen Logcat'e bakın.", Toast.LENGTH_SHORT).show();
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "Cihazda bu dosyayı açacak bir medya oynatıcı yok!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.util.Log.e("FILE_OPEN", "Bilinmeyen Hata: " + e.getMessage());
            Toast.makeText(requireContext(), "Beklenmeyen bir hata oluştu.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDownloadsFolder() {
        Toast.makeText(requireContext(), "Dosyalarınız cihazın 'Download' (İndirilenler) klasöründedir.", Toast.LENGTH_LONG).show();

        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivity(Intent.createChooser(intent, "Dosya Yöneticisini Seçin"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Cihazda dosya yöneticisi bulunamadı.", Toast.LENGTH_SHORT).show();
        }
    }
}