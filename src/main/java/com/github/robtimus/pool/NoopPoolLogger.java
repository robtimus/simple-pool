/*
 * NoopPoolLogger.java
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

final class NoopPoolLogger extends PoolLogger {

    static final NoopPoolLogger INSTANCE = new NoopPoolLogger();

    private NoopPoolLogger() {
    }

    @Override
    public void creatingPool(PoolConfig config) {
        // does nothing
    }

    @Override
    public void createdPool(PoolConfig config) {
        // does nothing
    }

    @Override
    public void failedToCreatePool(Exception exception) {
        // does nothing
    }

    @Override
    public void drainedPool(int poolSize) {
        // does nothing
    }

    @Override
    public void shutDownPool() {
        // does nothing
    }

    @Override
    public void createdObject(long objectId) {
        // does nothing
    }

    @Override
    public void createdNonPooledObject(long objectId) {
        // does nothing
    }

    @Override
    public void increasedRefCount(long objectId, int refCount) {
        // does nothing
    }

    @Override
    public void decreasedRefCount(long objectId, int refCount) {
        // does nothing
    }

    @Override
    public void releasedResources(long objectId) {
        // does nothing
    }

    @Override
    public void acquiredObject(long objectId, int idleCount, int poolSize) {
        // does nothing
    }

    @Override
    public void returnedObject(long objectId, int idleCount, int poolSize) {
        // does nothing
    }

    @Override
    public void objectInvalidated(long objectId, int idleCount, int poolSize) {
        // does nothing
    }

    @Override
    public void objectIdleTooLong(long objectId, int idleCount, int poolSize) {
        // does nothing
    }
}
