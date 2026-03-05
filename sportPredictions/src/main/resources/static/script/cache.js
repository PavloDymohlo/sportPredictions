const SportCache = {
  META_KEY: 'sp_scheduler_meta',
  PREFIX: 'sp_cache_',

  async getMeta() {
    const raw = localStorage.getItem(this.META_KEY);
    if (raw) {
      try {
        const meta = JSON.parse(raw);
        const validUntil = meta.status === 'COMPLETED'
          ? new Date(meta.nextRunAt).getTime()
          : (meta._cachedUntil || 0);
        if (Date.now() < validUntil) return meta;
      } catch (e) {}
    }
    try {
      const resp = await fetch('/api/v0/scheduler/status');
      if (!resp.ok) return null;
      const meta = await resp.json();
      if (meta.status !== 'COMPLETED') {
        meta._cachedUntil = Date.now() + 10 * 60 * 1000;
      }
      localStorage.setItem(this.META_KEY, JSON.stringify(meta));
      return meta;
    } catch (e) {
      return null;
    }
  },

  async get(key) {
    const meta = await this.getMeta();
    if (!meta || meta.status !== 'COMPLETED') return null;
    const raw = localStorage.getItem(this.PREFIX + key);
    if (!raw) return null;
    try {
      const { data, cachedAt } = JSON.parse(raw);
      if (meta.lastRunAt && new Date(cachedAt) < new Date(meta.lastRunAt)) return null;
      return data;
    } catch (e) {
      return null;
    }
  },

  async set(key, data) {
    const meta = await this.getMeta();
    if (!meta || meta.status !== 'COMPLETED') return;
    localStorage.setItem(this.PREFIX + key, JSON.stringify({
      data,
      cachedAt: new Date().toISOString()
    }));
  },

  invalidate(key) {
    localStorage.removeItem(this.PREFIX + key);
  }
};
