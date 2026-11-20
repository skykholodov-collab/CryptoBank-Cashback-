#!/usr/bin/env node
/**
 * Optional live sandbox check. Skips when APIPAY_API_KEY is missing.
 * Flow: create QR → simulate paid → refresh top-up → expect wallet credit.
 */
import { spawn } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const key = process.env.APIPAY_API_KEY ?? "";
if (!key) {
  console.log("skip sandbox-e2e: APIPAY_API_KEY not set");
  process.exit(0);
}

const dataDir = fs.mkdtempSync(path.join(os.tmpdir(), "wallet-e2e-"));
const env = {
  ...process.env,
  APIPAY_API_KEY: key,
  APIPAY_SANDBOX_MODE: "true",
  PORT: "8799",
  DATA_DIR: dataDir,
};

const child = spawn("npx", ["tsx", "src/index.ts"], {
  cwd: root,
  env,
  stdio: ["ignore", "pipe", "pipe"],
});

let ready = false;
child.stdout.on("data", (buf) => {
  const text = buf.toString();
  process.stdout.write(text);
  if (text.includes("listening")) ready = true;
});
child.stderr.on("data", (buf) => process.stderr.write(buf));

async function waitReady() {
  for (let i = 0; i < 50; i++) {
    if (ready) return;
    await new Promise((r) => setTimeout(r, 200));
  }
  throw new Error("server did not start");
}

async function main() {
  await waitReady();
  const deviceId = "e2e-device-1";
  const created = await fetch("http://127.0.0.1:8799/v1/wallet/topups", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ deviceId, amountKzt: 1000 }),
  });
  const topup = await created.json();
  if (!created.ok) throw new Error(JSON.stringify(topup));
  const invoiceId = topup.apipayInvoiceId;
  const sim = await fetch(`https://api.apipay.kz/api/v1/invoices/${invoiceId}/simulate-status`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-API-Key": key,
    },
    body: JSON.stringify({ status: "paid" }),
  });
  if (!sim.ok) {
    const body = await sim.text();
    throw new Error(`simulate failed: ${sim.status} ${body}`);
  }
  const refreshed = await fetch(`http://127.0.0.1:8799/v1/wallet/topups/${topup.topupId}`);
  const paid = await refreshed.json();
  if (paid.status !== "PAID" || paid.walletBalanceKzt < 1000) {
    throw new Error(`unexpected top-up state: ${JSON.stringify(paid)}`);
  }
  console.log("sandbox-e2e ok", paid.walletBalanceKzt);
}

main()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(() => {
    child.kill("SIGTERM");
    fs.rmSync(dataDir, { recursive: true, force: true });
  });
