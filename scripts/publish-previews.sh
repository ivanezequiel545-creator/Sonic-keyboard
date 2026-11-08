#!/usr/bin/env bash
set -euo pipefail
commit="${1:?Specify the tested commit SHA}"
# Never move an existing tag or silently replace a release from another commit.
for stage in 1 2 3; do
  tag="v1.1.0-alpha.$stage"
  if gh release view "$tag" >/dev/null 2>&1; then
    existing=$(gh api "repos/$GH_REPO/git/ref/tags/$tag" --jq '.object.sha')
    if [[ "$existing" != "$commit" ]]; then
      echo "Release $tag belongs to another commit. Choose a new version; refusing to overwrite." >&2
      exit 1
    fi
  fi
  test -s "apk/alpha-$stage/velocity-android.apk"
  (cd "apk/alpha-$stage" && sha256sum -c velocity-android.apk.sha256)
done
for stage in 1 2 3; do
  tag="v1.1.0-alpha.$stage"
  notes=$(mktemp)
  cat "docs/releases/alpha-$stage.md" > "$notes"
  printf '\n\nCompilación comprobada: `%s`.\n' "$commit" >> "$notes"
  if gh release view "$tag" >/dev/null 2>&1; then
    gh release edit "$tag" --prerelease --notes-file "$notes"
  else
    gh release create "$tag" --target "$commit" --prerelease --title "Velocity 1.1 · Alpha $stage" --notes-file "$notes"
  fi
  gh release upload "$tag" "apk/alpha-$stage/velocity-android.apk" "apk/alpha-$stage/velocity-android.apk.sha256" --clobber
  rm "$notes"
done
