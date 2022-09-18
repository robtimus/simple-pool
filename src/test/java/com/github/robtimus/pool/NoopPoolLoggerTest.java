/*
 * NoopPoolLoggerTest.java
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

import static com.github.robtimus.junit.support.util.DisplayNameUtils.getMethodDisplayName;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

@SuppressWarnings("nls")
class NoopPoolLoggerTest {

    @TestFactory
    @DisplayName("overrides all methods")
    Stream<DynamicTest> testDelegatesAllMethods() {
        return Arrays.stream(PoolLogger.class.getMethods())
                .filter(m -> m.getDeclaringClass() == PoolLogger.class)
                .filter(m -> !Modifier.isStatic(m.getModifiers()))
                .map(this::overridesMethod);
    }

    private DynamicTest overridesMethod(Method method) {
        return dynamicTest(getMethodDisplayName(method),
                () -> assertDoesNotThrow(() -> NoopPoolLogger.class.getDeclaredMethod(method.getName(), method.getParameterTypes())));
    }

    @TestFactory
    @DisplayName("methods don't fail")
    Stream<DynamicTest> testMethodsDontFail() {
        return Arrays.stream(PoolLogger.class.getMethods())
                .filter(m -> m.getDeclaringClass() == PoolLogger.class)
                .filter(m -> !Modifier.isStatic(m.getModifiers()))
                .map(this::methodDoesNotFail);
    }

    private DynamicTest methodDoesNotFail(Method method) {
        return dynamicTest(getMethodDisplayName(method), () -> {
            Object[] args = Arrays.stream(method.getParameterTypes())
                    .map(this::getValue)
                    .toArray();

            assertDoesNotThrow(() -> method.invoke(NoopPoolLogger.INSTANCE, args));
        });
    }

    private Object getValue(Class<?> type) {
        if (type == int.class) {
            return Integer.MAX_VALUE;
        }
        if (type == PoolableObject.class) {
            return new TestObject();
        }
        if (type == PoolConfig.class) {
            return PoolConfig.defaultConfig();
        }
        if (type == Exception.class) {
            return new NullPointerException();
        }
        if (type == Supplier.class) {
            return (Supplier<String>) () -> "custom message";
        }
        if (type == LogLevel.class) {
            return LogLevel.DEBUG;
        }
        return UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("toString")
    void testtoString() {
        PoolLogger logger = PoolLogger.noopLogger();

        assertEquals("NoopPoolLogger", logger.toString());
    }
}
