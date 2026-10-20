"""Source-contract checks runnable without taking the slot's Gradle lease.

These do not prove Kotlin compilation or a successful provider generation.
"""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

MAIN = Path(__file__).resolve().parents[5] / "main"
PACKAGE = MAIN / "java/com/gexetr/plinthproductphotos"


class StudioWiringTest(unittest.TestCase):
    def source(self, name):
        path = PACKAGE / name
        return path.read_text(encoding="utf-8") if path.exists() else ""

    def test_destination_is_not_the_zero_balance_toast_facade(self):
        app = self.source("ui/PlayKilnApp.kt")
        studio = app.split("entry<StudioToolsDestination>", 1)[1].split("entry<WalletDestination>", 1)[0]
        self.assertIn("StudioRoute(", studio)
        self.assertNotIn("balanceCredits = 0", studio)
        self.assertNotIn("Toast.makeText", studio)

    def test_existing_api_owns_upload_poll_and_verified_delivery(self):
        source = self.source("ui/studio/StudioViewModel.kt")
        for call in ("api.getWallet(", "api.getConfig(", "api.uploadPhoto(",
                     "api.submitJob(", "api.pollJob(", "api.downloadResult(",
                     "api.submitDeliveryReceipt(", "sessionStore.updateServerBalance("):
            self.assertIn(call, source)
        self.assertNotIn("OkHttpClient", source)

    def test_pending_submission_is_saved_before_http_and_reuses_request_id(self):
        source = self.source("ui/studio/StudioViewModel.kt")
        self.assertIn("store.save(pending)", source)
        self.assertLess(source.index("store.save(pending)"), source.index("api.submitJob("))
        self.assertIn("clientRequestId = pending.requestId", source)
        self.assertIn("if (pending.jobId == null)", source)

    def test_application_keeps_source_pixels_and_records_generated_output(self):
        source = self.source("ui/studio/StudioViewModel.kt")
        self.assertIn("database.outputDao().insert(", source)
        self.assertIn("provenance = Provenance.ENHANCED", source)
        self.assertIn('put("generated", true)', source)
        self.assertIn("R.string.studio_live_generated", source)
        self.assertNotIn("item.copy(sourcePath", source)
        self.assertNotIn("Provenance.ORIGINAL_PIXELS", source)

    def test_both_locales_have_the_same_new_keys(self):
        keys = []
        for locale in ("values", "values-ru"):
            path = MAIN / f"res/{locale}/strings_studio_live.xml"
            self.assertTrue(path.is_file())
            keys.append({entry.attrib["name"] for entry in ET.parse(path).getroot()})
        self.assertEqual(keys[0], keys[1])

    def test_live_provider_results_are_ready_and_server_config_still_gates_operations(self):
        route = self.source("ui/studio/StudioRoute.kt")
        model = self.source("ui/studio/StudioViewModel.kt")
        registry = Path(__file__).resolve().parents[7] / "server/src/registry.ts"
        server = registry.read_text(encoding="utf-8")
        self.assertIn("STUDIO_PROVIDER_RESULTS_READY = true", route)
        self.assertIn("STUDIO_PROVIDER_RESULTS_READY && state.photoId != null", route)
        self.assertIn("quote != null && quote.enabled", route)
        self.assertIn('if (!STUDIO_PROVIDER_RESULTS_READY) throw StudioProblem("PROVIDER_PROTOCOL_UNAVAILABLE")', model)
        self.assertRegex(server, r"O-07[\s\S]{0,500}enabled:\s*true")
        self.assertRegex(server, r"O-08[\s\S]{0,500}enabled:\s*true")
        self.assertRegex(server, r"O-10[\s\S]{0,500}enabled:\s*true")

    def test_kept_variant_can_be_saved_without_the_marketplace_exporter(self):
        route = self.source("ui/studio/StudioRoute.kt")
        model = self.source("ui/studio/StudioViewModel.kt")
        self.assertIn('ActivityResultContracts.CreateDocument("image/png")', route)
        self.assertIn("model.saveDocument(", route)
        self.assertIn('app.contentResolver.openOutputStream(uri, "w")', model)

    def test_kept_variants_remain_reachable_after_closing_the_preview(self):
        route = self.source("ui/studio/StudioRoute.kt")
        model = self.source("ui/studio/StudioViewModel.kt")
        self.assertIn("items(state.savedVariants", route)
        self.assertIn("model.openVariant(", route)
        self.assertIn("database.outputDao().byItem(", model)
        self.assertIn("savedVariantId = output.id", model)

    def test_before_after_result_uses_a_semantic_source_preview_not_decorative_artwork(self):
        route = self.source("ui/studio/StudioRoute.kt")
        result_sections = route.split("StudioBeforeAfterComparison(")
        self.assertGreaterEqual(len(result_sections), 3)
        self.assertIn("StudioBeforePhoto(", result_sections[1])
        self.assertIn("StudioBeforePhoto(", result_sections[2])
        self.assertIn('contentDescription = stringResource(R.string.studio_live_before_photo)', route)
        self.assertIn('R.string.studio_live_before_unavailable', route)


if __name__ == "__main__":
    unittest.main()
