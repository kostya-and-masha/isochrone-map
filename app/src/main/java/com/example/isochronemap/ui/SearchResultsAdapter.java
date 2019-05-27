package com.example.isochronemap.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.isochronemap.R;
import com.example.isochronemap.geocoding.Location;
import com.example.isochronemap.util.TEMP_DummyResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SearchResultsAdapter extends
        RecyclerView.Adapter<SearchResultsAdapter.SearchResultsViewHolder> {

    List<Location> results = new ArrayList<>();
    OnResultClickListener onResultClickListener = null;

    public interface OnResultClickListener {
        void onResultClick(Location result);
    }

    public void setOnResultClickListener(OnResultClickListener listener) {
        this.onResultClickListener = listener;
    }

    public void setItems(Collection<Location> newResults) {
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
        holder.text.setText(results.get(position).name);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public class SearchResultsViewHolder extends RecyclerView.ViewHolder {
        private ImageView icon;
        private TextView text;

        public SearchResultsViewHolder(View view) {
            super(view);

            icon = view.findViewById(R.id.icon);
            text = view.findViewById(R.id.text);

            view.setOnClickListener(v -> {
                Location result = results.get(getLayoutPosition());
                onResultClickListener.onResultClick(result);
            });
        }

    }
}
