export class ProviderConcurrency {
  private active = 0;
  private readonly listeners = new Set<() => void>();

  constructor(private readonly limit: number) {
    if (!Number.isSafeInteger(limit) || limit < 1) throw new Error('INVALID_JOB_CONCURRENCY');
  }

  tryAcquire(): boolean {
    if (this.active >= this.limit) return false;
    this.active += 1;
    return true;
  }

  release(): void {
    if (this.active < 1) throw new Error('PROVIDER_CONCURRENCY_UNDERFLOW');
    this.active -= 1;
    for (const listener of this.listeners) listener();
  }

  onAvailable(listener: () => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }
}
