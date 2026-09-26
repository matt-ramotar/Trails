# Controller first failures

- Initial Claude sandbox probe: `Execution error` (/private/tmp/roots-model-probe.log). Elevated API probe: `Credit balance is too low` (/private/tmp/roots-model-probe-escalated.log), exit 1. User authorized GPT-6 Astra for all roles.
- Immutable dependency check: `python3 integration-tests/store6-consumer/prepare.py --check-candidate`, exit 1. `git -C /private/tmp/trails-c3-20260915/store6 rev-parse HEAD` exit 128: `fatal: not a git repository (or any of the parent directories): .git`. Dependency audit pending; guard unchanged.
- Figma asset download via urllib in sandbox: `socket.gaierror: [Errno 8] nodename nor servname provided, or not known`. Exports themselves succeeded at 390 x 844. Elevated download requested.
- Figma URL downloads outside sandbox returned zero-byte HTML rather than PNG. Recovered using get_screenshot enableBase64Response; three 390x844 PNG signatures/dimensions/hashes verified in figma/manifest.json.
