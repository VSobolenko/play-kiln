"""Throw-away probe round 4."""
import json
import xml.etree.ElementTree as ET

import requests

from common import SCRATCH, USER_AGENT

OUT = SCRATCH / "probe" / "round4.txt"
lines = []


def emit(s):
    lines.append(str(s))
    OUT.write_text("\n".join(lines), encoding="utf-8")


def show(title, fn):
    emit("\n=== " + title + " ===")
    try:
        fn()
    except Exception as e:  # noqa: BLE001
        emit("  FAILED: " + repr(e)[:500])


NS = {"v": "http://www.ivoa.net/xml/VOTable/v1.3"}


def hea_tap(adql):
    r = requests.post("https://heasarc.gsfc.nasa.gov/xamin/vo/tap/sync",
                      data={"REQUEST": "doQuery", "LANG": "ADQL", "FORMAT": "votable", "QUERY": adql},
                      headers={"User-Agent": USER_AGENT}, timeout=120)
    r.raise_for_status()
    root = ET.fromstring(r.content)
    info = [(i.get("name"), i.get("value")) for i in root.iter("{http://www.ivoa.net/xml/VOTable/v1.3}INFO")]
    emit("INFO " + json.dumps(info)[:300])
    tables = []
    for tbl in root.iter("{http://www.ivoa.net/xml/VOTable/v1.3}TABLE"):
        fields = [f.get("name") for f in tbl.iter("{http://www.ivoa.net/xml/VOTable/v1.3}FIELD")]
        rows = []
        for tr in tbl.iter("{http://www.ivoa.net/xml/VOTable/v1.3}TR"):
            rows.append([td.text for td in tr])
        tables.append((fields, rows[:3]))
    return tables


def hea_list_tables():
    for like in ("%ngc%", "%messier%", "%galaxy%"):
        try:
            t = hea_tap(f"SELECT table_name FROM TAP_SCHEMA.tables WHERE table_name LIKE '{like}'")
            emit(like + " -> " + json.dumps([r[0] for r in t[0][1]]) if t else like + " -> none")
        except Exception as e:  # noqa: BLE001
            emit(like + " FAILED " + repr(e)[:200])


def hea_messier_sample():
    t = hea_tap("SELECT TOP 3 * FROM messier")
    emit("cols: " + json.dumps(t[0][0]))
    for r in t[0][1]:
        emit(r)


def gavo_fk6():
    r = requests.post("https://dc.g-vo.org/tap/sync",
                      data={"REQUEST": "doQuery", "LANG": "ADQL", "FORMAT": "json",
                            "QUERY": "SELECT COUNT(*) AS n FROM fk6.fk6join"},
                      headers={"User-Agent": USER_AGENT}, timeout=90)
    emit("status " + str(r.status_code))
    emit(r.text[:400])


def simbad_flux_cols():
    r = requests.post("https://simbad.cds.unistra.fr/simbad/sim-tap/sync",
                      data={"REQUEST": "doQuery", "LANG": "ADQL", "FORMAT": "json",
                            "QUERY": "SELECT TOP 2 * FROM flux"},
                      headers={"User-Agent": USER_AGENT}, timeout=60)
    emit("status " + str(r.status_code))
    emit(r.text[:800])


def viz_ngc_sample():
    r = requests.post("https://tapvizier.cds.unistra.fr/TAPVizieR/tap/sync",
                      data={"REQUEST": "doQuery", "LANG": "ADQL", "FORMAT": "json",
                            "QUERY": 'SELECT TOP 2 * FROM "VII/1B/catalog"'},
                      headers={"User-Agent": USER_AGENT}, timeout=90)
    emit("status " + str(r.status_code))
    emit(r.text[:900])


def ned_list():
    r = requests.get("https://ned.ipac.caltech.edu/tap/sync",
                     params={"REQUEST": "doQuery", "LANG": "ADQL", "FORMAT": "json",
                             "QUERY": "SELECT TOP 2 * FROM TAP_SCHEMA.tables"},
                     headers={"User-Agent": USER_AGENT}, timeout=60)
    emit("status " + str(r.status_code))
    emit(r.text[:400])


if __name__ == "__main__":
    show("HEASARC tables", hea_list_tables)
    show("HEASARC messier sample", hea_messier_sample)
    show("GAVO FK6 count", gavo_fk6)
    show("SIMBAD flux columns", simbad_flux_cols)
    show("VizieR VII/1B sample", viz_ngc_sample)
    show("NED TAP", ned_list)
    emit("\nDONE")
