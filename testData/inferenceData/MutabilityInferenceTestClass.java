package inferenceData;

import inferenceData.annotations.ExpectMutable;
import java.util.*;
import java.util.List;

public class MutabilityInferenceTestClass {

    public void mutableCollection(@ExpectMutable Collection<Integer> collection) {
        collection.add(2);
    }

    public void immutableCollection(Collection<Integer> collection) {
        collection.iterator();
    }

    public void iterateOverMutableCollection(@ExpectMutable Collection<Integer> collection) {
        Iterator<Integer> iterator = collection.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == 0) {
                iterator.remove();
            }
        }
    }

    public void mapEntry(@ExpectMutable Map.Entry<Integer, String> entry) {
        entry.setValue("");
    }

    public void changeKeySetInMap(@ExpectMutable Map<Integer, String> map) {
        Set<Integer> set = map.keySet();
        set.remove(42);
    }

    public void entrySetInMap(@ExpectMutable Map<Integer, String> map) {
        Iterator<Map.Entry<Integer, String>> iterator = map.entrySet().iterator();
        iterator.remove();
    }

    public void entrySetInMap2(@ExpectMutable Map<Integer, String> map) {
        Set<Map.Entry<Integer, String>> entrySet = map.entrySet();
        Iterator<Map.Entry<Integer, String>> iterator = entrySet.iterator();
        iterator.remove();
    }

    public void invokeProcessMutable(@ExpectMutable List<String> list) {
        new MutabilityInferenceTestLib().processMutable(list);
    }

    public void invokeProcessReadableAndMutable(@ExpectMutable Collection<String> collection, List<String> list) {
        MutabilityInferenceTestLib.processReadableAndMutable(list, collection);
    }

    private void foo() throws Exception {

    }

    public void walk(@ExpectMutable List<Object> ancestors) {
        Exception exc = null;
        try {
            foo();
        } catch (Exception x2) {
            exc = x2;
        }

        if (exc != null) {
            return;
        }

        ancestors.add("");
    }

    public void scopeExit(@ExpectMutable List<Object> ancestors, boolean c) {
        Exception exc = null;
        if (c) {
            Exception x2 = new Exception();
            exc = x2;
        }

        if (exc != null) {
            return;
        }

        ancestors.add("");
    }
}
