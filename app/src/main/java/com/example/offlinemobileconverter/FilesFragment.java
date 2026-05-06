package com.example.offlinemobileconverter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

    // YENİ: Kullanıcının seçtiği sıralama türünü hafızada tutuyoruz (Varsayılan: 0 - En Yeni)
    private int currentSortType = 0;

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

        // İlk açılışta dosyaları yükle
        refreshFiles();

        new androidx.recyclerview.widget.ItemTouchHelper(new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT | androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                File fileToDelete = convertedFilesList.get(position);

                final File backupFile = fileToDelete;
                final int backupPosition = position;

                adapter.removeItem(position);

                com.google.android.material.snackbar.Snackbar.make(filesRecyclerView, "Dosya silindi", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setAction("GERİ AL", v -> {
                            convertedFilesList.add(backupPosition, backupFile);
                            adapter.notifyItemInserted(backupPosition);
                            updateEmptyView();
                        })
                        .addCallback(new com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback<com.google.android.material.snackbar.Snackbar>() {
                            @Override
                            public void onDismissed(com.google.android.material.snackbar.Snackbar transientBottomBar, int event) {
                                if (event != DISMISS_EVENT_ACTION) {
                                    if (backupFile.exists()) {
                                        backupFile.delete();
                                        android.media.MediaScannerConnection.scanFile(requireContext(), new String[]{backupFile.getAbsolutePath()}, null, null);
                                    }
                                }
                            }
                        }).show();

                updateEmptyView();
            }
        }).attachToRecyclerView(filesRecyclerView);

        return view;
    }

    // YENİ: Dosyaları baştan okuyup, mevcut sıralamaya göre tekrar listeler
    private void refreshFiles() {
        loadConvertedFilesRaw();
        sortFiles(currentSortType);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshFiles();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFiles();
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
        this.currentSortType = sortType; // Seçilen sıralamayı hafızaya al
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

            String fileName = file.getName().toLowerCase();
            String mimeType = "*/*";

            if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".avi") || fileName.endsWith(".mov")) {
                mimeType = "video/*";
            } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".m4a") || fileName.endsWith(".aac") || fileName.endsWith(".flac")) {
                mimeType = "audio/*";
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".webp")) {
                mimeType = "image/*";
            } else if (fileName.endsWith(".pdf")) {
                mimeType = "application/pdf";
            } else if (fileName.endsWith(".zip")) {
                mimeType = "application/zip";
            }

            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "Bu dosyayı açacak uygun bir uygulama bulunamadı.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Dosya açılırken bir hata oluştu.", Toast.LENGTH_SHORT).show();
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