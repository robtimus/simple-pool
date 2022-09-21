/*
 * PoolableObject.java
 * Copyright 2022 Rob Spoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robtimus.pool;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * An object that can be pooled. Note that the functionality to return the object back to the pool it was acquired from is not part of its
 * <em>public</em> API. This allows implementations to call {@link #release()} from other methods, e.g. a {@link Closeable}'s {@code close} method.
 * <p>
 * Instances of this class are not expected to be thread-safe. Once acquired from a pool, they should usually be used in only one thread until they
 * are released and returned back to the pool.
 *
 * @author Rob Spoor
 * @param <X> The type of exception that operations on the object can throw.
 */
public abstract class PoolableObject<X extends Exception> {

    private static final AtomicLong OBJECT_COUNTER = new AtomicLong();

    private final long objectId;

    private final Set<Object> references;

    private Pool<PoolableObject<X>, X> pool;
    private PoolLogger logger;
    private long idleSince;

    /**
     * Creates a new poolable object.
     */
    protected PoolableObject() {
        objectId = OBJECT_COUNTER.incrementAndGet();

        references = Collections.newSetFromMap(new IdentityHashMap<>());

        logger = PoolLogger.noopLogger();
    }

    long objectId() {
        return objectId;
    }

    void setPool(Pool<PoolableObject<X>, X> pool) {
        this.pool = pool;
        logger = pool.logger();
        resetIdleSince();
    }

    void clearPool() {
        pool = null;
        logger = PoolLogger.noopLogger();
    }

    boolean isPooled() {
        return pool != null;
    }

    int referenceCount() {
        return references.size();
    }

    long idleSince() {
        return idleSince;
    }

    void resetIdleSince() {
        idleSince = System.nanoTime();
    }

    /**
     * Adds a reference to this object. An object will only be returned to the pool it was acquired from if all references to the object are removed.
     * This allows objects to return other (closeable) objects like {@link InputStream} or {@link OutputStream}. These should be added as reference,
     * and {@linkplain #removeReference(Object) removed} when they are no longer needed (e.g. when they are closed).
     *
     * @param reference The non-{@code null} reference to add.
     * @throws NullPointerException If the given reference is {@code null}.
     */
    protected final void addReference(Object reference) {
        Objects.requireNonNull(reference);
        if (references.add(reference)) {
            logger.increasedObjectRefCount(this, references.size());
        }
    }

    /**
     * Removes a reference to this object.
     * If no more references remain, this object will be returned to the pool it was acquired from. If this object is not associated with a pool,
     * {@link #releaseResources()} will be called instead.
     *
     * @param reference The non-{@code null} reference to remove.
     * @throws NullPointerException If the given reference is {@code null}.
     * @throws X If an exception is thrown when calling {@link #releaseResources()}.
     * @see #addReference(Object)
     */
    protected final void removeReference(Object reference) throws X {
        Objects.requireNonNull(reference);
        if (references.remove(reference)) {
            logger.decreasedObjectRefCount(this, references.size());

            if (references.isEmpty()) {
                if (pool != null) {
                    pool.returnToPool(this);
                } else {
                    releaseResources();
                }
            }
        }
    }

    /**
     * Checks whether or not this object is still valid.
     * Invalid object will be removed from the pool instead of being returned from {@link Pool#acquire()} or {@link Pool#acquireNow()}.
     * They will also have their {@linkplain #releaseResources() resources released}.
     *
     * @return {@code true} if this object is still valid, or {@code false} otherwise.
     */
    protected abstract boolean validate();

    /**
     * Releases any resources associated with this object.
     *
     * @throws X If the resources could not be released.
     */
    protected abstract void releaseResources() throws X;

    void acquired() {
        addReference(this);
    }

    /**
     * Releases this object. If no more {@linkplain #addReference(Object) references} remain, this object will be returned to the pool it was acquired
     * from. If this object is not associated with a pool, {@link #releaseResources()} will be called instead.
     *
     * @throws X If an exception is thrown when calling {@link #releaseResources()}.
     */
    protected void release() throws X {
        removeReference(this);
    }

    /**
     * Logs a custom event for this object at debug level.
     * The message should preferably be a compile-time constant; for calculated messages, use {@link #logEvent(Supplier)} instead.
     * <p>
     * Note: this method will use the logger of the {@link Pool} that manages this object. If this object is not pooled, this method will do nothing.
     *
     * @param message The event message.
     */
    protected final void logEvent(String message) {
        logEvent(LogLevel.DEBUG, message);
    }

    /**
     * Logs a custom event for this object.
     * The message should preferably be a compile-time constant; for calculated messages, use {@link #logEvent(LogLevel, Supplier)} instead.
     * <p>
     * Note: this method will use the logger of the {@link Pool} that manages this object. If this object is not pooled, this method will do nothing.
     *
     * @param level The log level to use.
     * @param message The event message.
     */
    protected final void logEvent(LogLevel level, String message) {
        logger.objectEvent(level, this, message);
    }

    /**
     * Logs a custom event for this object at debug level.
     * <p>
     * Note: this method will use the logger of the {@link Pool} that manages this object. If this object is not pooled, this method will do nothing.
     *
     * @param messageSupplier A supplier for the event message.
     */
    protected final void logEvent(Supplier<String> messageSupplier) {
        logEvent(LogLevel.DEBUG, messageSupplier);
    }

    /**
     * Logs a custom event for this object.
     * <p>
     * Note: this method will use the logger of the {@link Pool} that manages this object. If this object is not pooled, this method will do nothing.
     *
     * @param level The log level to use.
     * @param messageSupplier A supplier for the event message.
     */
    protected final void logEvent(LogLevel level, Supplier<String> messageSupplier) {
        logger.objectEvent(level, this, messageSupplier);
    }

    /**
     * Returns whether or not logging at a specific level is enabled.
     * This can be used to perform conditional configuration, like adding logging listeners conditionally.
     * <p>
     * Note: this method will use the logger of the {@link Pool} that manages this object. If this object is not pooled, this method will return
     * {@code false}.
     *
     * @param level The level to check.
     * @return {@code true} if logging at the given level is enabled, or {@code false} otherwise.
     */
    protected final boolean isEnabled(LogLevel level) {
        return logger.isEnabled(level);
    }
}
