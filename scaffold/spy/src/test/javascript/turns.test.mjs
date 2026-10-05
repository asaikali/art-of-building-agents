import test from 'node:test';
import assert from 'node:assert/strict';
import { groupTurns, inspectExchange } from '../../main/resources/static/spy/turns.js';

const tool = (id, name = 'weather') => ({ id, type: 'function', function: { name, arguments: '{}' } });
const body = text => ({ text, truncated: false, totalBytes: text.length });
function exchange(id, { prompt = 'Weather?', results = [], calls = [], finish = calls.length ? 'tool_calls' : 'stop',
  messages, complete = true, status = 200, response, destination = 'https://api.openai.com/v1/chat/completions' } = {}) {
  messages ||= [{ role: 'user', content: prompt }, ...results.flatMap(callId => [
    { role: 'assistant', tool_calls: [tool(callId)] }, { role: 'tool', tool_call_id: callId, content: '{}' }
  ])];
  // Parallel results belong to one trailing result block.
  if (results.length > 1) messages = [{ role: 'user', content: prompt },
    { role: 'assistant', tool_calls: results.map(id => tool(id)) },
    ...results.map(id => ({ role: 'tool', tool_call_id: id, content: '{}' }))];
  return {
    summary: { id, destination, status, complete, error: null, startedAt: '2026-10-05T20:00:00Z', durationMs: 10 },
    requestBody: body(JSON.stringify({ messages })),
    responseBody: body(response ?? JSON.stringify({ choices: [{ message: {
      role: 'assistant', content: calls.length ? null : 'Final answer', tool_calls: calls
    }, finish_reason: finish }] }))
  };
}

test('one round with parallel tools counts tool calls separately from HTTP exchanges', () => {
  const turns = groupTurns([exchange(2, { results: ['a', 'b'] }), exchange(1, { calls: [tool('a'), tool('b')] })]);
  assert.equal(turns.length, 1);
  assert.equal(turns[0].state, 'Complete');
  assert.equal(turns[0].toolCallCount, 2);
  assert.deepEqual(turns[0].steps.map(step => step.exchange.id), [1, 2]);
  assert.deepEqual(turns[0].steps.map(step => `${step.input} → ${step.phase}`),
    ['Initial prompt → Tool request', 'Tool results → Final response']);
  assert.equal(turns[0].steps[1].results.length, 2);
});

test('interleaved turns with identical prompts are connected only by their own IDs', () => {
  const turns = groupTurns([
    exchange(1, { calls: [tool('a')] }), exchange(2, { calls: [tool('b')] }),
    exchange(3, { results: ['b'] }), exchange(4, { results: ['a'] })
  ]);
  assert.deepEqual(turns.map(turn => turn.steps.map(step => step.exchange.id)), [[1, 4], [2, 3]]);
});

test('several tool rounds stay in one turn and completed IDs cannot attach later calls', () => {
  const turns = groupTurns([
    exchange(1, { calls: [tool('a')] }),
    exchange(2, { results: ['a'], calls: [tool('b')] }),
    exchange(3, { results: ['b'] }), exchange(4, { results: ['a'] })
  ]);
  assert.deepEqual(turns.map(turn => turn.steps.map(step => step.exchange.id)), [[1, 2, 3], [4]]);
  assert.equal(turns[0].steps[1].phase, 'Tool request');
  assert.equal(turns[1].partial, true);
});

test('a new user message with conversation history starts a new turn', () => {
  const turns = groupTurns([exchange(1, { calls: [tool('a')] }), exchange(2, { messages: [
    { role: 'user', content: 'Weather?' }, { role: 'assistant', tool_calls: [tool('a')] },
    { role: 'tool', tool_call_id: 'a', content: '{}' }, { role: 'assistant', content: 'Done' },
    { role: 'user', content: 'A new question' }
  ] })]);
  assert.equal(turns.length, 2);
  assert.equal(turns[1].prompt, 'A new question');
});

test('IDs from another provider endpoint do not join a turn', () => {
  const turns = groupTurns([exchange(1, { calls: [tool('a')] }), exchange(2, {
    results: ['a'], destination: 'https://other.example/v1/chat/completions'
  })]);
  assert.equal(turns.length, 2);
  assert.equal(turns[1].partial, true);
});

test('streamed tool request deltas join their following result exchange', () => {
  const response = [
    { choices: [{ delta: { tool_calls: [{ index: 0, id: 'a', function: { name: 'get', arguments: '' } }] } }] },
    { choices: [{ delta: { tool_calls: [{ index: 0, function: { name: 'Weather', arguments: '{}' } }] } }] },
    { choices: [{ delta: {}, finish_reason: 'tool_calls' }] }
  ].map(chunk => `data: ${JSON.stringify(chunk)}\n\n`).join('') + 'data: [DONE]\n\n';
  const turns = groupTurns([exchange(1, { response }), exchange(2, { results: ['a'] })]);
  assert.equal(turns.length, 1);
  assert.equal(turns[0].steps[0].toolCalls[0].name, 'getWeather');
  assert.equal(turns[0].state, 'Complete');
});

test('pending, failed and limited responses do not claim a completed final answer', () => {
  assert.equal(groupTurns([exchange(1, { complete: false, calls: [tool('a')] })])[0].state, 'In progress');
  const failed = groupTurns([exchange(1, { calls: [tool('a')] }), exchange(2, { results: ['a'], status: 502 })]);
  assert.equal(failed[0].state, 'Failed');
  assert.equal(failed[0].steps[1].phase, 'HTTP 502');
  assert.equal(groupTurns([exchange(1, { finish: 'length' })])[0].state, 'Stopped');
});

test('missing history and unreadable bodies are displayed without inventing links', () => {
  assert.equal(groupTurns([exchange(2, { results: ['missing'] })])[0].partial, true);
  const unreadable = exchange(1, { response: '{"choices":' });
  unreadable.requestBody = body('not JSON');
  assert.equal(inspectExchange(unreadable).prompt, '(No user prompt captured)');
  assert.equal(groupTurns([unreadable])[0].state, 'HTTP complete');
  const truncated = exchange(2, { response: 'data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"a"}]}}]}\n\n' });
  truncated.responseBody.truncated = true;
  assert.equal(inspectExchange(truncated).toolCalls.length, 0);
});

test('unrecognized JSON shapes remain inspectable instead of breaking the viewer', () => {
  const unusual = exchange(1, { messages: [null, { role: 'user', content: [null, { text: 'Hello' }] }],
    response: '{"choices":[{"message":{"tool_calls":[null,"unexpected"]}}]}' });
  assert.equal(inspectExchange(unusual).prompt, 'Hello');
  assert.equal(inspectExchange(unusual).toolCalls.length, 0);
});
