/*
 * Pool.java
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

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * A simple object pool implementation.
 * <p>
 * Instances of this class are thread-safe.
 *
 * @author Rob Spoor
 * @param <T> The type of objects in the pool.
 * @param <X> The type of exception that operations on objects in the pool can throw.
 */
public final class Pool<T extends PoolableObject<X>, X extends Exception> {

    private static final Supplier<NoSuchElementException> DEFAULT_ERROR_SUPPLIER = () -> new NoSuchElementException(
            Messages.Pool.maxWaitTimeExpired.get());

    private final PoolConfig config;
    private final PoolableObjectFactory<T, X> factory;
    private final PoolLogger logger;

    private final Deque<T> idleObjects;
    private int size;

    private final Lock lock;
    private final Condition notEmpty;

    private final AtomicBoolean active;

    /**
     * Creates a new pool.
     *
     * @param config The configuration to use.
     * @param factory The object factory to use.
     * @throws NullPointerException If the given configuration or factory is {@code null}.
     * @throws X If the given config requests a positive initial size, and one or more objects could not be created.
     */
    public Pool(PoolConfig config, PoolableObjectFactory<T, X> factory) throws X {
        this(config, factory, null);
    }

    /**
     * Creates a new pool.
     *
     * @param config The configuration to use.
     * @param factory The object factory to use.
     * @param logger The optional logger to use to log events triggered by the pool or pooled objects.
     * @throws NullPointerException If the given configuration or factory is {@code null}.
     * @throws X If the given config requests a positive initial size, and one or more objects could not be created.
     */
    public Pool(PoolConfig config, PoolableObjectFactory<T, X> factory, PoolLogger logger) throws X {
        this.config = Objects.requireNonNull(config);
        this.factory = Objects.requireNonNull(factory);
        this.logger = logger != null ? logger : PoolLogger.noopLogger();

        idleObjects = new ArrayDeque<>(config.maxSize());
        size = 0;

        lock = new ReentrantLock();
        notEmpty = lock.newCondition();

        fillPool();

        active = new AtomicBoolean(true);
    }

    PoolLogger logger() {
        return logger;
    }

    private void fillPool() throws X {
        logger.creatingPool(config);

        int initialSize = config.initialSize();
        List<T> objects = new ArrayList<>(initialSize);
        try {
            for (int i = 0; i < initialSize; i++) {
                objects.add(createObject());
            }
        } catch (Exception e) {
            // Creating the initial objects failed, cleanup all resources
            logger.failedToCreatePool(e);
            for (T object : objects) {
                try {
                    object.releaseResources();
                } catch (Exception e2) {
                    e.addSuppressed(e2);
                }
            }
            throw cast(e);
        }

        lock.lock();
        try {
            idleObjects.addAll(objects);
            size = objects.size();
        } finally {
            lock.unlock();
        }

        logger.createdPool(config);
    }

    /**
     * Acquires an object. This method will block until an object is available or the maximum wait time, as defined in the configuration used to
     * create this pool, expires. If the configured maximum wait time is negative, this method will block until an object is available.
     *
     * @return The acquired object.
     * @throws X If an error occurs while acquiring an object.
     * @throws NoSuchElementException If the maximum wait time expires before an object could be acquired.
     * @throws InterruptedException If the current thread is interrupted while acquiring an object.
     * @throws IllegalStateException If this pool has {@linkplain #shutdown shut down}.
     */
    public T acquire() throws X, InterruptedException {
        return acquire(DEFAULT_ERROR_SUPPLIER);
    }

    /**
     * Acquires an object. This method will block until an object is available or the maximum wait time expires.
     *
     * @param maxWaitTime The maximum wait time. If {@linkplain Duration#isNegative() negative}, this method will block until an object is available.
     * @return The acquired object.
     * @throws NullPointerException If the given maximum wait time is {@code null}.
     * @throws X If an error occurs while acquiring an object.
     * @throws NoSuchElementException If the maximum wait time expires before an object could be acquired.
     * @throws InterruptedException If the current thread is interrupted while acquiring an object.
     * @throws IllegalStateException If this pool has {@linkplain #shutdown shut down}.
     */
    public T acquire(Duration maxWaitTime) throws X, InterruptedException {
        return acquire(maxWaitTime, DEFAULT_ERROR_SUPPLIER);
    }

