package ru.hse.isochronemap.isochronebuilding;

/**
 * An exception used for unimplemented features of {@link IsochroneBuilder}.
 * Will probably be deleted in release version.
 */
public class UnsupportedParameterException extends Exception {
    public UnsupportedParameterException () {
    }

    public UnsupportedParameterException (String message) {
        super (message);
    }

    public UnsupportedParameterException (Throwable cause) {
        super (cause);
    }

    public UnsupportedParameterException (String message, Throwable cause) {
        super (message, cause);
    }
}
