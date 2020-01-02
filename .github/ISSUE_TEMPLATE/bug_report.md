---
name: Bug report
about: Create a report to help us improve
title: ''
labels: P?
assignees: jasonwyatt

---

**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Step 1
2. Step 2
3. ???
4. Profit (error happened)

```wasm
(; Your web assembly which resulted in unexpected behavior, if applicable ;)
(module
  (import "output" "printLn" (func $printLn))
  (memory (data "Hello World"))
  (func $helloWorld
    (call $printLn (i32.const 0))
  )
  (start $helloWorld)
)
```

**Expected behavior**
A clear and concise description of what you expected to happen.

**Environment (please complete the following information):**
 - OS: [e.g. Android, MacOS, Windows]
 - JDK Version: [e.g. 6]
 - Kotlin Version: [e.g. 1.3.0]

**Additional context**
Add any other context about the problem here.
