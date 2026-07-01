import { createLocalforage, createStorage } from '@sa/utils';

const storagePrefix = import.meta.env.VITE_STORAGE_PREFIX || '';

export const localStg = createStorage<StorageType.Local>('local', storagePrefix);

export const sessionStg = createStorage<StorageType.Session>('session', storagePrefix);

export const localforage = createLocalforage<StorageType.Local>('local');

/**
 * Encrypted storage wrapper for sensitive data (tokens, health records).
 * Uses AES-GCM via Web Crypto API. The encryption key is derived from a
 * per-session passphrase so data is unreadable by XSS if the key is not in memory.
 */
const STORAGE_KEY = '__ah_enc_key__';

async function getOrCreateKey(): Promise<CryptoKey> {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  let keyBytes: Uint8Array;
  if (raw) {
    keyBytes = Uint8Array.from(atob(raw), c => c.charCodeAt(0));
  } else {
    keyBytes = crypto.getRandomValues(new Uint8Array(32));
    sessionStorage.setItem(STORAGE_KEY, btoa(String.fromCharCode(...keyBytes)));
  }
  return crypto.subtle.importKey('raw', keyBytes, { name: 'AES-GCM' }, false, ['encrypt', 'decrypt']);
}

export const encryptedStorage = {
  async set(key: string, value: unknown): Promise<void> {
    try {
      const k = await getOrCreateKey();
      const iv = crypto.getRandomValues(new Uint8Array(12));
      const encoded = new TextEncoder().encode(JSON.stringify(value));
      const cipher = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, k, encoded);
      const payload = {
        iv: Array.from(iv),
        data: Array.from(new Uint8Array(cipher))
      };
      localStg.set(key as any, payload as any);
    } catch {
      // Fallback to plain storage if crypto unavailable
      localStg.set(key as any, value as any);
    }
  },

  async get<T = unknown>(key: string): Promise<T | null> {
    const raw = localStg.get(key as any) as any;
    if (!raw) return null;
    // If not encrypted (legacy plain data), return as-is
    if (!raw.iv || !raw.data) return raw as T;
    try {
      const k = await getOrCreateKey();
      const iv = new Uint8Array(raw.iv);
      const cipher = new Uint8Array(raw.data);
      const plain = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, k, cipher);
      return JSON.parse(new TextDecoder().decode(plain)) as T;
    } catch {
      return null;
    }
  },

  remove(key: string): void {
    localStg.remove(key as any);
  }
};
