package ru.hse.isochronemap.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ru.hse.isochronemap.R;
import ru.hse.isochronemap.geocoding.Location;

/**
 * This class implements adapter for recycler view that is used to show hints and search results
 * in {@link IsochroneMenu}
 */
class SearchResultsAdapter
        extends RecyclerView.Adapter<SearchResultsAdapter.SearchResultsViewHolder> {

    private List<String> hints = new ArrayList<>();
    private List<Location> results;

    private OnHintClickListener onHintClickListener;
    private OnHintDeleteClickListener onHintDeleteClickListener;
    private OnResultClickListener onResultClickListener;

    private AdapterMode mode = AdapterMode.HINTS;

    /** {@see OnResultClickListener} */
    void setOnResultClickListener(@NonNull OnResultClickListener listener) {
        onResultClickListener = listener;
    }

    /** {@see OnHintClickListener} */
    void setOnHintClickListener(@NonNull OnHintClickListener listener) {
        onHintClickListener = listener;
    }

    /** {@see OnHintClickListener} */
    void setOnHintDeleteClickListener(@NonNull OnHintDeleteClickListener listener) {
        onHintDeleteClickListener = listener;
    }

    /** Sets search results and switches to results mode. */
    void setResults(List<Location> newResults) {
        hints = null;
        results = newResults;
        mode = AdapterMode.RESULTS;
        notifyDataSetChanged();
    }

    /** Sets hints and switches to hint mode. */
    void setHints(List<String> newHints) {
        results = null;
        hints = newHints;
        mode = AdapterMode.HINTS;
        notifyDataSetChanged();
    }

    /** Returns current mode (results/hints). */
    @NonNull AdapterMode getAdapterMode() {
        return mode;
    }

    /** Returns current hints list. */
    @NonNull ArrayList<String> getHintsList() {
        if (hints instanceof ArrayList) {
            return (ArrayList<String>) hints;
        } else if (hints != null) {
            return new ArrayList<>(hints);
        }
        return new ArrayList<>();
    }

    /** Returns current results list. */
    @NonNull
    ArrayList<Location> getResultsList() {
        if (results instanceof ArrayList) {
            return (ArrayList<Location>) results;
        } else if (hints != null) {
            return new ArrayList<>(results);
        }
        return new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull
    SearchResultsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.search_results_item, parent, false);
        ((ImageView) item.findViewById(R.id.right_icon))
                .setImageResource(R.drawable.ic_delete_black_24dp);
        return new SearchResultsViewHolder(item);
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public int getItemCount() {
        if (mode == AdapterMode.HINTS) {
            return hints.size();
        } else if (mode == AdapterMode.RESULTS) {
            return results.size();
        }
        return 0;
    }

    enum AdapterMode {
        HINTS, RESULTS
    }

    /** This listener will be invoked when user clicks on a search result. */
    interface OnResultClickListener {
        void onResultClick(Location result);
    }

    /** This listener will be invoked when user clicks on a hint. */
    interface OnHintClickListener {
        void onHintClick(String hint);
    }

    /** This listener will be invoked when user deletes hint. */
    interface OnHintDeleteClickListener {
        void onHintDeleteClick(String hint);
    }

    /** View holder for both hints or results. */
    class SearchResultsViewHolder extends RecyclerView.ViewHolder {
        private ImageView leftIcon;
        private TextView text;
        private ImageView rightIcon;

        SearchResultsViewHolder(View view) {
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
