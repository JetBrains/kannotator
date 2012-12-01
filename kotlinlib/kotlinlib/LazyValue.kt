package kotlinlib

class LazyValue<T: Any> {
    public var value: T? = null
        get() {
            return $value!!
        }
        set(newValue) {
            if ($value != null) {
                throw IllegalStateException("Value has been already initialized")
            }

            $value = newValue
        }
}