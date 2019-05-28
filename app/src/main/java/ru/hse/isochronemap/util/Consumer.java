package ru.hse.isochronemap.util;

/** Represents an operation that accepts a single input argument and returns no result. **/
public interface Consumer<T> {
    /** Performs this operation on the given argument. **/
    void accept(T t);
}
