package com.gexetr.plinthproductphotos.domain.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.gexetr.plinthproductphotos.domain.wallet.WalletAccessPolicy
import java.io.ByteArrayOutputStream
import java.io.IOException

object WatermarkGate {
    /** The single predicate every user-visible export route must call. */
    fun required(serverConfirmedUnlock: Boolean): Boolean =
        WalletAccessPolicy.watermarkRequired(WalletAccessPolicy.exportAccess(serverConfirmedUnlock, null))

    /** Decodes encoded PNG/JPEG bytes, paints [Watermark], re-encodes as lossless PNG. */
    fun applyToEncoded(bytes: ByteArray): ByteArray {
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IOException("EXPORT_WATERMARK_DECODE_FAILED")
        val mutable = try {
            decoded.copy(Bitmap.Config.ARGB_8888, true)
                ?: throw IOException("EXPORT_WATERMARK_DECODE_FAILED")
        } finally {
            decoded.recycle()
        }
        try {
            val pixels = IntArray(mutable.width * mutable.height)
            mutable.getPixels(pixels, 0, mutable.width, 0, 0, mutable.width, mutable.height)
            Watermark.applyArgb(pixels, mutable.width, mutable.height)
            mutable.setPixels(pixels, 0, mutable.width, 0, 0, mutable.width, mutable.height)
            return ByteArrayOutputStream().use { output ->
                if (!mutable.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw IOException("EXPORT_WATERMARK_ENCODE_FAILED")
                }
                output.toByteArray()
            }
        } finally {
            mutable.recycle()
        }
    }
}
