import type { WalletNotePayload } from "@/lib/wallet-note-payload";

const DB_NAME = "finance-manager";
const DB_VERSION = 1;
const STORE = "pending-transactions";

export interface PendingTransaction {
  id: string;
  createdAt: string;
  payload: WalletNotePayload;
  attempts: number;
  lastError?: string;
}

function isIndexedDbAvailable(): boolean {
  return typeof window !== "undefined" && "indexedDB" in window;
}

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE, { keyPath: "id" });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function withStore<T>(
  mode: IDBTransactionMode,
  fn: (store: IDBObjectStore) => IDBRequest<T>
): Promise<T> {
  const db = await openDb();
  return new Promise<T>((resolve, reject) => {
    const tx = db.transaction(STORE, mode);
    const store = tx.objectStore(STORE);
    const request = fn(store);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
    tx.oncomplete = () => db.close();
  });
}

export async function enqueue(payload: WalletNotePayload): Promise<PendingTransaction> {
  const item: PendingTransaction = {
    id:
      typeof crypto !== "undefined" && "randomUUID" in crypto
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    createdAt: new Date().toISOString(),
    payload,
    attempts: 0,
  };
  if (!isIndexedDbAvailable()) return item;
  await withStore("readwrite", (store) => store.put(item));
  return item;
}

export async function listPending(): Promise<PendingTransaction[]> {
  if (!isIndexedDbAvailable()) return [];
  const all = await withStore<PendingTransaction[]>("readonly", (store) =>
    store.getAll() as IDBRequest<PendingTransaction[]>
  );
  return all.sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}

export async function countPending(): Promise<number> {
  if (!isIndexedDbAvailable()) return 0;
  return withStore<number>("readonly", (store) => store.count());
}

export async function removePending(id: string): Promise<void> {
  if (!isIndexedDbAvailable()) return;
  await withStore("readwrite", (store) => store.delete(id));
}

export async function markAttempt(item: PendingTransaction, error?: string): Promise<void> {
  if (!isIndexedDbAvailable()) return;
  const updated: PendingTransaction = {
    ...item,
    attempts: item.attempts + 1,
    lastError: error,
  };
  await withStore("readwrite", (store) => store.put(updated));
}
