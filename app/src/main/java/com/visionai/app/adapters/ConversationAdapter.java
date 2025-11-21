package com.visionai.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionai.app.R;
import com.visionai.app.models.Conversation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
        void onDeleteConversation(Conversation conversation);
    }

    private List<Conversation> conversations = new ArrayList<>();
    private final OnConversationClickListener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

    public ConversationAdapter(List<Conversation> conversations, OnConversationClickListener listener) {
        if (conversations != null) {
            this.conversations = conversations;
        }
        this.listener = listener;
    }

    public void updateConversations(List<Conversation> newList) {
        conversations = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation c = conversations.get(position);
        holder.title.setText(c.title != null && !c.title.isEmpty()
                ? c.title
                : "New Conversation");

        String dateText = dateFormat.format(new Date(c.updatedAt));
        holder.timestamp.setText(dateText);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onConversationClick(c);
        });
        
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteConversation(c);
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView timestamp;
        ImageButton deleteButton;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.conversationTitle);
            timestamp = itemView.findViewById(R.id.conversationTimestamp);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
