package su.redbyte.androidkrdbot.data.repository

import java.net.URLEncoder

internal fun String.encodeUrlParam(): String = URLEncoder.encode(this, Charsets.UTF_8)