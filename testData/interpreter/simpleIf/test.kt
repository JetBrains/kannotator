package interpreter.simpleIf

class Test {
    fun foo(): Int {
        var i: Int
        if (1 > 2) {
            i = 1
        }
        else {
            i = 2
        }
        return i
    }

}