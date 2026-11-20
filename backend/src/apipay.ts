export type ApipayInvoice = {
  id: number;
  amount?: string | number;
  status: string;
  qr_token_url?: string | null;
  qr_image_url?: string | null;
  qr_expires_at?: string | null;
  external_order_id?: string | null;
  is_sandbox?: boolean;
  error_message?: string | null;
  paid_at?: string | null;
};

export type CreateQrResult =
  | { ok: true; invoice: ApipayInvoice }
  | { ok: false; status: number; body: unknown; message: string };

export type GetInvoiceResult =
  | { ok: true; invoice: ApipayInvoice }
  | { ok: false; status: number; body: unknown; message: string };

export type AccountHealth =
  | { ok: true; mode: string; raw: unknown }
  | { ok: false; status: number; message: string };

export class ApipayClient {
  constructor(
    private readonly baseUrl: string,
    private readonly apiKey: string,
    private readonly fetchImpl: typeof fetch = fetch,
  ) {}

  get configured(): boolean {
    return this.apiKey.trim().length > 0;
  }

  async createQrInvoice(input: {
    amountKzt: number;
    description: string;
    externalOrderId: string;
  }): Promise<CreateQrResult> {
    const response = await this.fetchImpl(`${this.baseUrl}/invoices/qr`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-API-Key": this.apiKey,
      },
      body: JSON.stringify({
        amount: input.amountKzt,
        description: input.description,
        external_order_id: input.externalOrderId,
        external_order_id_idempotency: input.externalOrderId,
      }),
    });
    const body = await safeJson(response);
    if (!response.ok) {
      return {
        ok: false,
        status: response.status,
        body,
        message: extractError(body) ?? `ApiPay QR create failed (${response.status})`,
      };
    }
    return { ok: true, invoice: body as ApipayInvoice };
  }

  async getInvoice(id: number): Promise<GetInvoiceResult> {
    const response = await this.fetchImpl(`${this.baseUrl}/invoices/${id}`, {
      headers: { "X-API-Key": this.apiKey },
    });
    const body = await safeJson(response);
    if (!response.ok) {
      return {
        ok: false,
        status: response.status,
        body,
        message: extractError(body) ?? `ApiPay get invoice failed (${response.status})`,
      };
    }
    return { ok: true, invoice: body as ApipayInvoice };
  }

  async accountHealth(): Promise<AccountHealth> {
    const response = await this.fetchImpl(`${this.baseUrl}/account/health`, {
      headers: { "X-API-Key": this.apiKey },
    });
    const body = await safeJson(response);
    if (!response.ok) {
      return {
        ok: false,
        status: response.status,
        message: extractError(body) ?? `health failed (${response.status})`,
      };
    }
    const mode =
      typeof body === "object" && body && "mode" in body
        ? String((body as { mode: unknown }).mode)
        : "unknown";
    return { ok: true, mode, raw: body };
  }
}

async function safeJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return { raw: text };
  }
}

function extractError(body: unknown): string | null {
  if (!body || typeof body !== "object") return null;
  const record = body as Record<string, unknown>;
  if (typeof record.message === "string") return record.message;
  if (typeof record.error === "string") return record.error;
  return null;
}

export function mapApipayStatus(status: string):
  | "PENDING"
  | "PAID"
  | "EXPIRED"
  | "CANCELLED"
  | "ERROR"
  | "CREATED" {
  switch (status.toLowerCase()) {
    case "paid":
      return "PAID";
    case "expired":
      return "EXPIRED";
    case "cancelled":
    case "canceled":
      return "CANCELLED";
    case "error":
      return "ERROR";
    case "pending":
    case "processing":
      return "PENDING";
    default:
      return "CREATED";
  }
}
