package util;

import org.junit.Rule;
import org.junit.rules.TestName;

public class NamedTest {
    @Rule public TestName name = new TestName();
    public String getName() {
        return name.getMethodName();
    }
}