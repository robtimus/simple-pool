/*
 * LogLevel.java
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

import org.slf4j.Logger;

/**
 * The available log levels.
 *
 * @author Rob Spoor
 */
public enum LogLevel {

    // This class doesn't use BiConsumers and Predicates in the constructor, because that results in NoClassDefFoundErrors if SLF4J is not available.
    // Instead, SLF4J is only needed when isEnabled or log is called, which can only be done when SLF4J is available (see PoolLogger).

    /** A log level representing {@link Logger#error}. */
    ERROR() {
        @Override
        boolean isEnabled(Logger logger) {
            return logger.isErrorEnabled();
        }

        @Override
        void log(Logger logger, String message) {
            logger.error(message);
        }
    },

    /** A log level representing {@link Logger#warn}. */
    WARN() {
        @Override
        boolean isEnabled(Logger logger) {
            return logger.isWarnEnabled();
        }

        @Override
        void log(Logger logger, String message) {
            logger.warn(message);
        }
    },

    /** A log level representing {@link Logger#info}. */
    INFO() {
        @Override
        boolean isEnabled(Logger logger) {
            return logger.isInfoEnabled();
        }

        @Override
        void log(Logger logger, String message) {
            logger.info(message);
        }
    },

    /** A log level representing {@link Logger#debug}. */
    DEBUG() {
        @Override
        boolean isEnabled(Logger logger) {
            return logger.isDebugEnabled();
        }

        @Override
        void log(Logger logger, String message) {
            logger.debug(message);
        }
    },

    /** A log level representing {@link Logger#trace}. */
    TRACE() {
        @Override
        boolean isEnabled(Logger logger) {
            return logger.isTraceEnabled();
        }

        @Override
        void log(Logger logger, String message) {
            logger.trace(message);
        }
    },
    ;

    abstract boolean isEnabled(Logger logger);

    abstract void log(Logger logger, String message);
}
