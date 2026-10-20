package com.gexetr.plinthproductphotos.data.session

import com.gexetr.plinthproductphotos.BuildConfig

internal class DevIntegrityTokenProvider : IntegrityTokenProvider {
    init {
        check(BuildConfig.DEBUG) { "Development integrity is restricted to debug builds" }
    }

    override suspend fun token(requestHash: String): String {
        if (!REQUEST_HASH.matches(requestHash)) {
            throw IntegrityTokenException(SessionFailure.INTEGRITY)
        }
        return "dev:$requestHash"
    }

    private companion object {
        val REQUEST_HASH = Regex("[A-Za-z0-9_-]{43}")
    }
}
