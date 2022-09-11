/*
 * PoolConfigTest.java
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
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.github.robtimus.pool.PoolConfig.Builder;

@SuppressWarnings("nls")
class PoolConfigTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Nested
        @DisplayName("maxWaitTime")
        class MaxWaitTime {

            @Test
            @DisplayName("default value")
            void testDefaultValue() {
                PoolConfig config = PoolConfig.custom()
                        .build();

                assertEquals(Duration.ZERO, config.maxWaitTime());
            }

            @Test
            @DisplayName("null value")
            void testNullValue() {
                Builder builder = PoolConfig.custom();

                assertThrows(NullPointerException.class, () -> builder.withMaxWaitTime(null));

                PoolConfig config = builder.build();

                assertEquals(Duration.ZERO, config.maxWaitTime());
            }

            @Test
            @DisplayName("negative value")
            void testNegativeValue() {
                PoolConfig config = PoolConfig.custom()
                        .withMaxWaitTime(Duration.ofNanos(-1))
                        .build();

                assertEquals(Duration.ofNanos(-1), config.maxWaitTime());
            }

            @Test
            @DisplayName("0 value")
            void testZeroValue() {
                PoolConfig config = PoolConfig.custom()
                        .withMaxWaitTime(Duration.ZERO)
                        .build();

                assertEquals(Duration.ZERO, config.maxWaitTime());
            }

            @Test
            @DisplayName("positive value")
            void testPositiveValue() {
                PoolConfig config = PoolConfig.custom()
                        .withMaxWaitTime(Duration.ofNanos(1))
                        .build();

                assertEquals(Duration.ofNanos(1), config.maxWaitTime());
            }
        }

        @Nested
        @DisplayName("maxIdleTime")
        class MaxIdleTime {

            @Test
            @DisplayName("default value")
            void testDefaultValue() {
                PoolConfig config = PoolConfig.custom()
                        .build();

                assertEquals(Duration.ofMillis(Long.MAX_VALUE), config.maxIdleTime());
            }

            @Test
            @DisplayName("null value")
            void testNullValue() {
                Builder builder = PoolConfig.custom();

                assertThrows(NullPointerException.class, () -> builder.withMaxIdleTime(null));

                PoolConfig config = builder.build();

                assertEquals(Duration.ofMillis(Long.MAX_VALUE), config.maxIdleTime());
            }

            @Test
            @DisplayName("negative value")
            void testNegativeValue() {
                PoolConfig config = PoolConfig.custom()
                        .withMaxIdleTime(Duration.ofNanos(-1))
                        .build();

                assertEquals(Duration.ofNanos(-1), config.maxIdleTime());
            }

            @Test
            @DisplayName("0 value")
            void testZeroValue() {
                PoolConfig config = PoolConfig.custom()
                        .withMaxIdleTime(Duration.ZERO)
                        .build();

                assertEquals(Duration.ZERO, config.maxIdleTime());
            }

            @Test
            @DisplayName("positive value")
            void testPositiveValue() {
                PoolConfig config = PoolConfig.custom()
                        .withMaxIdleTime(Duration.ofNanos(1))
                        .build();

                assertEquals(Duration.ofNanos(1), config.maxIdleTime());
            }
        }

        @Nested
        @DisplayName("initialSize")
        class InitialSize {

            @Test
            @DisplayName("default value")
            void testDefaultValue() {
                PoolConfig config = PoolConfig.custom()
                        .build();

                assertEquals(1, config.initialSize());
            }

            @Test
            @DisplayName("negative value")
            void testNegativeValue() {
                Builder builder = PoolConfig.custom();

                assertThrows(IllegalArgumentException.class, () -> builder.withInitialSize(-1));

                PoolConfig config = builder.build();

                assertEquals(1, config.initialSize());
            }

            @Test
            @DisplayName("0 value")
            void testZeroValue() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(0)
                        .build();

                assertEquals(0, config.initialSize());
            }

            @Test
            @DisplayName("positive value")
            void testPositiveValue() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .build();

                assertEquals(1, config.initialSize());
            }
        }

        @Nested
        @DisplayName("maxSize")
        class MaxSize {

            @Test
            @DisplayName("default value")
            void testDefaultValue() {
                PoolConfig config = PoolConfig.custom()
                        .build();

                assertEquals(5, config.maxSize());
            }

            @Test
            @DisplayName("negative value")
            void testNegativeValue() {
                Builder builder = PoolConfig.custom();

                assertThrows(IllegalArgumentException.class, () -> builder.withMaxSize(-1));

                PoolConfig config = builder.build();

                assertEquals(5, config.maxSize());
            }

            @Test
            @DisplayName("0 value")
            void testZeroValue() {
                Builder builder = PoolConfig.custom();

                assertThrows(IllegalArgumentException.class, () -> builder.withMaxSize(0));

                PoolConfig config = builder.build();

                assertEquals(5, config.maxSize());
            }

            @Test
            @DisplayName("positive value")
            void testPositiveValue() {
                PoolConfig config = PoolConfig.custom()
                        .withMaxSize(1)
                        .build();

                assertEquals(1, config.maxSize());
            }
        }

        @Nested
        @DisplayName("build")
        class Build {

            @Test
            @DisplayName("initialValue < maxValue")
            void testInitialValueSmallerThanMaxValue() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(2)
                        .build();

                assertEquals(1, config.initialSize());
                assertEquals(2, config.maxSize());
            }

            @Test
            @DisplayName("initialValue == maxValue")
            void testInitialValueEqualToMaxValue() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(2)
                        .withMaxSize(2)
                        .build();

                assertEquals(2, config.initialSize());
                assertEquals(2, config.maxSize());
            }

            @Test
            @DisplayName("initialValue > maxValue")
            void testInitialValueLArgerThanMaxValue() {
                Builder builder = PoolConfig.custom()
                        .withInitialSize(2)
                        .withMaxSize(1);

                assertThrows(IllegalStateException.class, builder::build);
            }
        }
    }

    @Test
    @DisplayName("toString")
    void testToString() {
        PoolConfig config = PoolConfig.custom()
                .withMaxIdleTime(Duration.ofSeconds(5))
                .build();

        assertEquals("PoolConfig[maxWaitTime=PT0S,maxIdleTime=PT5S,initialSize=1,maxSize=5]", config.toString());
    }
}
