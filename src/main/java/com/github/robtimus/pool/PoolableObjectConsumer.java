/*
 * PoolableObjectConsumer.java
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
 * An operation that takes a single {@link PoolableObject}.
 *
 * @author Rob Spoor
 * @param <T> The type of object to operate on.
 * @param <X> The type of exception that operations on objects can throw.
 */
public interface PoolableObjectConsumer<T extends PoolableObject<X>, X extends Exception> {

    /**
     * Performs this operation on the given argument.
     *
     * @param object The object to operate on.
     * @throws X If an error occurs when performing this operation.
     */
    void accept(T object) throws X;
}
