import assert from "node:assert/strict";
import { createHmac } from "node:crypto";
import { describe, it } from "node:test";
import { verifyWebhookSignature } from "../src/webhook.ts";

describe("verifyWebhookSignature", () => {
  it("accepts a valid HMAC over the raw body", () => {
    const body = Buffer.from('{"event":"invoice.status_changed","invoice":{"id":1,"status":"paid"}}');
    const secret = "test-secret";
    const digest = createHmac("sha256", secret).update(body).digest("hex");
    assert.equal(verifyWebhookSignature(body, `sha256=${digest}`, secret), true);
  });

  it("rejects a tampered body", () => {
    const body = Buffer.from('{"event":"invoice.status_changed"}');
    const secret = "test-secret";
    const digest = createHmac("sha256", secret).update(body).digest("hex");
    const other = Buffer.from('{"event":"invoice.status_changed","x":1}');
    assert.equal(verifyWebhookSignature(other, `sha256=${digest}`, secret), false);
  });

  it("rejects missing header or secret", () => {
    const body = Buffer.from("{}");
    assert.equal(verifyWebhookSignature(body, undefined, "secret"), false);
    assert.equal(verifyWebhookSignature(body, "sha256=abc", ""), false);
  });
});