    /**
     * Acquires an object. This method will block until an object is available or the maximum wait time expires.
     *
     * @param maxWaitTime The maximum wait time. If negative, this method will block until an object is available.
     * @param timeUnit The time unit for the maximum wait time.
     * @return The acquired object.
     * @throws NullPointerException If the given time unit is {@code null}.
     * @throws X If an error occurs while acquiring an object.
     * @throws NoSuchElementException If the maximum wait time expires before an object could be acquired.
     * @throws InterruptedException If the current thread is interrupted while acquiring an object.
     * @throws IllegalStateException If this pool has {@linkplain #shutdown shut down}.
     */
    public T acquire(long maxWaitTime, TimeUnit timeUnit) throws X, InterruptedException {
        return acquire(maxWaitTime, timeUnit, DEFAULT_ERROR_SUPPLIER);
    }

    /**
     * Acquires an object. This method will block until an object is available or the maximum wait time, as defined in the configuration used to
     * create this pool, expires. If the configured maximum wait time is negative, this method will block until an object is available.
     *
     * @param <E> The type of exception to throw if the maximum wait time expires.
     * @param errorSupplier A supplier for the exception to throw if the maximum wait time expires.
     * @return The acquired object.
     * @throws NullPointerException If the given supplier is {@code null} and the maximum wait time expires.
     * @throws X If an error occurs while acquiring an object.
     * @throws E If the maximum wait time expires before an object could be acquired.
     * @throws InterruptedException If the current thread is interrupted while acquiring an object.
     * @throws IllegalStateException If this pool has {@linkplain #shutdown shut down}.
     */
    public <E extends Exception> T acquire(Supplier<E> errorSupplier) throws X, E, InterruptedException {
        Duration maxWaitTime = config.maxWaitTime();
        return acquire(maxWaitTime, errorSupplier);
    }

    /**
     * Acquires an object. This method will block until an object is available or the maximum wait time expires.
     *
     * @param <E> The type of exception to throw if the maximum wait time expires.
     * @param maxWaitTime The maximum wait time. If {@linkplain Duration#isNegative() negative}, this method will block until an object is available.
     * @param errorSupplier A supplier for the exception to throw if the maximum wait time expires.
     * @return The acquired object.
     * @throws NullPointerException If the given maximum wait time is {@code null},
     *                                  or if the given supplier is {@code null} and the maximum wait time expires.
     * @throws X If an error occurs while acquiring an object.
     * @throws E If the maximum wait time expires before an object could be acquired.
     * @throws InterruptedException If the current thread is interrupted while acquiring an object.
     * @throws IllegalStateException If this pool has {@linkplain #shutdown shut down}.
     */
    public <E extends Exception> T acquire(Duration maxWaitTime, Supplier<E> errorSupplier) throws X, E, InterruptedException {
        long maxWaitTimeInNanos = maxWaitTime.toNanos();
        return acquireBlocking(maxWaitTimeInNanos, errorSupplier);
    }

    /**
     * Acquires an object. This method will block until an object is available or the maximum wait time expires.
     *
     * @param <E> The type of exception to throw if the maximum wait time expires.
     * @param maxWaitTime The maximum wait time. If negative, this method will block until an object is available.
     * @param timeUnit The time unit for the maximum wait time.
     * @param errorSupplier A supplier for the exception to throw if the maximum wait time expires.
     * @return The acquired object.
     * @throws NullPointerException If the given time unit is {@code null}.
     *                                  or if the given supplier is {@code null} and the maximum wait time expires.
     * @throws X If an error occurs while acquiring an object.
     * @throws E If the maximum wait time expires before an object could be acquired.
     * @throws InterruptedException If the current thread is interrupted while acquiring an object.
     * @throws IllegalStateException If this pool has {@linkplain #shutdown shut down}.
     */
    public <E extends Exception> T acquire(long maxWaitTime, TimeUnit timeUnit, Supplier<E> errorSupplier) throws X, E, InterruptedException {
        long maxWaitTimeInNanos = timeUnit.toNanos(maxWaitTime);
        return acquireBlocking(maxWaitTimeInNanos, errorSupplier);
    }

    private <E extends Exception> T acquireBlocking(long maxWaitTimeInNanos, Supplier<E> errorSupplier) throws X, E, InterruptedException {
        checkActive();

        ObjectSupplier<T, X> supplier;

        lock.lock();
        try {
            supplier = acquireBlocking(maxWaitTimeInNanos);
        } finally {
            lock.unlock();
        }

        if (supplier != null) {
            return supplier.get();
        }

        checkActive();

        throw errorSupplier.get();
    }

