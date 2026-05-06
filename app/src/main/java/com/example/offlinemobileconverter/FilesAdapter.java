package com.example.offlinemobileconverter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileViewHolder> {

    private final List<File> fileList;
    private final OnFileClickListener listener;

    public interface OnFileClickListener {
        void onPlayClick(File file);
        void onFolderClick();
    }

    public FilesAdapter(List<File> fileList, OnFileClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
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
        holder.fileSizeText.setText(fileSizeInMB > 0 ? fileSizeInMB + " MB" : fileSizeInKB + " KB");

        String name = file.getName().toLowerCase();

        boolean isImage = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
        boolean isVideo = name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov");

        if (isImage || isVideo) {
            holder.fileIcon.setImageTintList(null);

            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                    .load(file)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.fileIcon);
        } else {
            com.bumptech.glide.Glide.with(holder.itemView.getContext()).clear(holder.fileIcon);

            int tintColor = androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.blue_primary);
            holder.fileIcon.setImageTintList(android.content.res.ColorStateList.valueOf(tintColor));

            if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".flac")) {
                holder.fileIcon.setImageResource(android.R.drawable.ic_media_play);
            } else if (name.endsWith(".pdf")) {
                holder.fileIcon.setImageResource(android.R.drawable.ic_menu_edit);
            } else if (name.endsWith(".zip")) {
                holder.fileIcon.setImageResource(android.R.drawable.ic_menu_save);
            } else {
                holder.fileIcon.setImageResource(android.R.drawable.ic_menu_info_details);
            }
        }

        holder.btnPlayFile.setOnClickListener(v -> listener.onPlayClick(file));
        holder.btnOpenFolder.setOnClickListener(v -> listener.onFolderClick());
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileNameText;
        TextView fileSizeText;
        ImageButton btnPlayFile;
        ImageButton btnOpenFolder;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            fileSizeText = itemView.findViewById(R.id.fileSizeText);
            btnPlayFile = itemView.findViewById(R.id.btnPlayFile);
            btnOpenFolder = itemView.findViewById(R.id.btnOpenFolder);
        }
    }
    public void removeItem(int position) {
        fileList.remove(position);
        notifyItemRemoved(position);
    }
}