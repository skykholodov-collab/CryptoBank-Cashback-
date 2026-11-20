import { createHmac, timingSafeEqual } from "node:crypto";

export function verifyWebhookSignature(
  rawBody: Buffer,
  headerValue: string | undefined,
  secret: string,
): boolean {
  if (!secret) return false;
  if (!headerValue) return false;
  const expected = `sha256=${createHmac("sha256", secret).update(rawBody).digest("hex")}`;
  const provided = headerValue.trim();
  const a = Buffer.from(expected);
  const b = Buffer.from(provided);
  if (a.length !== b.length) return false;
  return timingSafeEqual(a, b);
}

export type StatusChangedPayload = {
  event: string;
  is_sandbox?: boolean;
  invoice?: {
    id: number;
    status: string;
    amount?: string | number;
    external_order_id?: string | null;
  };
  timestamp?: string;
};

export function parseStatusChanged(body: unknown): StatusChangedPayload | null {
  if (!body || typeof body !== "object") return null;
  const record = body as Record<string, unknown>;
  if (typeof record.event !== "string") return null;
  return body as StatusChangedPayload;
}
