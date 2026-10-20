"""Source wiring regressions, not Kotlin compilation or model-fidelity evidence."""
from pathlib import Path
import unittest

PACKAGE = Path(__file__).resolve().parents[5] / "main" / "java" / "com" / "gexetr" / "plinthproductphotos"


class StudioToolBindingTest(unittest.TestCase):
    def read(self, name):
        return (PACKAGE / name).read_text(encoding="utf-8")

    def test_job_snapshots_selection_before_network_and_uploads_that_source(self):
        source = self.read("ui/studio/StudioViewModel.kt")
        start = source.split("fun start(", 1)[1].split("fun retry()", 1)[0]
        self.assertIn("val photoId = requireNotNull(mutable.value.photoId)", start)
        self.assertIn("photoId = photoId,", start)
        self.assertLess(start.index("val photoId ="), start.index("refreshQuote()"))
        execute = source.split("private suspend fun execute(", 1)[1].split("val jobId = requireNotNull", 1)[0]
        self.assertIn("ProjectImageLoader(projects).load(pending.photoId).source", execute)
        self.assertIn('api.uploadPhoto(bytes, "image/jpeg", session.deviceToken)', execute)
        self.assertIn("pending = pending.copy(uploadId = upload.uploadId)", execute)
        self.assertIn("uploadIds = listOf(requireNotNull(pending.uploadId))", execute)
        self.assertIn("op = pending.operation", execute)
        self.assertNotIn("mutable.value.photoId", execute)
        self.assertNotIn("firstOrNull", execute)
        loader = self.read("data/import/ProjectImageLoader.kt")
        self.assertIn("projects.photo(id) ?: throw FileNotFoundException", loader)
        self.assertIn("projects.sourceFile(photo.sourcePath)", loader)

    def test_blank_guidance_does_not_block_tool_defaults(self):
        model = self.read("ui/studio/StudioViewModel.kt")
        route = self.read("ui/studio/StudioRoute.kt")
        self.assertIn("require(studioPromptIsValid(operation, prompt))", model)
        self.assertIn("studioPromptIsValid(activeOperation, prompt)", route)
        self.assertIn("operation != OperationId.GENERATED_SCENE || prompt.isNotBlank()", model)
        self.assertIn('if (prompt.isNotBlank()) put("prompt", prompt.trim())', model)
        self.assertNotIn("promptValid = prompt.isNotBlank()", route)

    def test_cutout_uses_loaded_source_and_only_returned_upload_id(self):
        model = self.read("ui/cutout/CutoutViewModel.kt")
        self.assertIn("pending.sourcePath != photo.sourcePath", model)
        self.assertIn('api.uploadPhoto(upload, "image/jpeg", session.deviceToken)', model)
        self.assertIn("uploadId = uploaded.value.uploadId", model)
        api = self.read("data/api/ApiClient.kt")
        cutout = api.split("fun submitCutout(", 1)[1].split("fun pollJob(", 1)[0]
        self.assertIn("op = OperationId.CUTOUT", cutout)
        self.assertIn("uploadIds = listOf(uploadId)", cutout)
        self.assertIn("params = JsonObject(emptyMap())", cutout)


if __name__ == "__main__":
    unittest.main()
