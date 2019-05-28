package com.example.isochronemap.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.isochronemap.R;
import com.example.isochronemap.geocoding.Location;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class SearchResultsAdapter extends
        RecyclerView.Adapter<SearchResultsAdapter.SearchResultsViewHolder> {

    private List<String> hints = new ArrayList<>();
    private List<Location> results;

    private OnHintClickListener onHintClickListener;
    private OnHintDeleteClickListener onHintDeleteClickListener;
    private OnResultClickListener onResultClickListener;

    private AdapterMode mode = AdapterMode.HINTS;

    enum AdapterMode {
        HINTS, RESULTS
    }

    interface OnResultClickListener {
        void onResultClick(Location result);
    }

    void setOnResultClickListener(@NonNull OnResultClickListener listener) {
        onResultClickListener = listener;
    }

    interface OnHintClickListener {
        void onHintClick(String hint);
    }

    void setOnHintClickListener(@NonNull OnHintClickListener listener) {
        onHintClickListener = listener;
    }

    interface OnHintDeleteClickListener {
        void onHintDeleteClick(String hint);
    }

    void setOnHintDeleteClickListener(@NotNull OnHintDeleteClickListener listener) {
        onHintDeleteClickListener = listener;
    }

    void setResults(List<Location> newResults) {
        hints = null;
        results = newResults;
        mode = AdapterMode.RESULTS;
        notifyDataSetChanged();
    }

    void setHints(List<String> newHints) {
        results = null;
        hints = newHints;
        mode = AdapterMode.HINTS;
        notifyDataSetChanged();
    }


    @NonNull AdapterMode getAdapterMode() {
        return mode;
    }

    @NonNull Serializable getAdapterContentSerializable() {
        List<?> list = (mode == AdapterMode.HINTS ? hints : results);

        if (hints instanceof Serializable) {
            return (Serializable)list;
        } else {
            return new ArrayList<>(list);
        }
    }

    @Override
    public @NonNull SearchResultsViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                               int viewType) {
        View item = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_results_item, parent,false);
        ((ImageView)item.findViewById(R.id.right_icon))
                .setImageResource(R.drawable.ic_delete_black_24dp);
        return new SearchResultsViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultsViewHolder holder, int position) {
        if (mode == AdapterMode.HINTS) {
            holder.leftIcon.setImageResource(R.drawable.ic_access_time_black_24dp);
            holder.rightIcon.setVisibility(View.VISIBLE);
            holder.text.setText(hints.get(position));
        } else if (mode == AdapterMode.RESULTS) {
            holder.leftIcon.setImageResource(R.drawable.ic_place_black_24dp);
            holder.rightIcon.setVisibility(View.GONE);
            holder.text.setText(results.get(position).name);
        }
    }

    @Override
    public int getItemCount() {
        if (mode == AdapterMode.HINTS) {
            return hints.size();
        } else if (mode == AdapterMode.RESULTS) {
            return results.size();
        }
        return 0;
    }

    public class SearchResultsViewHolder extends RecyclerView.ViewHolder {
        private ImageView leftIcon;
        private TextView text;
        private ImageView rightIcon;

        public SearchResultsViewHolder(View view) {
            super(view);

            leftIcon = view.findViewById(R.id.left_icon);
            text = view.findViewById(R.id.text);
            rightIcon = view.findViewById(R.id.right_icon);

            view.setOnClickListener(v -> {
                if (mode == AdapterMode.HINTS) {
                    String hint = hints.get(getLayoutPosition());
                    if (onHintClickListener != null) {
                        onHintClickListener.onHintClick(hint);
                    }
                } else if (mode == AdapterMode.RESULTS) {
                    Location result = results.get(getLayoutPosition());
                    if (onResultClickListener != null) {
                        onResultClickListener.onResultClick(result);
                    }
                }
            });

            rightIcon.setOnClickListener(v -> {
                String hint = hints.get(getLayoutPosition());
                if (onHintDeleteClickListener != null) {
                    onHintDeleteClickListener.onHintDeleteClick(hint);
                }
            });
        }

    }
}
