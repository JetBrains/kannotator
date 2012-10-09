package kotlinlib

// This is needed to make not-null T the expected type
// Example:
//   if (foo != null) Pair("X", foo.notNull) else bar
//                              ^ no smart cast unless .notNull is there
// You can always say "foo as WhateverType", but the type can be too long...
public val <T: Any> T.notNull: T
    get() = this