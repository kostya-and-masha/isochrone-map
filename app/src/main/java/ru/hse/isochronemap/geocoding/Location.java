package ru.hse.isochronemap.geocoding;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

import androidx.annotation.NonNull;
import ru.hse.isochronemap.mapstructure.Coordinate;

/** This class represents location suggested by {@link Geocoder}. **/
public class Location implements Parcelable {
    /** Creates Location instance from {@link Parcel}/ **/
    public static final Parcelable.Creator<Location> CREATOR = new Parcelable.Creator<Location>() {
        @Override
        public Location createFromParcel(@NonNull Parcel in) {
            return new Location(in);
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };

    /** Location name. **/
    public final @NonNull String name;
    /** Central geo coordinate. **/
    public final @NonNull Coordinate coordinate;

    public Location(@NonNull String name, @NonNull Coordinate coordinate) {
        this.name = name;
        this.coordinate = coordinate;
    }

    private Location(@NonNull Parcel in) {
        name = Objects.requireNonNull(in.readString());

        double latitude = in.readDouble();
        double longitude = in.readDouble();
        coordinate = new Coordinate(latitude, longitude);
    }

    /**
     * Describes the kinds of special objects contained in this {@link Parcelable} instance
     * (no special objects).
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Saves Location into {@link Parcel}.
     * @param out parcel to write in
     * @param flags ignored
     */
    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeString(name);
        out.writeDouble(coordinate.latitude);
        out.writeDouble(coordinate.longitude);
    }
}
