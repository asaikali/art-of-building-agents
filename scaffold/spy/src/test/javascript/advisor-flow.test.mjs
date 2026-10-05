import test from 'node:test';
import assert from 'node:assert/strict';
import { linkModelExchanges, sequenceEvents, advisorLabels } from '../../main/resources/static/spy/advisor-flow.js';

const exchange = (id, text) => ({ summary: { id }, responseBody: { text } });

test('advisor labels resolve class names from the nearest client while preserving instance names', () => {
  const spans = [{ id: 1, kind: 'client', advisors: [
    { name: 'First', order: 0, type: 'TraceAdvisor' },
    { name: 'Second', order: 1, type: 'TraceAdvisor' },
    { name: 'call', order: 100, type: 'ChatModelCallAdvisor' }] },
    { id: 2, parentId: 1, kind: 'advisor', name: 'First', order: 0 },
    { id: 3, parentId: 2, kind: 'advisor', name: 'Second', order: 1 },
    { id: 4, parentId: 3, kind: 'advisor', name: 'call', order: 100 },
    { id: 5, parentId: 4, kind: 'client', advisors: [{ name: 'call', order: 100, type: 'CustomAdvisor' }] },
    { id: 6, parentId: 5, kind: 'advisor', name: 'call', order: 100 },
    { id: 7, kind: 'advisor', name: 'Unknown', order: 0 }];
  assert.deepEqual([...advisorLabels(spans)], [
    [2, { type: 'TraceAdvisor', name: 'First' }], [3, { type: 'TraceAdvisor', name: 'Second' }],
    [4, { type: 'ChatModelCallAdvisor', name: 'call' }], [6, { type: 'CustomAdvisor', name: 'call' }],
    [7, { type: 'Unknown', name: 'Unknown' }]]);
});

test('model observations link only to an unambiguous provider response ID', () => {
  const spans = [{ id: 1, kind: 'model', responseId: 'first' },
    { id: 2, kind: 'model', responseId: 'second' },
    { id: 3, kind: 'advisor', responseId: 'first' },
    { id: 4, kind: 'model', responseId: 'missing' }];
  const exchanges = [exchange(12, '{"id":"second"}'), exchange(11, '{"id":"first"}')];
  assert.deepEqual([...linkModelExchanges(spans, exchanges)], [[1, 11], [2, 12]]);
  assert.deepEqual([...linkModelExchanges(spans, [...exchanges, exchange(13, '{"id":"first"}')])], [[2, 12]]);
});

test('streaming IDs link without headers and unreadable responses remain unlinked', () => {
  const spans = [{ id: 1, kind: 'model', responseId: 'stream' }];
  const exchanges = [exchange(10, 'data: {"id":"stream"}\n\ndata: [DONE]\n\n'), exchange(11, 'Provider error')];
  assert.deepEqual([...linkModelExchanges(spans, exchanges)], [[1, 10]]);
});

test('sequence preserves nested entry and reverse return order, including unfinished calls', () => {
  const spans = [{ id: 1, startSequence: 1, endSequence: 6 },
    { id: 2, startSequence: 2, endSequence: 5 },
    { id: 3, startSequence: 3, endSequence: 4 },
    { id: 7, startSequence: 7, endSequence: null }];
  assert.deepEqual(sequenceEvents(spans).map(event => [event.span.id, event.entering]),
    [[1, true], [2, true], [3, true], [3, false], [2, false], [1, false], [7, true]]);
});
