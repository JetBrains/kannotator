package annotations.ext;


public class TypePathEntry {

    public static final TypePathEntry WILDCARD = new TypePathEntry(TypePathEntryKind.WILDCARD);
    public static final TypePathEntry INNER_TYPE = new TypePathEntry(TypePathEntryKind.INNER_TYPE);
    public static final TypePathEntry ARRAY = new TypePathEntry(TypePathEntryKind.ARRAY);

    public final TypePathEntryKind tag;
    public final int arg;

    private TypePathEntry(TypePathEntryKind ptag) {
        tag = ptag;
        arg = 0;
    }

    public TypePathEntry(TypePathEntryKind ptag, int parg) {
        tag = ptag;
        arg = parg;
    }


    @Override
    public String toString() {
        if (tag == TypePathEntryKind.TYPE_ARGUMENT)
            return String.format("%i (%i)", tag, arg);
        else
            return tag.toString();
    }


    public static TypePathEntry fromBinary(int tag, int arg) throws InvalidPathEntryException {
        switch (tag) {
            case 0:
                return ARRAY;
            case 1:
                return INNER_TYPE;
            case 2:
                return WILDCARD;
            case 3:
                return new TypePathEntry(TypePathEntryKind.TYPE_ARGUMENT, arg);
            default:
                throw new InvalidPathEntryException();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof TypePathEntry) {
            final TypePathEntry typePathEntry = (TypePathEntry) other;
            return tag == typePathEntry.tag && arg == typePathEntry.arg;
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return tag.hashCode() * 27 + arg;
    }
}