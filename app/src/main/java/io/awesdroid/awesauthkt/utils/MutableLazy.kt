package io.awesdroid.awesauthkt.utils

import android.util.Log
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * @auther Awesdroid
 */
fun <T> mutableLazy(initializer: () -> T): ReadWriteProperty<Any?, T> = MutableLazy(initializer)

private class MutableLazy<T>(
    var initializer: (() -> T)?
) : ReadWriteProperty<Any?, T> {

    @Volatile
    private var value: Any? = null

    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): T {
        Log.d(TAG, "getValue(): value= $value, initializer = ${initializer?:"null"}")
        val _v1 = value
        if (_v1 !== null) {
            @Suppress("UNCHECKED_CAST")
            return _v1 as T
        }

        return synchronized(this) {
            val _v2 = value
            if (_v2 !== null) {
                @Suppress("UNCHECKED_CAST") (_v2 as T)
            } else {
                val typedValue = initializer!!()
                value = typedValue
                typedValue
            }
        }
    }

    override fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T
    ) {
        synchronized(this) {
            Log.d(TAG, "setValue(): this.value = ${this.value}, initializer = ${initializer?:"null"}")
            this.value = value
        }
    }
}