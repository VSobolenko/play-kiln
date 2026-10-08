package com.gexetr.unitwalk.ui.walk

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.gexetr.unitwalk.data.WalkStore
import java.io.File
import java.util.UUID

/**
 * The `content://` URI a camera capture needs, and the app-private file behind it.
 *
 * `ActivityResultContracts.TakePicture` requires a **pre-created** destination URI that the camera
 * process may write to, so the app must hand the camera a FileProvider URI for a file it already
 * owns. [uriFor] builds that URI with the slot's single FileProvider authority, and the authority is
 * derived from `context.packageName` (never a hardcoded literal) so the debug variant's
 * `.debug` applicationId suffix produces `${applicationId}.debug.fileprovider` correctly.
 *
 * ## The staging root, and the trap that was in it (resolved 2026-10-06)
 *
 * `res/xml/file_paths.xml` declares `reports/`, `photos/` and a `reports-cache/` root. The capture
 * is **staged** under `files/reports/` and then committed by the walk to the store's own `photos/`
 * directory with [WalkStore.putPhoto], so the durable bytes and the row that names them both live at
 * [durablePhotoFile]; the staging file is disposable once read.
 *
 * Declaring a root is not the same as the directory existing. The camera writes through our
 * FileProvider into the staging file, and on a first run `files/reports/` did not exist — a
 * FileProvider cannot create a file whose parent directory is missing — so the capture failed
 * **silently** and the room kept rendering "No photos for this room yet." [captureFile] now creates
 * the parent before handing out the URI, and its own KDoc carries the measurement.
 */
object PhotoUris {

    /** The suffix the manifest appends to the applicationId for its FileProvider authority. */
    const val AUTHORITY_SUFFIX: String = ".fileprovider"

    private const val REPORTS_DIR: String = "reports"

    /** The FileProvider authority declared as `${applicationId}.fileprovider` in the manifest. */
    fun authority(context: Context): String = context.packageName + AUTHORITY_SUFFIX

    /** A `content://` URI for [file], through the app's single FileProvider. */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, authority(context), file)

    /**
     * The file a capture writes to today.
     *
     * It lives under the `reports/` root `file_paths.xml` actually declares, so the FileProvider
     * hands out a URI the camera can write. It is staging, not storage: after the capture the bytes
     * are read back and committed to [durablePhotoFile] by `WalkViewModel.AttachPhoto`, so the
     * durable photo is in the store's own `photos/` directory.
     *
     * The parent directory is created **here**, and that is load-bearing rather than tidiness. The
     * camera writes through our FileProvider into this file, and on a first run `files/reports/` does
     * not exist yet: the provider cannot create a file whose parent directory is missing, so the
     * capture failed **silently** and the room kept saying "No photos for this room yet."
     * MEASURED on `R58MB3SGZVL` (SM-A405FM, Android 11) on 2026-10-06, on a clean install: with no
     * `files/reports/`, a capture left `files/` holding only `walk-state.json` and attached nothing;
     * after `mkdir -p files/reports` the identical flow wrote `files/photos/photo-<uuid>.jpg`
     * (1 802 031 bytes) and the room showed the photo. `ReportViewModel` and `WalkStore` already
     * `mkdirs()` their own targets, which is why only this path was broken.
     */
    fun captureFile(context: Context, fileName: String): File =
        File(File(context.filesDir, REPORTS_DIR), fileName).also { it.parentFile?.mkdirs() }

    /**
     * The durable home of a stored photo: exactly `WalkStore.photoFile(fileName)`.
     *
     * It is the same path [WalkStore] uses, imported rather than restated, so naming a photo here can
     * never drift from where the bytes are written.
     */
    fun durablePhotoFile(context: Context, fileName: String): File =
        WalkStore(context.filesDir).photoFile(fileName)

    /**
     * A collision-free name for one capture.
     *
     * The name is allocated **before** the camera launches and is then used for both the destination
     * file and the committed row, so the two cannot disagree.
     */
    fun newPhotoFileName(): String = "photo-${UUID.randomUUID()}.jpg"
}
