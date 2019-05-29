package ru.hse.isochronemap;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;
import ru.hse.isochronemap.geocoding.Location;
import ru.hse.isochronemap.mapstructure.Coordinate;

public class AuxiliaryFragment extends Fragment {
    private MainActivity mainActivity;
    private ArrayList<PolygonOptions> savedPolygons;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity)context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null;
    }

    public ArrayList<PolygonOptions> getSavedPolygons() {
        return savedPolygons;
    }

    public void setSavedPolygons(ArrayList<PolygonOptions> savedPolygons) {
        this.savedPolygons = savedPolygons;
    }

    public void asyncMapRequestCallback(MainActivity.IsochroneResponse response) {
        if (mainActivity != null) {
            mainActivity.asyncMapRequestCallback(response);
        }
    }
    public void gpsButtonCallback(Coordinate coordinate) {
        if (mainActivity != null) {
            mainActivity.gpsButtonCallback(coordinate);
        }
    }

    public void onSuccessSearchResultsCallback(List<Location> list) {
        if (mainActivity != null) {
            mainActivity.getMenu().onSuccessSearchResultsCallback(list);
        }
    }

    public void onFailureSearchResultsCallback(Exception exception) {
        if (mainActivity != null) {
            mainActivity.getMenu().onFailureSearchResultsCallback(exception);
        }
    }

}
