/*
 * None.java
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

/**
 * An exception type that can be used to indicate that no exceptions can be thrown.
 *
 * @author Rob Spoor
 */
@SuppressWarnings("serial")
public final class None extends RuntimeException {

    private None() {
        throw new IllegalStateException("cannot create instances of " + getClass().getName()); //$NON-NLS-1$
    }
}
