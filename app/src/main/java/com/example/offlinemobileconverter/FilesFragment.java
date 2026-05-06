package com.example.offlinemobileconverter;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
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
    private int currentSortType = 0;

    private LinearLayout selectionModeLayout;
    private TextView selectionCountText;
    private ImageButton btnDeleteSelected;
    private ImageButton btnCancelSelection;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);

        filesRecyclerView = view.findViewById(R.id.filesRecyclerView);
        emptyViewText = view.findViewById(R.id.emptyViewText);
        sortAutoComplete = view.findViewById(R.id.sortAutoComplete);
        TextView titleText = view.findViewById(R.id.titleText);
        selectionModeLayout = view.findViewById(R.id.selectionModeLayout);
        selectionCountText = view.findViewById(R.id.selectionCountText);
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected);
        btnCancelSelection = view.findViewById(R.id.btnCancelSelection);

        filesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new FilesAdapter(convertedFilesList, new FilesAdapter.OnFileClickListener() {
            @Override
            public void onPlayClick(File file) { openMediaFile(file); }
            @Override
            public void onFolderClick() { openDownloadsFolder(); }

            @Override
            public void onSelectionModeChanged(boolean isSelectionMode, int selectedCount) {
                if (isSelectionMode) {
                    selectionModeLayout.setVisibility(View.VISIBLE);
                    selectionCountText.setText(selectedCount + " Seçildi");
                    titleText.setVisibility(View.INVISIBLE);
                    if (selectedCount == 0) {
                        adapter.setSelectionMode(false);
                        selectionModeLayout.setVisibility(View.GONE);
                    }
                } else {
                    selectionModeLayout.setVisibility(View.GONE);
                    titleText.setVisibility(View.VISIBLE);
                }
            }
        });

        filesRecyclerView.setAdapter(adapter);

        setupSortingSpinner();
        refreshFiles();

        btnDeleteSelected.setOnClickListener(v -> {
            List<File> filesToDelete = adapter.getSelectedFiles();
            if (filesToDelete.isEmpty()) return;

            new AlertDialog.Builder(requireContext())
                    .setTitle("Silme Onayı")
                    .setMessage("Seçili " + filesToDelete.size() + " dosyayı kalıcı olarak silmek istediğinize emin misiniz?")
                    .setPositiveButton("Sil", (dialog, which) -> {
                        for (File f : filesToDelete) {
                            if (f.exists()) {
                                f.delete();
                                android.media.MediaScannerConnection.scanFile(requireContext(), new String[]{f.getAbsolutePath()}, null, null);
                            }
                        }
                        adapter.setSelectionMode(false);
                        selectionModeLayout.setVisibility(View.GONE);
                        refreshFiles();
                        Toast.makeText(requireContext(), filesToDelete.size() + " dosya silindi", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("İptal", null)
                    .show();
        });

        btnCancelSelection.setOnClickListener(v -> {
            adapter.setSelectionMode(false);
            selectionModeLayout.setVisibility(View.GONE);
        });

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                ColorDrawable background = new ColorDrawable(Color.parseColor("#EA4335"));
                Drawable icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete);
                icon.setTint(Color.WHITE);

                int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                int iconTop = itemView.getTop() + iconMargin;
                int iconBottom = iconTop + icon.getIntrinsicHeight();

                if (dX > 0) {
                    background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX), itemView.getBottom());
                    int iconLeft = itemView.getLeft() + iconMargin;
                    int iconRight = iconLeft + icon.getIntrinsicWidth();
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                } else if (dX < 0) {
                    background.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    int iconRight = itemView.getRight() - iconMargin;
                    int iconLeft = iconRight - icon.getIntrinsicWidth();
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                } else {
                    background.setBounds(0, 0, 0, 0);
                    icon.setBounds(0, 0, 0, 0);
                }

                background.draw(c);
                icon.draw(c);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

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
        };

        new ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(filesRecyclerView);

        return view;
    }

    private void refreshFiles() {
        loadConvertedFilesRaw();
        sortFiles(currentSortType);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refreshFiles();
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
        sortAutoComplete.setOnItemClickListener((parent, view, position, id) -> sortFiles(position));
    }

    private void loadConvertedFilesRaw() {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir.exists() && downloadDir.isDirectory()) {
            allFilesRaw = downloadDir.listFiles();
        }
    }

    private void sortFiles(int sortType) {
        this.currentSortType = sortType;
        convertedFilesList.clear();

        if (allFilesRaw != null) {
            for (File file : allFilesRaw) {
                if (file.isFile() && file.getName().contains("_converted")) {
                    convertedFilesList.add(file);
                }
            }
        }

        switch (sortType) {
            case 0: Collections.sort(convertedFilesList, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified())); break;
            case 1: Collections.sort(convertedFilesList, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified())); break;
            case 2: Collections.sort(convertedFilesList, (f1, f2) -> Long.compare(f2.length(), f1.length())); break;
            case 3: Collections.sort(convertedFilesList, (f1, f2) -> Long.compare(f1.length(), f2.length())); break;
            case 4: Collections.sort(convertedFilesList, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName())); break;
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

            if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".avi") || fileName.endsWith(".mov")) mimeType = "video/*";
            else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".m4a") || fileName.endsWith(".aac") || fileName.endsWith(".flac")) mimeType = "audio/*";
            else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".webp")) mimeType = "image/*";
            else if (fileName.endsWith(".pdf")) mimeType = "application/pdf";
            else if (fileName.endsWith(".zip")) mimeType = "application/zip";

            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Bu dosyayı açacak uygulama bulunamadı.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDownloadsFolder() {
        Toast.makeText(requireContext(), "Dosyalar 'Download' klasöründedir.", Toast.LENGTH_LONG).show();
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivity(Intent.createChooser(intent, "Dosya Yöneticisini Seçin"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Dosya yöneticisi bulunamadı.", Toast.LENGTH_SHORT).show();
        }
    }
}