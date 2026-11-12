const fs = require("fs");
const { createHash, createCipheriv, createDecipheriv, hkdfSync, randomBytes } = require("crypto");

// Cifrado de n8n v2 (n8n-core/dist/encryption/aes-256-gcm.js):
//   version(1) | salt(32) | authTag(16) | ciphertext   -> base64
// La clave se deriva con HKDF-SHA256 (info "n8n-encryption-v1") y la key
// que usa n8n es el dekWrappingKey = sha256(encryptionKey) en hex.
const FORMAT_VERSION = 0x01;
const HKDF_INFO = "n8n-encryption-v1";
const KEY_LENGTH_BYTES = 32;
const SALT_LENGTH_BYTES = 32;
const IV_LENGTH_BYTES = 12;
const AUTH_TAG_LENGTH_BYTES = 16;
const HKDF_OUTPUT_LENGTH_BYTES = KEY_LENGTH_BYTES + IV_LENGTH_BYTES;

const VERSION_OFFSET = 0;
const SALT_OFFSET = VERSION_OFFSET + 1;
const AUTH_TAG_OFFSET = SALT_OFFSET + SALT_LENGTH_BYTES;
const CIPHERTEXT_OFFSET = AUTH_TAG_OFFSET + AUTH_TAG_LENGTH_BYTES;

function dekWrappingKey(encryptionKey) {
  return createHash("sha256").update(encryptionKey).digest("hex");
}

function deriveKeyAndIv(keyBuf, salt) {
  const hkdf = Buffer.from(
    hkdfSync("sha256", keyBuf, salt, Buffer.from(HKDF_INFO, "utf8"), HKDF_OUTPUT_LENGTH_BYTES)
  );
  return [hkdf.subarray(0, KEY_LENGTH_BYTES), hkdf.subarray(KEY_LENGTH_BYTES, HKDF_OUTPUT_LENGTH_BYTES)];
}

function encrypt(data, key) {
  const keyBuf = Buffer.from(key, "hex");
  const salt = randomBytes(SALT_LENGTH_BYTES);
  const [derivedKey, iv] = deriveKeyAndIv(keyBuf, salt);
  const cipher = createCipheriv("aes-256-gcm", derivedKey, iv);
  const encrypted = Buffer.concat([cipher.update(data, "utf8"), cipher.final()]);
  const authTag = cipher.getAuthTag();
  return Buffer.concat([Buffer.from([FORMAT_VERSION]), salt, authTag, encrypted]).toString("base64");
}

function decrypt(data, key) {
  const keyBuf = Buffer.from(key, "hex");
  const buf = Buffer.from(data, "base64");
  if (buf.length < CIPHERTEXT_OFFSET) throw new Error("GCM ciphertext too short");
  if (buf[VERSION_OFFSET] !== FORMAT_VERSION) throw new Error("Unsupported GCM ciphertext version");
  const salt = buf.subarray(SALT_OFFSET, AUTH_TAG_OFFSET);
  const authTag = buf.subarray(AUTH_TAG_OFFSET, CIPHERTEXT_OFFSET);
  const ciphertext = buf.subarray(CIPHERTEXT_OFFSET);
  const [derivedKey, iv] = deriveKeyAndIv(keyBuf, salt);
  const decipher = createDecipheriv("aes-256-gcm", derivedKey, iv, { authTagLength: AUTH_TAG_LENGTH_BYTES });
  decipher.setAuthTag(authTag);
  return Buffer.concat([decipher.update(ciphertext), decipher.final()]).toString("utf8");
}

module.exports = { encrypt, decrypt, dekWrappingKey };

if (require.main === module) {
  const encryptionKey = JSON.parse(fs.readFileSync(process.argv[2], "utf8")).encryptionKey;
  const key = dekWrappingKey(encryptionKey);
  const plain = JSON.stringify({ apiKey: "prueba" });
  const enc = encrypt(plain, key);
  console.log("cifrado: " + enc.slice(0, 48) + "...");
  console.log("ida y vuelta OK:", decrypt(enc, key) === plain);
}
