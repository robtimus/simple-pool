/*
 * PoolableObjectTest.java
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;

@SuppressWarnings("nls")
class PoolableObjectTest {

    // Other functionality has been tested in other tests

    @Nested
    @DisplayName("isEnabled")
    class IsEnabled {

        @Nested
        @DisplayName("with logger")
        class WithLogger {

            private Logger logger;
            private Pool<ObjectWrapper<Integer>, None> pool;
            private ObjectWrapper<?> object;

            @BeforeEach
            void init() {
                logger = mock(Logger.class);

                PoolLogger poolLogger = new PoolLogger(logger, "pool - ", "object-");

                pool = ObjectWrapper.newPool(PoolConfig.defaultConfig(), () -> 0, poolLogger);
                object = assertDoesNotThrow(() -> pool.acquire());
            }

            @AfterEach
            void cleanup() {
                object.release();
                pool.shutdown();
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(booleans = { true, false })
            @DisplayName("ERROR")
            void testErrorLevel(boolean enabled) {
                when(logger.isErrorEnabled()).thenReturn(enabled);

                assertEquals(enabled, object.isEnabled(LogLevel.ERROR));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(booleans = { true, false })
            @DisplayName("WARN")
            void testWarnLevel(boolean enabled) {
                when(logger.isWarnEnabled()).thenReturn(enabled);

                assertEquals(enabled, object.isEnabled(LogLevel.WARN));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(booleans = { true, false })
            @DisplayName("INFO")
            void testInfoLevel(boolean enabled) {
                when(logger.isInfoEnabled()).thenReturn(enabled);

                assertEquals(enabled, object.isEnabled(LogLevel.INFO));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(booleans = { true, false })
            @DisplayName("DEBUG")
            void testDebugLevel(boolean enabled) {
                when(logger.isDebugEnabled()).thenReturn(enabled);

                assertEquals(enabled, object.isEnabled(LogLevel.DEBUG));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(booleans = { true, false })
            @DisplayName("TRACE")
            void testTraceLevel(boolean enabled) {
                when(logger.isTraceEnabled()).thenReturn(enabled);

                assertEquals(enabled, object.isEnabled(LogLevel.TRACE));
            }
        }

        @Nested
        @DisplayName("without logger")
        class WithoutLogger {

            private ObjectWrapper<?> object;

            @BeforeEach
            void init() {
                object = new ObjectWrapper<>(0);
            }

            @Test
            @DisplayName("ERROR")
            void testErrorLevel() {
                assertFalse(object.isEnabled(LogLevel.ERROR));
            }

            @Test
            @DisplayName("WARN")
            void testWarnLevel() {
                assertFalse(object.isEnabled(LogLevel.WARN));
            }

            @Test
            @DisplayName("INFO")
            void testInfoLevel() {
                assertFalse(object.isEnabled(LogLevel.INFO));
            }

            @Test
            @DisplayName("DEBUG")
            void testDebugLevel() {
                assertFalse(object.isEnabled(LogLevel.DEBUG));
            }

            @Test
            @DisplayName("TRACE")
            void testTraceLevel() {
                assertFalse(object.isEnabled(LogLevel.TRACE));
            }
        }
    }
}
