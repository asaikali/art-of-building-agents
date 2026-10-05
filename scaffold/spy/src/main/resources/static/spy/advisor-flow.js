const parse = text => { try { return JSON.parse(text); } catch { return null; } };

export function linkModelExchanges(spans, exchanges) {
  const responses = new Map();
  for (const exchange of exchanges) {
    const text = exchange.responseBody.text;
    let id = parse(text)?.id;
    if (!id) {
      for (const line of text.split(/\r?\n/)) {
        if (line.startsWith('data:')) id ||= parse(line.slice(5).trim())?.id;
      }
    }
    if (typeof id === 'string' && id) {
      const matches = responses.get(id) || [];
      matches.push(exchange.summary.id);
      responses.set(id, matches);
    }
  }
  return new Map(spans.filter(span => span.kind === 'model').flatMap(span => {
    const matches = responses.get(span.responseId);
    // IDs, never timestamps or prompts. An ambiguous or absent ID remains unlinked.
    return matches?.length === 1 ? [[span.id, matches[0]]] : [];
  }));
}

export function sequenceEvents(spans) {
  return spans.flatMap(span => [
    { sequence: span.startSequence, span, entering: true },
    ...(span.endSequence == null ? [] : [{ sequence: span.endSequence, span, entering: false }])
  ]).sort((a, b) => a.sequence - b.sequence);
}

export function advisorLabels(spans) {
  const byId = new Map(spans.map(span => [span.id, span]));
  return new Map(spans.filter(span => span.kind === 'advisor').map(span => {
    let ancestor = byId.get(span.parentId);
    while (ancestor && ancestor.kind !== 'client') ancestor = byId.get(ancestor.parentId);
    const types = new Set((ancestor?.advisors || [])
      .filter(advisor => advisor.name === span.name && advisor.order === span.order)
      .map(advisor => advisor.type).filter(Boolean));
    // Resolve the class from this invocation's configuration, including nested clients.
    const type = types.size === 1 ? [...types][0] : span.name;
    return [span.id, { type, name: span.name }];
  }));
}

export function renderSequence(container, spans, links, selectExchange) {
  const document = container.ownerDocument, ns = 'http://www.w3.org/2000/svg';
  const node = (tag, attrs, text) => {
    const element = document.createElementNS(ns, tag);
    for (const [name, value] of Object.entries(attrs)) element.setAttribute(name, value);
    if (text != null) element.textContent = text;
    return element;
  };
  const laneKey = span => `${span.kind}:${span.name}:${span.order ?? ''}`;
  const lanes = new Map([['app', { name: 'App', kind: 'app' }]]);
  for (const span of spans) if (!lanes.has(laneKey(span))) lanes.set(laneKey(span), span);
  const keys = [...lanes.keys()], events = sequenceEvents(spans);
  const labels = advisorLabels(spans);
  const width = Math.max(720, lanes.size * 210), height = Math.max(240, events.length * 50 + 150);
  const x = key => 105 + keys.indexOf(key) * 210;
  const y = index => 125 + index * 50;
  const byId = new Map(spans.map(span => [span.id, span]));
  const parentKey = span => byId.has(span.parentId) ? laneKey(byId.get(span.parentId)) : 'app';
  const svg = node('svg', { width, height, viewBox: `0 0 ${width} ${height}`, role: 'group',
    'aria-label': 'Observed advisor execution sequence. Solid arrows enter, dashed arrows return.' });
  const defs = node('defs', {});
  const marker = node('marker', { id: 'flow-arrow', viewBox: '0 0 10 10', refX: 9, refY: 5,
    markerWidth: 7, markerHeight: 7, orient: 'auto-start-reverse' });
  marker.append(node('path', { d: 'M 0 0 L 10 5 L 0 10 z', fill: '#526278' }));
  defs.append(marker); svg.append(defs);
  for (const [key, span] of lanes) {
    const left = x(key);
    const title = labels.get(span.id)?.type || span.name;
    svg.append(node('rect', { x: left - 95, y: 12, width: 190, height: 84, rx: 6,
      fill: span.kind === 'advisor' ? '#edf3ff' : span.kind === 'tool' ? '#e1f4eb' : '#f3f6fa', stroke: '#b8c5d6' }));
    const label = node('text', { x: left, y: 36, 'text-anchor': 'middle', class: 'flow-lane' });
    label.append(node('title', {}, title === span.name ? title : `${title} · Name: ${span.name}`));
    // Long framework advisor names wrap without losing the actual name.
    const words = title.match(/.{1,22}/g) || ['App'];
    for (const [index, word] of words.entries()) {
      label.append(node('tspan', { x: left, dy: index ? 16 : 0 }, word));
    }
    svg.append(label);
    if (title !== span.name) svg.append(node('text', { x: left, y: 70, 'text-anchor': 'middle', class: 'flow-name' }, `Name: ${span.name}`));
    if (span.order != null) svg.append(node('text', { x: left, y: 88, 'text-anchor': 'middle', class: 'flow-order' }, `order ${span.order}`));
    svg.append(node('line', { x1: left, x2: left, y1: 98, y2: height - 25,
      stroke: '#b8c5d6', 'stroke-dasharray': '4 4' }));
  }
  for (const span of spans) {
    const begin = events.findIndex(event => event.span.id === span.id && event.entering);
    const end = events.findIndex(event => event.span.id === span.id && !event.entering);
    svg.append(node('rect', { x: x(laneKey(span)) - 5, y: y(begin), width: 10,
      height: Math.max(6, (end < 0 ? height - 30 : y(end)) - y(begin)),
      fill: span.error ? '#ffe7e2' : '#dce7f8', stroke: span.error ? '#b65e4a' : '#7c96bb' }));
  }
  for (const [index, event] of events.entries()) {
    const span = event.span, own = laneKey(span), parent = parentKey(span);
    const from = x(event.entering ? parent : own), to = x(event.entering ? own : parent), top = y(index);
    const attrs = { fill: 'none', stroke: span.error ? '#b65e4a' : '#526278',
      'marker-end': 'url(#flow-arrow)', ...(event.entering ? {} : { 'stroke-dasharray': '5 4' }) };
    svg.append(node('path', { ...attrs, d: from === to
      ? `M ${from + 5} ${top} h 60 v 18 h -60` : `M ${from} ${top} H ${to}` }));
    const exchange = links.get(span.id);
    const labelText = event.entering
      ? `${span.kind === 'tool' ? 'Execute tool' : 'Enter'}${span.requestMessages ? ` · ${span.requestMessages} messages` : ''}`
      : `${span.error || span.outcome || 'Return'} · ${span.durationMs} ms${exchange == null ? '' : ` · HTTP #${exchange}`}`;
    const label = node('text', { x: Math.min(from, to) + 12, y: top - 9, class: exchange == null ? 'flow-label' : 'flow-label flow-link' }, labelText);
    if (exchange != null) {
      label.setAttribute('role', 'button'); label.setAttribute('tabindex', '0');
      label.setAttribute('aria-label', `${labelText}. Open HTTP exchange ${exchange}`);
      label.addEventListener('click', () => selectExchange(exchange));
      label.addEventListener('keydown', event => {
        if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); selectExchange(exchange); }
      });
    }
    svg.append(label);
  }
  container.replaceChildren(svg);
}
