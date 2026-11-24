const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validReminderPayload(uid, overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    userId: uid,
    title: "Morning hydration & vitamins",
    notes: "Take with breakfast",
    cadence: "DAILY",
    category: "HEALTH",
    priority: "HIGH",
    hour: 8,
    minute: 30,
    dayOfMonth: 1,
    monthOfYear: 1,
    year: 2026,
    recurring: true,
    paused: false,
    completedOnce: false,
    lastCompletedCycleKey: "",
    completedCount: 0,
    snoozedUntilEpochMs: 0,
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
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

test("Unauthenticated user: cannot read or write reminders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("reminders").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reminders")
      .doc("rem_1")
      .set(validReminderPayload(ALICE_UID))
  );
});

test("Authenticated owner: valid Kotlin create, partial updates, list, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const colRef = aliceDb.collection("users").doc(ALICE_UID).collection("reminders");

  // Empty collection list succeeds
  await assertSucceeds(colRef.get());

  // Create daily, monthly, yearly reminders
  await assertSucceeds(colRef.doc("rem_daily").set(validReminderPayload(ALICE_UID)));
  await assertSucceeds(
    colRef.doc("rem_monthly").set(
      validReminderPayload(ALICE_UID, {
        title: "Pay apartment rent",
        cadence: "MONTHLY",
        category: "FINANCE",
        dayOfMonth: 1,
      })
    )
  );
  await assertSucceeds(
    colRef.doc("rem_yearly").set(
      validReminderPayload(ALICE_UID, {
        title: "Renew passport",
        cadence: "YEARLY",
        category: "PERSONAL",
        dayOfMonth: 15,
        monthOfYear: 8,
        year: 2027,
      })
    )
  );

  // Populated collection list succeeds
  await assertSucceeds(colRef.get());

  // Partial update: complete / snooze action
  await assertSucceeds(
    colRef.doc("rem_daily").update({
      completedOnce: false,
      lastCompletedCycleKey: "2026-11-24",
      completedCount: 1,
      snoozedUntilEpochMs: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Partial update: pause action
  await assertSucceeds(
    colRef.doc("rem_daily").update({
      paused: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Partial update: edit action
  await assertSucceeds(
    colRef.doc("rem_daily").update({
      title: "Morning hydration & D3",
      notes: "2000 IU",
      cadence: "DAILY",
      category: "HEALTH",
      priority: "NORMAL",
      hour: 9,
      minute: 0,
      dayOfMonth: 1,
      monthOfYear: 1,
      year: 2026,
      recurring: true,
      paused: false,
      completedOnce: false,
      lastCompletedCycleKey: "2026-11-24",
      snoozedUntilEpochMs: 0,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Delete succeeds
  await assertSucceeds(colRef.doc("rem_yearly").delete());
});

test("Security & validation bounds: rejects @DocumentId id key, ghost fields, immutable edits, future timestamps, and out-of-range fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const colRef = aliceDb.collection("users").doc(ALICE_UID).collection("reminders");

  await assertSucceeds(colRef.doc("rem_1").set(validReminderPayload(ALICE_UID)));

  // Reject extra "id" key (@DocumentId must not be in payload)
  await assertFails(
    colRef.doc("rem_bad_id").set(validReminderPayload(ALICE_UID, { id: "rem_bad_id" }))
  );

  // Reject shadow/ghost field
  await assertFails(
    colRef
      .doc("rem_ghost")
      .set(validReminderPayload(ALICE_UID, { isVerified: true }))
  );

  // Reject future timestamp
  await assertFails(
    colRef.doc("rem_future").set(
      validReminderPayload(ALICE_UID, {
        createdAt: new Date(Date.now() + 60000),
      })
    )
  );

  // Reject invalid bounds
  await assertFails(
    colRef.doc("rem_empty_title").set(validReminderPayload(ALICE_UID, { title: "" }))
  );
  await assertFails(
    colRef.doc("rem_bad_hour").set(validReminderPayload(ALICE_UID, { hour: 25 }))
  );
  await assertFails(
    colRef.doc("rem_bad_cadence").set(validReminderPayload(ALICE_UID, { cadence: "WEEKLY" }))
  );

  // Reject mutating immutable createdAt or userId on update
  await assertFails(
    colRef.doc("rem_1").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    colRef.doc("rem_1").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Cross-user isolation: Alice cannot read, list, create, update, or delete Bob's reminders", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("reminders")
      .doc("bob_rem")
      .set(validReminderPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobCol = aliceDb.collection("users").doc(BOB_UID).collection("reminders");

  await assertFails(bobCol.get());
  await assertFails(bobCol.doc("bob_rem").get());
  await assertFails(bobCol.doc("alice_inject").set(validReminderPayload(BOB_UID)));
  await assertFails(
    bobCol.doc("bob_rem").update({
      paused: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobCol.doc("bob_rem").delete());
});
