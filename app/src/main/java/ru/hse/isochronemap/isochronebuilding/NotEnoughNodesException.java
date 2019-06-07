package ru.hse.isochronemap.isochronebuilding;

/**
 * This exception is thrown when the number of reachable nodes found by {@link IsochroneBuilder}
 * is not enough to build an isochrone polygon.
 */
public class NotEnoughNodesException extends Exception {
    public NotEnoughNodesException() {
    }

    public NotEnoughNodesException(String message) {
        super(message);
    }

    public NotEnoughNodesException(Throwable cause) {
        super(cause);
    }

    public NotEnoughNodesException(String message, Throwable cause) {
        super(message, cause);
    }
}
