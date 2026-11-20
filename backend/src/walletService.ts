import { randomUUID } from "node:crypto";
import { ApipayClient, mapApipayStatus } from "./apipay.js";
import { AppConfig, validateTopupAmount } from "./config.js";
import { TopupRow, TopupStatus, WalletStore } from "./db.js";

export type PublicTopup = {
  topupId: string;
  apipayInvoiceId: number | null;
  amountKzt: number;
  status: TopupStatus;
  qrImageUrl: string | null;
  qrTokenUrl: string | null;
  expiresAt: string | null;
  errorMessage: string | null;
  walletBalanceKzt: number;
  sandbox: boolean;
};

export type PublicWallet = {
  deviceId: string;
  balanceKzt: number;
  sandbox: boolean;
  configured: boolean;
  recent: Array<{
    id: string;
    amountKzt: number;
    kind: string;
    note: string;
    createdAt: string;
  }>;
};

export class WalletService {
  constructor(
    private readonly store: WalletStore,
    private readonly apipay: ApipayClient,
    private readonly config: AppConfig,
    private readonly now: () => Date = () => new Date(),
  ) {}

  private iso(): string {
    return this.now().toISOString();
  }

  getWallet(deviceId: string): PublicWallet {
    const wallet = this.store.ensureWallet(deviceId, this.iso());
    const recent = this.store.listLedger(deviceId).map((row) => ({
      id: row.id,
      amountKzt: row.amount_kzt,
      kind: row.kind,
      note: row.note,
      createdAt: row.created_at,
    }));
    return {
      deviceId: deviceId,
      balanceKzt: wallet.balance_kzt,
      sandbox: this.config.apipaySandboxMode,
      configured: this.apipay.configured,
      recent,
    };
  }

  async createTopup(deviceId: string, amountKzt: number): Promise<
    | { ok: true; topup: PublicTopup }
    | { ok: false; status: number; message: string }
  > {
    const amountError = validateTopupAmount(amountKzt);
    if (amountError) return { ok: false, status: 400, message: amountError };
    if (!deviceId || deviceId.length < 8 || deviceId.length > 128) {
      return { ok: false, status: 400, message: "Некорректный deviceId." };
    }
    if (!this.apipay.configured) {
      return {
        ok: false,
        status: 503,
        message: "ApiPay не настроен: задайте APIPAY_API_KEY на сервере.",
      };
    }

    const nowIso = this.iso();
    this.store.ensureWallet(deviceId, nowIso);
    const topupId = randomUUID();
    const externalOrderId = `topup_${topupId}`;
    const created: TopupRow = {
      id: topupId,
      device_id: deviceId,
      amount_kzt: amountKzt,
      status: "CREATED",
      apipay_invoice_id: null,
      external_order_id: externalOrderId,
      qr_image_url: null,
      qr_token_url: null,
      expires_at: null,
      error_message: null,
      created_at: nowIso,
      updated_at: nowIso,
      paid_at: null,
    };
    this.store.insertTopup(created);

    const result = await this.apipay.createQrInvoice({
      amountKzt,
      description: "Пополнение кошелька",
      externalOrderId,
    });

    if (!result.ok) {
      this.store.updateTopup(topupId, {
        status: "ERROR",
        error_message: result.message,
        updated_at: this.iso(),
      });
      return { ok: false, status: 502, message: result.message };
    }

    const mapped = mapApipayStatus(result.invoice.status);
    this.store.updateTopup(topupId, {
      status: mapped === "PAID" ? "PENDING" : mapped === "CREATED" ? "PENDING" : mapped,
      apipay_invoice_id: result.invoice.id,
      qr_image_url: result.invoice.qr_image_url ?? null,
      qr_token_url: result.invoice.qr_token_url ?? null,
      expires_at: result.invoice.qr_expires_at ?? null,
      updated_at: this.iso(),
    });

    if (mapped === "PAID") {
      this.store.creditTopupIfNeeded(topupId, this.iso(), "Пополнение через Kaspi Pay (ApiPay)");
    }

    return { ok: true, topup: this.toPublic(this.store.getTopup(topupId)!) };
  }

  async refreshTopup(topupId: string): Promise<
    | { ok: true; topup: PublicTopup }
    | { ok: false; status: number; message: string }
  > {
    const topup = this.store.getTopup(topupId);
    if (!topup) return { ok: false, status: 404, message: "Пополнение не найдено." };

    if (topup.status === "PAID") {
      return { ok: true, topup: this.toPublic(topup) };
    }

    if (!topup.apipay_invoice_id || !this.apipay.configured) {
      return { ok: true, topup: this.toPublic(topup) };
    }

    const result = await this.apipay.getInvoice(topup.apipay_invoice_id);
    if (!result.ok) {
      return { ok: false, status: 502, message: result.message };
    }

    const mapped = mapApipayStatus(result.invoice.status);
    // Keep non-terminal sync here; PAID is applied only via creditTopupIfNeeded
    // so a retry cannot mark PAID without a ledger credit.
    this.store.updateTopup(topupId, {
      status: mapped === "PAID" || mapped === "CREATED" ? "PENDING" : mapped,
      qr_image_url: result.invoice.qr_image_url ?? topup.qr_image_url,
      qr_token_url: result.invoice.qr_token_url ?? topup.qr_token_url,
      expires_at: result.invoice.qr_expires_at ?? topup.expires_at,
      error_message: result.invoice.error_message ?? null,
      updated_at: this.iso(),
    });

    if (mapped === "PAID") {
      this.store.creditTopupIfNeeded(topupId, this.iso(), "Пополнение через Kaspi Pay (ApiPay)");
    }

    return { ok: true, topup: this.toPublic(this.store.getTopup(topupId)!) };
  }

  applyPaidInvoice(invoiceId: number, eventKey: string): { credited: boolean; found: boolean } {
    const topup = this.store.getTopupByInvoiceId(invoiceId);
    if (!topup) return { credited: false, found: false };
    const first = this.store.markProcessedEvent(eventKey, this.iso());
    if (!first && topup.status === "PAID") {
      return { credited: false, found: true };
    }
    const credited = this.store.creditTopupIfNeeded(
      topup.id,
      this.iso(),
      "Пополнение через Kaspi Pay (ApiPay)",
    );
    return { credited, found: true };
  }

  private toPublic(row: TopupRow): PublicTopup {
    const wallet = this.store.ensureWallet(row.device_id, this.iso());
    return {
      topupId: row.id,
      apipayInvoiceId: row.apipay_invoice_id,
      amountKzt: row.amount_kzt,
      status: row.status,
      qrImageUrl: row.qr_image_url,
      qrTokenUrl: row.qr_token_url,
      expiresAt: row.expires_at,
      errorMessage: row.error_message,
      walletBalanceKzt: wallet.balance_kzt,
      sandbox: this.config.apipaySandboxMode,
    };
  }
}
