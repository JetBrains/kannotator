package interpreter.oneParam

class Test {
    fun foo(p: Any): Any? {
        var i: Any?
        if (1 > 2) {
            i = null
        }
        else {
            i = p
        }
        return i
    }

    fun fooUnit(p: Any) {
        var i: Any?
        if (1 > 2) {
            i = null
        }
        else {
            i = p
        }
        println(i)
    }

}