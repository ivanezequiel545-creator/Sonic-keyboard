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
message = Path(sys.argv[1]).read_text(errors='replace')[-16000:]
for report in Path('app/build/reports').glob('lint-results*.txt'):
    message += '\n' + report.read_text(errors='replace')[:9000]
message = message.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
print('::error title=Android build or test failure::' + message)
PY
  rm "$log"
  exit 1
fi
