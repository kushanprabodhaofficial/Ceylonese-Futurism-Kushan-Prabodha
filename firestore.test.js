const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write leaderboard", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("leaderboard").get());
  await assertFails(
    unauthDb.collection("leaderboard").doc("anon_user").set({
      userId: "anon_user",
      displayName: "Anon",
      totalScore: 100,
      storiesCompleted: 1,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Authenticated user can create and update own leaderboard entry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("leaderboard").doc(ALICE_UID);

  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      displayName: "Kushan Reader",
      totalScore: 80,
      storiesCompleted: 1,
      lastStoryTitle: "කාල යාත්‍රිකයා",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    docRef.update({
      displayName: "Ceylon Explorer",
      totalScore: 160,
      storiesCompleted: 2,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Authenticated user can list leaderboard but cannot spoof or overwrite another user's entry", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("leaderboard").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      totalScore: 200,
      storiesCompleted: 3,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("leaderboard").get());

  // Cross-user overwrite rejected
  await assertFails(
    aliceDb.collection("leaderboard").doc(BOB_UID).set({
      userId: ALICE_UID,
      displayName: "Hacked",
      totalScore: 999,
      storiesCompleted: 9,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Ghost field injection rejected
  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      totalScore: 100,
      storiesCompleted: 1,
      isAdmin: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Per-story scores are strictly isolated per user and validate bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const aliceStoryRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("story_scores")
    .doc("story_101");

  await assertSucceeds(
    aliceStoryRef.set({
      userId: ALICE_UID,
      storyId: "story_101",
      postId: "tag:blogger.com,1999:blog-1.post-101",
      postTitle: "සිංහල විද්‍යා ප්‍රබන්ධය",
      score: 80,
      maxScore: 100,
      correctAnswers: 4,
      totalQuestions: 5,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Alice can read own score list
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("story_scores").get()
  );

  // Bob CANNOT read or write Alice's story_scores
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("story_scores").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("story_scores")
      .doc("story_101")
      .get()
  );

  // Invalid score > maxScore rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("story_scores")
      .doc("story_102")
      .set({
        userId: ALICE_UID,
        storyId: "story_102",
        postId: "102",
        postTitle: "Test",
        score: 500,
        maxScore: 100,
        correctAnswers: 5,
        totalQuestions: 5,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      })
  );
});
