[![CircleCI](https://circleci.com/gh/jasonwyatt/KWasm/tree/master.svg?style=svg)](https://circleci.com/gh/jasonwyatt/KWasm/tree/master)

# KWasm
Kotlin Interpreter for WebAssembly

## Milestones

The development of KWasm will be done in a series of milestones:

1. Implement text-based Wasm Parser Capability & AST-generation.
1. Use the text-based Wasm Parser to develop/test the interpretation of the AST.
1. Implement a binary wasm parser with AST-generation.

With optional milestones:

4. Implement a WASM->Kotlin transpiler.
1. Implement a WASM->JVM Bytecode compiler.

Where milestones 2 and 3 are parallelizable.

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
