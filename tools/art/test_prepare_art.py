import unittest

from prepare_art import select_open_access


def row(uuid, artist="Artista", year=1850, w=3000, h=1800, openaccess="1", viewtype="primary",
        iiif="https://api.nga.gov/iiif/x"):
    return {
        "uuid": uuid, "artist": artist, "year": year, "width": w, "height": h,
        "openaccess": openaccess, "viewtype": viewtype, "iiifurl": iiif, "title": f"Obra {uuid}",
        "displaydate": str(year),
    }


class SelectOpenAccessTest(unittest.TestCase):
    def test_select_open_access_skips_rows_without_flag_and_without_image(self):
        rows = [
            row("ok"),
            row("closed", openaccess="0"),
            row("noflag", openaccess=""),
            row("noimage", iiif=""),
            row("secondary", viewtype="alternate"),
        ]
        self.assertEqual(["ok"], [r["uuid"] for r in select_open_access(rows)])

    def test_select_skips_portrait_tiny_and_extreme_ratios(self):
        rows = [
            row("land"),
            row("portrait", w=1800, h=3000),
            row("tiny", w=1200, h=700),
            row("strip", w=6000, h=1000),
        ]
        self.assertEqual(["land"], [r["uuid"] for r in select_open_access(rows)])

    def test_select_limits_two_per_artist(self):
        rows = [row(f"m{i}", artist="Monet") for i in range(5)] + [row("o1", artist="Otro")]
        picked = select_open_access(rows, n=10)
        self.assertEqual(2, sum(1 for r in picked if r["artist"] == "Monet"))
        self.assertEqual(3, len(picked))

    def test_select_spreads_centuries_and_caps_at_n(self):
        rows = []
        for century in (1500, 1600, 1700, 1800, 1900):
            for i in range(4):
                rows.append(row(f"{century}-{i}", artist=f"A{century}-{i}", year=century + 10))
        picked = select_open_access(rows, n=6)
        self.assertEqual(6, len(picked))
        self.assertGreaterEqual(len({r["year"] // 100 for r in picked}), 4)

    def test_select_prefers_scenic_titles_and_skips_multi_panel_works(self):
        rows = [
            {**row("a1", artist="Aaa", year=1500), "title": "Madonna and Child"},
            {**row("b1", artist="Bbb", year=1600), "title": "Harbor at Sunset"},
            {**row("c1", artist="Ccc", year=1700), "title": "Saints [entire triptych]"},
            {**row("d1", artist="Ddd", year=1800), "title": "View of the Grand Canal"},
        ]
        picked = [r["uuid"] for r in select_open_access(rows, n=3)]
        self.assertNotIn("c1", picked)
        self.assertEqual({"b1", "d1"}, set(picked[:2]))

    def test_select_is_deterministic(self):
        rows = [row(f"u{i}", artist=f"A{i}", year=1500 + i * 37) for i in range(30)]
        self.assertEqual(select_open_access(rows, n=8), select_open_access(list(reversed(rows)), n=8))


if __name__ == "__main__":
    unittest.main()
