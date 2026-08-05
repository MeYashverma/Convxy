/**
 * Joins a real room created by the app and drives playback as a member.
 *
 *   node join-live.mjs <BASE_URL> <ROOM_CODE>
 *
 * Exists because the interesting half of this feature — a member, not the host,
 * controlling playback — cannot be exercised from a single phone.
 */
const [base, code] = process.argv.slice(2);
if (!base || !code) {
  console.error('usage: node join-live.mjs <https-base> <ROOM_CODE>');
  process.exit(1);
}

const ws = new WebSocket(base.replace(/^http/, 'ws') + '/room/' + code.toUpperCase());
const inbox = [];
const send = (type, payload) => {
  ws.send(JSON.stringify(payload === undefined ? { type } : { type, payload }));
  console.log('  ->', type, payload ? JSON.stringify(payload) : '');
};
const waitFor = async (type, ms) => {
  const end = Date.now() + ms;
  while (Date.now() < end) {
    const hit = inbox.find((m) => m.type === type);
    if (hit) return hit;
    await new Promise((r) => setTimeout(r, 80));
  }
  return null;
};

ws.addEventListener('message', (e) => {
  const m = JSON.parse(e.data);
  if (m.type !== 'pong') console.log('  <-', m.type, m.payload ? JSON.stringify(m.payload).slice(0, 160) : '');
  inbox.push(m);
});

ws.addEventListener('open', async () => {
  console.log(`connected to room ${code.toUpperCase()}`);
  send('join_room', { username: 'claude-test' });

  console.log('\n>>> APPROVE THE JOIN REQUEST ON YOUR PHONE (60s) <<<\n');
  const approved = await waitFor('join_approved', 60000);
  if (!approved) {
    console.log('RESULT: never approved — nothing further to test.');
    process.exit(1);
  }

  const state = approved.payload.state;
  console.log('\njoined. control_mode =', state.control_mode);
  console.log('current_track =', state.current_track?.title ?? '(none)');
  console.log('members =', state.users.map((u) => u.username).join(', '), '\n');

  if (state.control_mode !== 'everyone') {
    console.log('RESULT: room is owner-only; flip it to Everyone on the phone and rerun.');
    process.exit(1);
  }

  const me = approved.payload.user_id;
  console.log('--- member sends PAUSE ---');
  inbox.length = 0;
  send('playback_action', { action: 'pause', position: 0, server_time: Date.now() });
  // Must match MY pause specifically. Matching any sync_playback gave a false
  // pass last run: the host's 10s heartbeat arrived first and was counted as
  // the echo, hiding that the pause had been dropped outright.
  const echo = await (async () => {
    const end = Date.now() + 6000;
    while (Date.now() < end) {
      const hit = inbox.find((m) => m.type === 'sync_playback' &&
        m.payload?.from_user_id === me && m.payload?.action === 'pause');
      if (hit) return hit;
      await new Promise((r) => setTimeout(r, 80));
    }
    return null;
  })();
  console.log(echo ? 'PASS  my pause was relayed back' : 'FAIL  my pause was never relayed');

  await new Promise((r) => setTimeout(r, 3000));
  console.log('\n--- member sends PLAY ---');
  send('playback_action', { action: 'play', position: 0, server_time: Date.now() });
  await new Promise((r) => setTimeout(r, 3000));

  console.log('\nDone. Check the phone: it should have paused then resumed.');
  ws.close();
  process.exit(0);
});

ws.addEventListener('error', (e) => { console.error('socket error', e.message); process.exit(1); });
setInterval(() => ws.readyState === 1 && ws.send(JSON.stringify({ type: 'ping' })), 25000);
