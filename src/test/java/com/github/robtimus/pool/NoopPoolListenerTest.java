/*
 * NoopPoolListenerTest.java
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
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class NoopPoolListenerTest {

    @TestFactory
    @DisplayName("overrides all methods")
    Stream<DynamicTest> testDelegatesAllMethods() {
        return Arrays.stream(PoolLogger.class.getMethods())
                .filter(m -> m.getDeclaringClass() == PoolLogger.class)
                .map(this::overridesMethod);
    }

    private DynamicTest overridesMethod(Method method) {
        return dynamicTest(method.getName(),
                () -> assertDoesNotThrow(() -> NoopPoolLogger.class.getMethod(method.getName(), method.getParameterTypes())));
    }

    @TestFactory
    @DisplayName("methods don't fail")
    Stream<DynamicTest> testMethodsDontFail() {
        return Arrays.stream(PoolLogger.class.getMethods())
                .filter(m -> m.getDeclaringClass() == PoolLogger.class)
                .map(this::methodDoesNotFail);
    }

    private DynamicTest methodDoesNotFail(Method method) {
        return dynamicTest(method.getName(), () -> {
            Object[] args = Arrays.stream(method.getParameterTypes())
                    .map(this::getValue)
                    .toArray();

            assertDoesNotThrow(() -> method.invoke(NoopPoolLogger.INSTANCE, args));
        });
    }

    private Object getValue(Class<?> type) {
        if (type == long.class) {
            return Long.MAX_VALUE;
        }
        if (type == int.class) {
            return Integer.MAX_VALUE;
        }
        if (type == PoolConfig.class) {
            return PoolConfig.defaultConfig();
        }
        if (type == Exception.class) {
            return new NullPointerException();
        }
        return UUID.randomUUID().toString();
    }
}
