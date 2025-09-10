package su.redbyte.androidkrdbot.infra.hooks

object CacheHooks {
    var onUserBanned: (suspend (Long) -> Unit)? = null
}