package com.gexetr.plinthproductphotos.data.batch

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels.toBitmap
import com.gexetr.plinthproductphotos.data.photoimport.ProjectImageLoader
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.domain.batch.BatchItemState
import com.gexetr.plinthproductphotos.domain.batch.BatchQueueProcessor
import com.gexetr.plinthproductphotos.domain.batch.BatchQueueItem
import com.gexetr.plinthproductphotos.domain.look.LookJsonCodec
import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.Canvas
import com.gexetr.plinthproductphotos.domain.pipeline.Compositor
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.ui.notification.BatchNotifier
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException

class BatchWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val batchId = inputData.getString(BATCH_ID) ?: return Result.failure()
        return try {
            val database = BatchDatabase.get(applicationContext)
            val store = BatchRoomStore(database)
            val batch = store.batch(batchId) ?: return Result.failure()
            val projects = ProjectRepository(applicationContext)
            val imageLoader = ProjectImageLoader(projects)
            val processor = BatchQueueProcessor(
                store = store,
                compose = { item: BatchQueueItem -> compose(item, batch.lookJson, imageLoader) },
            )
            processor.resume(batchId)
            val items = store.items(batchId)
            if (items.any { it.state == BatchItemState.QUEUED || it.state == BatchItemState.PROCESSING }) {
                return Result.retry()
            }
            store.settle(batchId)
            val counts = store.counts(batchId)
            BatchNotifier.postCompletion(applicationContext, batchId, counts.completed, counts.failed)
            Result.success(workDataOf(COMPLETED_COUNT to counts.completed, FAILED_COUNT to counts.failed))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.e(TAG, "Batch worker will retry batch $batchId", failure)
            Result.retry()
        }
    }

    private fun compose(
        item: BatchQueueItem,
        batchLookJson: String,
        loader: ProjectImageLoader,
    ): String {
        val look = LookJsonCodec.decodeOne(batchLookJson)
        val image = loader.load(item.sourceId)
        val scene = loadSceneIfNeeded(look.background)
        val result = Compositor.compose(
            source = image.source,
            mask = image.mask,
            canvas = Canvas(CARD_SIZE, CARD_SIZE),
            look = look,
            sceneBackground = scene,
        ).image
        val outputDirectory = File(applicationContext.filesDir, "outputs/batches/${item.batchId}").canonicalFile
        val outputRoot = File(applicationContext.filesDir, "outputs").canonicalFile
        require(outputDirectory.toPath().startsWith(outputRoot.toPath())) { "Batch output path is invalid" }
        if (!outputDirectory.isDirectory && !outputDirectory.mkdirs()) {
            throw IllegalStateException("Could not create batch output storage")
        }
        val fileName = item.id.sha256() + ".png"
        val destination = File(outputDirectory, fileName)
        val temporary = File(outputDirectory, "$fileName.tmp")
        val bitmap = result.toBitmap()
        try {
            FileOutputStream(temporary).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "Could not encode composed image"
                }
                output.fd.sync()
            }
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } finally {
            bitmap.recycle()
            if (temporary.exists() && !temporary.delete()) {
                Log.w(TAG, "Could not remove temporary batch output ${temporary.name}")
            }
        }
        return destination.relativeTo(applicationContext.filesDir).invariantSeparatorsPath
    }

    private fun loadSceneIfNeeded(background: BackgroundSpec): PixelBuffer? {
        if (background.kind == BackgroundSpec.Kind.SOLID ||
            background.kind == BackgroundSpec.Kind.GRADIENT
        ) return null
        val relative = requireNotNull(background.sceneAssetPath) {
            "This Look's scene asset is not available on this device"
        }.removePrefix("files/")
        val sceneDirectory = File(applicationContext.filesDir, "scenes").canonicalFile
        val sceneFile = File(applicationContext.filesDir, relative).canonicalFile
        require(sceneFile.toPath().startsWith(sceneDirectory.toPath())) {
            "Look scene path is outside private scene storage"
        }
        require(sceneFile.isFile) { "This Look's scene asset is missing on this device" }
        return PhotoPixels.load(sceneFile)
    }

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    companion object {
        const val BATCH_ID = "batch_id"
        const val COMPLETED_COUNT = "completed_count"
        const val FAILED_COUNT = "failed_count"
        private const val CARD_SIZE = 1200
        private const val TAG = "PlinthBatchWorker"

        fun enqueue(context: Context, batchId: String, appendIfRunning: Boolean = false) {
            require(batchId.isNotBlank())
            val request = OneTimeWorkRequestBuilder<BatchWorker>()
                .setInputData(Data.Builder().putString(BATCH_ID, batchId).build())
                .addTag(BatchRoomStore.workName(batchId))
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(
                    BatchRoomStore.workName(batchId),
                    if (appendIfRunning) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP,
                    request,
                )
        }
    }
}
