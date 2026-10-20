package com.gexetr.plinthproductphotos.ui.autocard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal object AutoCardImageWork {
    private val mutex = Mutex()

    suspend fun <T> run(block: () -> T): T = withContext(Dispatchers.Default) {
        // Cancelling an effect cannot interrupt synchronous pixel work already in progress.
        mutex.withLock {
            ensureActive()
            block()
        }
    }
}
