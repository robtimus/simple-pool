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
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A logger for events triggered by a {@link Pool} or {@link PoolableObject}.
 * <p>
 * Instances of this class are thread-safe.
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
            logger.debug(Messages.PoolLogger.creatingPool(messagePrefix, config));
        }
    }

    /**
     * Called when a {@link Pool} has been created.
     *
     * @param config The configuration for the {@link Pool}.
     */
    public void createdPool(PoolConfig config) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.createdPool(messagePrefix, config));
        }
    }

    /**
     * Called when a {@link Pool} could not be created.
     *
     * @param exception The exception that was thrown while creating the {@link Pool}.
     */
    public void failedToCreatePool(Exception exception) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.failedToCreatePool(messagePrefix), exception);
        }
    }

    /**
     * Called when a {@link Pool} is drained. This is like acquiring all available idle {@link PoolableObject}s in one call.
     *
     * @param poolSize The total pool size - the number of acquired {@link PoolableObject}s.
     */
    public void drainedPool(int poolSize) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.drainedPool(messagePrefix, poolSize));
        }
    }

    /**
     * Called when a {@link Pool} has shut down.
     */
    public void shutDownPool() {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.shutDownPool(messagePrefix));
        }
    }

    // poolable object related

    /**
     * Called when a new {@link PoolableObject} has been created.
     *
     * @param object The {@link PoolableObject}.
     */
    public void createdObject(PoolableObject<?> object) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.createdObject(messagePrefix, objectPrefix, object.objectId()));
        }
    }

    /**
     * Called when a new {@link PoolableObject} has been created that will not be returned to the pool it was
     * {@linkplain Pool#acquireOrCreate() acquired} from.
     *
     * @param object The {@link PoolableObject}.
     */
    public void createdNonPooledObject(PoolableObject<?> object) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.createdNonPooledObject(messagePrefix, objectPrefix, object.objectId()));
        }
    }

    /**
     * Called when an additional reference was added to a {@link PoolableObject}.
     *
     * @param object The {@link PoolableObject}.
     * @param refCount The new number of references, including the {@link PoolableObject} itself.
     * @see PoolableObject#addReference()
     */
    public void increasedObjectRefCount(PoolableObject<?> object, int refCount) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.increasedObjectRefCount(messagePrefix, objectPrefix, object.objectId(), refCount));
        }
    }

    /**
     * Called when a reference was removed from a {@link PoolableObject}.
     *
     * @param object The {@link PoolableObject}.
     * @param refCount The new number of references, including the {@link PoolableObject} itself.
     * @see PoolableObject.Reference#remove()
     */
    public void decreasedObjectRefCount(PoolableObject<?> object, int refCount) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.decreasedObjectRefCount(messagePrefix, objectPrefix, object.objectId(), refCount));
        }
    }

    /**
     * Called before the resources of a {@link PoolableObject} will be released.
     *
     * @param object The {@link PoolableObject}.
     * @see PoolableObject#releaseResources()
     */
    public void releasingObjectResources(PoolableObject<?> object) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.releasingObjectResources(messagePrefix, objectPrefix, object.objectId()));
        }
    }

    /**
     * Called when the resources of a {@link PoolableObject} have been released.
     *
     * @param object The {@link PoolableObject}.
     * @see PoolableObject#releaseResources()
     */
    public void releasedObjectResources(PoolableObject<?> object) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.releasedObjectResources(messagePrefix, objectPrefix, object.objectId()));
        }
    }

    /**
     * Called when an error occurs when quietly releasing the resources of a {@link PoolableObject}.
     *
     * @param object The {@link PoolableObject}.
     * @param exception The exception that was thrown while quietly releasing the resources of the {@link PoolableObject}.
     * @see PoolableObject#releaseResources()
     */
    public void releaseObjectResourcesFailed(PoolableObject<?> object, Exception exception) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.releaseObjectResourcesFailed(messagePrefix, objectPrefix, object.objectId()), exception);
        }
    }

    /**
     * Called when a {@link PoolableObject} is acquired from a {@link Pool}.
     *
     * @param object The {@link PoolableObject}.
     * @param idleCount The number of idle {@link PoolableObject}s after acquiring the {@link PoolableObject}.
     * @param poolSize The total pool size - the number of acquired and idle {@link PoolableObject}s combined.
     */
    public void acquiredObject(PoolableObject<?> object, int idleCount, int poolSize) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.acquiredObject(messagePrefix, objectPrefix, object.objectId(), idleCount, poolSize));
        }
    }

    /**
     * Called when a {@link PoolableObject} is returned to a {@link Pool}.
     *
     * @param object The {@link PoolableObject}.
     * @param idleCount The number of idle {@link PoolableObject}s after returning the {@link PoolableObject}.
     * @param poolSize The total pool size - the number of acquired and idle {@link PoolableObject}s combined.
     */
    public void returnedObject(PoolableObject<?> object, int idleCount, int poolSize) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.returnedObject(messagePrefix, objectPrefix, object.objectId(), idleCount, poolSize));
        }
    }

    /**
     * Called when a {@link PoolableObject} is no longer valid and has been removed from its {@link Pool}.
     *
     * @param object The {@link PoolableObject}.
     * @param idleCount The number of idle {@link PoolableObject}s after invalidating the {@link PoolableObject}.
     * @param poolSize The total pool size - the number of acquired and idle {@link PoolableObject}s combined.
     * @see PoolableObject#validate()
     */
    public void objectInvalidated(PoolableObject<?> object, int idleCount, int poolSize) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.objectInvalidated(messagePrefix, objectPrefix, object.objectId(), idleCount, poolSize));
        }
    }

    /**
     * Called when a {@link PoolableObject} has been idle for too long.
     *
     * @param object The {@link PoolableObject}.
     * @param idleCount The number of idle {@link PoolableObject}s after invalidating the {@link PoolableObject}.
     * @param poolSize The total pool size - the number of acquired and idle {@link PoolableObject}s combined.
     */
    public void objectIdleTooLong(PoolableObject<?> object, int idleCount, int poolSize) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.PoolLogger.objectIdleTooLong(messagePrefix, objectPrefix, object.objectId(), idleCount, poolSize));
        }
    }

    /**
     * Called when {@link PoolableObject#logEvent(LogLevel, String)} is called.
     *
     * @param level The log level to use.
     * @param object The {@link PoolableObject}.
     * @param message The event message.
     */
    public void objectEvent(LogLevel level, PoolableObject<?> object, String message) {
        if (level.isEnabled(logger)) {
            level.log(logger, Messages.PoolLogger.objectEvent(messagePrefix, objectPrefix, object.objectId(), message));
        }
    }

    /**
     * Called when {@link PoolableObject#logEvent(LogLevel, Supplier)} is called.
     *
     * @param level The log level to use.
     * @param object The {@link PoolableObject}.
     * @param messageSupplier A supplier for the event message.
     */
    public void objectEvent(LogLevel level, PoolableObject<?> object, Supplier<String> messageSupplier) {
        if (level.isEnabled(logger)) {
            level.log(logger, Messages.PoolLogger.objectEvent(messagePrefix, objectPrefix, object.objectId(), messageSupplier.get()));
        }
    }

    /**
     * Returns whether or not logging at a specific level is enabled.
     * This can be used to perform conditional configuration, like adding logging listeners conditionally.
     *
     * @param level The level to check.
     * @return {@code true} if logging at the given level is enabled, or {@code false} otherwise.
     */
    public boolean isEnabled(LogLevel level) {
        return level.isEnabled(logger);
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return getClass().getSimpleName()
                + "[logger=" + logger.getName()
                + ",messagePrefix=" + messagePrefix
                + ",objectPrefix=" + objectPrefix
                + "]";
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
        public Builder withMessagePrefix(String messagePrefix) {
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
