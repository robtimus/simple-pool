# simple-pool
[![Maven Central](https://img.shields.io/maven-central/v/com.github.robtimus/simple-pool)](https://search.maven.org/artifact/com.github.robtimus/simple-pool)
[![Build Status](https://github.com/robtimus/simple-pool/actions/workflows/build.yml/badge.svg)](https://github.com/robtimus/simple-pool/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.github.robtimus%3Asimple-pool&metric=alert_status)](https://sonarcloud.io/summary/overall?id=com.github.robtimus%3Asimple-pool)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=com.github.robtimus%3Asimple-pool&metric=coverage)](https://sonarcloud.io/summary/overall?id=com.github.robtimus%3Asimple-pool)
[![Known Vulnerabilities](https://snyk.io/test/github/robtimus/simple-pool/badge.svg)](https://snyk.io/test/github/robtimus/simple-pool)

A simple object pool implementation. It is designed to be small and lightweight.

This library does not enforce an exception type like `IOException`, nor is it limited to only unchecked exceptions. Instead, each pool and its related classes have a generic type for the exception type that can be thrown. This allows callers to provide their own exception types. For cases where no exceptions are thrown, a marker exception type is provided: [None](https://robtimus.github.io/simple-pool/apidocs/com/github/robtimus/pool/None.html).

## Creating pools

A pool needs 3 to 4 things:
* A type of object that can be pooled. This should be provided as a sub class of [PoolableObject](https://robtimus.github.io/simple-pool/apidocs/com/github/robtimus/pool/PoolableObject.html).
* Configuration for the pool. This includes the initial and maximum pool sizes.
* A factory for creating pooled objects.
* Optionally, a logger that will be triggered for specific events in the pool.

 Class [Pool](https://robtimus.github.io/simple-pool/apidocs/com/github/robtimus/pool/Pool.html) provides two factory methods that accept a `Supplier` instead of the factory, and use `None` as the exception type. For instance, where `MyObject` extends `PoolableObject<None>`:

```java
Pool<MyObject, None> pool = Pool.throwingNone(config, MyObject::new, logger);
```

### PoolableObject

`PoolableObject` is the base class for all object types that can be stored in a pool. Sub classes should implement `isValid`, `doReleaseResources` and optionally `doReleaseResourcesQuietly`. In addition, they should provide a method that calls `release`. This method should be called to return the object to the pool. The `close` method of interface `AutoCloseable` `Closeable` is a good candidate.

A `PoolableObject` can have other references added. Instances are only returned to the pool if all references have been removed. This can be used if the object can expose other objects like `InputStream` or `OutputStream`. By adding such objects as references to the object and removing the references when these objects are no longer necessary (e.g. when they are closed), it's possible to delay returning objects to the pool. This allows sub classes to be released without having to worry about these references themselves. For instance:

```java
class MyConnection extends PoolableObject<IOException> {

    ...

    InputStream getInputStream() throws IOException {
        InputStream inputStream = ...;
        // when inputStream.close() is called, it should call removeReference(inputStream)
        addReference(customInputStream);
        return inputStream;
    }
}
```

### Configuration

Class [PoolConfig](https://robtimus.github.io/simple-pool/apidocs/com/github/robtimus/pool/PoolConfig.html) can be used to provide configuration. It currently supports 4 settings:

* The initial size (default: 1) - the initial number of objects in the pool.
* The maximum pool size (default: 5).
* The maximum wait time (default: indefinitely) - if an object is acquired without an explicit wait time, this value will be used.
* The maximum idle time (default: indefinitely) - any objects that have been in the pool longer than this time will be removed from the pool.

When acquiring objects from the pool, an idle object will be used if available. Otherwise, if the current pool size is smaller than the maximum, a new object will be created.

### Factory

Interface [PoolableObjectFactory](https://robtimus.github.io/simple-pool/apidocs/com/github/robtimus/pool/PoolableObjectFactory.html) is a functional interface similar to `Supplier` that is used to create new objects. It shares the exception type it can throw with the `PoolableObject` instances it can create.

### Logger

Class [PoolLogger](https://robtimus.github.io/simple-pool/apidocs/com/github/robtimus/pool/PoolLogger.html) can be used to provide logging. It uses SLF4J for this; if it is not available on the class path logging will be disabled.
A pool logger can use a custom logger name, log message prefix or object prefix. This allows you to customize the logging somewhat. For instance, with log message prefix `ftp.example.org - ` and object prefix `connection-`, a message could look like this:

```
ftp.example.org - Acquired object 'connection-4' from pool; idle: 2, pool size: 5
```

## Using pools

A pool only has a few methods:

* `acquire` will return an object from the pool, blocking until one is available. The method is overloaded to provide the wait timeout, as well as the exception that is thrown when the timeout expires.
* `acquireNow` will return an object from the pool, wrapped in an `Optional`. If no object is available immediately, an empty `Optional` is returned instead.
* `acquireOrCreate` will return an object from the pool. If no object is available immediately, a new non-pooled object is created instead. This can be used in case an object is needed (so `acquireNow` cannot be used), but blocking may cause deadlock or similar issues. In these cases, it's usually better to just use a disposable object.
* `forAllIdleObjects` will internally acquire all idle objects, perform an operation on them, and then return them to the pool. This can for instance be used to send keep-alive signals to all idle objects.
* `shutdown` will release the resources of all idle objects, remove them, and prevent any other operation on the pool. Any call to `acquire` that is blocked will throw an `IllegalStateException`. This can be used to cleanup all resources.
* `isActive` can be used to query the state of the pool. It will return `true` until `shutdown` is called.

An acquired object should eventually be released. It is up to the `PoolableObject` sub class to expose the `release` method, either by overriding it and making it public, or calling it from a different public method like `close`.

For instance, assuming `MyObject` extends `PoolableObject<IOException>`, implements `AutoCloseable`, and calls `release` from its `close` method:

```java
Pool<MyObject, IOException> pool = new Pool<>(config, MyObject::new, logger);
try (MyObject object = pool.acquire()) { // note: may throw InterruptedException
    // use object as needed
}
// object.close() will have called object.release(), which means object has been returned to the pool
```

### ObjectWrapper

To make it easy to create pools of objects that are always valid and don't need any resource cleanup, class [ObjectWrapper](https://robtimus.github.io/simple-pool/apidocs/com/github/robtimus/pool/ObjectWrapper.html) is a `PoolableObject` sub class that simply wraps a value. It implements `AutoCloseable`, so it can be used in try-with-resources blocks:

```java
Pool<ObjectWrapper<MyObject>, None> pool = ObjectWrapper.newPool(config, MyObject::new, logger);
try (ObjectWrapper<MyObject> wrapper = pool.acquire()) { // note: may throw InterruptedException
    MyObject object = wrapper.value();
    // use object as needed
}
// wrapper.close() will have returned the wrapper to the pool
```