    private ObjectSupplier<T, X> acquireBlocking(long maxWaitTimeInNanos) throws InterruptedException {
        ObjectSupplier<T, X> supplier = findAvailableObject();
        if (supplier != null) {
            return supplier;
        }

        // There are no available idle objects and there is no space in the pool to create one.
        // Wait for an idle object to become available or for space in the pool to create one.

        awaitPoolNotEmpty(maxWaitTimeInNanos);

        // either !idleObjects.isEmpty(), or size < maxSize, or this pool is no longer active, or the max wait time expired

        checkActive();

        supplier = findAvailableObject();
        if (supplier != null) {
            return supplier;
        }

        // The max wait time expired.
        return null;
    }

    /**
     * Acquires an object if possible. This method will not block if no objects are available.
     *
     * @return An {@link Optional} describing the acquired object, or {@link Optional#empty()} if no object was available.
     * @throws X If an error occurs while acquiring an object.
     * @throws IllegalStateException If this pool has {@linkplain #shutdown shut down}.
     */
    public Optional<T> acquireNow() throws X {
        checkActive();

        ObjectSupplier<T, X> supplier;

        lock.lock();
        try {
            supplier = findAvailableObject();
        } finally {
            lock.unlock();
        }

        return supplier != null
                ? Optional.of(supplier.get())
                : Optional.empty();
    }

    /**
     * Acquires an object if possible. This method will not block if no objects are available; instead, a new object will be created that will
     * <em>not</em> be returned to the pool. This can be used in cases where an object is necessary right now, and blocking could lead to deadlock or
     * similar issues.
     *
     * @return The acquired or created object.
     * @throws X If an error occurs while acquiring an object.
     * @throws IllegalStateException If this pool has {@linkplain #shutdown shut down}.
     */
    public T acquireOrCreate() throws X {
        T object = acquireNow().orElse(null);
        if (object == null) {
            object = factory.newObject();
            logger.createdNonPooledObject(object.objectId());
            object.acquired();
            // The call to object.acquired() allows it to release resources once it has been released
            // Don't log acquired; the object is not part of the pool
        }
        return object;
    }

    private ObjectSupplier<T, X> findAvailableObject() {
        T object = findValidObject();
        if (object != null) {
            // object is valid
            int idleCount = idleObjects.size();
            int poolSize = size;
            return () -> {
                object.acquired();
                logger.acquiredObject(object.objectId(), idleCount, poolSize);
                return object;
            };
        }
        if (size < config.maxSize()) {
            // Increase size now so other threads won't be able to exceed the maximum size.
            // If an error occurs when creating the object, the size will be automatically decreased again.
            size++;
            int idleCount = idleObjects.size();
            int poolSize = size;
            return () -> {
                try {
                    T newObject = createObject();
                    newObject.acquired();
                    logger.acquiredObject(newObject.objectId(), idleCount, poolSize);
                    return newObject;

                } catch (Exception e) {
                    decreaseSize();
                    throw cast(e);
                }
            };
        }
        return null;
    }

