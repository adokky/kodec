[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

# kodec

Collection of binary and text processing primitives with focus on high performance and simplicity.

Why not [okio](https://github.com/square/okio) / [kotlinx-io](https://github.com/Kotlin/kotlinx-io)?

Because both of them don't provide a zero-copy array-like abstraction that allows you to wrap anything other than `ByteArray`.

There are other notable differences:
* allocation-free `Float`/`Double` encoding/decoding
* `TextReader` abstraction allows you to implement allocation-free decoding for any text format
* "template" modules for decoding basic primitives with zero performance impact. Compatible with any data structure.
* highly modular. Peek only what you want

## Modules

### `kodec-buffers-core`

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.adokky/kodec-buffers-core)
[![javadoc](https://javadoc.io/badge2/io.github.adokky/kodec-buffers-core/javadoc.svg)](https://javadoc.io/doc/io.github.adokky/kodec-buffers-core)

Simple array-like structure implementation.

Separated into read-only and mutable interfaces.
The module is compact and intended to be used as a basic memory IO integration layer between different libraries and frameworks.


### `kodec-buffers-data`

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.adokky/kodec-buffers-data)
[![javadoc](https://javadoc.io/badge2/io.github.adokky/kodec-buffers-data/javadoc.svg)](https://javadoc.io/doc/io.github.adokky/kodec-buffers-data)

`kodec-buffers-core` extensions for encoding/decoding strings and numbers.

### `kodec-strings-stream`

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.adokky/kodec-strings-stream)
[![javadoc](https://javadoc.io/badge2/io.github.adokky/kodec-strings-stream/javadoc.svg)](https://javadoc.io/doc/io.github.adokky/kodec-strings-stream)

* `TextReader` - streaming decoder for strings, numbers, characters. Supported sources: `Buffer`, `String`.
* `TextWriter` - streaming encoder for strings, chars, numbers. Supported outputs: `MutableBuffer`, `StringBuilder`.

### `kodec-strings-number`

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.adokky/kodec-strings-number)
[![javadoc](https://javadoc.io/badge2/io.github.adokky/kodec-strings-number/javadoc.svg)](https://javadoc.io/doc/io.github.adokky/kodec-strings-number)

Convert numbers to string and back (for both stream and random access structures).
  * `Int`,`Long` encoding/decoding
  * `Float`,`Double` encoding/decoding. 100% Kotlin. Allocation-free port of Java standard library implementation.

### `kodec-struct` (experimental)

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.adokky/kodec-struct)
[![javadoc](https://javadoc.io/badge2/io.github.adokky/kodec-struct/javadoc.svg)](https://javadoc.io/doc/io.github.adokky/kodec-struct)

Type-safe way to encode/decode flat binary structures.

## "Template" modules

By "template" we mean a module mostly consists of inline functions.
Template module does not depend on anything and can be applied to any byte stream or byte buffer. 

### `kodec-binary-num`

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.adokky/kodec-binary-num)
[![javadoc](https://javadoc.io/badge2/io.github.adokky/kodec-binary-num/javadoc.svg)](https://javadoc.io/doc/io.github.adokky/kodec-binary-num)

Templates for binary encoding/decoding of numbers in plain BE/LE and variable-length format

### `kodec-strings-common`

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.adokky/kodec-strings-common)
[![javadoc](https://javadoc.io/badge2/io.github.adokky/kodec-strings-common/javadoc.svg)](https://javadoc.io/doc/io.github.adokky/kodec-strings-common)

A tiny module with common string encoding utilities

### `kodec-strings-utf`

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.adokky/kodec-strings-utf)
[![javadoc](https://javadoc.io/badge2/io.github.adokky/kodec-strings-utf/javadoc.svg)](https://javadoc.io/doc/io.github.adokky/kodec-strings-utf)

Templates for encoding/decoding UTF-8 strings
