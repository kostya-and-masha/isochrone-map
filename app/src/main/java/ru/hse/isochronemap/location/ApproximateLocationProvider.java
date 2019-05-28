package ru.hse.isochronemap.location;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.hse.isochronemap.mapstructure.Coordinate;
import ru.hse.isochronemap.util.Consumer;

// TODO javadoc
public class ApproximateLocationProvider implements AutoCloseable {
    private static final int GET_LOCATION_PERIOD = 1000 * 60 * 10;

    private final HandlerThread handlerThread;
    private final Context context;
    private final AtomicReference<Coordinate> lastLocation;
    private final Timer timer;

    public ApproximateLocationProvider(@NonNull final Context context,
                                       @Nullable Coordinate initialLocation) {
        this.context = context;
        lastLocation = new AtomicReference<>(initialLocation);

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
                                OneTimeLocationProvider.getLocation(context, lastLocation::set);
                            }
                        });
                    }
                },
                0,
                GET_LOCATION_PERIOD
        );
    }

    public void getLocation(Consumer<Coordinate> callback) {
        Coordinate location = lastLocation.get();
        if (location == null) {
            if (OneTimeLocationProvider.hasPermissions(context)) {
                OneTimeLocationProvider.getLocation(context, callback);
            } else {
                callback.accept(null);
            }
        } else {
            callback.accept(location);
        }
    }

    public @Nullable Coordinate getLocationImmediately() {
        return lastLocation.get();
    }

    @Override
    public void close() {
        timer.cancel();
        handlerThread.quit();
    }
}