    private T findValidObject() {
        T object;
        boolean removedObjects = false;
        while ((object = idleObjects.poll()) != null) {
            if (!object.isValid()) {
                size--;
                object.clearPool();
                logger.objectInvalidated(object.objectId(), idleObjects.size(), size);
                removedObjects = true;
            } else if (config.maxIdleTimeExceeded(object)) {
                size--;
                logger.objectIdleTooLong(object.objectId(), idleObjects.size(), size);
                object.releaseResourcesQuietly();
                object.clearPool();
                removedObjects = true;
            } else {
                return object;
            }
        }
        // no valid objects in the pool

        if (removedObjects) {
            // new objects can possibly be created
            notEmpty.signalAll();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private T createObject() throws X {
        T object = factory.newObject();
        object.setPool((Pool<PoolableObject<X>, X>) this);
        logger.createdObject(object.objectId());
        return object;
    }

    private void awaitPoolNotEmpty(long maxWaitTimeInNanos) throws InterruptedException {
        if (maxWaitTimeInNanos < 0) {
            awaitPoolNotEmptyWithoutTimeout();
        } else {
            awaitPoolNotEmptyWithTimeout(maxWaitTimeInNanos);
        }
    }

    private void awaitPoolNotEmptyWithoutTimeout() throws InterruptedException {
        while (idleObjects.isEmpty() && size >= config.maxSize() && isActive()) {
            notEmpty.await();
        }
    }

    private void awaitPoolNotEmptyWithTimeout(long maxWaitTimeInNanos) throws InterruptedException {
        long nanos = maxWaitTimeInNanos;
        while (idleObjects.isEmpty() && size >= config.maxSize() && isActive()) {
            if (nanos <= 0) {
                // let the caller check again for pool.isEmpty() and poolSize < maxPoolSize
                return;
            }
            nanos = notEmpty.awaitNanos(nanos);
        }
    }

    private void decreaseSize() {
        lock.lock();
        try {
            size--;
            notEmpty.signalAll();

        } finally {
            lock.unlock();
        }
    }

    void returnToPool(T object) {
        assert object.referenceCount() == 0;

        lock.lock();
        try {
            if (isActive() && object.isValid()) {
                object.resetIdleSince();
                idleObjects.add(object);
                logger.returnedObject(object.objectId(), idleObjects.size(), size);
            } else {
                object.clearPool();
                size--;
                logger.objectInvalidated(object.objectId(), idleObjects.size(), size);
                if (!isActive()) {
                    object.releaseResourcesQuietly();
                }
            }
            // Either idleObjects has had an object added or size has been decreased.
            notEmpty.signalAll();

        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs an operation on all idle objects. These will be acquired in bulk, after which the operation is run on them sequentially. Afterwards, each
     * object will be returned to the pool.
     *
     * @param action The operation to run.
     * @throws X If an error occurs while running the operation on any of the idle objects.
     * @throws NullPointerException If the given operation is {@code null}.
     */
    public void forAllIdleObjects(PoolableObjectConsumer<T, X> action) throws X {
        Objects.requireNonNull(action);

        List<T> objects = drainPool(false);

        Exception exception = null;
        for (T object : objects) {
            try {
                action.accept(object);
            } catch (Exception e) {
                exception = add(exception, e);
            } finally {
                returnToPool(object);
            }
        }
        if (exception != null) {
            throw cast(exception);
        }
    }

    /**
     * Returns whether or not this pool is active.
     *
     * @return {@code true} if this pool is active, or {@code false} if it has {@linkplain #shutdown() shut down}.
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * Shuts down this pool. For all idle object the resources are {@linkplain PoolableObject#releaseResources()} released, and it will no longer be
     * possible to acquire new objects. Any already acquired objects will remain valid until they are returned to the pool.
     *
     * @throws X If an error occurs while releasing resources for any of the idle objects.
     */
    public void shutdown() throws X {
        if (active.compareAndSet(true, false)) {
            try {
                releaseIdleResources();

                lock.lock();
                try {
                    notEmpty.signalAll();
                } finally {
                    lock.unlock();
                }
            } finally {
                logger.shutDownPool();
            }
        }
    }

    private void releaseIdleResources() throws X {
        List<T> objects = drainPool(true);

        Exception exception = null;
        for (T object : objects) {
            try {
                object.releaseResources();
            } catch (Exception e) {
                exception = add(exception, e);
            }
            object.clearPool();
        }
        if (exception != null) {
            throw cast(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private X cast(Exception exception) {
        // Exception will either be compatible with X, or an unchecked exception. That means this unsafe cast is allowed.
        return (X) exception;
    }

    private List<T> drainPool(boolean updateSize) {
        lock.lock();
        try {
            List<T> objects = new ArrayList<>(idleObjects);

            idleObjects.clear();
            if (updateSize && !objects.isEmpty()) {
                size -= objects.size();
                notEmpty.signalAll();
            }
            logger.drainedPool(size);

            return objects;

        } finally {
            lock.unlock();
        }
    }

    private Exception add(Exception existing, Exception e) {
        if (existing == null) {
            return e;
        }
        existing.addSuppressed(e);
        return existing;
    }

    private void checkActive() {
        if (!isActive()) {
            throw new IllegalStateException(Messages.Pool.notActive.get());
        }
    }

    /**
     * Creates a new pool that throws no exceptions.
     *
     * @param <T> The type of objects in the pool.
     * @param config The configuration to use.
     * @param supplier A supplier to serve as object factory.
     * @return The created pool.
     * @throws NullPointerException If the given configuration or supplier is {@code null}.
     */
    public static <T extends PoolableObject<None>> Pool<T, None> throwingNone(PoolConfig config, Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        return new Pool<>(config, supplier::get);
    }

    /**
     * Creates a new pool that throws no exceptions.
     *
     * @param <T> The type of objects in the pool.
     * @param config The configuration to use.
     * @param supplier A supplier to serve as object factory.
     * @param logger The optional logger to use to log events triggered by the pool or pooled objects.
     * @return The created pool.
     * @throws NullPointerException If the given configuration or supplier is {@code null}.
     */
    public static <T extends PoolableObject<None>> Pool<T, None> throwingNone(PoolConfig config, Supplier<T> supplier, PoolLogger logger) {
        Objects.requireNonNull(supplier);
        return new Pool<>(config, supplier::get, logger);
    }

    private interface ObjectSupplier<T extends PoolableObject<X>, X extends Exception> {

        T get() throws X;
    }
}
