import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { after, before, describe, it } from "node:test";
import { ApipayClient } from "../src/apipay.ts";
import { createApp } from "../src/app.ts";
import { loadConfig, validateTopupAmount } from "../src/config.ts";
import { WalletStore } from "../src/db.ts";
import { WalletService } from "../src/walletService.ts";
import { createHmac } from "node:crypto";

describe("validateTopupAmount", () => {
  it("rejects out of range and non-integers", () => {
    assert.ok(validateTopupAmount(999));
    assert.ok(validateTopupAmount(100_001));
    assert.ok(validateTopupAmount(1500.5));
    assert.equal(validateTopupAmount(5_000), null);
  });
});

describe("WalletService credit idempotency", () => {
  let dir: string;
  let store: WalletStore;

  before(() => {
    dir = fs.mkdtempSync(path.join(os.tmpdir(), "wallet-test-"));
    store = new WalletStore(dir);
  });

  after(() => {
    store.close();
    fs.rmSync(dir, { recursive: true, force: true });
  });

  it("credits once for the same top-up", () => {
    const now = "2026-01-01T00:00:00.000Z";
    store.ensureWallet("device-a", now);
    store.insertTopup({
      id: "t1",
      device_id: "device-a",
      amount_kzt: 5000,
      status: "PENDING",
      apipay_invoice_id: 42,
      external_order_id: "topup_t1",
      qr_image_url: null,
      qr_token_url: null,
      expires_at: null,
      error_message: null,
      created_at: now,
      updated_at: now,
      paid_at: null,
    });
    assert.equal(store.creditTopupIfNeeded("t1", now, "note"), true);
    assert.equal(store.creditTopupIfNeeded("t1", now, "note"), false);
    assert.equal(store.getWallet("device-a")?.balance_kzt, 5000);
    assert.equal(store.listLedger("device-a").length, 1);
  });
});

describe("HTTP top-up with mocked ApiPay", () => {
  let dir: string;
  let store: WalletStore;
  let server: ReturnType<ReturnType<typeof createApp>["listen"]>;
  let baseUrl: string;

  before(async () => {
    dir = fs.mkdtempSync(path.join(os.tmpdir(), "wallet-http-"));
    store = new WalletStore(dir);
    const config = {
      ...loadConfig({
        APIPAY_API_KEY: "test-key",
        APIPAY_SANDBOX_MODE: "true",
        APIPAY_WEBHOOK_SECRET: "hook-secret",
        DATA_DIR: dir,
        PORT: "0",
      }),
      dataDir: dir,
    };

    let invoiceId = 100;
    const fetchImpl: typeof fetch = async (input, init) => {
      const url = String(input);
      if (url.endsWith("/invoices/qr") && init?.method === "POST") {
        invoiceId += 1;
        return new Response(
          JSON.stringify({
            id: invoiceId,
            amount: "5000.00",
            status: "pending",
            qr_token_url: "https://qr.apipay.kz/sandbox/demo",
            qr_image_url: "https://example.com/qr.png",
            qr_expires_at: "2026-01-01T00:03:00+00:00",
            is_sandbox: true,
          }),
          { status: 201, headers: { "Content-Type": "application/json" } },
        );
      }
      const match = url.match(/\/invoices\/(\d+)$/);
      if (match && (!init || !init.method || init.method === "GET")) {
        return new Response(
          JSON.stringify({
            id: Number(match[1]),
            status: "paid",
            amount: "5000.00",
            qr_token_url: "https://qr.apipay.kz/sandbox/demo",
            qr_image_url: "https://example.com/qr.png",
            qr_expires_at: "2026-01-01T00:03:00+00:00",
            paid_at: "2026-01-01T00:01:00+00:00",
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response(JSON.stringify({ error: "unexpected" }), { status: 500 });
    };

    const apipay = new ApipayClient(config.apipayBaseUrl, config.apipayApiKey, fetchImpl);
    const service = new WalletService(store, apipay, config, () => new Date("2026-01-01T00:00:00Z"));
    const app = createApp({ config, store, apipay, service });
    await new Promise<void>((resolve) => {
      server = app.listen(0, "127.0.0.1", () => resolve());
    });
    const address = server.address();
    if (!address || typeof address === "string") throw new Error("no port");
    baseUrl = `http://127.0.0.1:${address.port}`;
  });

  after(async () => {
    await new Promise<void>((resolve, reject) => {
      server.close((err) => (err ? reject(err) : resolve()));
    });
    store.close();
    fs.rmSync(dir, { recursive: true, force: true });
  });

  it("creates a QR top-up and credits on poll when paid", async () => {
    const created = await fetch(`${baseUrl}/v1/wallet/topups`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ deviceId: "device-http-1", amountKzt: 5000 }),
    });
    assert.equal(created.status, 201);
    const body = (await created.json()) as { topupId: string; status: string; qrImageUrl: string };
    assert.equal(body.status, "PENDING");
    assert.ok(body.qrImageUrl);

    const refreshed = await fetch(`${baseUrl}/v1/wallet/topups/${body.topupId}`);
    assert.equal(refreshed.status, 200);
    const paid = (await refreshed.json()) as { status: string; walletBalanceKzt: number };
    assert.equal(paid.status, "PAID");
    assert.equal(paid.walletBalanceKzt, 5000);

    const wallet = await fetch(`${baseUrl}/v1/wallet?deviceId=device-http-1`);
    const walletBody = (await wallet.json()) as { balanceKzt: number };
    assert.equal(walletBody.balanceKzt, 5000);
  });

  it("accepts a signed paid webhook and stays idempotent", async () => {
    const created = await fetch(`${baseUrl}/v1/wallet/topups`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ deviceId: "device-http-2", amountKzt: 1000 }),
    });
    const body = (await created.json()) as { topupId: string; apipayInvoiceId: number };
    const payload = JSON.stringify({
      event: "invoice.status_changed",
      is_sandbox: true,
      invoice: { id: body.apipayInvoiceId, status: "paid", amount: "1000.00" },
      timestamp: "2026-01-01T00:02:00+00:00",
    });
    const signature = `sha256=${createHmac("sha256", "hook-secret").update(payload).digest("hex")}`;
    const first = await fetch(`${baseUrl}/webhooks/apipay`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Webhook-Signature": signature },
      body: payload,
    });
    assert.equal(first.status, 200);
    const second = await fetch(`${baseUrl}/webhooks/apipay`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Webhook-Signature": signature },
      body: payload,
    });
    assert.equal(second.status, 200);
    const wallet = await fetch(`${baseUrl}/v1/wallet?deviceId=device-http-2`);
    const walletBody = (await wallet.json()) as { balanceKzt: number };
    assert.equal(walletBody.balanceKzt, 1000);
  });
});
