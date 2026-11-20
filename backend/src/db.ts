import fs from "node:fs";
import path from "node:path";
import { DatabaseSync } from "node:sqlite";

export type TopupStatus =
  | "CREATED"
  | "PENDING"
  | "PAID"
  | "EXPIRED"
  | "CANCELLED"
  | "ERROR";

export type TopupRow = {
  id: string;
  device_id: string;
  amount_kzt: number;
  status: TopupStatus;
  apipay_invoice_id: number | null;
  external_order_id: string;
  qr_image_url: string | null;
  qr_token_url: string | null;
  expires_at: string | null;
  error_message: string | null;
  created_at: string;
  updated_at: string;
  paid_at: string | null;
};

export type WalletRow = {
  device_id: string;
  balance_kzt: number;
  updated_at: string;
};

export type LedgerRow = {
  id: string;
  device_id: string;
  topup_id: string | null;
  amount_kzt: number;
  kind: string;
  note: string;
  created_at: string;
};

export class WalletStore {
  readonly db: DatabaseSync;

  constructor(dataDir: string) {
    fs.mkdirSync(dataDir, { recursive: true });
    const dbPath = path.join(dataDir, "wallet.sqlite");
    this.db = new DatabaseSync(dbPath);
    this.db.exec(`
      PRAGMA journal_mode = WAL;
      CREATE TABLE IF NOT EXISTS wallets (
        device_id TEXT PRIMARY KEY,
        balance_kzt INTEGER NOT NULL DEFAULT 0,
        updated_at TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS topups (
        id TEXT PRIMARY KEY,
        device_id TEXT NOT NULL,
        amount_kzt INTEGER NOT NULL,
        status TEXT NOT NULL,
        apipay_invoice_id INTEGER,
        external_order_id TEXT NOT NULL UNIQUE,
        qr_image_url TEXT,
        qr_token_url TEXT,
        expires_at TEXT,
        error_message TEXT,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        paid_at TEXT
      );
      CREATE TABLE IF NOT EXISTS ledger_entries (
        id TEXT PRIMARY KEY,
        device_id TEXT NOT NULL,
        topup_id TEXT,
        amount_kzt INTEGER NOT NULL,
        kind TEXT NOT NULL,
        note TEXT NOT NULL,
        created_at TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS processed_events (
        event_key TEXT PRIMARY KEY,
        created_at TEXT NOT NULL
      );
      CREATE INDEX IF NOT EXISTS idx_topups_device ON topups(device_id);
      CREATE INDEX IF NOT EXISTS idx_topups_invoice ON topups(apipay_invoice_id);
      CREATE INDEX IF NOT EXISTS idx_ledger_device ON ledger_entries(device_id);
    `);
  }

  close(): void {
    this.db.close();
  }

  ensureWallet(deviceId: string, nowIso: string): WalletRow {
    this.db
      .prepare(
        `INSERT INTO wallets (device_id, balance_kzt, updated_at)
         VALUES (?, 0, ?)
         ON CONFLICT(device_id) DO NOTHING`,
      )
      .run(deviceId, nowIso);
    const row = this.db
      .prepare(`SELECT device_id, balance_kzt, updated_at FROM wallets WHERE device_id = ?`)
      .get(deviceId) as WalletRow | undefined;
    if (!row) throw new Error("wallet missing after ensure");
    return row;
  }

  getWallet(deviceId: string): WalletRow | undefined {
    return this.db
      .prepare(`SELECT device_id, balance_kzt, updated_at FROM wallets WHERE device_id = ?`)
      .get(deviceId) as WalletRow | undefined;
  }

