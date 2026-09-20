#!/usr/bin/env python3
"""Compare captured M1 final-APK Offline/restart/reconnect reports without device access."""
import argparse
import hashlib
import json
from pathlib import Path
import sys

DIRECTORY = Path(__file__).resolve().parents[1] / 'docs/evidence/m1/android/production'
DEFAULTS = {
    'baseline': '65-account-settled-db.json',
    'pending': '120-final-offline-pending-db.json',
    'restarted': '123-final-restarted-db.json',
    'settled': '127-final-settled-db.json',
}
ROBIN = 'aad0c46afe3dae2af24cf39d93608de766035e02178db57d7c7b58e26958efd3'
TRAIL = 'alpine-lake-loop'
DESIRED = ['favorites', 'weekend']
FAULTS = {'trails_m1_debug_fail_admission': False, 'trails_m1_debug_fail_adoption': False}
JOURNAL_TABLES = tuple('store6_mutation_' + name for name in
                       ('ack', 'attempt', 'effect', 'execution', 'failure', 'intent'))
COUNTERS = ('applications', 'receiptCount', 'pushAttempts', 'requests')


def one(items, label):
    rows = list(items)
    if len(rows) != 1:
        raise ValueError('%s must identify exactly one row; found %d' % (label, len(rows)))
    return rows[0]


def ordered(rows):
    """Compare complete redacted rows independent of SQL result ordering."""
    return sorted(rows, key=lambda row: json.dumps(row, sort_keys=True))


