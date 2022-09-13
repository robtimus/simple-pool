/*
 * PoolConfig.java
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
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for a {@link Pool}.
 * <p>
 * Instances of this class are immutable and thread-safe.
 *
 * @author Rob Spoor
 */
public final class PoolConfig {

    private static final PoolConfig DEFAULT_CONFIG = custom().build();

    private final Duration maxWaitTime;
    private final Duration maxIdleTime;
    private final long maxIdleTimeMillis;
    private final int initialSize;
    private final int maxSize;

    private PoolConfig(Builder builder) {
        maxWaitTime = builder.maxWaitTime;
        maxIdleTime = builder.maxIdleTime;
        maxIdleTimeMillis = maxIdleTime != null ? maxIdleTime.toMillis() : 0;
        initialSize = builder.initialSize;
        maxSize = builder.maxSize;
    }

    /**
     * Returns the maximum time to wait when acquiring objects.
     * If {@linkplain Duration#isNegative() negative}, acquiring objects should block until an object is available.
     *
     * @return The maximum time to wait when acquiring objects.
     */
    public Duration maxWaitTime() {
        return maxWaitTime;
    }

    /**
     * Returns the maximum time that objects can be idle.
     *
     * @return An {@link Optional} describing the maximum time that objects can be idle,
     *         or {@link Optional#empty()} if objects can be idle indefinitely.
     */
    public Optional<Duration> maxIdleTime() {
        return Optional.ofNullable(maxIdleTime);
    }

    boolean maxIdleTimeExceeded(PoolableObject<?> object) {
        return maxIdleTime != null && System.currentTimeMillis() - object.idleSince() > maxIdleTimeMillis;
    }

    /**
     * Returns the initial pool size. This is the number of idle objects to start with.
     *
     * @return The initial pool size.
     */
    public int initialSize() {
        return initialSize;
    }

    /**
     * Returns the maximum pool size. This is the maximum number of objects, both idle and currently in use.
     *
     * @return The maximum pool size.
     */
    public int maxSize() {
        return maxSize;
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return getClass().getSimpleName()
                + "[maxWaitTime=" + maxWaitTime
                + ",maxIdleTime=" + maxIdleTime
                + ",initialSize=" + initialSize
                + ",maxSize=" + maxSize
                + "]";
    }

    /**
     * Returns a default {@link PoolConfig} object. This has the same configuration as an object returned by {@code custom().build()}.
     *
     * @return A default {@link PoolConfig} object.
     * @see #custom()
     */
    public static PoolConfig defaultConfig() {
        return DEFAULT_CONFIG;
    }

    /**
     * Returns a new builder for creating {@link PoolConfig} objects.
     *
     * @return A new builder for creating {@link PoolConfig} objects.
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * A builder for {@link PoolConfig} objects.
     *
     * @author Rob Spoor
     */
    public static final class Builder {

        private static final Duration DEFAULT_MAX_WAIT_TIME = Duration.ofSeconds(-1);

        private Duration maxWaitTime;
        private Duration maxIdleTime;
        private int initialSize;
        private int maxSize;

        private Builder() {
            maxWaitTime = DEFAULT_MAX_WAIT_TIME;
            maxIdleTime = null;
            initialSize = 1;
            maxSize = 5;
        }

        /**
         * Sets the maximum time to wait when acquiring a default {@link PoolConfig} object. If {@linkplain Duration#isNegative() negative},
         * acquiring objects should block until an object is available. The default is a negative duration.
         *
         * @param maxWaitTime The maximum wait time.
         * @return This builder.
         * @throws NullPointerException If the given maximum wait time is {@code null}.
         */
        public Builder withMaxWaitTime(Duration maxWaitTime) {
            this.maxWaitTime = Objects.requireNonNull(maxWaitTime);
            return this;
        }

        /**
         * Sets the maximum time that objects can be idle. The default is indefinitely.
         *
         * @param maxIdleTime The maximum idle time, or {@code null} if objects can be idle indefinitely.
         * @return This builder.
         */
        public Builder withMaxIdleTime(Duration maxIdleTime) {
            this.maxIdleTime = maxIdleTime;
            return this;
        }

        /**
         * Sets the initial pool size. This is the number of idle objects to start with. The default is 1.
         * <p>
         * If the {@linkplain #withMaxSize(int) maximum pool size} is smaller than the given initial size, it will be set to be equal to the given
         * initial size.
         *
         * @param initialSize The initial pool size.
         * @return This builder.
         * @throws IllegalArgumentException If the initial size is negative.
         */
        public Builder withInitialSize(int initialSize) {
            if (initialSize < 0) {
                throw new IllegalArgumentException(initialSize + " < 0"); //$NON-NLS-1$
            }
            this.initialSize = initialSize;
            maxSize = Math.max(initialSize, maxSize);
            return this;
        }

        /**
         * Sets the maximum pool size. This is the maximum number of objects, both idle and currently in use. The default is 5.
         * <p>
         * If the {@linkplain #withInitialSize(int) initial pool size} is larger than the given maximum size, it will be set to be equal to the given
         * maximum size.
         *
         * @param maxSize The maximum pool size.
         * @return This builder.
         * @throws IllegalArgumentException If the given size is not positive.
         */
        public Builder withMaxSize(int maxSize) {
            if (maxSize <= 0) {
                throw new IllegalArgumentException(initialSize + " <= 0"); //$NON-NLS-1$
            }
            this.maxSize = maxSize;
            initialSize = Math.min(initialSize, maxSize);
            return this;
        }

        /**
         * Creates a new {@link PoolConfig} object based on the settings of this builder.
         *
         * @return The created {@link PoolConfig} object.
         */
        public PoolConfig build() {
            return new PoolConfig(this);
        }
    }
}
