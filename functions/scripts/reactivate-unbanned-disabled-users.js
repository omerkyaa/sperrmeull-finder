const {initializeApp, getApps} = require('firebase-admin/app');
const {getFirestore} = require('firebase-admin/firestore');
const {getAuth} = require('firebase-admin/auth');

if (!getApps().length) {
  initializeApp();
}

const db = getFirestore();
const auth = getAuth();

function hasFlag(flag) {
  return process.argv.slice(2).includes(flag);
}

async function collectCandidateUserIds() {
  const ids = new Set();

  const [legacySnap, privateSnap] = await Promise.all([
    db.collection('users').where('isBanned', '==', false).get(),
    db.collection('users_private').where('isBanned', '==', false).get(),
  ]);

  legacySnap.docs.forEach((doc) => ids.add(doc.id));
  privateSnap.docs.forEach((doc) => ids.add(doc.id));

  return Array.from(ids);
}

async function main() {
  const apply = hasFlag('--apply');
  const dryRun = hasFlag('--dry-run') || !apply;

  const candidateIds = await collectCandidateUserIds();
  console.log(`Found ${candidateIds.length} unbanned candidate users.`);

  let reactivated = 0;
  let skipped = 0;
  let errors = 0;

  for (const userId of candidateIds) {
    try {
      const record = await auth.getUser(userId);
      if (!record.disabled) {
        skipped += 1;
        continue;
      }

      if (dryRun) {
        console.log(`[DRY-RUN] Would reactivate disabled user: ${userId}`);
        reactivated += 1;
        continue;
      }

      await auth.updateUser(userId, {disabled: false});
      await Promise.all([
        db.collection('users').doc(userId).set({authDisabled: false}, {merge: true}),
        db.collection('users_private').doc(userId).set({authDisabled: false}, {merge: true}),
      ]);
      console.log(`[APPLY] Reactivated user: ${userId}`);
      reactivated += 1;
    } catch (error) {
      errors += 1;
      console.error(`Failed to process user ${userId}:`, error.message || error);
    }
  }

  console.log('');
  console.log(`Mode: ${dryRun ? 'DRY-RUN' : 'APPLY'}`);
  console.log(`Candidates: ${candidateIds.length}`);
  console.log(`Reactivated: ${reactivated}`);
  console.log(`Skipped (already enabled): ${skipped}`);
  console.log(`Errors: ${errors}`);
}

main().catch((error) => {
  console.error('Recovery script failed:', error);
  process.exit(1);
});
