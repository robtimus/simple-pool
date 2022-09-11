/*
 * NoneTest.java
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoneTest {

    @Test
    @DisplayName("cannot be constructed")
    void testCannotBeConstructed() {
        Constructor<?>[] constructors = None.class.getDeclaredConstructors();
        assertEquals(1, constructors.length);
        assertEquals(0, constructors[0].getParameterCount());

        constructors[0].setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructors[0]::newInstance);
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }
}
