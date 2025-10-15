import { provideZonelessChangeDetection } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { appConfig } from './app/app.config';

/**
 * Safe UUID polyfill for browsers (like S3 website endpoint HTTP)
 * where crypto.randomUUID is missing or crypto is partially supported.
 * It never overwrites the read-only window.crypto object.
 */
(() => {
  const g: any = globalThis as any;

  // Already available → do nothing
  if (g.crypto && typeof g.crypto.randomUUID === 'function') return;

  // Generate RFC4122 v4 UUID (fallback)
  const uuidv4 = () => {
    const bytes: number[] =
      (g.crypto && g.crypto.getRandomValues)
        ? Array.from(g.crypto.getRandomValues(new Uint8Array(16)))
        : Array.from({ length: 16 }, () => Math.floor(Math.random() * 256));

    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;

    const hex = bytes.map(b => b.toString(16).padStart(2, '0')).join('');
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
  };

  // Define randomUUID safely (if possible)
  if (g.crypto && Object.getOwnPropertyDescriptor(g, 'crypto')) {
    try {
      Object.defineProperty(g.crypto, 'randomUUID', {
        value: uuidv4,
        configurable: true,
      });
    } catch {
      g.__uuidv4__ = uuidv4;
    }
  } else {
    g.__uuidv4__ = uuidv4;
  }
})();

/**
 * Safe helper for your code to call UUIDs consistently.
 */
export function randomId(): string {
  const g: any = globalThis as any;
  if (g.crypto && typeof g.crypto.randomUUID === 'function') {
    return g.crypto.randomUUID();
  }
  if (g.__uuidv4__) return g.__uuidv4__();
  return 'id-' + Math.random().toString(36).slice(2) + Date.now().toString(36);
}

// Bootstrap Angular app
bootstrapApplication(App, appConfig)
  .catch(err => console.error(err));
