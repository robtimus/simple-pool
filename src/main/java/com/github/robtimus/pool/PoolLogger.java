/*
 * PoolLogger.java
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A logger for events triggered by a {@link Pool} or {@link PoolableObject}.
 * <p>
 * This class needs SLF4J to be present on the class path to work. If it isn't nothing will be logged at all.
 *
 * @author Rob Spoor
 */
public class PoolLogger {

    private static final PoolLogger DEFAULT_LOGGER = custom().build();

    private static final String DEFAULT_MESSAGE_PREFIX = ""; //$NON-NLS-1$
    private static final String DEFAULT_OBJECT_PREFIX = "object-"; //$NON-NLS-1$

    private final Logger logger;

    private final String messagePrefix;
    private final String objectPrefix;

    PoolLogger(Logger logger, String messagePrefix, String objectPrefix) {
        this.logger = logger;
        this.messagePrefix = messagePrefix;
        this.objectPrefix = objectPrefix;
    }

    PoolLogger() {
        this(null, DEFAULT_MESSAGE_PREFIX, DEFAULT_OBJECT_PREFIX);
    }

    final Logger logger() {
        return logger;
    }

    final String messagePrefix() {
        return messagePrefix;
    }

    final String objectPrefix() {
        return objectPrefix;
    }

    // pool related

