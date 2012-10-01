package inferenceData;

import inferenceData.annotations.ExpectMutable;

import java.util.*;

public class MutabilityTest {

    public void testMutableCollection(@ExpectMutable Collection<Integer> collection) {
        collection.add(2);
    }

    public void testImmutableCollection(Collection<Integer> collection) {
        collection.iterator();
    }

    public void testIterateOverMutableCollection(@ExpectMutable Collection<Integer> collection) {
        Iterator<Integer> iterator = collection.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == 0) {
                iterator.remove();
            }
        }
    }


}
