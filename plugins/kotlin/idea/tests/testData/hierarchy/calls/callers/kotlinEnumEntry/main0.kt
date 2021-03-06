enum class C {
    A,
    B,
    <caret>C
}

fun foo(x: C): Int =
    when (x) {
        C.A -> 1
        C.B -> 2
        C.C -> 3
    }

fun bar(x: C) {
    if (x == C.C)
        println("4")
    else
        println("${foo(x)}")
}

val baz = bar(C.A)