    /**
     * Called when a {@link Pool} is being created.
     *
     * @param config The configuration for the {@link Pool}.
     */
    public void creatingPool(PoolConfig config) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.creatingPool.get(messagePrefix, config));
        }
    }

    /**
     * Called when a {@link Pool} has been created.
     *
     * @param config The configuration for the {@link Pool}.
     */
    public void createdPool(PoolConfig config) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.createdPool.get(messagePrefix, config));
        }
    }

    /**
     * Called when a {@link Pool} could not be created.
     *
     * @param exception The exception that was thrown while creating the {@link Pool}.
     */
    public void failedToCreatePool(Exception exception) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.failedToCreatePool.get(messagePrefix), exception);
        }
    }

    /**
     * Called when a {@link Pool} is drained. This is like acquiring all available idle {@link PoolableObject}s in one call.
     *
     * @param poolSize The total pool size - the number of acquired {@link PoolableObject}s.
     */
    public void drainedPool(int poolSize) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.drainedPool.get(messagePrefix, poolSize));
        }
    }

    /**
     * Called when a {@link Pool} has shut down.
     */
    public void shutDownPool() {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.shutDownPool.get(messagePrefix));
        }
    }

    // poolable object related

    /**
     * Called when a new {@link PoolableObject} has been created.
     *
     * @param objectId The id of the {@link PoolableObject}.
     */
    public void createdObject(long objectId) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.createdObject.get(messagePrefix, objectPrefix, objectId));
        }
    }

    /**
     * Called when a new {@link PoolableObject} has been created that will not be returned to the pool it was
     * {@linkplain Pool#acquireOrCreate() acquired} from.
     *
     * @param objectId The id of the {@link PoolableObject}.
     */
    public void createdNonPooledObject(long objectId) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.createdNonPooledObject.get(messagePrefix, objectPrefix, objectId));
        }
    }

    /**
     * Called when an additional reference was added to a {@link PoolableObject}.
     *
     * @param objectId The id of the {@link Pool}.
     * @param refCount The new number of references, including the {@link PoolableObject} itself.
     * @see PoolableObject#addReference(Object)
     */
    public void increasedRefCount(long objectId, int refCount) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.increasedRefCount.get(messagePrefix, objectPrefix, objectId, refCount));
        }
    }

    /**
     * Called when a reference was removed from a {@link PoolableObject}.
     *
     * @param objectId The id of the {@link PoolableObject}.
     * @param refCount The new number of references, including the {@link PoolableObject} itself.
     * @see PoolableObject#removeReference(Object)
     */
    public void decreasedRefCount(long objectId, int refCount) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.decreasedRefCount.get(messagePrefix, objectPrefix, objectId, refCount));
        }
    }

    /**
     * Called when the resources of a {@link PoolableObject} have been released.
     *
     * @param objectId The id of the {@link PoolableObject}.
     * @see PoolableObject#releaseResources()
     * @see PoolableObject#releaseResourcesQuietly()
     */
    public void releasedResources(long objectId) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.releasedResources.get(messagePrefix, objectPrefix, objectId));
        }
    }

    /**
     * Called when a {@link PoolableObject} is acquired from a {@link Pool}.
     *
     * @param objectId The id of the {@link PoolableObject}.
     * @param idleCount The number of idle {@link PoolableObject}s after acquiring the {@link PoolableObject}.
     * @param poolSize The total pool size - the number of acquired and idle {@link PoolableObject}s combined.
     */
    public void acquiredObject(long objectId, int idleCount, int poolSize) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.acquiredObject.get(messagePrefix, objectPrefix, objectId, idleCount, poolSize));
        }
    }

    /**
     * Called when a {@link PoolableObject} is returned to a {@link Pool}.
     *
     * @param objectId The id of the {@link PoolableObject}.
     * @param idleCount The number of idle {@link PoolableObject}s after returning the {@link PoolableObject}.
     * @param poolSize The total pool size - the number of acquired and idle {@link PoolableObject}s combined.
     */
    public void returnedObject(long objectId, int idleCount, int poolSize) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.returnedObject.get(messagePrefix, objectPrefix, objectId, idleCount, poolSize));
        }
    }

    /**
     * Called when a {@link PoolableObject} is no longer valid and has been removed from its {@link Pool}.
     *
     * @param objectId The id of the {@link PoolableObject}.
     * @param idleCount The number of idle {@link PoolableObject}s after invalidating the {@link PoolableObject}.
     * @param poolSize The total pool size - the number of acquired and idle {@link PoolableObject}s combined.
     * @see PoolableObject#isValid()
     */
    public void objectInvalidated(long objectId, int idleCount, int poolSize) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.objectInvalidated.get(messagePrefix, objectPrefix, objectId, idleCount, poolSize));
        }
    }

    /**
     * Called when a {@link PoolableObject} has been idle for too long.
     *
     * @param objectId The id of the {@link PoolableObject}.
     * @param idleCount The number of idle {@link PoolableObject}s after invalidating the {@link PoolableObject}.
     * @param poolSize The total pool size - the number of acquired and idle {@link PoolableObject}s combined.
     */
    public void objectIdleTooLong(long objectId, int idleCount, int poolSize) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.objectIdleTooLong.get(messagePrefix, objectPrefix, objectId, idleCount, poolSize));
        }
    }

    /**
     * Returns a default {@link PoolLogger} object. This has the same configuration as an object returned by {@code custom().build()}.
     *
     * @return A default {@link PoolLogger} object.
     */
    public static PoolLogger defaultLogger() {
        return DEFAULT_LOGGER;
    }

    /**
     * Returns a {@link PoolLogger} object that does not perform any actual logging.
     * This can be used as default value in case no logger should be used, instead of checking for {@code null}.
     *
     * @return A {@link PoolLogger} object that does not perform any actual logging.
     */
    public static PoolLogger noopLogger() {
        return NoopPoolLogger.INSTANCE;
    }

    /**
     * Returns a new builder for creating {@link PoolLogger} objects.
     *
     * @return A new builder for creating {@link PoolLogger} objects.
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * A builder for {@link PoolLogger} objects.
     *
     * @author Rob Spoor
     */
    public static final class Builder {

        private String loggerName;
        private String messagePrefix;
        private String objectPrefix;

        private Builder() {
            loggerName = Pool.class.getName();
            messagePrefix = DEFAULT_MESSAGE_PREFIX;
            objectPrefix = DEFAULT_OBJECT_PREFIX;
        }

        /**
         * Sets the name of the logger to use. The default is the name of the {@link Pool} class.
         *
         * @param loggerName The name of the logger to use.
         * @return This builder.
         * @throws NullPointerException If the given logger name is {@code null}.
         */
        public Builder withLoggerName(String loggerName) {
            this.loggerName = Objects.requireNonNull(loggerName);
            return this;
        }

        /**
         * Sets the name of the logger to use.
         * This method is shorthand for calling {@link #withLoggerName(String)} with the given class' fully qualified name.
         *
         * @param loggerClass The class to use for the name of the logger to use.
         * @return This builder.
         * @throws NullPointerException If the given logger class is {@code null}.
         */
        public Builder withLoggerClass(Class<?> loggerClass) {
            return withLoggerName(loggerClass.getName());
        }

        /**
         * Sets the message prefix to use. This is prepended to each logged message. The default is no prefix.
         *
         * @param messagePrefix The message prefix to use.
         * @return This builder.
         * @throws NullPointerException If the given message prefix is {@code null}.
         */
        public Builder withPrefix(String messagePrefix) {
            this.messagePrefix = Objects.requireNonNull(messagePrefix);
            return this;
        }

        /**
         * Sets the object prefix to use. This is prepended to object ids in each logged message. The default is no prefix.
         *
         * @param objectPrefix The object prefix to use.
         * @return This builder.
         * @throws NullPointerException If the given object prefix is {@code null}.
         */
        public Builder withObjectPrefix(String objectPrefix) {
            this.objectPrefix = Objects.requireNonNull(objectPrefix);
            return this;
        }

        /**
         * Creates a new {@link PoolLogger} object based on the settings of this builder.
         *
         * @return The created {@link PoolLogger} object.
         */
        public PoolLogger build() {
            Logger logger = getLogger(loggerName);
            return logger != null
                    ? new PoolLogger(logger, messagePrefix, objectPrefix)
                    : noopLogger();
        }

        private static Logger getLogger(String loggerName) {
            try {
                return LoggerFactory.getLogger(loggerName);
            } catch (@SuppressWarnings("unused") NoClassDefFoundError e) {
                return null;
            }
        }
    }
}
