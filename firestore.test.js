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
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid, name = "Alice") {
  return {
    userId: uid,
    displayName: name,
    bio: "Party host",
    avatarIndex: 0,
    accentIndex: 1,
    favoriteGame: "DRAW_GUESS",
    coins: 100,
    xp: 50,
    gamesPlayed: 2,
    gamesWon: 1,
    defaultRoundSeconds: 45,
    defaultMaxPlayers: 6,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validRoom(hostId, isPublic = true) {
  return {
    hostId,
    hostName: "Alice",
    title: "Friday Arcade Night",
    gameType: "DRAW_GUESS",
    status: "WAITING",
    isPublic,
    roundSeconds: 45,
    maxPlayers: 4,
    currentRound: 0,
    activePrompt: "",
    memberUids: [hostId],
    memberNames: [`Alice#${hostId.slice(0, 4)}`],
    createdAt: new Date(),
    updatedAt: new Date(),
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

// --- Unauthenticated Security Tests ---
test("Unauthenticated user cannot read or write users or rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("rooms").where("isPublic", "==", true).get());
  await assertFails(unauthDb.collection("rooms").doc("ROOM01").set(validRoom(ALICE_UID)));
});

// --- User Profile Isolation & Validation ---
test("Owner can create, read, and update own profile; non-owner is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Cross-user read & write must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ coins: 9999, updatedAt: new Date() })
  );

  // Shadow field injection must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(),
    })
  );

  // Valid partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "AlicePro",
      accentIndex: 3,
      updatedAt: new Date(),
    })
  );
});

// --- Custom Packs & Matches ---
test("Owner can manage customPacks and immutable matches; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customPacks").doc("pack_1").set({
      title: "Anime & Gaming",
      gameType: "WHO_IS_SPY",
      items: ["Pikachu|Raichu", "Mario|Luigi"],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customPacks").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customPacks").get()
  );

  // Match records are append-only (no update)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("match_1").set({
      gameType: "WORD_BOMB",
      mode: "SOLO_BOTS",
      roomTitle: "Solo Blitz",
      score: 420,
      coinsEarned: 45,
      xpEarned: 80,
      won: true,
      playersCount: 4,
      createdAt: new Date(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("match_1").update({
      score: 99999,
    })
  );
});

// --- Multiplayer Rooms: Query Trust, Join, Terminal Locking ---
test("Rooms enforce query alignment, private isolation, join flow, and terminal state lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Create public & private rooms
  await assertSucceeds(aliceDb.collection("rooms").doc("PUB123").set(validRoom(ALICE_UID, true)));
  await assertSucceeds(aliceDb.collection("rooms").doc("PRV456").set(validRoom(ALICE_UID, false)));

  // Unfiltered rooms list fails (rules are not filters)
  await assertFails(bobDb.collection("rooms").get());

  // Public filter succeeds
  await assertSucceeds(bobDb.collection("rooms").where("isPublic", "==", true).get());

  // Non-member cannot read private room
  await assertFails(bobDb.collection("rooms").doc("PRV456").get());

  // Bob joins private room atomically using arrayUnion
  await assertSucceeds(
    bobDb.collection("rooms").doc("PRV456").update({
      memberUids: [ALICE_UID, BOB_UID],
      memberNames: [`Alice#${ALICE_UID.slice(0, 4)}`, `Bob#${BOB_UID.slice(0, 4)}`],
      updatedAt: new Date(),
    })
  );

  // Now Bob can read PRV456
  await assertSucceeds(bobDb.collection("rooms").doc("PRV456").get());

  // Charlie still cannot read PRV456
  await assertFails(charlieDb.collection("rooms").doc("PRV456").get());

  // Non-host Bob cannot change room status
  await assertFails(
    bobDb.collection("rooms").doc("PRV456").update({
      status: "PLAYING",
      updatedAt: new Date(),
    })
  );

  // Host Alice finishes room -> terminal state lock blocks further updates
  await assertSucceeds(
    aliceDb.collection("rooms").doc("PRV456").update({
      status: "FINISHED",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    aliceDb.collection("rooms").doc("PRV456").update({
      status: "WAITING",
      updatedAt: new Date(),
    })
  );
});

// --- Room Messages & Strokes ---
test("Room messages and strokes enforce membership and sender integrity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("rooms").doc("PRV789").set(validRoom(ALICE_UID, false)));

  // Non-member Bob cannot post message or stroke
  await assertFails(
    bobDb.collection("rooms").doc("PRV789").collection("messages").doc("msg1").set({
      senderId: BOB_UID,
      senderName: "Bob",
      text: "Hello!",
      msgType: "CHAT",
      isPublicRoom: false,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );

  // Alice posts message & stroke
  await assertSucceeds(
    aliceDb.collection("rooms").doc("PRV789").collection("messages").doc("msg1").set({
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Welcome!",
      msgType: "CHAT",
      isPublicRoom: false,
      memberUids: [ALICE_UID],
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("rooms").doc("PRV789").collection("strokes").doc("s1").set({
      artistId: ALICE_UID,
      colorHex: "#FF4D6D",
      strokeWidth: 8,
      pointsEncoded: "100,200;150,250",
      roundNumber: 1,
      isPublicRoom: false,
      memberUids: [ALICE_UID],
      createdAt: new Date(),
    })
  );

  // Identity spoofing in message fails
  await assertFails(
    aliceDb.collection("rooms").doc("PRV789").collection("messages").doc("msg2").set({
      senderId: BOB_UID,
      senderName: "Bob",
      text: "Spoof!",
      msgType: "CHAT",
      isPublicRoom: false,
      memberUids: [ALICE_UID],
      createdAt: new Date(),
    })
  );
});
