(module
    (import "host" "toast" (func $toast (param i32 i32)))
    (import "host" "getMessageSetting" (func $getMessageSetting (result i32)))
    (memory 1 1)
    (data (i32.const 0) "Hello world!") (global $m1Len i32 (i32.const 12))
    (data (i32.const 12) "This is WASM Speaking") (global $m2Len i32 (i32.const 21))
    (data (i32.const 33) "Running on Android 🤣") (global $m3Len i32 (i32.const 26))
    (func $main (local i32 i32 i32)
        (local.set 0
            (i32.rem_u (call $getMessageSetting) (i32.const 3)))

        (if (i32.eq (i32.const 0) (local.get 0))
            (then
                (local.set 1 (i32.const 0))
                (local.set 2 (global.get $m1Len)))
            (else
                (if (i32.eq (i32.const 1) (local.get 0))
                    (then
                        (local.set 1 (i32.const 12))
                        (local.set 2 (global.get $m2Len)))
                    (else
                        (local.set 1 (i32.const 33))
                        (local.set 2 (global.get $m3Len))))))
        (call $toast (local.get 1) (local.get 2)))
    (start $main))
