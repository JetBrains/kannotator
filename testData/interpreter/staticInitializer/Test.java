package interpreter.staticInitializer;

import inferenceData.annotations.ExpectNotNull;
import inferenceData.annotations.ExpectNullable;

public class Test {
    public final static String VALUE;

    @ExpectNullable
    public final static String STRING_NULL_FIELD = null;

    @ExpectNotNull
    public final static Object NEW_OBJECT_FIELD = new Object();

    static {
        int i1 = 3;
        int i2 = 2 + i1;
        if (i1 > i2) {
            VALUE = "NOTNULL";
        } else {
            VALUE = null;
        }
    }
}
