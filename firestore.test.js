const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-mahadeva-b02d618c-62fc-4bda-aaf2-c02166833632";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 1000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write payroll settings or attendance", async () => {
  const db = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(db.collection("users").doc(ALICE_UID).collection("payroll").doc("settings").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("attendance").get());
  await assertFails(
    db.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-13").set({
      userId: ALICE_UID,
      date: "2026-11-13",
      status: "P",
      otHours: 2,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Owner can create, update, list, and delete their own attendance records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-13");
  const ts = pastTimestamp();

  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      date: "2026-11-13",
      status: "P",
      otHours: 2.5,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").get()
  );

  await assertSucceeds(
    docRef.update({
      status: "H",
      otHours: 1.0,
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(docRef.delete());
});

test("Owner can create and update their own payroll settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("payroll").doc("settings");
  const ts = pastTimestamp();

  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      startDay: 25,
      salary: 25000,
      otRate: 100,
      pf: 1800,
      esi: 200,
      advance: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    docRef.update({
      salary: 28000,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Cross-user access to attendance or payroll settings is denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore(DATABASE_ID);
    const ts = pastTimestamp();
    await adminDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-13").set({
      userId: ALICE_UID,
      date: "2026-11-13",
      status: "P",
      otHours: 2,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-13").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("attendance").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("payroll").doc("settings").get());
});

test("Validation rejects shadow fields, invalid status, negative OT, or invalid date keys", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("attendance");
  const ts = pastTimestamp();

  // Shadow field
  await assertFails(
    col.doc("2026-11-13").set({
      userId: ALICE_UID,
      date: "2026-11-13",
      status: "P",
      otHours: 2,
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid status code
  await assertFails(
    col.doc("2026-11-13").set({
      userId: ALICE_UID,
      date: "2026-11-13",
      status: "X",
      otHours: 2,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Negative OT
  await assertFails(
    col.doc("2026-11-13").set({
      userId: ALICE_UID,
      date: "2026-11-13",
      status: "P",
      otHours: -1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid document ID format
  await assertFails(
    col.doc("bad-id").set({
      userId: ALICE_UID,
      date: "bad-id",
      status: "P",
      otHours: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
