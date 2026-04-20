const fs = require("fs");
const path = require("path");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { doc, getDoc, setDoc, updateDoc } = require("firebase/firestore");

async function run() {
  const rulesPath = path.resolve(__dirname, "../../firestore.rules");
  const rules = fs.readFileSync(rulesPath, "utf8");

  const testEnv = await initializeTestEnvironment({
    projectId: "sperrmuellfinder-rules-test",
    firestore: { rules },
  });

  try {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const adminDb = context.firestore();
      await setDoc(doc(adminDb, "users_public", "userA"), {
        username: "User A",
        level: 1,
        badges: [],
        created_at: new Date(),
      });
      await setDoc(doc(adminDb, "users_public", "userB"), {
        username: "User B",
        level: 1,
        badges: [],
        created_at: new Date(),
      });
      await setDoc(doc(adminDb, "users_private", "userA"), {
        email: "a@example.com",
        xp: 100,
      });
      await setDoc(doc(adminDb, "admin_roles", "adminUser"), {
        role: "admin",
      });
      await setDoc(doc(adminDb, "admin_roles", "superAdmin"), {
        role: "super_admin",
      });
      await setDoc(doc(adminDb, "posts", "post1"), {
        ownerid: "userA",
        images: ["https://example.com/a.jpg"],
        description: "post",
        location: { latitude: 50.0, longitude: 8.0 },
        category_en: ["furniture"],
        category_de: ["Möbel"],
        likes_count: 0,
        comments_count: 0,
        status: "active",
        created_at: new Date(),
        updated_at: new Date(),
      });
    });

    const unauthDb = testEnv.unauthenticatedContext().firestore();
    const userADb = testEnv.authenticatedContext("userA").firestore();
    const userBDb = testEnv.authenticatedContext("userB").firestore();
    const adminDb = testEnv.authenticatedContext("adminUser", { admin: true }).firestore();
    const superAdminDb = testEnv.authenticatedContext("superAdmin", { super_admin: true }).firestore();

    // 1) Owner spoofing blocked
    await assertFails(
      setDoc(doc(userBDb, "posts", "spoofed"), {
        ownerid: "userA",
        images: ["https://example.com/x.jpg"],
        description: "spoof",
        location: { latitude: 52.0, longitude: 13.0 },
        category_en: ["furniture"],
        category_de: ["Möbel"],
        likes_count: 0,
        comments_count: 0,
        status: "active",
        created_at: new Date(),
        updated_at: new Date(),
      }),
    );

    // 2) Scraping/privacy boundaries
    await assertFails(getDoc(doc(unauthDb, "users_public", "userA")));
    await assertFails(getDoc(doc(unauthDb, "users_private", "userA")));
    await assertSucceeds(getDoc(doc(userBDb, "users_public", "userA")));
    await assertFails(getDoc(doc(userBDb, "users_private", "userA")));

    // 3) Owner update allowed (non-counter mutable fields only)
    await assertSucceeds(
      updateDoc(doc(userADb, "posts", "post1"), {
        description: "updated post content",
      }),
    );

    // 4) Counter tampering is blocked for clients
    await assertFails(
      updateDoc(doc(userADb, "posts", "post1"), {
        likes_count: 999,
      }),
    );
    await assertFails(
      updateDoc(doc(userADb, "posts", "post1"), {
        comments_count: 999,
      }),
    );

    // 5) Non-owner post update denied (and must not crash when admin claim is undefined)
    await assertFails(
      updateDoc(doc(userBDb, "posts", "post1"), {
        description: "hijack attempt",
      }),
    );

    // 6) Admin and super_admin can read admin-only path
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const bypassDb = context.firestore();
      await setDoc(doc(bypassDb, "admin_logs", "log1"), {
        action: "seed",
        timestamp: new Date(),
      });
    });
    await assertSucceeds(getDoc(doc(adminDb, "admin_logs", "log1")));
    await assertSucceeds(getDoc(doc(superAdminDb, "admin_logs", "log1")));

    console.log("All firestore rules tests passed.");
  } finally {
    await testEnv.cleanup();
  }
}

run().catch((error) => {
  console.error("Firestore rules tests failed:", error);
  process.exit(1);
});
