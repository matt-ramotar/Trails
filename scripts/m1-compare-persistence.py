#!/usr/bin/env python3
"""Verify the four captured M1 persistence stages without changing reports or using a device."""
import argparse
import hashlib
import json
from pathlib import Path
import sys

STAGES = {
    '31': ('31-offline-pending-db.json', 'production-offline-pending.tar'),
    '36': ('36-settled-db.json', 'production-settled.tar'),
    '43': ('43-acked-offline-db.json', 'production-acked-offline.tar'),
    '46': ('46-adopted-offline-db.json', 'production-adopted-offline.tar'),
}
NONTERMINAL = {'UNPREPARED', 'READY', 'INFLIGHT', 'REFRESH_REQUIRED', 'ACKED', 'EFFECTS_PENDING'}


def sha(path):
    result = hashlib.sha256()
    with path.open('rb') as stream:
        while chunk := stream.read(1024 * 1024):
            result.update(chunk)
    return result.hexdigest()


def one(items):
    items = list(items)
    if len(items) != 1:
        raise ValueError('Evidence selection must identify exactly one row; found %d' % len(items))
    return items[0]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--directory', type=Path, default=Path(__file__).resolve().parents[1] / 'docs/evidence/m1/android/production')
    parser.add_argument('--archive-dir', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        parser.error('Output must be a new path; existing evidence is never overwritten')
    reports = {stage: json.loads((args.directory / names[0]).read_text()) for stage, names in STAGES.items()}
    checks = []
    def check(name, actual, expected=True):
        checks.append(dict(name=name, passed=actual == expected, expected=expected, observed=actual))

    manifest = {}
    for stage, (report_name, archive_name) in STAGES.items():
        report = reports[stage]
        archive_hash = sha(args.archive_dir / archive_name)
        manifest[stage] = dict(report=report_name, reportSha256=sha(args.directory / report_name),
                               archive=archive_name, archiveSha256=archive_hash)
        check(stage + ': archive hash matches original report', archive_hash, report['archiveSha256'])
        check(stage + ': report complete', report['complete'])
        check(stage + ': caller declared force-stop', report['captureStateDeclaredByCaller'], 'force-stopped')
        check(stage + ': all copied database files pass integrity and are untruncated',
              all(db['integrityOk'] and db['complete'] and not db['truncated'] for db in report['databases']))

    initial = one(db for db in reports['31']['databases'] if db['role'] == 'journal' and db['acceptanceCount'] == 1)
    account = initial['accountSha256']
    def db(stage, role):
        return one(item for item in reports[stage]['databases'] if item['role'] == role
                   and (role in ('backend', 'catalog') or item['accountSha256'] == account))
    journals = {stage: db(stage, 'journal') for stage in STAGES}
    servers = {stage: db(stage, 'backend') for stage in STAGES}
    def local(stage):
        return one(row['value'] for row in db(stage, 'values')['confirmedMemberships'] if row['trail'] == 'alpine-lake-loop')
    def canonical(stage):
        return one(row['value'] for row in servers[stage]['canonicalMemberships']
                   if row['accountSha256'] == account and row['value']['trail'] == 'alpine-lake-loop')
    def counters(stage):
        return {key: servers[stage]['server'][key] for key in ('pushAttempts', 'applications', 'receiptCount', 'requests')}
    def acceptance(stage, sequence):
        return one(row for row in journals[stage]['acceptances'] if row['clientSequence'] == sequence)
    def mode(stage):
        return servers[stage]['server']['settings']['effective']['mode']
    def pending(stage):
        return sum(count for phase, count in journals[stage]['phaseCounts'].items() if phase in NONTERMINAL)

    identity = initial['identity']
    original = acceptance('31', 1)
    client = one(initial['clients'])['clientSha256']
    for stage in STAGES:
        check(stage + ': same account and journal installation', journals[stage]['identity'], identity)
        check(stage + ': filename partition agrees with journal account', journals[stage]['identityMatchesPartition'])
        check(stage + ': same durable mutation client', one(journals[stage]['clients'])['clientSha256'], client)
        check(stage + ': original application admission receipt retained byte-for-byte', acceptance(stage, 1), original)

    check('31: fake backend Offline persisted', mode('31'), 'OFFLINE')
    check('31: no save transport/application/receipt before reconnect', {k:v for k,v in counters('31').items() if k != 'requests'},
          dict(pushAttempts=0, applications=0, receiptCount=0))
    check('31: one unprepared intent', journals['31']['phaseCounts'], {'UNPREPARED': 1})
    check('31: offline command requests weekend collection', original['command']['desired']['collections'], ['weekend'])
    check('31: confirmed base remains empty before ACK', local('31')['collections'], [])
    check('36: reconnect Online persisted', mode('36'), 'ONLINE')
    check('36: exactly one push/effect/receipt cumulatively', {k:v for k,v in counters('36').items() if k != 'requests'},
          dict(pushAttempts=1, applications=1, receiptCount=1))
    check('36: initial journal work pruned', journals['36']['retainedRows'], {key:0 for key in journals['36']['retainedRows']})
    check('36: local membership confirmed weekend', local('36')['collections'], ['weekend'])
    check('36: local and server canonical bytes agree', local('36')['payloadSha256'], canonical('36')['payloadSha256'])
    first_receipt = one(servers['36']['receipts'])
    check('36: server receipt uses persisted account/installation',
          {key:first_receipt[key] for key in ('accountSha256', 'installationSha256')}, identity)
    check('36: receipt result equals confirmed value', first_receipt['result']['payloadSha256'], local('36')['payloadSha256'])

    check('43: exactly one ACKED execution', journals['43']['phaseCounts'], {'ACKED': 1})
    check('43: one durable ACK row', len(journals['43']['acks']), 1)
    ack = one(journals['43']['acks'])
    attempt = one(journals['43']['attempts'])
    third_receipt = one(row for row in servers['43']['receipts'] if row['idempotencyKeySha256'] == attempt['idempotencyKeySha256'])
    check('43: ACK belongs to command 3 generation 1', [ack['clientSequence'], ack['generation']], [3, 1])
    check('43: ACK authoritative target favorites', ack['authoritative']['collections'], ['favorites'])
    check('43: canonical server already favorites', canonical('43')['collections'], ['favorites'])
    check('43: ACK/receipt/canonical payloads identical',
          len({ack['authoritative']['payloadSha256'], third_receipt['result']['payloadSha256'], canonical('43')['payloadSha256']}), 1)
    check('43: confirmed local base still preceding two-list value', local('43')['collections'], ['favorites', 'weekend'])
    check('43: adoption fault is armed in participating account VALUES file',
          db('43', 'values')['debugFaults']['trails_m1_debug_fail_adoption'])

    for stage in ('43', '46'):
        check(stage + ': fake backend stays Offline', mode(stage), 'OFFLINE')
        check(stage + ': cumulative server totals reflect all three saves', counters(stage),
              dict(pushAttempts=4, applications=3, receiptCount=3, requests=11))
    check('43→46: no recorded backend push, effect, receipt, or request increase', counters('46'), counters('43'))
    check('43→46: backend receipts are unchanged', servers['46']['receipts'], servers['43']['receipts'])
    check('43→46: application command receipts are unchanged', journals['46']['acceptances'], journals['43']['acceptances'])
    check('43→46: same ACK retained, not recreated', journals['46']['acks'], journals['43']['acks'])
    check('46: adoption fault removed', db('46', 'values')['debugFaults']['trails_m1_debug_fail_adoption'], False)
    check('46: local membership adopted favorites while Offline', local('46')['collections'], ['favorites'])
    check('46: local adopted bytes equal previous durable ACK', local('46')['payloadSha256'], ack['authoritative']['payloadSha256'])
    check('46: no nonterminal mutation phases remain', pending('46'), 0)
    check('46: retained execution is RETIRED, not ACKED', journals['46']['phaseCounts'], {'RETIRED': 1})
    retirement = one(journals['46']['clients'])
    check('46: local retirement progressed while server confirmation waits Online',
          [retirement['retiredThroughSequence'], retirement['serverConfirmedRetiredThroughSequence']], [3, 2])

    other = one(item for item in reports['31']['databases'] if item['role'] == 'journal' and item['accountSha256'] != account)
    for stage in STAGES:
        other_journal = one(item for item in reports[stage]['databases'] if item['role'] == 'journal' and item['accountSha256'] == other['accountSha256'])
        other_values = one(item for item in reports[stage]['databases'] if item['role'] == 'values' and item['accountSha256'] == other['accountSha256'])
        check(stage + ': other account installation and empty admission state preserved',
              other_journal['identity'] == other['identity'] and other_journal['acceptanceCount'] == 0 and not other_journal['executions'])
        check(stage + ': other account confirmed memberships remain empty', all(not row['value']['collections'] for row in other_values['confirmedMemberships']))

    summaries = {stage: dict(backendMode=mode(stage), cumulativeCounters=counters(stage),
        acceptanceCount=journals[stage]['acceptanceCount'], phaseCounts=journals[stage]['phaseCounts'],
        retainedAckRows=len(journals[stage]['acks']), historicalFailureRows=len(journals[stage]['failures']),
        confirmedCollections=local(stage)['collections'], nonterminalExecutionCount=pending(stage)) for stage in STAGES}
    deltas = {before + '→' + after: {key: counters(after)[key] - counters(before)[key] for key in counters(before)}
              for before, after in (('31','36'), ('36','43'), ('43','46'))}
    result = dict(passed=all(item['passed'] for item in checks), assertionCount=len(checks),
                  failedAssertions=[item['name'] for item in checks if not item['passed']],
                  sourceReports=manifest, participatingIdentity=identity, mutationClientSha256=client,
                  stageSummaries=summaries, counterDeltas=deltas,
                  interpretation=[
                      '31→36 proves Offline durable admission followed by one server application and receipt on reconnect.',
                      '36→43 contains intervening saves and response-loss work; its +3 pushes/+2 effects/+2 receipts is not the cost of one save.',
                      '43→46 proves local adoption of the existing durable ACK while Offline with no new backend pushes.',
                      '46 retains one RETIRED execution and its ACK/history until the server confirms retirement Online. The journal is not empty at this stage.',
                      'Failure history grows from three to four rows during the adoption scenario; no nonterminal work remains in 46.',
                      'Force-stop provenance is caller-declared in the reports; archive bytes and report hashes are independently checked here.'
                  ], checks=checks)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open('x') as stream:
        json.dump(result, stream, indent=2, sort_keys=True)
        stream.write('\n')
    print(json.dumps({key:result[key] for key in ('passed','assertionCount','failedAssertions')}))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    sys.exit(main())
