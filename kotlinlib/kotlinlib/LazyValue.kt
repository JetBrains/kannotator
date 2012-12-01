package kotlinlib

class LazyValue<T: Any> {
    private var value: T? = null
        set(newValue) {
            if ($value != null) {
                throw IllegalStateException("Value has been already initialized")
            }

            $value = newValue
        }

    public fun isInitialized(): Boolean = value != null
    public fun get(): T = value!!
    public fun set(newValue: T): Unit = value = newValue
}