def compare(reports, manifest):
    checks = []

    def check(name, observed, expected=True):
        checks.append(dict(name=name, passed=observed == expected,
                           expected=expected, observed=observed))

    initial = one((db for db in reports['baseline']['databases']
                   if db['role'] == 'journal' and db['acceptanceCount'] == 5),
                  'baseline account with five acceptances')
    identity = initial['identity']
    account = initial['accountSha256']
    client = one(initial['clients'], 'baseline mutation client')['clientSha256']

    def db(stage, role, private_account=account):
        return one((item for item in reports[stage]['databases'] if item['role'] == role
                    and (role in ('backend', 'catalog') or item['accountSha256'] == private_account)),
                   stage + ' ' + role)

    journals = {stage: db(stage, 'journal') for stage in reports}
    backends = {stage: db(stage, 'backend') for stage in reports}
    original_acceptances = ordered(initial['acceptances'])
    original_receipts = ordered(backends['baseline']['receipts'])
    original_receipt_keys = {row['idempotencyKeySha256'] for row in original_receipts}
    partitions = ordered([{'role': item['role'], 'accountSha256': item.get('accountSha256')}
                          for item in reports['baseline']['databases']])

    def counters(stage):
        return {key: backends[stage]['server'][key] for key in COUNTERS}

    def local(stage, private_account=account):
        return one((row['value'] for row in db(stage, 'values', private_account)['confirmedMemberships']
                    if row['trail'] == TRAIL), stage + ' confirmed membership')

    def canonical(stage):
        return one((row['value'] for row in backends[stage]['canonicalMemberships']
                    if row['accountSha256'] == account and row['value']['trail'] == TRAIL),
                   stage + ' canonical membership')

    def command(stage, sequence):
        return one((row for row in journals[stage]['acceptances'] if row['clientSequence'] == sequence),
                   stage + ' command ' + str(sequence))

    def no_journal_work(stage, journal, label):
        check(stage + ': ' + label + ' mutation rows reclaimed', journal['retainedRows'],
              {table: 0 for table in JOURNAL_TABLES})
        check(stage + ': ' + label + ' no execution phases', journal['phaseCounts'], {})
        for name in ('intents', 'executions', 'attempts', 'acks', 'failures'):
            check(stage + ': ' + label + ' no ' + name, journal[name], [])

    for stage, report in reports.items():
        check(stage + ': report complete', report['complete'])
        check(stage + ': force-stop declared by capture caller', report['captureStateDeclaredByCaller'], 'force-stopped')
        check(stage + ': supported report reader', report['readerVersion'], 1)
        check(stage + ': same pinned Store6 source', report['store6SourceRevision'],
              reports['baseline']['store6SourceRevision'])
        check(stage + ': all databases complete, integral and untruncated',
              bool(report['databases']) and all(item['complete'] and item['integrityOk'] and not item['truncated']
                                               for item in report['databases']))
        check(stage + ': same private and shared database partitions',
              ordered([{'role': item['role'], 'accountSha256': item.get('accountSha256')}
                       for item in report['databases']]), partitions)
        check(stage + ': all debug database faults disarmed',
              all(item['debugFaults'] == FAULTS for item in report['databases']))
        check(stage + ': loss-next-ACK fault disarmed', backends[stage]['server']['loseNextAck'], False)
        check(stage + ': same account and installation', journals[stage]['identity'], identity)
        check(stage + ': journal account matches filename partition', journals[stage]['identityMatchesPartition'])
        check(stage + ': same mutation client', one(journals[stage]['clients'], stage + ' client')['clientSha256'], client)
        expected_count = 5 if stage == 'baseline' else 6
        check(stage + ': exact admitted command count', journals[stage]['acceptanceCount'], expected_count)
        check(stage + ': exact command sequences', sorted(row['clientSequence'] for row in journals[stage]['acceptances']),
              list(range(1, expected_count + 1)))
        check(stage + ': original five command receipts retained exactly',
              ordered(row for row in journals[stage]['acceptances'] if row['clientSequence'] <= 5), original_acceptances)
        check(stage + ': all command identities belong to same account and client',
              all(row['accountSha256'] == account and row['clientSha256'] == client
                  and row['command']['accountSha256'] == account
                  and row['commandSha256'] == row['command']['commandSha256']
                  and row['command']['recognized'] and row['version'] == 1
                  for row in journals[stage]['acceptances']))
        check(stage + ': unique application command identities',
              len({row['commandSha256'] for row in journals[stage]['acceptances']}), expected_count)
        receipts = backends[stage]['receipts']
        check(stage + ': original five backend receipts retained exactly',
              ordered(row for row in receipts if row['idempotencyKeySha256'] in original_receipt_keys), original_receipts)
        receipt_count = 6 if stage == 'settled' else 5
        check(stage + ': exact backend receipt row count', len(receipts), receipt_count)
        check(stage + ': distinct backend receipt keys', len({row['idempotencyKeySha256'] for row in receipts}), receipt_count)
        check(stage + ': all backend receipts retain account and installation partition',
              all({key: row[key] for key in ('accountSha256', 'installationSha256')} == identity for row in receipts))
        check(stage + ': exact cumulative effects, receipts and push attempts',
              {key: value for key, value in counters(stage).items() if key != 'requests'},
              dict(applications=6, receiptCount=6, pushAttempts=7) if stage == 'settled'
              else dict(applications=5, receiptCount=5, pushAttempts=6))
        check(stage + ': backend settings recognized', backends[stage]['server']['settings']['recognized'])
        check(stage + ': persisted backend mode', backends[stage]['server']['settings']['effective']['mode'],
              'OFFLINE' if stage in ('pending', 'restarted') else 'ONLINE')

        # Check Robin explicitly, and preserve every other historical private account too.
        check(stage + ': Robin partition present',
              any(item.get('accountSha256') == ROBIN and item['role'] == 'journal' for item in report['databases']))
        for other in (item for item in reports['baseline']['databases']
                      if item['role'] == 'journal' and item['accountSha256'] != account):
            other_id = other['accountSha256']
            other_journal = db(stage, 'journal', other_id)
            label = 'Robin' if other_id == ROBIN else 'other account ' + other_id[:12]
            check(stage + ': ' + label + ' identity unchanged', other_journal['identity'], other['identity'])
            check(stage + ': ' + label + ' no accepted commands', other_journal['acceptances'], [])
            check(stage + ': ' + label + ' acceptance count zero', other_journal['acceptanceCount'], 0)
            no_journal_work(stage, other_journal, label)
            memberships = db(stage, 'values', other_id)['confirmedMemberships']
            check(stage + ': ' + label + ' confirmed memberships unchanged', ordered(memberships),
                  ordered(db('baseline', 'values', other_id)['confirmedMemberships']))
            check(stage + ': ' + label + ' all confirmed memberships empty',
                  bool(memberships) and all(row['value']['recognized'] and row['value']['collections'] == [] for row in memberships))
            check(stage + ': ' + label + ' server memberships empty or absent',
                  all(row['value']['recognized'] and row['value']['collections'] == []
                      for row in backends[stage]['canonicalMemberships'] if row['accountSha256'] == other_id))

        check(stage + ': confirmed membership recognized', local(stage)['recognized'])
        check(stage + ': canonical membership recognized', canonical(stage)['recognized'])
        check(stage + ': expected confirmed collections', local(stage)['collections'], DESIRED if stage == 'settled' else ['weekend'])
        check(stage + ': expected canonical collections', canonical(stage)['collections'], DESIRED if stage == 'settled' else ['weekend'])
        check(stage + ': confirmed and canonical payload bytes agree', local(stage)['payloadSha256'], canonical(stage)['payloadSha256'])

    no_journal_work('baseline', journals['baseline'], 'participating account')
    no_journal_work('settled', journals['settled'], 'participating account')
    for stage in reports:
        expected_watermarks = [5, 5, 5] if stage == 'baseline' else ([6, 6, 6] if stage == 'settled' else [6, 5, 5])
        watermarks = one(journals[stage]['clients'], stage + ' retirement watermarks')
        check(stage + ': allocated, retired and server-confirmed watermarks',
              [watermarks[key] for key in ('lastAllocatedSequence', 'retiredThroughSequence', 'serverConfirmedRetiredThroughSequence')],
              expected_watermarks)
        if stage != 'baseline':
            accepted = command(stage, 6)
            check(stage + ': command six contains complete desired set', accepted['command']['desired'],
                  dict(collections=DESIRED, recognized=True, trail=TRAIL))
            check(stage + ': command six receipt identical to original Offline admission', accepted, command('pending', 6))
        if stage in ('pending', 'restarted'):
            journal = journals[stage]
            check(stage + ': exactly one unprepared durable execution', journal['phaseCounts'], {'UNPREPARED': 1})
            execution = one(journal['executions'], stage + ' pending execution')
            check(stage + ': pending execution is sequence six before any transport attempt',
                  {key: execution[key] for key in ('clientSha256', 'clientSequence', 'generation', 'attempt', 'phase')},
                  dict(clientSha256=client, clientSequence=6, generation=0, attempt=0, phase='UNPREPARED'))
            intent = one(journal['intents'], stage + ' pending intent')
            check(stage + ': pending intent matches original admitted command and mutation',
                  {key: intent[key] for key in ('clientSha256', 'clientSequence', 'mutationSha256', 'command')},
                  {key: command('pending', 6)[key] for key in ('clientSha256', 'clientSequence', 'mutationSha256', 'command')})
            check(stage + ': pending intent targets expected trail and argument version',
                  [intent['trail'], intent['argumentVersion']], [TRAIL, 1])
            expected_rows = {table: int(table.endswith('_intent') or table.endswith('_execution')) for table in JOURNAL_TABLES}
            check(stage + ': only pending intent and execution are retained', journal['retainedRows'], expected_rows)
            for name in ('attempts', 'acks', 'failures'):
                check(stage + ': no ' + name + ' before reconnect', journal[name], [])

    check('pending to restarted: all backend counters unchanged, including requests', counters('restarted'), counters('pending'))
    check('pending to restarted: exact backend receipt rows unchanged', ordered(backends['restarted']['receipts']), ordered(backends['pending']['receipts']))
    check('pending to restarted: exact backend settings persisted', backends['restarted']['server']['settings'], backends['pending']['server']['settings'])
    check('pending to restarted: durable pending intent unchanged', journals['restarted']['intents'], journals['pending']['intents'])
    check('pending to restarted: durable execution unchanged', journals['restarted']['executions'], journals['pending']['executions'])
    new_receipt = one((row for row in backends['settled']['receipts'] if row['idempotencyKeySha256'] not in original_receipt_keys),
                      'new settled backend receipt')
    for field in ('request', 'result'):
        check('settled: new receipt ' + field + ' is the complete desired set',
              {key: new_receipt[field][key] for key in ('collections', 'recognized', 'trail')},
              dict(collections=DESIRED, recognized=True, trail=TRAIL))
        check('settled: new receipt ' + field + ' bytes equal confirmed membership',
              new_receipt[field]['payloadSha256'], local('settled')['payloadSha256'])

    summaries = {stage: dict(cumulativeCounters=counters(stage),
                            backendMode=backends[stage]['server']['settings']['effective']['mode'],
                            acceptanceCount=journals[stage]['acceptanceCount'],
                            phaseCounts=journals[stage]['phaseCounts'],
                            confirmedCollections=local(stage)['collections'],
                            retirement=one(journals[stage]['clients'], stage + ' retirement')) for stage in reports}
    deltas = {before + ' to ' + after: {key: counters(after)[key] - counters(before)[key] for key in COUNTERS}
              for before, after in (('baseline', 'pending'), ('pending', 'restarted'), ('restarted', 'settled'))}
    return dict(passed=all(item['passed'] for item in checks), assertionCount=len(checks),
                failedAssertions=[item['name'] for item in checks if not item['passed']],
                sourceReports=manifest, participatingIdentity=identity, mutationClientSha256=client,
                newCommandSha256=command('pending', 6)['commandSha256'],
                newReceiptKeySha256=new_receipt['idempotencyKeySha256'],
                stageSummaries=summaries, counterDeltas=deltas, checks=checks,
                interpretation=[
                    'The baseline is historical evidence. The other stages must be captured from the final installed APK; APK identity and UI actions require the separate install/action log.',
                    'Only one new sequence-six desired-set command is expected after the original five admissions. Original command and backend receipt contents must remain unchanged.',
                    'The two Offline stages must have identical backend counters. Baseline-to-pending requests may include intervening read verification and are reported rather than attributed to this save.',
                    'Reconnect must add one push, one server application and one receipt, retire sequence six, and confirm the exact two-collection membership locally.',
                    'This script hashes the report bytes. Archive hashes and force-stop status are reported by the extractor/capture caller, not independently established here.',
                    'Database snapshots establish persisted boundaries; they do not establish accessibility, visual fidelity or every intervening callback.'
                ])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for stage, filename in DEFAULTS.items():
        parser.add_argument('--' + stage, type=Path, default=DIRECTORY / filename,
                            help=stage + ' report (default: ' + filename + ')')
    parser.add_argument('--output', type=Path, required=True, help='new comparison JSON path; never overwritten')
    args = parser.parse_args()
    if args.output.exists():
        parser.error('Output must be a new path; existing evidence is never overwritten')
    try:
        reports, manifest = {}, {}
        for stage in DEFAULTS:
            path = getattr(args, stage)
            contents = path.read_bytes()
            reports[stage] = json.loads(contents)
            manifest[stage] = dict(path=str(path.resolve()), reportSha256=hashlib.sha256(contents).hexdigest(),
                                   reportedArchiveSha256=reports[stage]['archiveSha256'])
        result = compare(reports, manifest)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        with args.output.open('x') as stream:
            json.dump(result, stream, indent=2, sort_keys=True)
            stream.write('\n')
    except (OSError, KeyError, TypeError, ValueError) as error:
        parser.error('Comparison not completed: ' + str(error))
    print(json.dumps({key: result[key] for key in ('passed', 'assertionCount', 'failedAssertions')}))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    sys.exit(main())
