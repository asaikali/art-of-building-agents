// A turn is inferred from the tool-call IDs connecting captured model exchanges.
// This module only reads captured bodies; it never changes provider traffic.
const parse = text => { try { return JSON.parse(text); } catch { return null; } };

export function inspectExchange(exchange) {
  const request = parse(exchange.requestBody.text);
  const messages = Array.isArray(request?.messages) ? request.messages : [];
  const lastUser = messages.findLastIndex(message => message?.role === 'user');
  const content = messages[lastUser]?.content;
  const prompt = typeof content === 'string' ? content
    : Array.isArray(content) ? content.map(part => part?.text || '').join(' ') : '';
  const results = [];
  // Only the current tool-result block, not results from earlier conversation turns.
  for (let index = messages.length - 1; index > lastUser && messages[index]?.role === 'tool'; index--) {
    if (messages[index].tool_call_id) results.unshift(messages[index].tool_call_id);
  }

  const calls = new Map();
  let finishReason = null, assistant = false;
  const response = parse(exchange.responseBody.text);
  const consume = (value, streaming) => {
    const choice = value?.choices?.[0];
    if (!choice) return;
    const message = streaming ? choice.delta : choice.message;
    if (message && typeof message === 'object') assistant = true;
    if (choice.finish_reason) finishReason = choice.finish_reason;
    const tools = Array.isArray(message?.tool_calls) ? message.tool_calls : [];
    for (const [position, tool] of tools.entries()) {
      if (!tool || typeof tool !== 'object') continue;
      const key = streaming ? tool.index : position;
      const call = calls.get(key) || { id: '', name: '' };
      if (tool.id) call.id = tool.id;
      if (tool.function?.name) call.name += tool.function.name;
      calls.set(key, call);
    }
  };
  if (response) consume(response, false);
  else if (!exchange.responseBody.truncated) {
    // OpenAI SSE chunks retain the same IDs; completed streams can also form a turn.
    for (const line of exchange.responseBody.text.split(/\r?\n/)) {
      if (line.startsWith('data:')) consume(parse(line.slice(5).trim()), true);
    }
  }
  const toolCalls = [...calls.values()];
  const summary = exchange.summary;
  const failed = summary.error || (summary.complete && summary.status >= 400);
  let phase = 'Response', state = summary.complete ? 'HTTP complete' : 'In progress';
  if (failed) { phase = summary.error || `HTTP ${summary.status}`; state = 'Failed'; }
  else if (toolCalls.length || finishReason === 'tool_calls') {
    phase = 'Tool request'; state = summary.complete ? 'Waiting for tools' : 'In progress';
  } else if (finishReason === 'stop' && assistant) {
    phase = 'Final response'; state = summary.complete ? 'Complete' : 'In progress';
  } else if (finishReason) {
    phase = `Stopped: ${finishReason}`; state = summary.complete ? 'Stopped' : 'In progress';
  } else if (!summary.complete) phase = 'In progress';
  return {
    prompt: prompt.replace(/\s+/g, ' ').trim().slice(0, 180) || '(No user prompt captured)',
    results, toolCalls, phase, state,
    input: results.length ? 'Tool results' : lastUser >= 0 ? 'Initial prompt' : 'Request'
  };
}

export function groupTurns(exchanges) {
  const turns = [], pending = new Map();
  const key = (exchange, id) => `${exchange.summary.destination}\n${id}`;
  for (const exchange of [...exchanges].sort((a, b) => a.summary.id - b.summary.id)) {
    const analysis = exchange.analysis || inspectExchange(exchange);
    const parents = new Set(analysis.results.map(id => pending.get(key(exchange, id))).filter(Boolean));
    // Conflicting links are left separate rather than guessing across concurrent turns.
    let turn = parents.size === 1 ? [...parents][0] : null;
    if (!turn) {
      turn = {
        id: exchange.summary.id, number: turns.length + 1, prompt: analysis.prompt,
        partial: analysis.results.length > 0, steps: [], toolCallCount: 0, state: analysis.state
      };
      turns.push(turn);
    } else {
      // Retire this round's IDs so older messages cannot join a later independent call.
      for (const [id, owner] of pending) if (owner === turn) pending.delete(id);
    }
    turn.steps.push({ exchange: exchange.summary, ...analysis });
    turn.toolCallCount += analysis.toolCalls.length;
    turn.state = analysis.state;
    for (const call of analysis.toolCalls) if (call.id) pending.set(key(exchange, call.id), turn);
  }
  return turns;
}
