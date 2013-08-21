package annotations.ext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TypeAnnotationPosition {
    public static List<TypePathEntry> getTypePathFromBinary(final Iterable<Integer> list) {
        final ArrayList<TypePathEntry> result = new ArrayList<TypePathEntry>();

        final Iterator<Integer> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Integer comp0 = iterator.next();
            final Integer comp1 = iterator.next();
            try {
                result.add(TypePathEntry.fromBinary(comp0, comp1));
            } catch (InvalidPathEntryException e) {
                e.printStackTrace();//shouldn't we ignore?
            }
        }
        return result;
    }

}