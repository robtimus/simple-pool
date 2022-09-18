/*
 * TestObject.java
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

import java.util.function.Supplier;

class TestObject extends PoolableObject<None> {

    private final boolean valid;
    private final Supplier<RuntimeException> releaseExceptionSupplier;

    TestObject() {
        this(true);
    }

    TestObject(boolean valid) {
        this(valid, null);
    }

    TestObject(boolean valid, Supplier<RuntimeException> releaseExceptionSupplier) {
        this.valid = valid;
        this.releaseExceptionSupplier = releaseExceptionSupplier;
    }

    @Override
    protected boolean isValid() {
        return valid;
    }

    @Override
    protected void doReleaseResources() throws None {
        if (releaseExceptionSupplier != null) {
            throw releaseExceptionSupplier.get();
        }
    }
}
