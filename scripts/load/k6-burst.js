import http from 'k6/http';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const attendees = positiveInteger('ATTENDEES', 100);
const capacity = positiveInteger('CAPACITY', 10);
const concurrency = positiveInteger('CONCURRENCY', 50);
const holdPeriodSeconds = positiveInteger('HOLD_PERIOD_SECONDS', 60);

const holdsCreated = new Counter('holds_created');
const holdsRejected = new Counter('holds_rejected');
const holdErrors = new Counter('hold_errors');
const holdLatency = new Trend('hold_latency', true);

http.setResponseCallback(http.expectedStatuses(201, 409));

export const options = {
  scenarios: {
    hold_burst: {
      executor: 'shared-iterations',
      vus: Math.min(concurrency, attendees),
      iterations: attendees,
      maxDuration: '2m',
    },
  },
  thresholds: {
    hold_errors: ['count == 0'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)'],
};

export function setup() {
  const response = http.post(
    `${baseUrl}/api/events`,
    JSON.stringify({
      name: 'k6 Burst Lab',
      capacity,
      holdPeriodSeconds,
    }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'POST /api/events' },
    },
  );

  if (response.status !== 201) {
    throw new Error(
      `could not create Event: status=${response.status} body=${response.body}`,
    );
  }

  return { eventId: response.json('eventId') };
}

export default function (data) {
  const attendeeId = `k6-attendee-${exec.scenario.iterationInTest}`;
  const response = http.post(
    `${baseUrl}/api/events/${data.eventId}/holds`,
    JSON.stringify({ attendeeId }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'POST /api/events/:eventId/holds' },
    },
  );

  const created = response.status === 201;
  const rejected =
    response.status === 409 &&
    response.body.includes('"reason":"CAPACITY_EXHAUSTED"');
  const error = !created && !rejected;

  holdsCreated.add(created ? 1 : 0);
  holdsRejected.add(rejected ? 1 : 0);
  holdErrors.add(error ? 1 : 0);
  holdLatency.add(response.timings.duration);

  if (error) {
    console.error(
      `unclassified Hold result attendee=${attendeeId} status=${response.status} body=${response.body}`,
    );
  }
}

export function handleSummary(data) {
  const created = metric(data, 'holds_created', 'count');
  const rejected = metric(data, 'holds_rejected', 'count');
  const errors = metric(data, 'hold_errors', 'count');
  const total = created + rejected + errors;
  const latency = data.metrics.hold_latency?.values || {};
  const report = [
    '',
    'RushBook Hold burst result',
    `configuration  attendees=${attendees} capacity=${capacity} concurrency=${Math.min(concurrency, attendees)}`,
    `domain results created=${created} rejected=${rejected} errors=${errors} total=${total}`,
    `latency (ms)   avg=${format(latency.avg)} med=${format(latency.med)} p90=${format(latency['p(90)'])} p95=${format(latency['p(95)'])} max=${format(latency.max)}`,
    '',
    'Interpretation: created/rejected/errors classify outcomes; latency measures the Event-row hotspot.',
    'Throughput and latency are performance observations, not a proof of the capacity invariant.',
    'Run the deterministic PostgreSQL integration suite for correctness evidence.',
    '',
  ].join('\n');

  const outputs = { stdout: report };
  if (__ENV.SUMMARY_FILE) {
    outputs[__ENV.SUMMARY_FILE] = JSON.stringify(data, null, 2);
  }
  return outputs;
}

function positiveInteger(name, fallback) {
  const value = Number(__ENV[name] || fallback);
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${name} must be a positive integer, received ${__ENV[name]}`);
  }
  return value;
}

function metric(data, name, aggregation) {
  return data.metrics[name]?.values[aggregation] || 0;
}

function format(value) {
  return Number.isFinite(value) ? value.toFixed(2) : 'n/a';
}
