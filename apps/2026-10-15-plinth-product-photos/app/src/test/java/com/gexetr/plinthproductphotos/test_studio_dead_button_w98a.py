"""W98a source contracts: a generation tap is never swallowed and a failure is always visible."""
from pathlib import Path
import unittest

MAIN = Path(__file__).resolve().parents[5] / "main"
PACKAGE = MAIN / "java/com/gexetr/plinthproductphotos"


def read(name):
    return (PACKAGE / name).read_text(encoding="utf-8")


class DeadButtonTest(unittest.TestCase):
    def test_route_never_resumes_a_leftover_via_retry_on_hasPending(self):
        route = read("ui/studio/StudioRoute.kt")
        self.assertNotIn("if (state.hasPending) model.retry()", route)
        self.assertIn("model.generate(activeOperation, prompt, quote.credits)", route)

    def test_generate_resumes_only_same_unresolved_operation(self):
        model = read("ui/studio/StudioViewModel.kt")
        body = model.split("fun generate(", 1)[1].split("private fun start(", 1)[0]
        self.assertIn("!pending.canReplace && pending.operation == operation", body)

    def test_every_unexpected_exception_surfaces_an_error(self):
        model = read("ui/studio/StudioViewModel.kt")
        self.assertIn('localFailure("UNEXPECTED_FAILURE"', model)

    def test_all_paths_share_one_failure_notice_with_retry(self):
        route = read("ui/studio/StudioRoute.kt")
        self.assertEqual(route.count("fun StudioFailureNotice("), 1)
        panel = route.split("StudioWorkflowPanel(", 1)[1]
        self.assertIn("onRetry = {", panel)
        self.assertLess(panel.index("StudioFailureNotice("), panel.index("StudioBeforeAfterComparison("))

    def test_error_strings_in_both_locales_and_no_charged_line(self):
        for loc in ("values", "values-ru"):
            text = (MAIN / "res" / loc / "strings_studio_live.xml").read_text(encoding="utf-8")
            self.assertIn('name="studio_live_error_detail"', text)
            for chip in ("sea", "forest", "campsite"):
                self.assertIn(f'name="studio_live_scene_suggestion_{chip}"', text)
        self.assertNotIn("studio_live_charged", read("ui/studio/StudioRoute.kt"))


if __name__ == "__main__":
    unittest.main()
