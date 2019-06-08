package ru.hse.isochronemap.ui;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import ru.hse.isochronemap.util.Consumer;

/**
 * This fragment is retained during configuration changes and is used to
 * deliver callbacks of asynchronous operations back to currently resumed activity
 * or to store heavy objects (which could not be saved into Bundle due to their size).
 */
public class AuxiliaryFragment extends Fragment {
    private MainActivity mainActivity;
    private ArrayList<PolygonOptions> savedPolygons;

    /** {@inheritDoc} */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    /** {@inheritDoc} */
    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null;
    }

    /** Returns previously saved GoogleMap polygons */
    public @Nullable ArrayList<PolygonOptions> getSavedPolygons() {
        return savedPolygons;
    }

    /** Saves GoogleMap polygons */
    public void setSavedPolygons(@Nullable ArrayList<PolygonOptions> savedPolygons) {
        this.savedPolygons = savedPolygons;
    }

    /** Executes callback on currently working MainActivity */
    public void transferActionToMainActivity(@NonNull Consumer<MainActivity> callback) {
        if (mainActivity != null) {
            callback.accept(mainActivity);
        }
    }
}
