# KWasm

KWasm is an Embeddable WebAssembly interpreter for the JVM

[![CircleCI](https://img.shields.io/circleci/build/github/jasonwyatt/KWasm/master?style=flat-square)](https://circleci.com/gh/jasonwyatt/KWasm/tree/master)
[![CII Best Practices Summary](https://img.shields.io/cii/summary/3559?label=cii%20best%20practices&style=flat-square)](https://bestpractices.coreinfrastructure.org/en/projects/3559)
[![Code Climate maintainability](https://img.shields.io/codeclimate/maintainability-percentage/jasonwyatt/KWasm?style=flat-square)](https://codeclimate.com/github/jasonwyatt/KWasm)

## Goals

The primary goal of KWasm is to be a relatively lightweight, embeddable, WebAssembly interpreter for applications 
running in the JVM (or on Android).  Beyond that, KWasm strives to:

* meet the latest current [WebAssembly Specification](https://webassembly.github.io/spec/core/index.html)'s requirements,
* use minimal system resources,
* maintain WebAssembly's strict sandbox-like functionality, and
* support interoperation between Java/Kotlin code and the WebAssembly it is tasked with running.

Achieving good performance is a goal as well, but as with all interpeters: performance will never be as good as a 
compiled solution.

The primary benefit of interpretation over compilation is the capability for dynamic module loading & execution.

## Milestones

The development of KWasm will be done in a series of milestones:

1. ✅ Implement text-based Wasm Parser Capability & AST-generation. (Completed 2020-01-01)
1. ✅ Use the text-based Wasm Parser to develop/test the interpretation of the AST. (Completed 2020-12-23)
1. ✅ Implement a binary wasm parser with AST-generation. (Completed 2021-01-07)

With optional milestones:

4. Implement a WASM->Kotlin transpiler.
1. Implement a WASM->JVM Bytecode compiler.

Where milestones 2 and 3 are parallelizable.

## Recommended Development Environment Setup

Prerequisites:

* [IntelliJ IDEA](https://www.jetbrains.com/idea/download) or [Android Studio](https://developer.android.com/studio/index.html)

From the directory of your choice:

1. Clone the repository to your local machine.
1. Open IntelliJ/Android Studio, and Choose "New Project from Existing Sources"
1. Select the `KWasm` directory created in step 1.

That's it! The IDE will sync the project and you can begin working on KWasm.

### Running Tests

You can run the entire test suite with:

```bash
./gradlew test
```

## License

```
Copyright 2019 Google LLC 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
