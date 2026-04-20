/**
 * One-time local script to set custom admin claims for a Firebase Auth user.
 *
 * Usage:
 *   node set-super-admin.js /absolute/path/to/serviceAccountKey.json <uid>
 *
 * Example:
 *   node set-super-admin.js ./serviceAccountKey.json <your-firebase-uid>
 *
 * The service-account key is read locally only and MUST NEVER be committed.
 */
const path = require("path");
const admin = require("firebase-admin");

async function main() {
  const keyPathArg = process.argv[2];
  const uid = process.argv[3];

  if (!keyPathArg || !uid) {
    console.error("Missing required arguments.");
    console.error("Usage: node set-super-admin.js /path/to/serviceAccountKey.json <uid>");
    process.exit(1);
  }

  const keyPath = path.resolve(process.cwd(), keyPathArg);
  const serviceAccount = require(keyPath);

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });

  await admin.auth().setCustomUserClaims(uid, {
    super_admin: true,
    admin: true,
    moderator: true,
  });

  const user = await admin.auth().getUser(uid);
  console.log("✅ Claims updated for UID:", uid);
  console.log("Current custom claims:", user.customClaims || {});
  console.log("⚠️ User must logout/login to receive new token claims.");
}

main().catch((error) => {
  console.error("❌ Failed to set claims:", error.message);
  process.exit(1);
});