  insertTopup(row: TopupRow): void {
    this.db
      .prepare(
        `INSERT INTO topups (
          id, device_id, amount_kzt, status, apipay_invoice_id, external_order_id,
          qr_image_url, qr_token_url, expires_at, error_message, created_at, updated_at, paid_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      )
      .run(
        row.id,
        row.device_id,
        row.amount_kzt,
        row.status,
        row.apipay_invoice_id,
        row.external_order_id,
        row.qr_image_url,
        row.qr_token_url,
        row.expires_at,
        row.error_message,
        row.created_at,
        row.updated_at,
        row.paid_at,
      );
  }

  updateTopup(
    id: string,
    patch: Partial<
      Pick<
        TopupRow,
        | "status"
        | "apipay_invoice_id"
        | "qr_image_url"
        | "qr_token_url"
        | "expires_at"
        | "error_message"
        | "updated_at"
        | "paid_at"
      >
    >,
  ): void {
    const current = this.getTopup(id);
    if (!current) return;
    const next = { ...current, ...patch };
    this.db
      .prepare(
        `UPDATE topups SET
          status = ?, apipay_invoice_id = ?, qr_image_url = ?, qr_token_url = ?,
          expires_at = ?, error_message = ?, updated_at = ?, paid_at = ?
         WHERE id = ?`,
      )
      .run(
        next.status,
        next.apipay_invoice_id,
        next.qr_image_url,
        next.qr_token_url,
        next.expires_at,
        next.error_message,
        next.updated_at,
        next.paid_at,
        id,
      );
  }

  getTopup(id: string): TopupRow | undefined {
    return this.db.prepare(`SELECT * FROM topups WHERE id = ?`).get(id) as TopupRow | undefined;
  }

  getTopupByInvoiceId(invoiceId: number): TopupRow | undefined {
    return this.db
      .prepare(`SELECT * FROM topups WHERE apipay_invoice_id = ?`)
      .get(invoiceId) as TopupRow | undefined;
  }

  listLedger(deviceId: string, limit = 20): LedgerRow[] {
    return this.db
      .prepare(
        `SELECT * FROM ledger_entries WHERE device_id = ? ORDER BY created_at DESC LIMIT ?`,
      )
      .all(deviceId, limit) as LedgerRow[];
  }

  /**
   * Credits wallet once per top-up. Safe under retries/webhooks.
   * Returns true when this call performed the credit.
   */
  creditTopupIfNeeded(topupId: string, nowIso: string, note: string): boolean {
    const topup = this.getTopup(topupId);
    if (!topup) return false;
    if (topup.status === "PAID") return false;

    this.db.exec("BEGIN IMMEDIATE");
    try {
      const again = this.getTopup(topupId);
      if (!again || again.status === "PAID") {
        this.db.exec("ROLLBACK");
        return false;
      }
      this.ensureWallet(again.device_id, nowIso);
      this.db
        .prepare(
          `UPDATE wallets SET balance_kzt = balance_kzt + ?, updated_at = ? WHERE device_id = ?`,
        )
        .run(again.amount_kzt, nowIso, again.device_id);
      this.db
        .prepare(
          `INSERT INTO ledger_entries (id, device_id, topup_id, amount_kzt, kind, note, created_at)
           VALUES (?, ?, ?, ?, 'TOPUP', ?, ?)`,
        )
        .run(`${topupId}-credit`, again.device_id, again.id, again.amount_kzt, note, nowIso);
      this.db
        .prepare(
          `UPDATE topups SET status = 'PAID', updated_at = ?, paid_at = ?, error_message = NULL WHERE id = ?`,
        )
        .run(nowIso, nowIso, again.id);
      this.db.exec("COMMIT");
      return true;
    } catch (error) {
      try {
        this.db.exec("ROLLBACK");
      } catch {
        // ignore rollback errors
      }
      throw error;
    }
  }

  markProcessedEvent(eventKey: string, nowIso: string): boolean {
    const existing = this.db
      .prepare(`SELECT event_key FROM processed_events WHERE event_key = ?`)
      .get(eventKey);
    if (existing) return false;
    this.db
      .prepare(`INSERT INTO processed_events (event_key, created_at) VALUES (?, ?)`)
      .run(eventKey, nowIso);
    return true;
  }
}
