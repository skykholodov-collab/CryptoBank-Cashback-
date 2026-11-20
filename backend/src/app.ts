import cors from "cors";
import express, { Express, NextFunction, Request, Response } from "express";
import { ApipayClient } from "./apipay.js";
import { AppConfig, TOPUP_PRESETS_KZT } from "./config.js";
import { WalletStore } from "./db.js";
import { parseStatusChanged, verifyWebhookSignature } from "./webhook.js";
import { WalletService } from "./walletService.js";

export type AppDeps = {
  config: AppConfig;
  store: WalletStore;
  apipay: ApipayClient;
  service: WalletService;
};

export function createApp(deps: AppDeps): Express {
  const app = express();
  app.use(cors({ origin: deps.config.corsOrigin === "*" ? true : deps.config.corsOrigin }));

  app.get("/health", (_req, res) => {
    res.json({
      ok: true,
      configured: deps.apipay.configured,
      sandbox: deps.config.apipaySandboxMode,
      presetsKzt: TOPUP_PRESETS_KZT,
    });
  });

  app.get("/v1/wallet", (req, res) => {
    const deviceId = String(req.query.deviceId ?? "");
    if (!deviceId) {
      res.status(400).json({ message: "Нужен deviceId." });
      return;
    }
    res.json(deps.service.getWallet(deviceId));
  });

  app.post("/v1/wallet/topups", express.json(), async (req, res, next) => {
    try {
      const deviceId = String(req.body?.deviceId ?? "");
      const amountKzt = req.body?.amountKzt;
      const result = await deps.service.createTopup(deviceId, amountKzt);
      if (!result.ok) {
        res.status(result.status).json({ message: result.message });
        return;
      }
      res.status(201).json(result.topup);
    } catch (error) {
      next(error);
    }
  });

  app.get("/v1/wallet/topups/:id", async (req, res, next) => {
    try {
      const result = await deps.service.refreshTopup(req.params.id);
      if (!result.ok) {
        res.status(result.status).json({ message: result.message });
        return;
      }
      res.json(result.topup);
    } catch (error) {
      next(error);
    }
  });

  app.post(
    "/webhooks/apipay",
    express.raw({ type: "application/json" }),
    (req: Request, res: Response) => {
      const raw = Buffer.isBuffer(req.body) ? req.body : Buffer.from(String(req.body ?? ""));
      const secret = deps.config.apipayWebhookSecret;
      if (secret) {
        const signature = req.header("X-Webhook-Signature") ?? undefined;
        if (!verifyWebhookSignature(raw, signature, secret)) {
          res.status(401).json({ message: "Invalid signature" });
          return;
        }
      }

      let parsed: unknown;
      try {
        parsed = JSON.parse(raw.toString("utf8") || "{}");
      } catch {
        res.status(400).json({ message: "Invalid JSON" });
        return;
      }

      const payload = parseStatusChanged(parsed);
      if (!payload) {
        res.status(200).json({ ok: true, ignored: true });
        return;
      }

      if (payload.event === "webhook.test") {
        res.status(200).json({ ok: true, ignored: "webhook.test" });
        return;
      }

      if (!deps.config.apipaySandboxMode && payload.is_sandbox === true) {
        res.status(200).json({ ok: true, ignored: "sandbox_event_in_production" });
        return;
      }

      if (payload.event !== "invoice.status_changed" || !payload.invoice) {
        res.status(200).json({ ok: true, ignored: true });
        return;
      }

      const invoice = payload.invoice;
      const eventKey = `${invoice.id}:${invoice.status}`;
      if (String(invoice.status).toLowerCase() === "paid") {
        deps.service.applyPaidInvoice(invoice.id, eventKey);
      } else {
        deps.store.markProcessedEvent(eventKey, new Date().toISOString());
        const topup = deps.store.getTopupByInvoiceId(invoice.id);
        if (topup && topup.status !== "PAID") {
          const mapped = String(invoice.status).toUpperCase();
          const status =
            mapped === "EXPIRED" || mapped === "CANCELLED" || mapped === "ERROR"
              ? (mapped as "EXPIRED" | "CANCELLED" | "ERROR")
              : "PENDING";
          deps.store.updateTopup(topup.id, {
            status,
            updated_at: new Date().toISOString(),
          });
        }
      }
      res.status(200).json({ ok: true });
    },
  );

  app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
    console.error(err);
    res.status(500).json({ message: "Internal error" });
  });

  return app;
}
