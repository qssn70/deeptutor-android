package dev.qssn70.deeptutor.config

import android.content.Context
import android.net.Uri

/** Only trusted HTTPS origins are accepted; no silent HTTP downgrade. */
class ServerConfig(context: Context) {
    private val prefs = context.getSharedPreferences("server_config", Context.MODE_PRIVATE)
    var baseUrl: String?
        get() = prefs.getString("base_url", null)
        set(value) { prefs.edit().putString("base_url", value?.let(::normalize)).apply() }

    companion object {
        fun normalize(raw: String): String {
            val uri = Uri.parse(raw.trim().trimEnd('/'))
            require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank()) { "请输入 HTTPS 服务器地址" }
            require(uri.userInfoOrNull() == null && uri.query == null && uri.fragment == null) { "不支持账号信息、查询参数或片段" }
            return uri.toString().trimEnd('/')
        }
        private fun Uri.userInfoOrNull(): String? = encodedAuthority?.substringBefore('@')?.takeIf { '@' in (encodedAuthority ?: "") }
    }
}
