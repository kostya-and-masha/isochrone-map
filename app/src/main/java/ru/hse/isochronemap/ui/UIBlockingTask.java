package ru.hse.isochronemap.ui;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

abstract class UIBlockingTask extends AsyncTask<Void, String, Void> {
    AuxiliaryFragment auxiliaryFragment;

    UIBlockingTask(@NonNull AuxiliaryFragment auxiliaryFragment) {
        this.auxiliaryFragment = auxiliaryFragment;
    }

    @Override
    protected void onPreExecute() {
        auxiliaryFragment.transferActionToMainActivity(MainActivity::showOnBackgroundActionUI);
        auxiliaryFragment.setCurrentAction(this);
    }

    @Override
    protected void onPostExecute(Void v) {
        auxiliaryFragment.transferActionToMainActivity(MainActivity::hideOnBackgroundActionUI);
    }

    @Override
    protected void onProgressUpdate(String ... messages) {
        String message = messages[0];
        auxiliaryFragment.updateActionMessage(message);
    }
}
