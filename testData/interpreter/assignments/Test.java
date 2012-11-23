package interpreter.assignments;

public class Test {
    public static final Object[] NO_ARGS = {};

    public Object foo(Object[] args) {
        if (args == null) {
            args = NO_ARGS;
        }
        return args.length;
    }

    /*public static Throwable getRootCause(Throwable throwable) {
        Throwable cause;
        while ((cause = throwable.getCause()) != null) {
            throwable = cause;
        }
        return throwable;
    }*/
}
