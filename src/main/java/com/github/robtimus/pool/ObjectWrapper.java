/*
 * ObjectWrapper.java
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

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A poolable object that wraps a single value. Instances will always be considered {@linkplain #validate() valid} and have no resources to
 * {@linkplain #doReleaseResources() release}. This class implements {@link AutoCloseable}, which allows instances to be used in try-with-resources
 * blocks; when an instance is closed it is returned to the pool.
 *
 * @author Rob Spoor
 * @param <T> The type of wrapped objects.
 */
public final class ObjectWrapper<T> extends PoolableObject<None> implements AutoCloseable {

    private final T value;

    /**
     * Creates a new object wrapper.
     *
     * @param value The value to wrap.
     */
    public ObjectWrapper(T value) {
        this.value = value;
    }

    /**
     * The wrapped value.
     *
     * @return The wrapped value.
     */
    public T value() {
        return value;
    }

    @Override
    protected boolean validate() {
        return true;
    }

    @Override
    protected void doReleaseResources() throws None {
        // does nothing
    }

    @Override
    public void close() {
        release();
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return getClass().getSimpleName() + "<" + value + ">";
    }

    /**
     * Creates a new pool of {@link ObjectWrapper} instances.
     *
     * @param <T> The type of objects wrapped by {@link ObjectWrapper} instances in the pool.
     * @param config The configuration to use.
     * @param supplier A supplier to serve as factory for wrapped objects.
     * @param logger The logger to use to log events triggered by the pool or pooled objects.
     * @return The created pool.
     * @throws NullPointerException If the given configuration, supplier or logger is {@code null}.
     */
    public static <T> Pool<ObjectWrapper<T>, None> newPool(PoolConfig config, Supplier<T> supplier, PoolLogger logger) {
        Objects.requireNonNull(supplier);
        return Pool.throwingNone(config, () -> new ObjectWrapper<>(supplier.get()), logger);
    }
}
