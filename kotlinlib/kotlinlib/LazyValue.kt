package kotlinlib

class LazyValue<T: Any> {
    private var value: T? = null
        set(newValue) {
            if (field != null) {
                throw IllegalStateException("Value has been already initialized")
            }

            field = newValue
        }

    public fun isInitialized(): Boolean = value != null
    public fun get(): T = value!!
    public fun set(newValue: T) { value = newValue }
}