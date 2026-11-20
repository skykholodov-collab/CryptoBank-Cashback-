export type AppConfig = {
  port: number;
  corsOrigin: string;
  dataDir: string;
  apipayApiKey: string;
  apipayWebhookSecret: string;
  apipaySandboxMode: boolean;
  apipayBaseUrl: string;
};

export const MIN_TOPUP_KZT = 1_000;
export const MAX_TOPUP_KZT = 100_000;
export const TOPUP_PRESETS_KZT = [1_000, 5_000, 10_000] as const;

export function loadConfig(env: NodeJS.ProcessEnv = process.env): AppConfig {
  const sandboxRaw = (env.APIPAY_SANDBOX_MODE ?? "true").toLowerCase();
  return {
    port: Number(env.PORT ?? "8787"),
    corsOrigin: env.CORS_ORIGIN ?? "*",
    dataDir: env.DATA_DIR ?? "./data",
    apipayApiKey: env.APIPAY_API_KEY ?? "",
    apipayWebhookSecret: env.APIPAY_WEBHOOK_SECRET ?? "",
    apipaySandboxMode: sandboxRaw === "1" || sandboxRaw === "true" || sandboxRaw === "yes",
    apipayBaseUrl: (env.APIPAY_BASE_URL ?? "https://api.apipay.kz/api/v1").replace(/\/$/, ""),
  };
}

export function validateTopupAmount(amountKzt: unknown): string | null {
  if (typeof amountKzt !== "number" || !Number.isInteger(amountKzt)) {
    return "Сумма должна быть целым числом тенге.";
  }
  if (amountKzt < MIN_TOPUP_KZT || amountKzt > MAX_TOPUP_KZT) {
    return `Сумма пополнения должна быть от ${MIN_TOPUP_KZT} до ${MAX_TOPUP_KZT} ₸.`;
  }
  return null;
}
