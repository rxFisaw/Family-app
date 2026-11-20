const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserProfile(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    fullName: "কর্নেল ইসলাম",
    nickname: "রনি",
    bio: "পরিবারের সবার সাথে থাকতে ভালোবাসি।",
    birthDay: 19,
    birthMonth: 11,
    birthYear: 1995,
    relationLabel: "আমি",
    profession: "সফটওয়্যার ইঞ্জিনিয়ার",
    bloodGroup: "O+",
    phone: "01711000000",
    address: "ঢাকা, বাংলাদেশ",
    hobbies: "বই পড়া, ফটোগ্রাফি",
    favoriteFood: "বিরিয়ানি",
    favoriteColor: "সবুজ",
    favoriteQuote: "পরিবারই সবচেয়ে বড় শক্তি।",
    photoBase64: "",
    notifyEnabled: true,
    notifyDaysBefore: 1,
    notifyHour: 9,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validFamilyMember(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    fullName: "রহিম উদ্দিন",
    nickname: "আব্বু",
    relation: "বাবা",
    category: "PARENTS",
    bio: "আমাদের পরিবারের বটবৃক্ষ।",
    birthDay: 12,
    birthMonth: 5,
    birthYear: 1965,
    profession: "অবসরপ্রাপ্ত শিক্ষক",
    bloodGroup: "B+",
    phone: "01811000000",
    address: "রাজশাহী",
    hobbies: "বাগান করা",
    favoriteThing: "ইলিশ মাছ ও চা",
    specialNote: "প্রতি শুক্রবার একসাথে গল্প করি।",
    photoBase64: "",
    birthdayReminder: true,
    selfProfile: false,
    createdAt: now,
    updatedAt: now,
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

test("Unauthenticated user cannot read or write user profile or family members", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("members").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("mem_1")
      .set(validFamilyMember(ALICE_UID))
  );
});

test("Authenticated user can create, read, and partially update own UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(validUserProfile(ALICE_UID)));
  await assertSucceeds(profileRef.get());

  await assertSucceeds(
    profileRef.update({
      fullName: "কর্নেল ইসলাম আপডেট",
      bio: "নতুন বর্ণনা",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    profileRef.update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject ghost field shadow update
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Authenticated user can create, list, update, and delete own FamilyMember", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const membersRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("members");

  await assertSucceeds(
    membersRef.doc("member_1").set(validFamilyMember(ALICE_UID))
  );

  // List subcollection (mirrors Kotlin observeMembers)
  await assertSucceeds(membersRef.get());

  // Partial update (mirrors Kotlin updateMember)
  await assertSucceeds(
    membersRef.doc("member_1").update({
      bio: "আপডেট করা বর্ণনা",
      birthdayReminder: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject changing immutable selfProfile flag on update
  await assertFails(
    membersRef.doc("member_1").update({
      selfProfile: true,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reject writing @DocumentId `id` field
  await assertFails(
    membersRef.doc("member_2").set(
      validFamilyMember(ALICE_UID, {
        id: "member_2",
      })
    )
  );

  // Reject invalid category enum
  await assertFails(
    membersRef.doc("member_3").set(
      validFamilyMember(ALICE_UID, {
        category: "INVALID_CAT",
      })
    )
  );

  // Reject future timestamp
  await assertFails(
    membersRef.doc("member_4").set(
      validFamilyMember(ALICE_UID, {
        createdAt: new Date(Date.now() + 60000),
      })
    )
  );

  await assertSucceeds(membersRef.doc("member_1").delete());
});

test("Cross-user isolation blocks Bob from Alice's profile and family members", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .collection("users")
      .doc(ALICE_UID)
      .set(validUserProfile(ALICE_UID));
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("mem_1")
      .set(validFamilyMember(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).set(validUserProfile(BOB_UID))
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("members").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("mem_1")
      .update({ fullName: "Hijacked", updatedAt: new Date(Date.now() - 500) })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("mem_1")
      .delete()
  );
});
