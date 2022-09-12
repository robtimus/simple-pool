/*
 * PoolLoggerTest.java
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import com.github.robtimus.pool.PoolLogger.Builder;

@SuppressWarnings("nls")
class PoolLoggerTest {

    @Nested
    @DisplayName("with logging enabled")
    class LoggingEnabled {

        private Logger logger;
        private PoolLogger poolLogger;

        @BeforeEach
        void initLogger() {
            logger = mock(Logger.class);
            when(logger.isDebugEnabled()).thenReturn(true);

            poolLogger = new PoolLogger(logger, "pool - ", "object-");
        }

        @Test
        @DisplayName("creatingPool")
        void testCreatingPool() {
            PoolConfig config = PoolConfig.defaultConfig();

            poolLogger.creatingPool(config);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.creatingPool.get("pool - ", config));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("createdPool")
        void testCreatedPool() {
            PoolConfig config = PoolConfig.defaultConfig();

            poolLogger.createdPool(config);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.createdPool.get("pool - ", config));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("failedToCreatePool")
        void testFailedToCreatePool() {
            Exception exception = new NullPointerException();

            poolLogger.failedToCreatePool(exception);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.failedToCreatePool.get("pool - "), exception);
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("drainedPool")
        void testDrainedPool() {
            poolLogger.drainedPool(15);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.drainedPool.get("pool - ", 15));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("shutDownPool")
        void testShutDownPool() {
            poolLogger.shutDownPool();

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.shutDownPool.get("pool - "));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("createdObject")
        void testCreatedObject() {
            poolLogger.createdObject(4);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.createdObject.get("pool - ", "object-", 4));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("createdNonPooledObject")
        void testCreatedNonPooledObject() {
            poolLogger.createdNonPooledObject(4);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.createdNonPooledObject.get("pool - ", "object-", 4));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("increasedObjectRefCount")
        void testIncreasedObjectRefCount() {
            poolLogger.increasedObjectRefCount(4, 2);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.increasedObjectRefCount.get("pool - ", "object-", 4, 2));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("decreasedObjectRefCount")
        void testDecreasedObjectRefCount() {
            poolLogger.decreasedObjectRefCount(4, 2);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.decreasedObjectRefCount.get("pool - ", "object-", 4, 2));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("releasingObjectResources")
        void testReleasingObjectResources() {
            poolLogger.releasingObjectResources(4);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.releasingObjectResources.get("pool - ", "object-", 4));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("releasedObjectResources")
        void testReleasedObjectResources() {
            poolLogger.releasedObjectResources(4);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.releasedObjectResources.get("pool - ", "object-", 4));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("releaseObjectResourcesFailed")
        void testReleaseObjectResourcesFailed() {
            Exception exception = new NullPointerException();

            poolLogger.releaseObjectResourcesFailed(4, exception);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.releaseObjectResourcesFailed.get("pool - ", "object-", 4), exception);
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("acquiredObject")
        void testAcquiredObject() {
            poolLogger.acquiredObject(4, 2, 10);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.acquiredObject.get("pool - ", "object-", 4, 2, 10));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("returnedObject")
        void testReturnedObject() {
            poolLogger.returnedObject(4, 2, 10);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.returnedObject.get("pool - ", "object-", 4, 2, 10));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("objectInvalidated")
        void testObjectInvalidated() {
            poolLogger.objectInvalidated(4, 2, 10);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.objectInvalidated.get("pool - ", "object-", 4, 2, 10));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("objectIdleTooLong")
        void testObjectIdleTooLong() {
            poolLogger.objectIdleTooLong(4, 2, 10);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.objectIdleTooLong.get("pool - ", "object-", 4, 2, 10));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("objectEvent(String)")
        void testObjectEventWithMessage() {
            poolLogger.objectEvent(4, "custom event");

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.objectEvent.get("pool - ", "object-", 4, "custom event"));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("objectEvent(Supplier)")
        void testObjectEventWithMessageSupplier() {
            poolLogger.objectEvent(4, () -> "custom event");

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.objectEvent.get("pool - ", "object-", 4, "custom event"));
            verifyNoMoreInteractions(logger);
        }
    }

    @Nested
    @DisplayName("with logging disabled")
    class LoggingDisabled {

        private Logger logger;
        private PoolLogger poolLogger;

        @BeforeEach
        void initLogger() {
            logger = mock(Logger.class);
            when(logger.isDebugEnabled()).thenReturn(false);

            poolLogger = new PoolLogger(logger, "pool - ", "object-");
        }

        @Test
        @DisplayName("creatingPool")
        void testCreatingPool() {
            PoolConfig config = PoolConfig.defaultConfig();

            poolLogger.creatingPool(config);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("createdPool")
        void testCreatedPool() {
            PoolConfig config = PoolConfig.defaultConfig();

            poolLogger.createdPool(config);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("failedToCreatePool")
        void testFailedToCreatePool() {
            Exception exception = new NullPointerException();

            poolLogger.failedToCreatePool(exception);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("drainedPool")
        void testDrainedPool() {
            poolLogger.drainedPool(15);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("poolShutDown")
        void testPoolShutDown() {
            poolLogger.shutDownPool();

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("createdObject")
        void testCreatedObject() {
            poolLogger.createdObject(4);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("createdNonPooledObject")
        void testCreatedNonPooledObject() {
            poolLogger.createdNonPooledObject(4);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("increasedObjectRefCount")
        void testIncreasedObjectRefCount() {
            poolLogger.increasedObjectRefCount(4, 2);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("decreasedObjectRefCount")
        void testDecreasedObjectRefCount() {
            poolLogger.decreasedObjectRefCount(4, 2);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("releasingObjectResources")
        void testReleasingObjectResources() {
            poolLogger.releasingObjectResources(4);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("releasedObjectResources")
        void testReleasedObjectResources() {
            poolLogger.releasedObjectResources(4);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("releaseObjectResourcesFailed")
        void testReleaseObjectResourcesFailed() {
            Exception exception = new NullPointerException();

            poolLogger.releaseObjectResourcesFailed(4, exception);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("acquiredObject")
        void testAcquiredObject() {
            poolLogger.acquiredObject(4, 2, 10);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("returnedObject")
        void testReturnedObject() {
            poolLogger.returnedObject(4, 2, 10);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("objectInvalidated")
        void testObjectInvalidated() {
            poolLogger.objectInvalidated(4, 2, 10);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("objectIdleTooLong")
        void testObjectIdleTooLong() {
            poolLogger.objectIdleTooLong(4, 2, 10);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("objectEvent(String)")
        void testObjectEventWithMessage() {
            poolLogger.objectEvent(4, "custom event");

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("objectEvent(Supplier)")
        void testObjectEventWithMessageSupplier() {
            poolLogger.objectEvent(4, () -> "custom event");

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }
    }

    @Test
    @DisplayName("defaultLogger")
    void testDefaultLogger() {
        PoolLogger logger = PoolLogger.defaultLogger();

        assertEquals(Pool.class.getName(), logger.logger().getName());
        assertEquals("", logger.messagePrefix());
        assertEquals("object-", logger.objectPrefix());
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Nested
        @DisplayName("loggerName")
        class LoggerName {

            @Test
            @DisplayName("default value")
            void testDefaultValue() {
                PoolLogger logger = PoolLogger.custom()
                        .build();

                assertEquals(Pool.class.getName(), logger.logger().getName());
            }

            @Test
            @DisplayName("null value")
            void testNullValue() {
                Builder builder = PoolLogger.custom();

                assertThrows(NullPointerException.class, () -> builder.withLoggerName(null));

                PoolLogger logger = builder.build();

                assertEquals(Pool.class.getName(), logger.logger().getName());
            }

            @Test
            @DisplayName("empty value")
            void testEmptyValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withLoggerName("")
                        .build();

                assertEquals("", logger.logger().getName());
            }

            @Test
            @DisplayName("blank value")
            void testBlankValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withLoggerName(" ")
                        .build();

                assertEquals(" ", logger.logger().getName());
            }

            @Test
            @DisplayName("non-blank value")
            void testPositiveValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withLoggerName("test")
                        .build();

                assertEquals("test", logger.logger().getName());
            }
        }

        @Nested
        @DisplayName("loggerClass")
        class LoggerClass {

            @Test
            @DisplayName("null value")
            void testNullValue() {
                Builder builder = PoolLogger.custom();

                assertThrows(NullPointerException.class, () -> builder.withLoggerClass(null));

                PoolLogger logger = builder.build();

                assertEquals(Pool.class.getName(), logger.logger().getName());
            }

            @Test
            @DisplayName("non-null value")
            void testPositiveValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withLoggerClass(getClass())
                        .build();

                assertEquals(getClass().getName(), logger.logger().getName());
            }
        }

        @Nested
        @DisplayName("messagePrefix")
        class MessagePrefix {

            @Test
            @DisplayName("default value")
            void testDefaultValue() {
                PoolLogger logger = PoolLogger.custom()
                        .build();

                assertEquals("", logger.messagePrefix());
            }

            @Test
            @DisplayName("null value")
            void testNullValue() {
                Builder builder = PoolLogger.custom();

                assertThrows(NullPointerException.class, () -> builder.withPrefix(null));

                PoolLogger logger = builder.build();

                assertEquals("", logger.messagePrefix());
            }

            @Test
            @DisplayName("empty value")
            void testEmptyValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withPrefix("")
                        .build();

                assertEquals("", logger.messagePrefix());
            }

            @Test
            @DisplayName("blank value")
            void testBlankValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withPrefix(" ")
                        .build();

                assertEquals(" ", logger.messagePrefix());
            }

            @Test
            @DisplayName("non-blank value")
            void testPositiveValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withPrefix("test")
                        .build();

                assertEquals("test", logger.messagePrefix());
            }
        }

        @Nested
        @DisplayName("objectPrefix")
        class ObjectPrefix {

            @Test
            @DisplayName("default value")
            void testDefaultValue() {
                PoolLogger logger = PoolLogger.custom()
                        .build();

                assertEquals("object-", logger.objectPrefix());
            }

            @Test
            @DisplayName("null value")
            void testNullValue() {
                Builder builder = PoolLogger.custom();

                assertThrows(NullPointerException.class, () -> builder.withObjectPrefix(null));

                PoolLogger logger = builder.build();

                assertEquals("object-", logger.objectPrefix());
            }

            @Test
            @DisplayName("empty value")
            void testEmptyValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withObjectPrefix("")
                        .build();

                assertEquals("", logger.objectPrefix());
            }

            @Test
            @DisplayName("blank value")
            void testBlankValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withObjectPrefix(" ")
                        .build();

                assertEquals(" ", logger.objectPrefix());
            }

            @Test
            @DisplayName("non-blank value")
            void testPositiveValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withObjectPrefix("test")
                        .build();

                assertEquals("test", logger.objectPrefix());
            }
        }
    }
}