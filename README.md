# GeAnTyRef

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.leangen.geantyref/geantyref/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.leangen.geantyref/geantyref)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/io.leangen.geantyref/geantyref/badge.svg)](http://www.javadoc.io/doc/io.leangen.geantyref/geantyref)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg?maxAge=2592000)](https://raw.githubusercontent.com/leangen/geantyref/master/LICENSE)
[![Semver](http://img.shields.io/SemVer/2.0.0.png)](http://semver.org/spec/v2.0.0.html)

A fork of the excellent [GenTyRef](https://code.google.com/archive/p/gentyref/) library, adding support for working with [AnnotatedTypes](https://jaxenter.com/jsr-308-explained-java-type-annotations-107706.html) introduced in Java 8 plus many nifty features.

**Table of Contents**

- [GeAnTyRef](#)
	- [Goal](#goal)
	- [Overview](#overview)
	- [Usage](#usage)
		- [Maven](#maven)
		- [Other build tools](#other-build-tools])
	- [Examples](#examples)
		- [Getting the exact return type of a method](#getting-the-exact-return-type-of-a-method)
		- [Getting the exact type of a field](#getting-the-exact-type-of-a-field)
		- [Getting the exact types of method parameters](#getting-the-exact-types-of-method-parameters)
		- [Getting the exact super type](#getting-the-exact-super-type)
		- [Getting the exact sub type](#getting-the-exact-sub-type)
		- [Getting annotated return/parameter/field/sub/super types](#getting-annotated-returnparameterfieldsubsuper-types)
		- [Creating type literals using TypeToken](#creating-type-literals-using-typetoken)
		- [Creating annotated type literals using TypeToken](#creating-annotated-type-literals-using-typetoken)
		- [Creating types dynamically using TypeFactory](#creating-types-dynamically-using-typefactory)
		- [Creating annotated types dynamically using TypeFactory](#creating-annotated-types-dynamically-using-typefactory)
		- [Turning any Type into an AnnotatedType](#turning-any-type-into-an-annotatedtype)
		- [More](#more)
	- [Wiki](#wiki)
	- [License](#license)

## Goal

This library aims to provide a simple way to analyse generic type information and dynamically create
`(Annotated)Type` instances, all at runtime.

## Overview

All functionality of the library is exposed via a handful of classes:

* `GenericTypeReflector` : contains static methods used for generic type analysis
* `TypeFactory` : contains static methods used for `Type/AnnotatedType` instance creation
* `TypeToken` : Used to create `Type/AnnotatedType` literals (using [THC pattern](http://gafter.blogspot.nl/2006/12/super-type-tokens.html))

## Usage

### Maven:

```xml
<dependency>
    <groupId>io.leangen.geantyref</groupId>
    <artifactId>geantyref</artifactId>
    <version>1.2.1</version>
</dependency>
```

### Other build tools:

You can find instructions at [maven.org](https://search.maven.org/#artifactdetails%7Cio.leangen.geantyref%7Cgeantyref%7C1.2.1%7Cjar)

## Examples

### Getting the exact return type of a method

The simplest example would be having a class similar to this:

```java
class StringList extends ArrayList<String> {
    ...
}
```
Getting the exact return type of `StringList`'s `get` method is rather difficult:
```java
Method get = StringList.class.getMethod(""get", int.class);
get.getGenericReturnType() //yields T, which is not very useful information
```

On the other hand, running `GenericTypeReflector.getExactReturnType(get, StringClass.class)`
would yield `String` which is what we were looking for.

### Getting the exact type of a field

Presume we have two simple classes:

```java
class Container<T> {
    public T item;
}

class NumberContainer extends Container<Number> {}
```
We again face issues when trying to discover the exact type of the `item` field:

```java
Field item = NumberContainer.class.getField("item");
item.getGenericType(); //yields T again
```

Instead, `GenericTypeReflector.getExactFieldType(item, NumberContainer.class)` returns `Number` as desired.

### Getting the exact types of method parameters

`GenericTypeReflector.getExactParameterTypes(methodOrConstructor, aType)`

### Getting the exact super type

If we had the classes defined as follows:

```java
class Container<T> {
    public T item;
}

class NumberContainer<T extends Number> extends Container<T> {}

class LongContainer extends NumberContainer<Long> {}
```
If we'd call `LongContainer.class.getGenericSuperclass()` it would correctly return `NumberContainer<Long>`
but getting from there to `Container<Long>` is much more difficult, as there's no direct way.

GeAnTyRef allows us to simply call
`GenericTypeReflector.getExactSuperType(LongContainer.class, Container.class)` to get `Container<Long>`

### Getting the exact sub type

Even more interestingly, it is sometimes possible to get the exact sub type of a type.
For example, if we had `List<String>` and we wanted `ArrayList<String>` if would be possible,
as the `ArrayList`'s sole type parameter is coming from `List`, i.e. `ArrayList` does not define
and type parameters itself, `List<String>` already contains all the needed information.
This is rather difficult to calculate using standard reflection, but if we already had a reference to
`List<String>` called `listOfString`, it is enough to call:

`GenericTypeReflector.getExactSubType(listOfString, ArrayList.class)` to get `ArrayList<String>`

Still, how to get to `listOfString`? Probably by calling one of the methods described above.
But, it also possible to create a type literal directly via `TypeToken`, or construct the desired 
`Type` (`List<String>`) dynamically using `TypeFactory`.

### Getting annotated return/parameter/field/sub/super types

It is worth noting that all the basic methods on the `GenericTypeReflector` listed above have 
overloads that work with `AnnotatedType`s.

```java
class Person {
    public List<@NonNull String> nicknames;
}

AnnotatedType listOfNonNullStrings = Person.class.getField("nicknames").getAnnotatedType();

Class raw = GenericTypeReflector.erase(listOfNonNullStrings.getType());
Method get = raw.getMethod("get", int.class);

//returns an AnnotatedType corresponding to: @NonNull String
AnnotatedType nonNullString = GenericTypeReflector.getExactReturnType(get, listOfNonNullStrings);
```
Similarly, `getExactFieldType`, `getExactParameterTypes`, `getExactSuperType`, `getExactSubType`
work with `AnnotatedType`s.

### Creating type literals using `TypeToken`

This approach is known as _Typesafe Heterogenous Container_ (THC) or _type token_, is widely used
in libraries like Jackson or Gson that need to work with generic types. There are various sources
describing the intricacies of this approach, [Neal Gafter's blog](http://gafter.blogspot.nl/2006/12/super-type-tokens.html) being a classic one.

To obtain a `Type` instance representing a know generic type, such as `List<String>` it is enough to
do the following:

`Type listOfString = new TypeToken<List<String>>(){}.getType();`

What we're doing here is creating an anonymous subclass of a parameterized type (`TypeToken`) and
getting it's generic super class via `getGenericSuperclass`.

### Creating annotated type literals using `TypeToken`

If instead we wanted an instance of an annotated type, such as `List<@NonNull String>`, the same
principle would apply:

`AnnotatedType listOfNonNullString = new TypeToken<List<@NonNull String>>(){}.getAnnotatedType();`

### Creating types dynamically using `TypeFactory`

`TypeToken` is only useful if all the generic type information is known ahead of time. It will not
allow us to create a `Type` or `AnnotatedType` dynamically. For such a task, `TypeFactory` provides
adequate methods.

```java
Class<List> listType = List.class;
Class<String> typeParameter = String.class;

//returns a Type representing List<String>
Type listOfString = TypeFactory.parameterizedClass(listType, typeParameter);

Class<Map> mapType = Map.class;
Class<String> keyTypeParameter = String.class;
Class<Number> valueTypeParameter = Number.class;

//returns a Type representing Map<String, Number>
Type mapOfStringNumber = TypeFactory.parameterizedClass(mapType, keyTypeParameter, valueTypeParameter);
```

### Creating annotated types dynamically using `TypeFactory`

`TypeFactory` can also produce `AnnotatedType` instances, but this means we need to somehow obtain
instances of the annotations themselves (instances of `Annotation`).
An obvious way of getting them would be from an existing `AnnotatedElement` (`annotatedElement.getAnnotations()`).
`Class`, `Parameter`, `Method`, `Constructor`, `Field` all implement `AnnotatedElement`.

But, `TypeFactory` also allows you to instantiate an `Annotation` dynamically:

```java
Map<String, Object> annotationParameters = new HashMap<>();
annotationParameters.put("name", "someName");
MyAnnotation myAnnotation = TypeFactory.annotation(MyAnnotation.class, annotationParameters);
```
This produces an `Annotation` instance as if `@MyAnnotation(name = "someName")` was found in the sources.

Armed with this knowledge, it's easy to produce an `AnnotatedType`:
```java
Class<List> listType = List.class;
Class<String> typeParameter = String.class;
Annotation[] annotations = { myAnnotation };

//returns an AnnotatedType representing: @MyAnnotation(name = "someName") List<String>
AnnotatedType annotatedListOfString = TypeFactory.parameterizedClass(listType, annotations, typeParameter);
```

### Turning any `Type` into an `AnnotatedType`

`GenericTypeReflector` also allows you to wrap any `Type` into an `AnnotatedType` by collecting its
direct annotations. For example:

```java
@SuppressWarnings("unchecked")
class Something {}

//returns an AnnotatedType representing: @SuppressWarnings("unchecked") Something
AnnotatedType something = GenericTypeReflector.annotate(Something.class);
```

This method will correctly turn a `ParameterizedType` into an `AnnotatedParameterizedType`,
`WildcardType` into an `AnnotatedWildcardType` etc.

### More

There are more features provided by `GenericTypeReflector` that were not described in depth here,
like the possibility to replace annotations on an `AnnotatedType` via `replaceAnnotations`,
or update them via `updateAnnotations`, calculate hash codes and check equality of `AnnoatedType`s
(as `equals` and `hasCode` are not overridden in Java's `AnnotatedType` implementations) etc, so
feel free to explore a bit on your own.

## Wiki

More info can be found at the project [Wiki](https://github.com/leangen/geantyref/wiki).

## License
[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)
