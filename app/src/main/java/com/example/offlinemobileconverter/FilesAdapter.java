package com.example.offlinemobileconverter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileViewHolder> {

    private List<File> fileList;
    private OnFileClickListener listener;

    private boolean isSelectionMode = false;
    private List<File> selectedFiles = new ArrayList<>();

    public interface OnFileClickListener {
        void onPlayClick(File file);
        void onFolderClick();
        void onSelectionModeChanged(boolean isSelectionMode, int selectedCount);
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

        if (isSelectionMode) {
            holder.fileCheckBox.setVisibility(View.VISIBLE);
            holder.fileCheckBox.setChecked(selectedFiles.contains(file));
            holder.btnPlayFile.setVisibility(View.GONE);
            holder.btnOpenFolder.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                if (selectedFiles.contains(file)) {
                    selectedFiles.remove(file);
                } else {
                    selectedFiles.add(file);
                }
                notifyItemChanged(position);
                listener.onSelectionModeChanged(true, selectedFiles.size());
            });
            holder.fileCheckBox.setOnClickListener(v -> holder.itemView.performClick());

        } else {
            holder.fileCheckBox.setVisibility(View.GONE);
            holder.btnPlayFile.setVisibility(View.VISIBLE);
            holder.btnOpenFolder.setVisibility(View.VISIBLE);

            holder.itemView.setOnClickListener(null);
            holder.btnPlayFile.setOnClickListener(v -> listener.onPlayClick(file));
            holder.btnOpenFolder.setOnClickListener(v -> listener.onFolderClick());

            holder.itemView.setOnLongClickListener(v -> {
                setSelectionMode(true);
                selectedFiles.add(file);
                notifyDataSetChanged();
                listener.onSelectionModeChanged(true, 1);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public void removeItem(int position) {
        fileList.remove(position);
        notifyItemRemoved(position);
    }

    public void setSelectionMode(boolean enabled) {
        isSelectionMode = enabled;
        if (!enabled) selectedFiles.clear();
        notifyDataSetChanged();
    }

    public List<File> getSelectedFiles() {
        return new ArrayList<>(selectedFiles);
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileNameText;
        TextView fileSizeText;
        ImageButton btnPlayFile;
        ImageButton btnOpenFolder;
        CheckBox fileCheckBox;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            fileSizeText = itemView.findViewById(R.id.fileSizeText);
            btnPlayFile = itemView.findViewById(R.id.btnPlayFile);
            btnOpenFolder = itemView.findViewById(R.id.btnOpenFolder);
            fileCheckBox = itemView.findViewById(R.id.fileCheckBox);
        }
    }
}