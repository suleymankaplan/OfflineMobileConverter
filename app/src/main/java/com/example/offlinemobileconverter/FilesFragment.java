package com.example.offlinemobileconverter;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilesFragment extends Fragment {

    private RecyclerView filesRecyclerView;
    private TextView emptyViewText;
    private FilesAdapter adapter;
    private List<File> convertedFilesList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);

        filesRecyclerView = view.findViewById(R.id.filesRecyclerView);
        emptyViewText = view.findViewById(R.id.emptyViewText);

        filesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FilesAdapter(convertedFilesList);
        filesRecyclerView.setAdapter(adapter);

        loadConvertedFiles();

        return view;
    }

    private void loadConvertedFiles() {
        convertedFilesList.clear();

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        if (downloadDir.exists() && downloadDir.isDirectory()) {
            File[] files = downloadDir.listFiles();
            if (files != null) {
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

                for (File file : files) {
                    if (file.isFile() && file.getName().contains("_converted")) {
                        convertedFilesList.add(file);
                    }
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (convertedFilesList.isEmpty()) {
            emptyViewText.setVisibility(View.VISIBLE);
            filesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyViewText.setVisibility(View.GONE);
            filesRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}