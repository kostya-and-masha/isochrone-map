package ru.hse.isochronemap.ui;

import android.content.Context;
import android.os.AsyncTask;
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
    private boolean wasDeadBefore = true;
    private MainActivity mainActivity;
    private ArrayList<PolygonOptions> savedPolygons;
    private AsyncTask<?, ?, ?> currentAction;

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
        setRetainInstance(true);
    }

    /** {@inheritDoc} */
    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null;
    }

    /** Returns whether this fragment died since last call to this method. */
    boolean wasDead() {
        if (wasDeadBefore) {
            wasDeadBefore = false;
            return true;
        }
        return false;
    }

    /** Returns previously saved GoogleMap polygons */
    @Nullable ArrayList<PolygonOptions> getSavedPolygons() {
        return savedPolygons;
    }

    /** Saves GoogleMap polygons */
    void setSavedPolygons(@Nullable ArrayList<PolygonOptions> savedPolygons) {
        this.savedPolygons = savedPolygons;
    }

    /** Executes callback on currently working MainActivity */
    void transferActionToMainActivity(@NonNull Consumer<MainActivity> callback) {
        if (mainActivity != null) {
            callback.accept(mainActivity);
        }
    }

    void setCurrentAction(@NonNull AsyncTask<?, ?, ?> currentAction) {
        this.currentAction = currentAction;
    }

    void cancelCurrentAction() {
        mainActivity.cancelCurrentAction();
        if (currentAction != null) {
            currentAction.cancel(true);
        }
    }

    void updateActionMessage(@NonNull String message) {
        mainActivity.updateActionMessage(message);
    }
}
