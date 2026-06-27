# Offline Queue + Quick Add — Implementation Spec

**Branch:** `feature/offline-queue-quick-add` (created)  
**Status:** Ready to implement  
**Requires:** Agent mode for code changes

---

## Summary

- PWA Quick Add (3 taps) + IndexedDB offline queue
- **Fully automatic sync** — no manual "Sync now" button
- **Local notifications only** — Web Notifications API + toast + app badge (no Firebase/third-party push)
- Shortcuts queue-first via Apple Notes + automated Wi-Fi/cellular sync shortcut

---

## Git

```bash
git checkout feature/offline-queue-quick-add
```

Commit order: backend → queue/sync libs → UI → PWA assets → docs

---

## Backend

### Files to create/modify

| File | Change |
|------|--------|
| `WalletNoteJsonRequest.java` | Add optional `categoryId` |
| `WalletNotesBatchItemResult.java` | New record |
| `WalletNotesBatchImportResult.java` | New record |
| `WalletNotesImportService.java` | `importBatchFromJson`, categoryId on save |
| `TransactionController.java` | `POST /import/wallet-notes-batch` |

### Batch endpoint

`POST /api/transaction/import/wallet-notes-batch`  
Body: `WalletNoteJsonRequest[]`  
Per-item status: `created` | `duplicate` | `skipped` — drain queue on `created` and `duplicate`.

---

## Frontend — auto-sync (no manual intervention)

### `frontend/src/lib/offline-queue.ts`

IndexedDB store `pending-transactions`.

### `frontend/src/lib/sync-pending-transactions.ts`

- Mutex + 2s debounce
- Triggers: `online`, `visibilitychange` (visible), `pageshow`, `focus`, 30s interval while pending > 0, post-enqueue attempt
- **No manual sync API exposed to UI**

### `frontend/src/lib/sync-notifications.ts`

| Channel | When |
|---------|------|
| Toast | App visible, sync completed |
| `new Notification(...)` | Permission granted + document hidden + sync completed |
| `navigator.setAppBadge(n)` | Pending count |

Permission asked once after first offline save or install banner dismiss.

### `frontend/src/components/sync-provider.tsx`

Registers all triggers in dashboard layout.

### `frontend/src/components/pending-sync-status.tsx`

Read-only: "2 expenses waiting to sync…" — **no button**.

---

## Frontend — Quick Add

### `frontend/src/components/quick-add-sheet.tsx`

Amount → category chips → Save. Payload → batch JSON (`merchant: "Cash"`, `categoryId`).

### `frontend/src/components/dashboard/sidebar.tsx`

Mobile FAB opens QuickAddSheet (not full form).

---

## PWA install

- `frontend/public/manifest.json`
- `frontend/public/icons/` (180, 192, 512, apple-touch-icon)
- Layout metadata + `viewportFit: cover`
- `ios-install-banner.tsx`

---

## Shortcuts (`docs/shortcuts-offline-queue.md`)

**Capture:** try POST → else append JSONL to Apple Notes note `FinanceManager Pending` → Show Notification.

**Sync (automated only):**
- When Wi-Fi connected
- When cellular connected (optional)
- Every 4 hours (fallback)

→ batch POST → remove synced lines → Show Notification if N > 0.

---

## QA

- [ ] Airplane mode save → reconnect → auto-sync within 30s, zero taps
- [ ] Background notification when permission granted
- [ ] Toast when app foreground
- [ ] No Sync now button anywhere
- [ ] Shortcuts Wi-Fi automation drains note

---

## Related

- [pwa-phase-1-plan.md](./pwa-phase-1-plan.md)
- [future-features-roadmap.md](./future-features-roadmap.md)
