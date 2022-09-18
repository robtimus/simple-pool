/*
 * ObjectWrapperTest.java
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

import static com.github.robtimus.junit.support.OptionalAssertions.assertIsEmpty;
import static com.github.robtimus.junit.support.OptionalAssertions.assertIsPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("nls")
class ObjectWrapperTest {

    @Test
    @DisplayName("test ObjectWrapper")
    void testObjectWrapper() {
        PoolConfig config = PoolConfig.custom()
                .withInitialSize(1)
                .withMaxSize(2)
                .withMaxWaitTime(Duration.ofMillis(100))
                .build();
        AtomicInteger counter = new AtomicInteger(0);
        PoolLogger logger = mock(PoolLogger.class);

        Pool<ObjectWrapper<Integer>, None> pool = ObjectWrapper.newPool(config, counter::getAndIncrement, logger);

        testPool(pool);

        pool.shutdown();

        verifyPoolEvents(config, logger);
    }

    private void testPool(Pool<ObjectWrapper<Integer>, None> pool) {
        try (ObjectWrapper<Integer> wrapper = assertIsPresent(pool.acquireNow())) {
            assertEquals(0, wrapper.value());
            assertEquals("ObjectWrapper<0>", wrapper.toString());
        }
        try (ObjectWrapper<Integer> wrapper1 = assertIsPresent(pool.acquireNow())) {
            // the same wrapper is returned
            assertEquals(0, wrapper1.value());
            assertEquals("ObjectWrapper<0>", wrapper1.toString());

            try (ObjectWrapper<Integer> wrapper2 = assertIsPresent(pool.acquireNow())) {
                assertEquals(1, wrapper2.value());
                assertEquals("ObjectWrapper<1>", wrapper2.toString());

                assertIsEmpty(pool.acquireNow());
            }
        }
    }

    @SuppressWarnings("resource")
    private void verifyPoolEvents(PoolConfig config, PoolLogger logger) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<ObjectWrapper<Integer>> objectCaptor = ArgumentCaptor.forClass(ObjectWrapper.class);

        // logger calls in order, apart from createdObject
        // create pool
        verify(logger).creatingPool(config);
        verify(logger, times(2)).createdObject(objectCaptor.capture());
        List<ObjectWrapper<Integer>> objects = objectCaptor.getAllValues();
        verify(logger).createdObject(objects.get(0));
        verify(logger).createdPool(config);
        // acquire wrapper / wrapper1
        verify(logger, times(2)).increasedObjectRefCount(objects.get(0), 1);
        verify(logger, times(2)).acquiredObject(objects.get(0), 0, 1);
        // release wrapper / wrapper1; the second return is with wrapper2 added to the pool
        verify(logger, times(2)).decreasedObjectRefCount(objects.get(0), 0);
        verify(logger).returnedObject(objects.get(0), 1, 1);
        verify(logger).returnedObject(objects.get(0), 2, 2);
        // acquire wrapper2
        verify(logger).createdObject(objects.get(1));
        verify(logger).increasedObjectRefCount(objects.get(1), 1);
        verify(logger).acquiredObject(objects.get(1), 0, 2);
        // release wrapper2
        verify(logger).decreasedObjectRefCount(objects.get(1), 0);
        verify(logger).returnedObject(objects.get(1), 1, 2);
        // shutdown
        verify(logger).drainedPool(0);
        verify(logger).releasingObjectResources(objects.get(0));
        verify(logger).releasedObjectResources(objects.get(0));
        verify(logger).releasingObjectResources(objects.get(1));
        verify(logger).releasedObjectResources(objects.get(1));
        verify(logger).shutDownPool();

        verifyNoMoreInteractions(logger);
    }
}
