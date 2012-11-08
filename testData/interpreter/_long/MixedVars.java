package interpreter._long;

public class MixedVars {
    public long run(Object o) {
        long a;
        if (o != null) {
            int i = (Math.random() > 0.5) ? 0 : 1;
            int j = 0;
            a = i + j;
        } else {
            long l = (Math.random() > 0.5) ? 12L : 21L;
            long k = 10L;
            a = l + k;
        }
        return a;
    }
}
