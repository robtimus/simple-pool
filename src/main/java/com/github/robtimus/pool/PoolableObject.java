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
    private long poolTimestamp;

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
        poolTimestamp = System.currentTimeMillis();
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

    void resetPoolTimestamp() {
        poolTimestamp = System.currentTimeMillis();
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
            logger.increasedObjectRefCount(objectId, references.size());
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
            logger.decreasedObjectRefCount(objectId, references.size());

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
     * Returns whether or not this object is still valid.
     * Invalid object will be removed from the pool instead of being returned from {@link Pool#acquire()} or {@link Pool#acquireNow()}.
     *
     * @return {@code true} if this object is still valid, or {@code false} otherwise.
     */
    protected abstract boolean isValid();

    boolean isIdleTooLong() {
        if (pool != null) {
            long idleTime = System.currentTimeMillis() - poolTimestamp;
            return idleTime > pool.maxIdleTimeMillis();
        }
        return false;
    }

    /**
     * Releases any resources associated with this object.
     *
     * @throws X If the resources could not be released.
     */
    protected final void releaseResources() throws X {
        logger.releasingObjectResources(objectId);
        doReleaseResources();
        logger.releasedObjectResources(objectId);
    }

    /**
     * Releases any resources associated with this object, without throwing exceptions.
     */
    protected final void releaseResourcesQuietly() {
        logger.releasingObjectResources(objectId);
        doReleaseResourcesQuietly();
        logger.releasedObjectResources(objectId);
    }

    /**
     * Releases any resources associated with this object.
     *
     * @throws X If the resources could not be released.
     */
    protected abstract void doReleaseResources() throws X;

    /**
     * Releases any resources associated with this object, without throwing any exceptions.
     * <p>
     * This implementation calls {@link #doReleaseResources()}, catching and ignoring any exceptions. Sub classes should override this method if
     * {@link #doReleaseResources()} contains any try-catch-throw logic, to prevent unnecessarily creating a new exception.
     */
    protected void doReleaseResourcesQuietly() {
        try {
            doReleaseResources();
        } catch (Exception e) {
            releaseResourcesFailed(e);
        }
    }

    /**
     * Logs an event that releasing resources associated with this object failed.
     * This method should be called from {@link #doReleaseResourcesQuietly()} if an exception occurs.
     *
     * @param exception The exception that was thrown while quietly releasing the resources associated to this object.
     */
    protected final void releaseResourcesFailed(Exception exception) {
        logger.releaseObjectResourcesFailed(objectId, exception);
    }

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
     * Logs a custom event for this object.
     * The message should preferably be a compile-time constant; for calculated messages, use {@link #logEvent(Supplier)} instead.
     *
     * @param message The event message.
     */
    protected final void logEvent(String message) {
        logger.objectEvent(objectId, message);
    }

    /**
     * Logs a custom event for this object.
     *
     * @param messageSupplier A supplier for the event message.
     */
    protected final void logEvent(Supplier<String> messageSupplier) {
        logger.objectEvent(objectId, messageSupplier);
    }
}
