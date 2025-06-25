package su.redbyte.androidkrdbot.data

class TtlCache<V>(
    private val ttlMillis: Long = 30 * 60 * 1000L
) {
    private data class Box<V>(val value: V, val expiresAt: Long)
    private val map = mutableMapOf<String, Box<V>>()

    fun get(key: String): V? =
        map[key]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.value

    fun put(key: String, value: V) {
        map[key] = Box(value, System.currentTimeMillis() + ttlMillis)
    }
}
