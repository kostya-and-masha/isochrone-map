package ru.hse.isochronemap.location;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import ru.hse.isochronemap.R;

public class EnableGPSDialogFragment extends DialogFragment {
    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        // disable inspection (this method is called after fragment has been attached to activity).
        assert getContext() != null;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.LocationDialog);
        Intent openSettingsIntent =
                new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);

        builder.setMessage(getString(R.string.enable_gps_dialog_message))
               .setCancelable(false)
               .setPositiveButton(getString(R.string.enable_gps_dialog_positive_text),
                                  (dialog, id) -> getContext().startActivity(openSettingsIntent))
               .setNegativeButton(getString(R.string.enable_gps_dialog_negative_text),
                                  (dialog, id) -> dialog.cancel());
        return builder.create();
    }
}
