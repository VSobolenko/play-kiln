package com.gexetr.capability.core

fun interface Clock {
    fun nowMillis(): Long

    object System : Clock {
        override fun nowMillis(): Long = java.lang.System.currentTimeMillis()
    }
}
