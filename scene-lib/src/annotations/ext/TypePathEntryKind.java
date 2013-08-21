package annotations.ext;

public enum TypePathEntryKind {
    ARRAY(0),
    INNER_TYPE(1),
    WILDCARD(2),
    TYPE_ARGUMENT(3);

    TypePathEntryKind(final int ptag) {
        tag = ptag;
    }

    public final int tag;
}
