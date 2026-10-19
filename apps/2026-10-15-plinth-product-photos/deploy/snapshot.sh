#!/usr/bin/env bash
# snapshot.sh - turn a named revision into the only thing release.sh accepts.
#
#   deploy/snapshot.sh <tree-ish> [--out <file.tgz>]
#
# It runs `git archive`, so the bytes are the revision's bytes and nothing from
# the working tree: a Codex worker mid-edit cannot leak into a release. The
# sidecar (<file>.tgz.identity) records the tree object and the archive sha256 so
# the host can refuse a tarball whose name and content disagree.
#
# The tree-ish may be a commit, a tag, or a tree object. On this repository the
# slot is not in any commit yet, so the honest input is the tree written by
# `git write-tree` over the index - an immutable object with a name, not a
# directory.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SLOT_DIR="$(cd "$HERE/.." && pwd)"
REPO="$(git -C "$SLOT_DIR" rev-parse --show-toplevel)"
# Relative to the repository root as git sees it, which survives Windows/MSYS
# path mangling where a string-prefix comparison would not.
SLOT_REL="$(git -C "$SLOT_DIR" rev-parse --show-prefix)"
SLOT_REL="${SLOT_REL%/}"

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

TREEISH=""
OUT=""

usage() {
  sed -n '2,16p' "$0" | sed 's/^# \{0,1\}//'
  exit 0
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --out) OUT="${2:-}"; shift 2 ;;
    -h|--help) usage ;;
    -*) die "unknown flag: $1" ;;
    *) [ -z "$TREEISH" ] || die "one revision per invocation"; TREEISH="$1"; shift ;;
  esac
done

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
[ -n "$TREEISH" ] || die "usage: snapshot.sh <tree-ish> [--out <file.tgz>]"
TREE="$(git -C "$REPO" rev-parse --verify "$TREEISH^{tree}")" || die "not a tree-ish: $TREEISH"
[ -d "$SLOT_DIR/server" ] || die "no server/ directory under $SLOT_DIR"

mkdir -p "$REPO/.kiln/scratch/releases"
if [ -z "$OUT" ]; then
  OUT="$REPO/.kiln/scratch/releases/server-${TREE:0:12}-$(date -u +%Y%m%d-%H%M%S).tgz"
fi
mkdir -p "$(dirname "$OUT")"
rm -f "$OUT"

git -C "$REPO" archive --format=tar.gz -o "$OUT" "$TREE" "$SLOT_REL/server"

SHA="$(sha256sum "$OUT" | awk '{print $1}')"
SIZE="$(stat -c '%s' "$OUT" 2>/dev/null || stat -f '%z' "$OUT")"
{
  printf 'tree=%s\n' "$TREE"
  printf 'slot=%s\n' "$SLOT_REL"
  printf 'archive_sha256=%s\n' "$SHA"
  printf 'created_at=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf 'source=%s (%s)\n' "$TREEISH" "$(git -C "$REPO" rev-parse --abbrev-ref HEAD)"
} > "$OUT.identity"

echo "SNAPSHOT: OK"
echo "  tree      $TREE"
echo "  archive   $OUT"
echo "  sha256    $SHA"
echo "  size      $SIZE bytes"
echo "  identity  $OUT.identity"
echo
echo "next:"
echo "  scp \"$OUT\" \"$OUT.identity\" root@78.111.89.46:/opt/plinth-product-photos/incoming/"
echo "  ssh root@78.111.89.46 '/opt/plinth-product-photos/deploy/release.sh staging --tarball /opt/plinth-product-photos/incoming/$(basename "$OUT")'"
