package com.squareup.inject.assisted.processor.internal

// TODO Maybe replaced by https://youtrack.jetbrains.com/issue/KT-13814?
fun <K, V : Any> Iterable<K>.associateWithNotNull(func: (K) -> V?): Map<K, V> {
  val map = mutableMapOf<K, V>()
  for (key in this) {
    val value = func(key)
    if (value != null) {
      map[key] = value
    }
  }
  return map
}

/** Equivalent to `this as T` for use in function chains. */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Any.cast(): T = this as T

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Iterable<*>.castEach() = map { it as T }

inline fun <T : Any, I> T.applyEach(items: Iterable<I>, func: T.(I) -> Unit): T {
  items.forEach { item -> func(item) }
  return this
}
