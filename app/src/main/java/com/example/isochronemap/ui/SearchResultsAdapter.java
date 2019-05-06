package com.example.isochronemap.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.isochronemap.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SearchResultsAdapter extends
        RecyclerView.Adapter<SearchResultsAdapter.SearchResultsViewHolder> {

    List<String> results = new ArrayList<>();

    public void setItems(Collection<String> newResults) {
        results.clear();
        results.addAll(newResults);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchResultsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_results_item, parent,false);
        return new SearchResultsViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultsViewHolder holder, int position) {
        holder.icon.setImageResource(R.drawable.ic_place_black_24dp);
        holder.text.setText(results.get(position));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class SearchResultsViewHolder extends RecyclerView.ViewHolder {
        private ImageView icon;
        private TextView text;

        public SearchResultsViewHolder(View view) {
            super(view);

            icon = view.findViewById(R.id.icon);
            text = view.findViewById(R.id.text);
        }

    }
}
