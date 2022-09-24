/*
 * PoolTest.java
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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("nls")
class PoolTest {

    @Nested
    @DisplayName("fillPool")
    class FillPool {

        @Test
        @DisplayName("no initial objects")
        void testNoInitialObjects() {
            PoolConfig config = PoolConfig.custom()
                    .withInitialSize(0)
                    .build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);

            assertDoesNotThrow(() -> Pool.throwingNone(config, supplier, logger));

            // logger calls in order
            verify(logger).creatingPool(config);
            verify(logger).createdPool(config);

            verifyNoMoreInteractions(supplier, logger);
        }

        @Test
        @DisplayName("with initial objects")
        void testWithInitialObjects() {
            PoolConfig config = PoolConfig.custom().build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);

            when(supplier.get()).thenAnswer(i -> new TestObject());

            Pool<?, None> pool = assertDoesNotThrow(() -> Pool.throwingNone(config, supplier, logger));

            ArgumentCaptor<TestObject> objectCaptor = ArgumentCaptor.forClass(TestObject.class);

            verify(supplier).get();
            // logger calls in order
            verify(logger).creatingPool(config);
            verify(logger).createdObject(objectCaptor.capture());
            verify(logger).createdPool(config);

            verifyNoMoreInteractions(supplier, logger);

            pool.shutdown();

            verify(logger).releasingObjectResources(objectCaptor.getValue());
            verify(logger).releasedObjectResources(objectCaptor.getValue());
            verify(logger).drainedPool(0);
            verify(logger).shutDownPool();

            verifyNoMoreInteractions(supplier, logger);
        }

        @Test
        @DisplayName("failure for first object")
        void testFailureForFirstObject() {
            NumberFormatException error = new NumberFormatException();

            PoolConfig config = PoolConfig.custom().build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);

            when(supplier.get()).thenThrow(error);

            NumberFormatException exception = assertThrows(NumberFormatException.class, () -> Pool.throwingNone(config, supplier, logger));
            assertSame(error, exception);

            verify(supplier).get();
            // logger calls in order
            verify(logger).creatingPool(config);
            verify(logger).failedToCreatePool(error);

            verifyNoMoreInteractions(supplier, logger);
        }

        @Test
        @DisplayName("failure for second object")
        void testFailureForSecondObject() {
            NumberFormatException error = new NumberFormatException();

            PoolConfig config = PoolConfig.custom()
                    .withInitialSize(2)
                    .build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);

            AtomicInteger count = new AtomicInteger(0);
            when(supplier.get()).thenAnswer(i -> {
                if (count.getAndIncrement() == 0) {
                    return new TestObject();
                }
                throw error;
            });

            NumberFormatException exception = assertThrows(NumberFormatException.class, () -> Pool.throwingNone(config, supplier, logger));
            assertSame(error, exception);

            ArgumentCaptor<TestObject> objectCaptor = ArgumentCaptor.forClass(TestObject.class);

            verify(supplier, times(2)).get();
            // logger calls in order
            verify(logger).creatingPool(config);
            verify(logger).createdObject(objectCaptor.capture());
            verify(logger).failedToCreatePool(error);
            verify(logger).releasingObjectResources(objectCaptor.getValue());
            verify(logger).releasedObjectResources(objectCaptor.getValue());

            verifyNoMoreInteractions(supplier, logger);
        }

        @Test
        @DisplayName("failure for second object with release resources error")
        void testFailureForSecondObjectWithReleaseResourcesError() {
            NumberFormatException error = new NumberFormatException();
            NumberFormatException suppressed = new NumberFormatException();

            PoolConfig config = PoolConfig.custom()
                    .withInitialSize(2)
                    .build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);

            TestObject object = new TestObject() {
                @Override
                protected void releaseResources() {
                    throw suppressed;
                }
            };
            AtomicInteger count = new AtomicInteger(0);
            when(supplier.get()).thenAnswer(i -> {
                if (count.getAndIncrement() == 0) {
                    return object;
                }
                throw error;
            });

            NumberFormatException exception = assertThrows(NumberFormatException.class, () -> Pool.throwingNone(config, supplier, logger));
            assertSame(error, exception);
            assertArrayEquals(new Throwable[] { suppressed }, exception.getSuppressed());

            verify(supplier, times(2)).get();
            // logger calls in order
            verify(logger).creatingPool(config);
            verify(logger).createdObject(object);
            verify(logger).failedToCreatePool(error);
            verify(logger).releasingObjectResources(object);
            verify(logger).releaseObjectResourcesFailed(object, suppressed);

            verifyNoMoreInteractions(logger);
        }
    }

    @Nested
    @DisplayName("pool flow")
    class PoolFlow {

        private ScheduledExecutorService executor;

        @BeforeEach
        void initExecutor() {
            executor = Executors.newScheduledThreadPool(5);
        }

        @AfterEach
        void shutdownExecutor() {
            executor.shutdown();
        }

        @Nested
        @DisplayName("acquire blocking")
        class AcquireBlocking {

            @Test
            @DisplayName("acquire with idle remaining")
            void testAcquireWithIdleRemaining() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(1)
                        .withMaxWaitTime(Duration.ofMillis(100))
                        .build();
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                TestObject object = assertDoesNotThrow(() -> pool.acquire());

                object.release();

                verify(supplier).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(object);
                verify(logger).createdPool(config);
                // acquire object
                verify(logger).increasedObjectRefCount(object, 1);
                verify(logger).acquiredObject(object, 0, 1);
                // release object
                verify(logger).decreasedObjectRefCount(object, 0);
                verify(logger).returnedObject(object, 1, 1);

                verifyNoMoreInteractions(supplier, logger);

                verify(object, never()).releaseResources();
            }

            @Test
            @DisplayName("acquire with new object")
            void testAcquireWithNewObject() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(2)
                        .withMaxWaitTime(Duration.ofMillis(100))
                        .build();
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                TestObject object1 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));
                TestObject object2 = assertDoesNotThrow(() -> pool.acquire());

                object1.release();
                object2.release();

                verify(supplier, times(2)).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(object1);
                verify(logger).createdPool(config);
                // acquire object1
                verify(logger).increasedObjectRefCount(object1, 1);
                verify(logger).acquiredObject(object1, 0, 1);
                // acquire object2
                verify(logger).createdObject(object2);
                verify(logger).increasedObjectRefCount(object2, 1);
                verify(logger).acquiredObject(object2, 0, 2);
                // release object1
                verify(logger).decreasedObjectRefCount(object1, 0);
                verify(logger).returnedObject(object1, 1, 2);
                // release object2
                verify(logger).decreasedObjectRefCount(object2, 0);
                verify(logger).returnedObject(object2, 2, 2);

                verifyNoMoreInteractions(supplier, logger);

                verify(object1, never()).releaseResources();
                verify(object2, never()).releaseResources();
            }

            @Nested
            @DisplayName("acquire with empty pool")
            class WithEmptyPool {

                @Test
                @DisplayName("with max wait time")
                void testWithMaxWaitTime() {
                    PoolConfig config = PoolConfig.custom()
                            .withInitialSize(1)
                            .withMaxSize(2)
                            .withMaxWaitTime(Duration.ofMillis(100))
                            .build();
                    @SuppressWarnings("unchecked")
                    Supplier<TestObject> supplier = mock(Supplier.class);
                    PoolLogger logger = mock(PoolLogger.class);

                    when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                    Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                    TestObject object1 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));
                    TestObject object2 = assertDoesNotThrow(() -> pool.acquire(Duration.ofSeconds(1)));

                    assertThrows(NoSuchElementException.class, () -> pool.acquire());

                    object1.release();
                    object2.release();

                    verify(supplier, times(2)).get();
                    // logger calls in order
                    // create pool
                    verify(logger).creatingPool(config);
                    verify(logger).createdObject(object1);
                    verify(logger).createdPool(config);
                    // acquire object1
                    verify(logger).increasedObjectRefCount(object1, 1);
                    verify(logger).acquiredObject(object1, 0, 1);
                    // acquire object2
                    verify(logger).createdObject(object2);
                    verify(logger).increasedObjectRefCount(object2, 1);
                    verify(logger).acquiredObject(object2, 0, 2);
                    // release object1
                    verify(logger).decreasedObjectRefCount(object1, 0);
                    verify(logger).returnedObject(object1, 1, 2);
                    // release object2
                    verify(logger).decreasedObjectRefCount(object2, 0);
                    verify(logger).returnedObject(object2, 2, 2);

                    verifyNoMoreInteractions(supplier, logger);

                    verify(object1, never()).releaseResources();
                    verify(object2, never()).releaseResources();
                }

                @Test
                @DisplayName("with zero max wait time")
                void testWithZeroMaxWaitTime() {
                    PoolConfig config = PoolConfig.custom()
                            .withInitialSize(1)
                            .withMaxSize(2)
                            .withMaxWaitTime(Duration.ZERO)
                            .build();
                    @SuppressWarnings("unchecked")
                    Supplier<TestObject> supplier = mock(Supplier.class);
                    PoolLogger logger = mock(PoolLogger.class);

                    when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                    Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                    TestObject object1 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));
                    TestObject object2 = assertDoesNotThrow(() -> pool.acquire(Duration.ofSeconds(1)));

                    assertThrows(NoSuchElementException.class, () -> pool.acquire());

                    object1.release();
                    object2.release();

                    verify(supplier, times(2)).get();

                    // logger calls in order
                    // create pool
                    verify(logger).creatingPool(config);
                    verify(logger).createdObject(object1);
                    verify(logger).createdPool(config);
                    // acquire object1
                    verify(logger).increasedObjectRefCount(object1, 1);
                    verify(logger).acquiredObject(object1, 0, 1);
                    // acquire object2
                    verify(logger).createdObject(object2);
                    verify(logger).increasedObjectRefCount(object2, 1);
                    verify(logger).acquiredObject(object2, 0, 2);
                    // release object1
                    verify(logger).decreasedObjectRefCount(object1, 0);
                    verify(logger).returnedObject(object1, 1, 2);
                    // release object2
                    verify(logger).decreasedObjectRefCount(object2, 0);
                    verify(logger).returnedObject(object2, 2, 2);

                    verifyNoMoreInteractions(supplier, logger);

                    verify(object1, never()).releaseResources();
                    verify(object2, never()).releaseResources();
                }

                @Test
                @DisplayName("without max wait time")
                void testWithoutMaxWaitTime() {
                    PoolConfig config = PoolConfig.custom()
                            .withInitialSize(1)
                            .withMaxSize(2)
                            .withMaxWaitTime(Duration.ofNanos(-1))
                            .build();
                    @SuppressWarnings("unchecked")
                    Supplier<TestObject> supplier = mock(Supplier.class);
                    PoolLogger logger = mock(PoolLogger.class);

                    when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                    Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                    TestObject object1 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));
                    TestObject object2 = assertDoesNotThrow(() -> pool.acquire(Duration.ofSeconds(1)));

                    // JUnit does not provide any assertions for testing that code takes longer.
                    // Instead, shutdown the pool after 100ms, and check that the acquire call throws an IllegalStateException
                    executor.schedule(pool::shutdown, 100, TimeUnit.MILLISECONDS);

                    assertThrows(IllegalStateException.class, () -> pool.acquire());

                    object1.release();
                    object2.release();

                    assertTrue(assertDoesNotThrow(() -> pool.awaitShutdown(100, TimeUnit.MILLISECONDS)));

                    verify(supplier, times(2)).get();
                    // logger calls in order
                    // create pool
                    verify(logger).creatingPool(config);
                    verify(logger).createdObject(object1);
                    verify(logger).createdPool(config);
                    // acquire object1
                    verify(logger).increasedObjectRefCount(object1, 1);
                    verify(logger).acquiredObject(object1, 0, 1);
                    // acquire object2
                    verify(logger).createdObject(object2);
                    verify(logger).increasedObjectRefCount(object2, 1);
                    verify(logger).acquiredObject(object2, 0, 2);
                    // shutdown - no idle objects available
                    verify(logger).drainedPool(2);
                    verify(logger).shutDownPool();
                    // release object1 - not returned but resources released
                    verify(logger).decreasedObjectRefCount(object1, 0);
                    verify(logger).releasingObjectResources(object1);
                    verify(logger).releasedObjectResources(object1);
                    // release object2 - not returned but resources released
                    verify(logger).decreasedObjectRefCount(object2, 0);
                    verify(logger).releasingObjectResources(object2);
                    verify(logger).releasedObjectResources(object2);

                    verifyNoMoreInteractions(supplier, logger);

                    // releaseResources is called because the pool is no longer active when the objects are returned
                    verify(object1).releaseResources();
                    verify(object2).releaseResources();
                }

                @Test
                @DisplayName("with explicit null max wait time")
                void testWithNullMaxWaitTime() {
                    PoolConfig config = PoolConfig.custom()
                            .withInitialSize(1)
                            .withMaxSize(2)
                            .withMaxWaitTime(Duration.ofNanos(-1))
                            .build();
                    @SuppressWarnings("unchecked")
                    Supplier<TestObject> supplier = mock(Supplier.class);
                    PoolLogger logger = mock(PoolLogger.class);

                    when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                    Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                    TestObject object1 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));
                    TestObject object2 = assertDoesNotThrow(() -> pool.acquire(Duration.ofSeconds(1)));

                    // JUnit does not provide any assertions for testing that code takes longer.
                    // Instead, shutdown the pool after 100ms, and check that the acquire call throws an IllegalStateException
                    executor.schedule(pool::shutdown, 100, TimeUnit.MILLISECONDS);

                    assertThrows(IllegalStateException.class, () -> pool.acquire((Duration) null));

                    object1.release();
                    object2.release();

                    assertTrue(assertDoesNotThrow(() -> pool.awaitShutdown(100, TimeUnit.MILLISECONDS)));

                    verify(supplier, times(2)).get();
                    // logger calls in order
                    // create pool
                    verify(logger).creatingPool(config);
                    verify(logger).createdObject(object1);
                    verify(logger).createdPool(config);
                    // acquire object1
                    verify(logger).increasedObjectRefCount(object1, 1);
                    verify(logger).acquiredObject(object1, 0, 1);
                    // acquire object2
                    verify(logger).createdObject(object2);
                    verify(logger).increasedObjectRefCount(object2, 1);
                    verify(logger).acquiredObject(object2, 0, 2);
                    // shutdown - no idle objects available
                    verify(logger).drainedPool(2);
                    verify(logger).shutDownPool();
                    // release object1 - not returned but resources released
                    verify(logger).decreasedObjectRefCount(object1, 0);
                    verify(logger).releasingObjectResources(object1);
                    verify(logger).releasedObjectResources(object1);
                    // release object2 - not returned but resources released
                    verify(logger).decreasedObjectRefCount(object2, 0);
                    verify(logger).releasingObjectResources(object2);
                    verify(logger).releasedObjectResources(object2);

                    verifyNoMoreInteractions(supplier, logger);

                    // releaseResources is called because the pool is no longer active when the objects are returned
                    verify(object1).releaseResources();
                    verify(object2).releaseResources();
                }
            }

            @Test
            @DisplayName("create failure doesn't lead to starvation")
            void testCreateFailureDoesNotLeadToStarvation() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(2)
                        .withMaxWaitTime(Duration.ofMillis(100))
                        .build();
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                AtomicInteger count = new AtomicInteger(0);
                when(supplier.get()).thenAnswer(i -> {
                    if (count.getAndIncrement() == 1) {
                        throw new NumberFormatException();
                    }
                    return spy(new TestObject());
                });

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                TestObject object1 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));

                assertThrows(NumberFormatException.class, () -> pool.acquire());

                TestObject object2 = assertDoesNotThrow(() -> pool.acquire());

                assertThrows(NoSuchElementException.class, () -> pool.acquire());

                assertNotEquals(object1, object2);

                object1.release();
                object2.release();

                verify(supplier, times(3)).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(object1);
                verify(logger).createdPool(config);
                // acquire object1
                verify(logger).increasedObjectRefCount(object1, 1);
                verify(logger).acquiredObject(object1, 0, 1);
                // acquire object2
                verify(logger).createdObject(object2);
                verify(logger).increasedObjectRefCount(object2, 1);
                verify(logger).acquiredObject(object2, 0, 2);
                // release object1
                verify(logger).decreasedObjectRefCount(object1, 0);
                verify(logger).returnedObject(object1, 1, 2);
                // release object2
                verify(logger).decreasedObjectRefCount(object2, 0);
                verify(logger).returnedObject(object2, 2, 2);

                verifyNoMoreInteractions(supplier, logger);

                verify(object1, never()).releaseResources();
                verify(object2, never()).releaseResources();
            }

            @Nested
            @DisplayName("acquire when returned while waiting")
            class AcquireWhenReturnedWhileWaiting {

                @Test
                @DisplayName("with max wait time")
                void testWithMaxWaitTime() {
                    PoolConfig config = PoolConfig.custom()
                            .withInitialSize(1)
                            .withMaxSize(1)
                            .withMaxWaitTime(Duration.ofMillis(100))
                            .build();
                    @SuppressWarnings("unchecked")
                    Supplier<TestObject> supplier = mock(Supplier.class);
                    PoolLogger logger = mock(PoolLogger.class);

                    when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                    Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                    TestObject object1 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));

                    executor.schedule(object1::release, 100, TimeUnit.MILLISECONDS);

                    TestObject object2 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));

                    assertSame(object1, object2);

                    verify(supplier).get();
                    // logger calls in order
                    // create pool
                    verify(logger).creatingPool(config);
                    verify(logger).createdObject(object1);
                    verify(logger).createdPool(config);
                    // acquire object1 / object2
                    verify(logger, times(2)).increasedObjectRefCount(object1, 1);
                    verify(logger, times(2)).acquiredObject(object1, 0, 1);
                    // release object1
                    verify(logger).decreasedObjectRefCount(object1, 0);
                    verify(logger).returnedObject(object1, 1, 1);

                    verifyNoMoreInteractions(supplier, logger);

                    verify(object1, never()).releaseResources();
                    verify(object2, never()).releaseResources();
                }

                @Test
                @DisplayName("without max wait time")
                void testWithoutMaxWaitTime() {
                    PoolConfig config = PoolConfig.custom()
                            .withInitialSize(1)
                            .withMaxSize(1)
                            .withMaxWaitTime(Duration.ZERO)
                            .build();
                    @SuppressWarnings("unchecked")
                    Supplier<TestObject> supplier = mock(Supplier.class);
                    PoolLogger logger = mock(PoolLogger.class);

                    when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                    Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                    TestObject object1 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));

                    executor.schedule(object1::release, 100, TimeUnit.MILLISECONDS);

                    TestObject object2 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));

                    assertSame(object1, object2);

                    verify(supplier).get();
                    // logger calls in order
                    // create pool
                    verify(logger).creatingPool(config);
                    verify(logger).createdObject(object1);
                    verify(logger).createdPool(config);
                    // acquire object1 / object2
                    verify(logger, times(2)).increasedObjectRefCount(object1, 1);
                    verify(logger, times(2)).acquiredObject(object1, 0, 1);
                    // release object1
                    verify(logger).decreasedObjectRefCount(object1, 0);
                    verify(logger).returnedObject(object1, 1, 1);

                    verifyNoMoreInteractions(supplier, logger);

                    verify(object1, never()).releaseResources();
                    verify(object2, never()).releaseResources();
                }
            }

            @Test
            @DisplayName("acquire when shut down while waiting")
            void testAcquireWhenShutDownWhileWaiting() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(1)
                        .withMaxWaitTime(Duration.ofMillis(100))
                        .build();
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                TestObject object = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));

                executor.schedule(pool::shutdown, 100, TimeUnit.MILLISECONDS);

                assertThrows(IllegalStateException.class, () -> pool.acquire(200, TimeUnit.SECONDS));

                object.release();

                assertTrue(assertDoesNotThrow(() -> pool.awaitShutdown(100, TimeUnit.MILLISECONDS)));

                verify(supplier).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(object);
                verify(logger).createdPool(config);
                // acquire object
                verify(logger).increasedObjectRefCount(object, 1);
                verify(logger).acquiredObject(object, 0, 1);
                // shutdown
                verify(logger).drainedPool(1);
                verify(logger).shutDownPool();
                // release object
                verify(logger).decreasedObjectRefCount(object, 0);
                verify(logger).releasingObjectResources(object);
                verify(logger).releasedObjectResources(object);

                verifyNoMoreInteractions(supplier, logger);

                // releaseResources is called because the pool is no longer active when the object is returned
                verify(object).releaseResources();
            }
        }

        @Nested
        @DisplayName("acquireNow")
        class AcquireNow {

            @Test
            @DisplayName("acquire with idle remaining")
            void testAcquireWithIdleRemaining() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(1)
                        .build();
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                TestObject object = assertIsPresent(pool.acquireNow());

                object.release();

                verify(supplier).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(object);
                verify(logger).createdPool(config);
                // acquire object
                verify(logger).increasedObjectRefCount(object, 1);
                verify(logger).acquiredObject(object, 0, 1);
                // release object
                verify(logger).decreasedObjectRefCount(object, 0);
                verify(logger).returnedObject(object, 1, 1);

                verifyNoMoreInteractions(supplier, logger);

                verify(object, never()).releaseResources();
            }

            @Test
            @DisplayName("acquire with new object")
            void testAcquireWithNewObject() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(2)
                        .build();
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                TestObject object1 = assertIsPresent(pool.acquireNow());
                TestObject object2 = assertIsPresent(pool.acquireNow());

                object1.release();
                object2.release();

                verify(supplier, times(2)).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(object1);
                verify(logger).createdPool(config);
                // acquire object1
                verify(logger).increasedObjectRefCount(object1, 1);
                verify(logger).acquiredObject(object1, 0, 1);
                // acquire object2
                verify(logger).createdObject(object2);
                verify(logger).increasedObjectRefCount(object2, 1);
                verify(logger).acquiredObject(object2, 0, 2);
                // release object1
                verify(logger).decreasedObjectRefCount(object1, 0);
                verify(logger).returnedObject(object1, 1, 2);
                // release object2
                verify(logger).decreasedObjectRefCount(object2, 0);
                verify(logger).returnedObject(object2, 2, 2);

                verifyNoMoreInteractions(supplier, logger);

                verify(object1, never()).releaseResources();
                verify(object2, never()).releaseResources();
            }

            @Test
            @DisplayName("acquire with empty pool")
            void testWithEmptyPool() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(2)
                        .build();
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                TestObject object1 = assertIsPresent(pool.acquireNow());
                TestObject object2 = assertIsPresent(pool.acquireNow());

                assertIsEmpty(pool.acquireNow());

                object1.release();
                object2.release();

                verify(supplier, times(2)).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(object1);
                verify(logger).createdPool(config);
                // acquire object1
                verify(logger).increasedObjectRefCount(object1, 1);
                verify(logger).acquiredObject(object1, 0, 1);
                // acquire object2
                verify(logger).createdObject(object2);
                verify(logger).increasedObjectRefCount(object2, 1);
                verify(logger).acquiredObject(object2, 0, 2);
                // release object1
                verify(logger).decreasedObjectRefCount(object1, 0);
                verify(logger).returnedObject(object1, 1, 2);
                // release object2
                verify(logger).decreasedObjectRefCount(object2, 0);
                verify(logger).returnedObject(object2, 2, 2);

                verifyNoMoreInteractions(supplier, logger);

                verify(object1, never()).releaseResources();
                verify(object2, never()).releaseResources();
            }

            @Test
            @DisplayName("create failure doesn't lead to starvation")
            void testCreateFailureDoesNotLeadToStarvation() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(2)
                        .withMaxWaitTime(Duration.ofMillis(100))
                        .build();
                AtomicInteger count = new AtomicInteger(0);
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                when(supplier.get()).thenAnswer(i -> {
                    if (count.getAndIncrement() == 1) {
                        throw new NumberFormatException();
                    }
                    return spy(new TestObject());
                });

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                TestObject object1 = assertIsPresent(pool.acquireNow());

                assertThrows(NumberFormatException.class, () -> pool.acquireNow());

                TestObject object2 = assertIsPresent(pool.acquireNow());

                assertIsEmpty(pool.acquireNow());

                assertNotEquals(object1, object2);

                object1.release();
                object2.release();

                verify(supplier, times(3)).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(object1);
                verify(logger).createdPool(config);
                // acquire object1
                verify(logger).increasedObjectRefCount(object1, 1);
                verify(logger).acquiredObject(object1, 0, 1);
                // acquire object2
                verify(logger).createdObject(object2);
                verify(logger).increasedObjectRefCount(object2, 1);
                verify(logger).acquiredObject(object2, 0, 2);
                // release object1
                verify(logger).decreasedObjectRefCount(object1, 0);
                verify(logger).returnedObject(object1, 1, 2);
                // release object2
                verify(logger).decreasedObjectRefCount(object2, 0);
                verify(logger).returnedObject(object2, 2, 2);

                verifyNoMoreInteractions(supplier, logger);

                verify(object1, never()).releaseResources();
                verify(object2, never()).releaseResources();
            }
        }

        @Nested
        @DisplayName("acquireOrCreate")
        class AcquireOrCreate {

            @Test
            @DisplayName("acquire with idle remaining")
            void testAcquireWithIdleRemaining() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(1)
                        .build();
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                TestObject object = pool.acquireOrCreate();
                assertTrue(object.isPooled());

                object.release();

                verify(supplier).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(object);
                verify(logger).createdPool(config);
                // acquire object
                verify(logger).increasedObjectRefCount(object, 1);
                verify(logger).acquiredObject(object, 0, 1);
                // release object
                verify(logger).decreasedObjectRefCount(object, 0);
                verify(logger).returnedObject(object, 1, 1);

                verifyNoMoreInteractions(supplier, logger);

                verify(object, never()).releaseResources();
            }

            @Test
            @DisplayName("acquire with new object")
            void testAcquireWithNewObject() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(2)
                        .build();
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                TestObject object1 = pool.acquireOrCreate();
                assertTrue(object1.isPooled());

                TestObject object2 = pool.acquireOrCreate();
                assertTrue(object2.isPooled());

                object1.release();
                object2.release();

                verify(supplier, times(2)).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(object1);
                verify(logger).createdPool(config);
                // acquire object1
                verify(logger).increasedObjectRefCount(object1, 1);
                verify(logger).acquiredObject(object1, 0, 1);
                // acquire object2
                verify(logger).createdObject(object2);
                verify(logger).increasedObjectRefCount(object2, 1);
                verify(logger).acquiredObject(object2, 0, 2);
                // release object1
                verify(logger).decreasedObjectRefCount(object1, 0);
                verify(logger).returnedObject(object1, 1, 2);
                // release object2
                verify(logger).decreasedObjectRefCount(object2, 0);
                verify(logger).returnedObject(object2, 2, 2);

                verifyNoMoreInteractions(supplier, logger);

                verify(object1, never()).releaseResources();
                verify(object2, never()).releaseResources();
            }

            @Test
            @DisplayName("acquire with empty pool")
            void testWithEmptyPool() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(2)
                        .build();
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                TestObject object1 = pool.acquireOrCreate();
                assertTrue(object1.isPooled());

                TestObject object2 = pool.acquireOrCreate();
                assertTrue(object2.isPooled());

                TestObject object3 = pool.acquireOrCreate();
                assertFalse(object3.isPooled());

                object1.release();
                object2.release();
                object3.release();

                verify(supplier, times(3)).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(object1);
                verify(logger).createdPool(config);
                // acquire object1; the pool size was 1
                verify(logger).increasedObjectRefCount(object1, 1);
                verify(logger).acquiredObject(object1, 0, 1);
                // acquire object2
                verify(logger).createdObject(object2);
                verify(logger).increasedObjectRefCount(object2, 1);
                verify(logger).acquiredObject(object2, 0, 2);
                // acquire object3
                verify(logger).createdNonPooledObject(object3);
                // release object1
                verify(logger).decreasedObjectRefCount(object1, 0);
                verify(logger).returnedObject(object1, 1, 2);
                // release object2
                verify(logger).decreasedObjectRefCount(object2, 0);
                verify(logger).returnedObject(object2, 2, 2);

                verifyNoMoreInteractions(supplier, logger);

                // object3 is not pooled, so when it is released its resources are released
                verify(object1, never()).releaseResources();
                verify(object2, never()).releaseResources();
                verify(object3).releaseResources();
            }

            @Test
            @DisplayName("create failure doesn't lead to starvation")
            void testCreateFailureDoesNotLeadToStarvation() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(2)
                        .withMaxWaitTime(Duration.ofMillis(100))
                        .build();
                AtomicInteger count = new AtomicInteger(0);
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                when(supplier.get()).thenAnswer(i -> {
                    if (count.getAndIncrement() == 1) {
                        throw new NumberFormatException();
                    }
                    return spy(new TestObject());
                });

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                TestObject object1 = assertIsPresent(pool.acquireNow());

                assertThrows(NumberFormatException.class, () -> pool.acquireNow());

                TestObject object2 = assertIsPresent(pool.acquireNow());

                assertIsEmpty(pool.acquireNow());

                assertNotEquals(object1, object2);

                object1.release();
                object2.release();

                verify(supplier, times(3)).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(object1);
                verify(logger).createdPool(config);
                // acquire object1
                verify(logger).increasedObjectRefCount(object1, 1);
                verify(logger).acquiredObject(object1, 0, 1);
                // acquire object2
                verify(logger).createdObject(object2);
                verify(logger).increasedObjectRefCount(object2, 1);
                verify(logger).acquiredObject(object2, 0, 2);
                // release object1
                verify(logger).decreasedObjectRefCount(object1, 0);
                verify(logger).returnedObject(object1, 1, 2);
                // release object2
                verify(logger).decreasedObjectRefCount(object2, 0);
                verify(logger).returnedObject(object2, 2, 2);

                verifyNoMoreInteractions(supplier, logger);

                verify(object1, never()).releaseResources();
                verify(object2, never()).releaseResources();
            }
        }

        @Test
        @DisplayName("acquire returns idle objects if available")
        void testAcquireIdleObjectsFirst() {
            PoolConfig config = PoolConfig.custom()
                    .withInitialSize(2)
                    .withMaxSize(100)
                    .build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);

            when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

            Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

            for (int i = 0; i < 10; i++) {
                TestObject object = assertDoesNotThrow(() -> pool.acquire());
                object.release();
            }

            ArgumentCaptor<TestObject> objectCaptor = ArgumentCaptor.forClass(TestObject.class);

            verify(supplier, times(2)).get();
            // logger calls in order
            // create pool
            verify(logger).creatingPool(config);
            verify(logger, times(2)).createdObject(objectCaptor.capture());
            List<TestObject> objects = objectCaptor.getAllValues();
            verify(logger).createdObject(objects.get(0));
            verify(logger).createdObject(objects.get(1));
            verify(logger).createdPool(config);
            // acquire object
            verify(logger, times(5)).increasedObjectRefCount(objects.get(0), 1);
            verify(logger, times(5)).acquiredObject(objects.get(0), 1, 2);
            verify(logger, times(5)).increasedObjectRefCount(objects.get(1), 1);
            verify(logger, times(5)).acquiredObject(objects.get(1), 1, 2);
            // release object
            verify(logger, times(5)).decreasedObjectRefCount(objects.get(0), 0);
            verify(logger, times(5)).returnedObject(objects.get(0), 2, 2);
            verify(logger, times(5)).decreasedObjectRefCount(objects.get(1), 0);
            verify(logger, times(5)).returnedObject(objects.get(1), 2, 2);

            verifyNoMoreInteractions(supplier, logger);
        }

        @Test
        @DisplayName("inactive objects are removed")
        void testInactiveObjectsAreRemoved() {
            PoolConfig config = PoolConfig.custom()
                    .withInitialSize(1)
                    .withMaxSize(1)
                    .withMaxWaitTime(Duration.ofMillis(100))
                    .build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);

            when(supplier.get()).thenAnswer(i -> spy(new TestObject(false)));

            Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

            TestObject object1 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));

            object1.release();

            TestObject object2 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));

            assertNotSame(object1, object2);

            object2.release();

            ArgumentCaptor<TestObject> objectCaptor = ArgumentCaptor.forClass(TestObject.class);

            verify(supplier, times(3)).get();
            // logger calls in order
            // create pool
            verify(logger).creatingPool(config);
            verify(logger, times(3)).createdObject(objectCaptor.capture());
            List<TestObject> objects = objectCaptor.getAllValues();
            verify(logger).createdObject(objects.get(0));
            verify(logger).createdPool(config);
            // acquire object1
            verify(logger).releasingObjectResources(objects.get(0));
            verify(logger).releasedObjectResources(objects.get(0));
            verify(logger).objectInvalidated(objects.get(0), 0, 0);
            verify(logger).createdObject(object1);
            verify(logger).increasedObjectRefCount(object1, 1);
            verify(logger).acquiredObject(object1, 0, 1);
            // release object1
            verify(logger).decreasedObjectRefCount(object1, 0);
            verify(logger).returnedObject(object1, 1, 1);
            // acquire object2
            verify(logger).releasingObjectResources(object1);
            verify(logger).releasedObjectResources(object1);
            verify(logger).objectInvalidated(object1, 0, 0);
            verify(logger).createdObject(object2);
            verify(logger).increasedObjectRefCount(object2, 1);
            verify(logger).acquiredObject(object2, 0, 1);
            // release object2
            verify(logger).decreasedObjectRefCount(object2, 0);
            verify(logger).returnedObject(object2, 1, 1);

            verifyNoMoreInteractions(supplier, logger);

            assertSame(object1, objects.get(1));
            assertSame(object2, objects.get(2));

            // releaseResources is called for object1 when acquiring object2, because the object is not valid
            verify(object1).releaseResources();
            verify(object2, never()).releaseResources();
        }

        @Test
        @DisplayName("idle objects are removed")
        void testIdleObjectsAreRemoved() {
            PoolConfig config = PoolConfig.custom()
                    .withInitialSize(0)
                    .withMaxSize(1)
                    .withMaxWaitTime(Duration.ofMillis(100))
                    .withMaxIdleTime(Duration.ofMillis(-1))
                    .build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);

            RuntimeException releaseError = new NumberFormatException();
            when(supplier.get()).thenAnswer(i -> spy(new TestObject(true, () -> releaseError)));

            Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

            TestObject object1 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));

            object1.release();

            TestObject object2 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));

            assertNotSame(object1, object2);

            object2.release();

            verify(supplier, times(2)).get();
            // logger calls in order
            // create pool
            verify(logger).creatingPool(config);
            verify(logger).createdObject(object1);
            verify(logger).createdPool(config);
            // acquire object1
            verify(logger).increasedObjectRefCount(object1, 1);
            verify(logger).acquiredObject(object1, 0, 1);
            // release object1
            verify(logger).decreasedObjectRefCount(object1, 0);
            verify(logger).returnedObject(object1, 1, 1);
            // acquire object2; the size is decreased to 0 which allows a new object to be created
            verify(logger).objectIdleTooLong(object1, 0, 0);
            verify(logger).releasingObjectResources(object1);
            verify(logger).releaseObjectResourcesFailed(object1, releaseError);
            verify(logger).createdObject(object2);
            verify(logger).increasedObjectRefCount(object2, 1);
            verify(logger).acquiredObject(object2, 0, 1);
            // release object2
            verify(logger).decreasedObjectRefCount(object2, 0);
            verify(logger).returnedObject(object2, 1, 1);

            verifyNoMoreInteractions(logger);

            // releaseResources is called for object1 when acquiring object2, because the object has been idle for too long
            // object2 is freshly created
            verify(object1).releaseResources();
            verify(object2, never()).releaseResources();
        }

        @Test
        @DisplayName("return with multiple references")
        void testReturnWithMultipleReferences() {
            PoolConfig config = PoolConfig.custom()
                    .withInitialSize(1)
                    .withMaxSize(1)
                    .withMaxWaitTime(Duration.ofMillis(100))
                    .build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);

            when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

            Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

            TestObject object1 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));
            object1.addReference(pool);
            // second reference to the same object does not count
            object1.addReference(pool);

            object1.release();

            // object1 hasn't been returned yet
            assertThrows(NoSuchElementException.class, () -> pool.acquire(10, TimeUnit.MILLISECONDS));

            object1.removeReference("foo");

            // object1 hasn't been returned yet, an invalid reference was removed
            assertThrows(NoSuchElementException.class, () -> pool.acquire(10, TimeUnit.MILLISECONDS));

            object1.removeReference(pool);

            TestObject object2 = assertDoesNotThrow(() -> pool.acquire(1, TimeUnit.SECONDS));

            assertSame(object1, object2);

            object2.release();

            verify(supplier).get();
            // logger calls in order
            // create pool
            verify(logger).creatingPool(config);
            verify(logger).createdObject(object1);
            verify(logger).createdPool(config);
            // acquire object1 / object2
            verify(logger, times(2)).increasedObjectRefCount(object1, 1);
            verify(logger, times(2)).acquiredObject(object1, 0, 1);
            // add reference
            verify(logger).increasedObjectRefCount(object1, 2);
            // release object1
            verify(logger).decreasedObjectRefCount(object1, 1);
            // remove reference / release object2
            verify(logger, times(2)).decreasedObjectRefCount(object1, 0);
            verify(logger, times(2)).returnedObject(object1, 1, 1);

            verifyNoMoreInteractions(supplier, logger);

            verify(object1, never()).releaseResources();
            verify(object2, never()).releaseResources();
        }

        @Nested
        @DisplayName("release resources throws exception")
        class ReleaseResourcesThrowsException {

            @Test
            @DisplayName("single exception")
            void testSingleException() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(1)
                        .withMaxSize(1)
                        .withMaxWaitTime(Duration.ofMillis(100))
                        .build();
                AtomicInteger counter = new AtomicInteger(0);
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                when(supplier.get())
                        .thenAnswer(i -> new TestObject(true, () -> new NumberFormatException(Integer.toString(counter.getAndIncrement()))));

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                NumberFormatException exception = assertThrows(NumberFormatException.class, pool::shutdown);
                assertEquals("0", exception.getMessage());
                assertEquals(0, exception.getSuppressed().length);

                ArgumentCaptor<TestObject> objectCaptor = ArgumentCaptor.forClass(TestObject.class);

                verify(supplier).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger).createdObject(objectCaptor.capture());
                verify(logger).createdPool(config);
                // shutdown
                verify(logger).drainedPool(0);
                verify(logger).releasingObjectResources(objectCaptor.getValue());
                verify(logger).releaseObjectResourcesFailed(objectCaptor.getValue(), exception);
                verify(logger).shutDownPool();

                verifyNoMoreInteractions(supplier, logger);
            }

            @Test
            @DisplayName("multiple exceptions")
            void testMultipleExceptions() {
                PoolConfig config = PoolConfig.custom()
                        .withInitialSize(3)
                        .withMaxSize(3)
                        .withMaxWaitTime(Duration.ofMillis(100))
                        .build();
                @SuppressWarnings("unchecked")
                Supplier<TestObject> supplier = mock(Supplier.class);
                PoolLogger logger = mock(PoolLogger.class);

                AtomicInteger counter = new AtomicInteger(0);
                when(supplier.get())
                        .thenAnswer(i -> new TestObject(true, () -> new NumberFormatException(Integer.toString(counter.getAndIncrement()))));

                Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

                NumberFormatException exception = assertThrows(NumberFormatException.class, pool::shutdown);
                assertEquals("0", exception.getMessage());

                Throwable[] suppressed = exception.getSuppressed();
                assertEquals(2, suppressed.length);

                assertInstanceOf(NumberFormatException.class, suppressed[0]);
                assertEquals("1", suppressed[0].getMessage());

                assertInstanceOf(NumberFormatException.class, suppressed[1]);
                assertEquals("2", suppressed[1].getMessage());

                ArgumentCaptor<TestObject> objectCaptor = ArgumentCaptor.forClass(TestObject.class);

                verify(supplier, times(3)).get();
                // logger calls in order
                // create pool
                verify(logger).creatingPool(config);
                verify(logger, times(3)).createdObject(objectCaptor.capture());
                List<TestObject> objects = objectCaptor.getAllValues();
                verify(logger).createdPool(config);
                // shutdown
                verify(logger).drainedPool(0);
                verify(logger).releasingObjectResources(objects.get(0));
                verify(logger).releaseObjectResourcesFailed(objects.get(0), exception);
                verify(logger).releasingObjectResources(objects.get(1));
                verify(logger).releaseObjectResourcesFailed(objects.get(1), (Exception) suppressed[0]);
                verify(logger).releasingObjectResources(objects.get(2));
                verify(logger).releaseObjectResourcesFailed(objects.get(2), (Exception) suppressed[1]);
                verify(logger).shutDownPool();

                verifyNoMoreInteractions(supplier, logger);
            }
        }

        @Test
        @DisplayName("shutdown multiple times")
        void testShutdownMultipleTimes() {
            PoolConfig config = PoolConfig.custom()
                    .withInitialSize(2)
                    .withMaxSize(2)
                    .withMaxWaitTime(Duration.ofMillis(100))
                    .build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);

            when(supplier.get()).thenAnswer(i -> new TestObject());

            Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

            pool.shutdown();
            pool.shutdown();
            pool.shutdown();

            ArgumentCaptor<TestObject> objectCaptor = ArgumentCaptor.forClass(TestObject.class);

            verify(supplier, times(2)).get();
            // logger calls in order
            // create pool
            verify(logger).creatingPool(config);
            verify(logger, times(2)).createdObject(objectCaptor.capture());
            verify(logger).createdPool(config);
            // shutdown 1
            verify(logger).drainedPool(0);
            for (TestObject object : objectCaptor.getAllValues()) {
                verify(logger).releasingObjectResources(object);
                verify(logger).releasedObjectResources(object);
            }
            verify(logger).shutDownPool();
            // no events for shutdown 2+

            verifyNoMoreInteractions(supplier, logger);
        }
    }

    @Nested
    @DisplayName("forAllIdleObjects")
    class ForAllIdleObjects {

        @Test
        @DisplayName("no idle objects")
        void testNoIdleObjects() {
            PoolConfig config = PoolConfig.custom()
                    .withInitialSize(0)
                    .withMaxSize(2)
                    .build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);
            @SuppressWarnings("unchecked")
            PoolableObjectConsumer<TestObject, None> action = mock(PoolableObjectConsumer.class);

            when(supplier.get()).thenAnswer(i -> spy(new TestObject()));

            Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

            pool.forAllIdleObjects(action);

            verify(supplier, never()).get();

            // check that the pool is still usable
            TestObject object1 = assertIsPresent(pool.acquireNow());
            TestObject object2 = assertIsPresent(pool.acquireNow());
            assertIsEmpty(pool.acquireNow());

            object1.release();
            object2.release();

            verify(supplier, times(2)).get();
            // logger calls in order
            // create pool
            verify(logger).creatingPool(config);
            verify(logger).createdPool(config);
            // forAllIdleObjects
            verify(logger).drainedPool(0);
            // acquire object1
            verify(logger).createdObject(object1);
            verify(logger).increasedObjectRefCount(object1, 1);
            verify(logger).acquiredObject(object1, 0, 1);
            // acquire object2
            verify(logger).createdObject(object2);
            verify(logger).increasedObjectRefCount(object2, 1);
            verify(logger).acquiredObject(object2, 0, 2);
            // release object1
            verify(logger).decreasedObjectRefCount(object1, 0);
            verify(logger).returnedObject(object1, 1, 2);
            // release object2
            verify(logger).decreasedObjectRefCount(object2, 0);
            verify(logger).returnedObject(object2, 2, 2);

            verifyNoMoreInteractions(supplier, logger, action);

            verify(object1, never()).releaseResources();
            verify(object2, never()).releaseResources();
        }

        @Test
        @DisplayName("with idle objects")
        void testWithIdleObjects() {
            PoolConfig config = PoolConfig.custom()
                    .withInitialSize(3)
                    .withMaxSize(3)
                    .build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);
            @SuppressWarnings("unchecked")
            PoolableObjectConsumer<TestObject, None> action = mock(PoolableObjectConsumer.class);
            @SuppressWarnings("unchecked")
            Supplier<String> messageSupplier = mock(Supplier.class);

            when(supplier.get()).thenAnswer(i -> spy(new TestObject()));
            doAnswer(i -> {
                TestObject object = i.getArgument(0, TestObject.class);
                object.logEvent("custom event");
                object.logEvent(messageSupplier);
                return null;
            }).when(action).accept(any());
            when(messageSupplier.get()).thenReturn("custom supplied event");

            Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

            TestObject object1 = assertIsPresent(pool.acquireNow());

            pool.forAllIdleObjects(action);

            // check that the pool is still usable
            TestObject object2 = assertIsPresent(pool.acquireNow());
            TestObject object3 = assertIsPresent(pool.acquireNow());
            assertIsEmpty(pool.acquireNow());

            object1.release();
            object2.release();
            object3.release();

            verify(supplier, times(3)).get();
            verify(action).accept(object2);
            verify(action).accept(object3);
            // logger calls in order
            // create pool
            verify(logger).creatingPool(config);
            verify(logger).createdObject(object1);
            verify(logger).createdObject(object2);
            verify(logger).createdObject(object3);
            verify(logger).createdPool(config);
            // acquire object1
            verify(logger).increasedObjectRefCount(object1, 1);
            verify(logger).acquiredObject(object1, 2, 3);
            // forAllIdleObjects
            verify(logger).drainedPool(3);
            verify(logger).objectEvent(LogLevel.DEBUG, object2, "custom event");
            verify(logger).objectEvent(LogLevel.DEBUG, object2, messageSupplier);
            verify(logger).returnedObject(object2, 1, 3);
            verify(logger).objectEvent(LogLevel.DEBUG, object3, "custom event");
            verify(logger).objectEvent(LogLevel.DEBUG, object3, messageSupplier);
            verify(logger).returnedObject(object3, 2, 3);
            // acquire object2
            verify(logger).increasedObjectRefCount(object2, 1);
            verify(logger).acquiredObject(object2, 1, 3);
            // acquire object3
            verify(logger).increasedObjectRefCount(object3, 1);
            verify(logger).acquiredObject(object3, 0, 3);
            // release object1
            verify(logger).decreasedObjectRefCount(object1, 0);
            verify(logger).returnedObject(object1, 1, 3);
            // release object2
            verify(logger).decreasedObjectRefCount(object2, 0);
            verify(logger).returnedObject(object2, 2, 3);
            // release object3
            verify(logger).decreasedObjectRefCount(object3, 0);
            verify(logger).returnedObject(object3, 3, 3);

            verifyNoMoreInteractions(supplier, logger, action);

            verify(object1, never()).releaseResources();
            verify(object2, never()).releaseResources();
            verify(object3, never()).releaseResources();
        }

        @Test
        @DisplayName("with inactive idle objects")
        void testWithInactiveIdleObjects() {
            PoolConfig config = PoolConfig.custom()
                    .withInitialSize(2)
                    .withMaxSize(2)
                    .build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);
            @SuppressWarnings("unchecked")
            PoolableObjectConsumer<TestObject, None> action = mock(PoolableObjectConsumer.class);
            @SuppressWarnings("unchecked")
            Supplier<String> messageSupplier = mock(Supplier.class);

            when(supplier.get()).thenAnswer(i -> spy(new TestObject(false)));
            doAnswer(i -> {
                TestObject object = i.getArgument(0, TestObject.class);
                object.logEvent("custom event");
                object.logEvent(messageSupplier);
                return null;
            }).when(action).accept(any());
            when(messageSupplier.get()).thenReturn("custom supplied event");

            Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

            pool.forAllIdleObjects(action);

            // check that the pool is still usable
            TestObject object1 = assertIsPresent(pool.acquireNow());
            TestObject object2 = assertIsPresent(pool.acquireNow());
            assertIsEmpty(pool.acquireNow());

            object1.release();
            object2.release();

            ArgumentCaptor<TestObject> objectCaptor = ArgumentCaptor.forClass(TestObject.class);

            verify(supplier, times(4)).get();
            // logger calls in order
            // create pool
            verify(logger).creatingPool(config);
            verify(logger, times(4)).createdObject(objectCaptor.capture());
            List<TestObject> objects = objectCaptor.getAllValues();
            verify(logger).createdObject(objects.get(0));
            verify(logger).createdObject(objects.get(1));
            verify(logger).createdPool(config);
            // forAllIdleObjects
            verify(logger).releasingObjectResources(objects.get(0));
            verify(logger).releasedObjectResources(objects.get(0));
            verify(logger).objectInvalidated(objects.get(0), 1, 1);
            verify(logger).releasingObjectResources(objects.get(1));
            verify(logger).releasedObjectResources(objects.get(1));
            verify(logger).objectInvalidated(objects.get(1), 0, 0);
            verify(logger).drainedPool(0);
            // acquire object1
            verify(logger).createdObject(object1);
            verify(logger).increasedObjectRefCount(object1, 1);
            verify(logger).acquiredObject(object1, 0, 1);
            // acquire object2
            verify(logger).createdObject(object2);
            verify(logger).increasedObjectRefCount(object2, 1);
            verify(logger).acquiredObject(object2, 0, 2);
            // release object1
            verify(logger).decreasedObjectRefCount(object1, 0);
            verify(logger).returnedObject(object1, 1, 2);
            // release object2
            verify(logger).decreasedObjectRefCount(object2, 0);
            verify(logger).returnedObject(object2, 2, 2);

            verifyNoMoreInteractions(supplier, logger, action);

            assertSame(object1, objects.get(2));
            assertSame(object2, objects.get(3));

            verify(object1, never()).releaseResources();
            verify(object2, never()).releaseResources();
        }

        @Test
        @DisplayName("with throwing action")
        void testWithThrowingAction() {
            PoolConfig config = PoolConfig.custom()
                    .withInitialSize(3)
                    .withMaxSize(3)
                    .build();
            @SuppressWarnings("unchecked")
            Supplier<TestObject> supplier = mock(Supplier.class);
            PoolLogger logger = mock(PoolLogger.class);
            @SuppressWarnings("unchecked")
            PoolableObjectConsumer<TestObject, None> action = mock(PoolableObjectConsumer.class);

            when(supplier.get()).thenAnswer(i -> spy(new TestObject()));
            AtomicInteger counter = new AtomicInteger(0);
            doAnswer(i -> {
                throw new NumberFormatException(Integer.toString(counter.getAndIncrement()));
            }).when(action).accept(any());

            Pool<TestObject, None> pool = Pool.throwingNone(config, supplier, logger);

            TestObject object1 = assertIsPresent(pool.acquireNow());

            NumberFormatException exception = assertThrows(NumberFormatException.class, () -> pool.forAllIdleObjects(action));
            assertEquals("0", exception.getMessage());

            Throwable[] suppressed = exception.getSuppressed();
            assertEquals(1, suppressed.length);

            assertInstanceOf(NumberFormatException.class, suppressed[0]);
            assertEquals("1", suppressed[0].getMessage());

            // check that the pool is still usable
            TestObject object2 = assertIsPresent(pool.acquireNow());
            TestObject object3 = assertIsPresent(pool.acquireNow());
            assertIsEmpty(pool.acquireNow());

            object1.release();
            object2.release();
            object3.release();

            verify(supplier, times(3)).get();
            verify(action).accept(object2);
            verify(action).accept(object3);
            // logger calls in order
            // create pool
            verify(logger).creatingPool(config);
            verify(logger).createdObject(object1);
            verify(logger).createdObject(object2);
            verify(logger).createdObject(object3);
            verify(logger).createdPool(config);
            // acquire object1
            verify(logger).increasedObjectRefCount(object1, 1);
            verify(logger).acquiredObject(object1, 2, 3);
            // forAllIdleObjects
            verify(logger).drainedPool(3);
            verify(logger).returnedObject(object2, 1, 3);
            verify(logger).returnedObject(object3, 2, 3);
            // acquire object2
            verify(logger).increasedObjectRefCount(object2, 1);
            verify(logger).acquiredObject(object2, 1, 3);
            // acquire object3
            verify(logger).increasedObjectRefCount(object3, 1);
            verify(logger).acquiredObject(object3, 0, 3);
            // release object1
            verify(logger).decreasedObjectRefCount(object1, 0);
            verify(logger).returnedObject(object1, 1, 3);
            // release object2
            verify(logger).decreasedObjectRefCount(object2, 0);
            verify(logger).returnedObject(object2, 2, 3);
            // release object3
            verify(logger).decreasedObjectRefCount(object3, 0);
            verify(logger).returnedObject(object3, 3, 3);

            verifyNoMoreInteractions(supplier, logger, action);

            verify(object1, never()).releaseResources();
            verify(object2, never()).releaseResources();
            verify(object3, never()).releaseResources();
        }
    }
}
