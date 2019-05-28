package ru.hse.isochronemap.location;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.util.Consumer;

// TODO javadoc
public class ApproximateLocationProvider implements AutoCloseable {
    private static final int GET_LOCATION_PERIOD = 1000 * 60 * 10;

    private volatile Coordinate lastLocation;
    private final HandlerThread handlerThread;
    private final Context context;
    private final Timer timer;

    public ApproximateLocationProvider(@NonNull final Context context,
                                       @Nullable Coordinate initialLocation) {
        this.context = context;
        lastLocation = initialLocation;

        handlerThread = new HandlerThread("GET_LOCATION_HANDLER_THREAD");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        timer = new Timer(true);
        timer.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(() -> {
                            if (OneTimeLocationProvider.hasPermissions(context)) {
                                OneTimeLocationProvider.getLocation(context, location -> {
                                    lastLocation = location;
                                });
                            }
                        });
                    }
                },
                0,
                GET_LOCATION_PERIOD
        );
    }

    public void getLocation(Consumer<Coordinate> callback) {
        Coordinate locationSnapshot = lastLocation;
        if (locationSnapshot == null) {
            if (OneTimeLocationProvider.hasPermissions(context)) {
                OneTimeLocationProvider.getLocation(context, location -> {
                    lastLocation = location;
                    callback.accept(location);
                });
            } else {
                callback.accept(null);
            }
        } else {
            callback.accept(locationSnapshot);
        }
    }

    public @Nullable Coordinate getLocationImmediately() {
        return lastLocation;
    }

    @Override
    public void close() {
        timer.cancel();
        handlerThread.quit();
    }
}
