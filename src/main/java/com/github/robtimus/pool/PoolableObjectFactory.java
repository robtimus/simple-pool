/*
 * PoolableObjectFactory.java
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
 * A factory for {@link PoolableObject}s.
 *
 * @author Rob Spoor
 * @param <T> The type of object to create.
 * @param <X> The type of exception that operations on created objects can throw.
 *                This is also the type of exception that can occur when instances are created.
 */
public interface PoolableObjectFactory<T extends PoolableObject<X>, X extends Exception> {

    /**
     * Creates a new {@link PoolableObject}.
     *
     * @return The created object.
     * @throws X If an error occurs while creating the object.
     */
    T newObject() throws X;
}
