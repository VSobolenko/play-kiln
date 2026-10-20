package com.gexetr.plinthproductphotos.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun <T> loadDestinationData(load: () -> T): T =
    withContext(Dispatchers.IO) { load() }
