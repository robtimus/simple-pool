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
            verify(logger).debug(Messages.PoolLogger.creatingPool("pool - ", config));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("createdPool")
        void testCreatedPool() {
            PoolConfig config = PoolConfig.defaultConfig();

            poolLogger.createdPool(config);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.createdPool("pool - ", config));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("failedToCreatePool")
        void testFailedToCreatePool() {
            Exception exception = new NullPointerException();

            poolLogger.failedToCreatePool(exception);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.failedToCreatePool("pool - "), exception);
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("drainedPool")
        void testDrainedPool() {
            poolLogger.drainedPool(15);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.drainedPool("pool - ", 15));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("shutDownPool")
        void testShutDownPool() {
            poolLogger.shutDownPool();

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.shutDownPool("pool - "));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("createdObject")
        void testCreatedObject() {
            PoolableObject<?> object = new TestObject();

            poolLogger.createdObject(object);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.createdObject("pool - ", "object-", object.objectId()));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("createdNonPooledObject")
        void testCreatedNonPooledObject() {
            PoolableObject<?> object = new TestObject();

            poolLogger.createdNonPooledObject(object);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.createdNonPooledObject("pool - ", "object-", object.objectId()));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("increasedObjectRefCount")
        void testIncreasedObjectRefCount() {
            PoolableObject<?> object = new TestObject();

            poolLogger.increasedObjectRefCount(object, 2);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.increasedObjectRefCount("pool - ", "object-", object.objectId(), 2));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("decreasedObjectRefCount")
        void testDecreasedObjectRefCount() {
            PoolableObject<?> object = new TestObject();

            poolLogger.decreasedObjectRefCount(object, 2);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.decreasedObjectRefCount("pool - ", "object-", object.objectId(), 2));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("releasingObjectResources")
        void testReleasingObjectResources() {
            PoolableObject<?> object = new TestObject();

            poolLogger.releasingObjectResources(object);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.releasingObjectResources("pool - ", "object-", object.objectId()));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("releasedObjectResources")
        void testReleasedObjectResources() {
            PoolableObject<?> object = new TestObject();

            poolLogger.releasedObjectResources(object);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.releasedObjectResources("pool - ", "object-", object.objectId()));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("releaseObjectResourcesFailed")
        void testReleaseObjectResourcesFailed() {
            PoolableObject<?> object = new TestObject();
            Exception exception = new NullPointerException();

            poolLogger.releaseObjectResourcesFailed(object, exception);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.releaseObjectResourcesFailed("pool - ", "object-", object.objectId()), exception);
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("acquiredObject")
        void testAcquiredObject() {
            PoolableObject<?> object = new TestObject();

            poolLogger.acquiredObject(object, 2, 10);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.acquiredObject("pool - ", "object-", object.objectId(), 2, 10));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("returnedObject")
        void testReturnedObject() {
            PoolableObject<?> object = new TestObject();

            poolLogger.returnedObject(object, 2, 10);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.returnedObject("pool - ", "object-", object.objectId(), 2, 10));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("objectInvalidated")
        void testObjectInvalidated() {
            PoolableObject<?> object = new TestObject();

            poolLogger.objectInvalidated(object, 2, 10);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.objectInvalidated("pool - ", "object-", object.objectId(), 2, 10));
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("objectIdleTooLong")
        void testObjectIdleTooLong() {
            PoolableObject<?> object = new TestObject();

            poolLogger.objectIdleTooLong(object, 2, 10);

            verify(logger).isDebugEnabled();
            verify(logger).debug(Messages.PoolLogger.objectIdleTooLong("pool - ", "object-", object.objectId(), 2, 10));
            verifyNoMoreInteractions(logger);
        }

        @Nested
        @DisplayName("objectEvent(String)")
        class ObjectEventWithMessage {

            @Test
            @DisplayName("ERROR")
            void testErrorLevel() {
                PoolableObject<?> object = new TestObject();

                when(logger.isDebugEnabled()).thenReturn(false);
                when(logger.isErrorEnabled()).thenReturn(true);

                poolLogger.objectEvent(LogLevel.ERROR, object, "custom event");

                verify(logger).isErrorEnabled();
                verify(logger).error(Messages.PoolLogger.objectEvent("pool - ", "object-", object.objectId(), "custom event"));
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("WARN")
            void testWarnLevel() {
                PoolableObject<?> object = new TestObject();

                when(logger.isDebugEnabled()).thenReturn(false);
                when(logger.isWarnEnabled()).thenReturn(true);

                poolLogger.objectEvent(LogLevel.WARN, object, "custom event");

                verify(logger).isWarnEnabled();
                verify(logger).warn(Messages.PoolLogger.objectEvent("pool - ", "object-", object.objectId(), "custom event"));
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("INFO")
            void testInfoLevel() {
                PoolableObject<?> object = new TestObject();

                when(logger.isDebugEnabled()).thenReturn(false);
                when(logger.isInfoEnabled()).thenReturn(true);

                poolLogger.objectEvent(LogLevel.INFO, object, "custom event");

                verify(logger).isInfoEnabled();
                verify(logger).info(Messages.PoolLogger.objectEvent("pool - ", "object-", object.objectId(), "custom event"));
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("DEBUG")
            void testDebugLevel() {
                PoolableObject<?> object = new TestObject();

                // isDebugEnabled is already mocked

                poolLogger.objectEvent(LogLevel.DEBUG, object, "custom event");

                verify(logger).isDebugEnabled();
                verify(logger).debug(Messages.PoolLogger.objectEvent("pool - ", "object-", object.objectId(), "custom event"));
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("TRACE")
            void testTraceLevel() {
                PoolableObject<?> object = new TestObject();

                when(logger.isDebugEnabled()).thenReturn(false);
                when(logger.isTraceEnabled()).thenReturn(true);

                poolLogger.objectEvent(LogLevel.TRACE, object, "custom event");

                verify(logger).isTraceEnabled();
                verify(logger).trace(Messages.PoolLogger.objectEvent("pool - ", "object-", object.objectId(), "custom event"));
                verifyNoMoreInteractions(logger);
            }
        }

        @Nested
        @DisplayName("objectEvent(Supplier)")
        class ObjectEventWithMessageSupplier {

            @Test
            @DisplayName("ERROR")
            void testErrorLevel() {
                PoolableObject<?> object = new TestObject();

                when(logger.isDebugEnabled()).thenReturn(false);
                when(logger.isErrorEnabled()).thenReturn(true);

                poolLogger.objectEvent(LogLevel.ERROR, object, () -> "custom event");

                verify(logger).isErrorEnabled();
                verify(logger).error(Messages.PoolLogger.objectEvent("pool - ", "object-", object.objectId(), "custom event"));
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("WARN")
            void testWarnLevel() {
                PoolableObject<?> object = new TestObject();

                when(logger.isDebugEnabled()).thenReturn(false);
                when(logger.isWarnEnabled()).thenReturn(true);

                poolLogger.objectEvent(LogLevel.WARN, object, () -> "custom event");

                verify(logger).isWarnEnabled();
                verify(logger).warn(Messages.PoolLogger.objectEvent("pool - ", "object-", object.objectId(), "custom event"));
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("INFO")
            void testInfoLevel() {
                PoolableObject<?> object = new TestObject();

                when(logger.isDebugEnabled()).thenReturn(false);
                when(logger.isInfoEnabled()).thenReturn(true);

                poolLogger.objectEvent(LogLevel.INFO, object, () -> "custom event");

                verify(logger).isInfoEnabled();
                verify(logger).info(Messages.PoolLogger.objectEvent("pool - ", "object-", object.objectId(), "custom event"));
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("DEBUG")
            void testDebugLevel() {
                PoolableObject<?> object = new TestObject();

                // isDebugEnabled is already mocked

                poolLogger.objectEvent(LogLevel.DEBUG, object, "custom event");

                verify(logger).isDebugEnabled();
                verify(logger).debug(Messages.PoolLogger.objectEvent("pool - ", "object-", object.objectId(), "custom event"));
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("TRACE")
            void testTraceLevel() {
                PoolableObject<?> object = new TestObject();

                when(logger.isDebugEnabled()).thenReturn(false);
                when(logger.isTraceEnabled()).thenReturn(true);

                poolLogger.objectEvent(LogLevel.TRACE, object, () -> "custom event");

                verify(logger).isTraceEnabled();
                verify(logger).trace(Messages.PoolLogger.objectEvent("pool - ", "object-", object.objectId(), "custom event"));
                verifyNoMoreInteractions(logger);
            }
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
            PoolableObject<?> object = new TestObject();

            poolLogger.createdObject(object);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("createdNonPooledObject")
        void testCreatedNonPooledObject() {
            PoolableObject<?> object = new TestObject();

            poolLogger.createdNonPooledObject(object);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("increasedObjectRefCount")
        void testIncreasedObjectRefCount() {
            PoolableObject<?> object = new TestObject();

            poolLogger.increasedObjectRefCount(object, 2);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("decreasedObjectRefCount")
        void testDecreasedObjectRefCount() {
            PoolableObject<?> object = new TestObject();

            poolLogger.decreasedObjectRefCount(object, 2);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("releasingObjectResources")
        void testReleasingObjectResources() {
            PoolableObject<?> object = new TestObject();

            poolLogger.releasingObjectResources(object);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("releasedObjectResources")
        void testReleasedObjectResources() {
            PoolableObject<?> object = new TestObject();

            poolLogger.releasedObjectResources(object);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("releaseObjectResourcesFailed")
        void testReleaseObjectResourcesFailed() {
            PoolableObject<?> object = new TestObject();
            Exception exception = new NullPointerException();

            poolLogger.releaseObjectResourcesFailed(object, exception);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("acquiredObject")
        void testAcquiredObject() {
            PoolableObject<?> object = new TestObject();

            poolLogger.acquiredObject(object, 2, 10);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("returnedObject")
        void testReturnedObject() {
            PoolableObject<?> object = new TestObject();

            poolLogger.returnedObject(object, 2, 10);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("objectInvalidated")
        void testObjectInvalidated() {
            PoolableObject<?> object = new TestObject();

            poolLogger.objectInvalidated(object, 2, 10);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Test
        @DisplayName("objectIdleTooLong")
        void testObjectIdleTooLong() {
            PoolableObject<?> object = new TestObject();

            poolLogger.objectIdleTooLong(object, 2, 10);

            verify(logger).isDebugEnabled();
            verifyNoMoreInteractions(logger);
        }

        @Nested
        @DisplayName("objectEvent(String)")
        class ObjectEventWithMessage {

            @Test
            @DisplayName("ERROR")
            void testErrorLevel() {
                PoolableObject<?> object = new TestObject();

                poolLogger.objectEvent(LogLevel.ERROR, object, "custom event");

                verify(logger).isErrorEnabled();
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("WARN")
            void testWarnLevel() {
                PoolableObject<?> object = new TestObject();

                poolLogger.objectEvent(LogLevel.WARN, object, "custom event");

                verify(logger).isWarnEnabled();
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("INFO")
            void testInfoLevel() {
                PoolableObject<?> object = new TestObject();

                poolLogger.objectEvent(LogLevel.INFO, object, "custom event");

                verify(logger).isInfoEnabled();
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("DEBUG")
            void testDebugLevel() {
                PoolableObject<?> object = new TestObject();

                poolLogger.objectEvent(LogLevel.DEBUG, object, "custom event");

                verify(logger).isDebugEnabled();
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("TRACE")
            void testTraceLevel() {
                PoolableObject<?> object = new TestObject();

                poolLogger.objectEvent(LogLevel.TRACE, object, "custom event");

                verify(logger).isTraceEnabled();
                verifyNoMoreInteractions(logger);
            }
        }

        @Nested
        @DisplayName("objectEvent(Supplier)")
        class ObjectEventWithMessageSupplier {

            @Test
            @DisplayName("ERROR")
            void testErrorLevel() {
                PoolableObject<?> object = new TestObject();

                poolLogger.objectEvent(LogLevel.ERROR, object, () -> "custom event");

                verify(logger).isErrorEnabled();
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("WARN")
            void testWarnLevel() {
                PoolableObject<?> object = new TestObject();

                poolLogger.objectEvent(LogLevel.WARN, object, () -> "custom event");

                verify(logger).isWarnEnabled();
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("INFO")
            void testInfoLevel() {
                PoolableObject<?> object = new TestObject();

                poolLogger.objectEvent(LogLevel.INFO, object, () -> "custom event");

                verify(logger).isInfoEnabled();
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("DEBUG")
            void testDebugLevel() {
                PoolableObject<?> object = new TestObject();

                poolLogger.objectEvent(LogLevel.DEBUG, object, "custom event");

                verify(logger).isDebugEnabled();
                verifyNoMoreInteractions(logger);
            }

            @Test
            @DisplayName("TRACE")
            void testTraceLevel() {
                PoolableObject<?> object = new TestObject();

                poolLogger.objectEvent(LogLevel.TRACE, object, () -> "custom event");

                verify(logger).isTraceEnabled();
                verifyNoMoreInteractions(logger);
            }
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

                assertThrows(NullPointerException.class, () -> builder.withMessagePrefix(null));

                PoolLogger logger = builder.build();

                assertEquals("", logger.messagePrefix());
            }

            @Test
            @DisplayName("empty value")
            void testEmptyValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withMessagePrefix("")
                        .build();

                assertEquals("", logger.messagePrefix());
            }

            @Test
            @DisplayName("blank value")
            void testBlankValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withMessagePrefix(" ")
                        .build();

                assertEquals(" ", logger.messagePrefix());
            }

            @Test
            @DisplayName("non-blank value")
            void testPositiveValue() {
                PoolLogger logger = PoolLogger.custom()
                        .withMessagePrefix("test")
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

    @Test
    @DisplayName("toString")
    void testtoString() {
        PoolLogger logger = PoolLogger.custom()
                .withLoggerName("custom-logger")
                .withMessagePrefix("prefix - ")
                .withObjectPrefix("obj-")
                .build();

        assertEquals("PoolLogger[logger=custom-logger,messagePrefix=prefix - ,objectPrefix=obj-]", logger.toString());
    }
}
