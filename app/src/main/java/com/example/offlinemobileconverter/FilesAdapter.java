package com.example.offlinemobileconverter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileViewHolder> {

    private final List<File> fileList;

    public FilesAdapter(List<File> fileList) {
        this.fileList = fileList;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = fileList.get(position);
        holder.fileNameText.setText(file.getName());

        long fileSizeInBytes = file.length();
        long fileSizeInKB = fileSizeInBytes / 1024;
        long fileSizeInMB = fileSizeInKB / 1024;

        if (fileSizeInMB > 0) {
            holder.fileSizeText.setText(fileSizeInMB + " MB");
        } else {
            holder.fileSizeText.setText(fileSizeInKB + " KB");
        }

        if (file.getName().toLowerCase().endsWith(".mp3") || file.getName().toLowerCase().endsWith(".wav")) {
            holder.fileIcon.setImageResource(android.R.drawable.ic_media_play); // Ses ikonu
        } else {
            holder.fileIcon.setImageResource(android.R.drawable.presence_video_online); // Video ikonu
        }
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileNameText;
        TextView fileSizeText;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            fileSizeText = itemView.findViewById(R.id.fileSizeText);
        }
    }
}