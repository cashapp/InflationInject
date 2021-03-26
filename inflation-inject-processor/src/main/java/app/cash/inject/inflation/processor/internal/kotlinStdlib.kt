package app.cash.inject.inflation.processor.internal

// TODO https://youtrack.jetbrains.com/issue/KT-4734
fun <K, V : Any> Map<K, V?>.filterNotNullValues(): Map<K, V> {
  @Suppress("UNCHECKED_CAST")
  return filterValues { it != null } as Map<K, V>
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
