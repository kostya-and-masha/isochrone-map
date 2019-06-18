package ru.hse.isochronemap.ui;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

/** Base class for blocking UI asynchronous tasks. **/
abstract class UIBlockingTask extends AsyncTask<Void, String, Void> {
    /** {@link AuxiliaryFragment} used to deliver callbacks to {@link MainActivity}. **/
    AuxiliaryFragment auxiliaryFragment;

    UIBlockingTask(@NonNull AuxiliaryFragment auxiliaryFragment) {
        this.auxiliaryFragment = auxiliaryFragment;
    }

    /** {@inheritDoc} **/
    @Override
    protected void onPreExecute() {
        auxiliaryFragment.transferActionToMainActivity(MainActivity::showOnBackgroundActionUI);
        auxiliaryFragment.setCurrentAction(this);
    }

    /** {@inheritDoc} **/
    @Override
    protected void onPostExecute(Void v) {
        auxiliaryFragment.transferActionToMainActivity(MainActivity::hideOnBackgroundActionUI);
    }

    /** {@inheritDoc} **/
    @Override
    protected void onProgressUpdate(String ... messages) {
        String message = messages[0];
        auxiliaryFragment.updateActionMessage(message);
    }
}
