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
import sys, re
import xml.etree.ElementTree as ET
from pathlib import Path
def annotate(title, message):
    message = message.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
    print('::error title=' + title + '::' + message)
text = Path(sys.argv[1]).read_text(errors='replace')
failed_tasks = '\n'.join(line for line in text.splitlines() if re.search(r'> Task .* FAILED|Execution failed for task', line))
annotate('Android build or test failure', failed_tasks + '\n' + text[-3000:])
for report in Path('app/build/reports').glob('lint-results*.xml'):
    for issue in ET.parse(report).getroot().findall('issue'):
        if issue.get('severity') in ('Error', 'Fatal'):
            annotate('Android lint error', ET.tostring(issue, encoding='unicode')[:3300])
for report in Path('app/build/outputs/androidTest-results').rglob('TEST-*.xml'):
    for failure in ET.parse(report).getroot().iter('failure'):
        details = ET.tostring(failure, encoding='unicode')[:26400]
        for start in range(0, len(details), 3300):
            annotate('Native test failure ' + str(start), details[start:start+3300])


PY
  rm "$log"
  exit 1
fi
