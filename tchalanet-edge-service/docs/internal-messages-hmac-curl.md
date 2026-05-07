# Internal Messages HMAC Curl

The internal messages API is server-to-edge only. Web and mobile clients must call Spring Boot, not this edge-service route.

Canonical route:

```text
POST /internal/messages/send
```

The messages route must verify HMAC headers against the raw JSON request body.

## Required Headers

```text
Content-Type: application/json
X-Request-Id: <stable request id>
Idempotency-Key: <stable idempotency key>
X-Tch-Timestamp: <ISO-8601 instant>
X-Tch-Signature: sha256=<hex hmac>
```

## Signature Algorithm

```text
payload_to_sign = X-Tch-Timestamp + "." + raw_json_body
signature = "sha256=" + hex(HMAC_SHA256(EDGE_HMAC_SECRET, payload_to_sign))
```

Important: the request body sent by curl must be exactly the same bytes used to compute the signature.

## Curl Example

```bash
EDGE_URL="http://localhost:3000"
EDGE_HMAC_SECRET="dev-secret"

BODY='{"eventId":"local-message-test-001","severity":"INFO","title":"Tchalanet message test","message":"Edge-service can send signed messages.","recipients":[{"channel":"SLACK","channelKey":"batch-draws"}],"context":{"source":"curl"}}'
TIMESTAMP="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
REQUEST_ID="local-message-test-001"
IDEMPOTENCY_KEY="local-message-test-001"
SIGNATURE="sha256=$(printf '%s.%s' "$TIMESTAMP" "$BODY" | openssl dgst -sha256 -hmac "$EDGE_HMAC_SECRET" -binary | xxd -p -c 256)"

curl -i -X POST "$EDGE_URL/internal/messages/send" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: $REQUEST_ID" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -H "X-Tch-Timestamp: $TIMESTAMP" \
  -H "X-Tch-Signature: $SIGNATURE" \
  --data-binary "$BODY"
```
