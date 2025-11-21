package com.visionai.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.visionai.app.R;
import com.visionai.app.models.Mentor;

import java.util.List;

public class MentorAdapter extends RecyclerView.Adapter<MentorAdapter.ViewHolder> {

    public interface OnMentorClickListener {
        void onMentorClick(Mentor mentor, int position);
    }

    private List<Mentor> mentors;
    private OnMentorClickListener listener;
    private int selectedPosition = -1;

    public MentorAdapter(List<Mentor> mentors, OnMentorClickListener listener) {
        this.mentors = mentors;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int previousSelected = selectedPosition;
        selectedPosition = position;
        if (previousSelected != -1) {
            notifyItemChanged(previousSelected);
        }
        notifyItemChanged(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mentor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Mentor mentor = mentors.get(position);
        holder.mentorName.setText(mentor.name);
        holder.mentorRole.setText(mentor.role);

        // Show selection state
        boolean isSelected = position == selectedPosition;
        holder.selectedIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        
        // Update card background for selection
        if (isSelected) {
            holder.cardView.setCardBackgroundColor(0xFFF0F8FF); // Light blue tint
        } else {
            holder.cardView.setCardBackgroundColor(0xFFFFFFFF); // White
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMentorClick(mentor, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mentors.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mentorName;
        TextView mentorRole;
        ImageView selectedIcon;
        CardView cardView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mentorName = itemView.findViewById(R.id.mentorName);
            mentorRole = itemView.findViewById(R.id.mentorRole);
            selectedIcon = itemView.findViewById(R.id.selectedIcon);
            cardView = (CardView) itemView;
        }
    }
}