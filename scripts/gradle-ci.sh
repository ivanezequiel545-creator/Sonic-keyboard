#!/usr/bin/env bash
# Run from android/. Mirror failure details into GitHub check annotations because
# the sandbox cannot follow GitHub's external log-storage download redirects.
set -euo pipefail
log=$(mktemp)
if ./gradlew "$@" > "$log" 2>&1; then
  cat "$log"
  rm "$log"
else
  cat "$log"
  python3 - "$log" <<'PY'
import sys
from pathlib import Path
def annotate(title, message):
    message = message.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
    print('::error title=' + title + '::' + message)
annotate('Android build or test failure', Path(sys.argv[1]).read_text(errors='replace')[-3500:])
for report in Path('app/build/reports').glob('lint-results*.txt'):
    annotate('Android lint details', report.read_text(errors='replace')[:3500])

PY
  rm "$log"
  exit 1
fi
