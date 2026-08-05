/**
 * Per-IP request throttle.
 *
 * Cloudflare's WAF rate-limiting rules live under a zone, which means a domain
 * you own. On a bare *.workers.dev subdomain there is no zone, so the throttle
 * has to be in code. One Durable Object per client IP, fixed window.
 *
 * This exists because room codes are six characters and therefore walkable: a
 * script can dial codes until it lands in someone's room. Join approval is the
 * second line of defence; this is the first.
 */

interface Window {
  count: number;
  resets_at: number;
}

const WINDOW_MS = 60_000;

export class Guard {
  private state: DurableObjectState;

  constructor(state: DurableObjectState) {
    this.state = state;
  }

  async fetch(request: Request): Promise<Response> {
    const limit = Number(new URL(request.url).searchParams.get('limit') ?? '20');
    const now = Date.now();

    // Storage rather than a field: this object hibernates like any other, and
    // an in-memory counter would silently reset to zero on every eviction —
    // which is exactly the same as having no throttle at all.
    let w = await this.state.storage.get<Window>('w');
    if (!w || now >= w.resets_at) {
      w = { count: 0, resets_at: now + WINDOW_MS };
    }
    w.count += 1;
    await this.state.storage.put('w', w);
    // Let the window expire itself so idle IPs do not hold storage forever.
    await this.state.storage.setAlarm(w.resets_at + WINDOW_MS);

    const allowed = w.count <= limit;
    return new Response(allowed ? 'ok' : 'slow down', {
      status: allowed ? 200 : 429,
      headers: { 'retry-after': String(Math.ceil((w.resets_at - now) / 1000)) },
    });
  }

  async alarm() {
    await this.state.storage.deleteAll();
  }
}
