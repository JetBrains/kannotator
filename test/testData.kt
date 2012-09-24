class TestSubject {
    fun foo(param: String) {
        var i: Any? = param
        if (1 > 2) {
            //            i = "b"
            //        }
            //        else {
            i = null
        }
        //        while (i != null) {
        //             i = 2
        //             if (i == null) {
        //                 i = 3
        //             }
        //             else {
        //                 i = null
        //             }
        //        }
        println(i)
    }
}
