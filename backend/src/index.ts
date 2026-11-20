import fs from "node:fs";
import path from "node:path";
import { ApipayClient } from "./apipay.js";
import { createApp } from "./app.js";
import { loadConfig } from "./config.js";
import { WalletStore } from "./db.js";
import { WalletService } from "./walletService.js";

function loadDotEnv(): void {
  const envPath = path.resolve(process.cwd(), ".env");
  if (!fs.existsSync(envPath)) return;
  const text = fs.readFileSync(envPath, "utf8");
  for (const line of text.split("\n")) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eq = trimmed.indexOf("=");
    if (eq < 0) continue;
    const key = trimmed.slice(0, eq).trim();
    let value = trimmed.slice(eq + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (!(key in process.env)) process.env[key] = value;
  }
}

loadDotEnv();

const config = loadConfig();
const store = new WalletStore(config.dataDir);
const apipay = new ApipayClient(config.apipayBaseUrl, config.apipayApiKey);
const service = new WalletService(store, apipay, config);
const app = createApp({ config, store, apipay, service });

if (apipay.configured) {
  apipay.accountHealth().then((health) => {
    if (!health.ok) {
      console.warn(`[apipay] health check failed: ${health.message}`);
      return;
    }
    const expected = config.apipaySandboxMode ? "sandbox" : "production";
    if (health.mode !== expected) {
      console.warn(
        `[apipay] mode mismatch: config APIPAY_SANDBOX_MODE=${config.apipaySandboxMode} but account health mode=${health.mode}`,
      );
    } else {
      console.log(`[apipay] account mode=${health.mode}`);
    }
  });
} else {
  console.warn("[apipay] APIPAY_API_KEY is empty — top-ups will return 503 until configured.");
}

app.listen(config.port, () => {
  console.log(`wallet backend listening on :${config.port}`);
});
