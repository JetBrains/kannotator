package kotlinlib

public fun flags(f1: Int, f2: Int): Int = f1 or f2

public fun flags(f1: Int, f2: Int, f3: Int): Int = f1 or f2 or f3

public fun flags(f1: Int, f2: Int, f3: Int, f4: Int): Int = f1 or f2 or f3 or f4

public fun flags(f1: Int, f2: Int, f3: Int, f4: Int, f5: Int): Int = f1 or f2 or f3 or f4 or f5

public fun flags(f1: Int, f2: Int, f3: Int, f4: Int, f5: Int, vararg otherFlags: Int): Int {
    var res = f1 or f2 or f3 or f4 or f5
    for (f in otherFlags) {
        res = res or f
    }
    return res
}

