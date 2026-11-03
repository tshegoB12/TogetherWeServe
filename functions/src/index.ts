import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import express, { Request, Response, NextFunction } from "express";
import cors from "cors";

/**
 * TogetherWeServe REST API
 * ------------------------
 * This is the custom REST layer described in the Planning & Design
 * document: the Android client (Retrofit) never talks to Firestore,
 * Firebase Auth, or FCM directly. Every request comes through here,
 * where the Firebase ID token is verified and business rules (slot
 * availability, role checks, group membership) are applied before
 * anything is read from or written to Cloud Firestore.
 */

admin.initializeApp();
const db = admin.firestore();
const app = express();

app.use(cors({ origin: true }));
app.use(express.json());

// ---------------------------------------------------------------------
// Auth middleware - verifies the Firebase ID token sent in the
// Authorization header ("Bearer <token>") on every request except the
// public event-browsing endpoints.
// ---------------------------------------------------------------------
interface AuthedRequest extends Request {
  uid?: string;
}

async function requireAuth(req: AuthedRequest, res: Response, next: NextFunction) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.substring(7) : null;
  if (!token) {
    return res.status(401).json({ error: "Missing Authorization header" });
  }
  try {
    const decoded = await admin.auth().verifyIdToken(token);
    req.uid = decoded.uid;
    next();
  } catch (err) {
    return res.status(401).json({ error: "Invalid or expired token" });
  }
}

function randomInviteCode(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no ambiguous chars
  let code = "TWS-";
  for (let i = 0; i < 4; i++) code += chars[Math.floor(Math.random() * chars.length)];
  return code;
}

// ---------------------------------------------------------------------
// GET /events - list/search events (public - no auth required so a
// potential volunteer can browse before creating an account).
// ---------------------------------------------------------------------
app.get("/events", async (req: Request, res: Response) => {
  try {
    let query: FirebaseFirestore.Query = db.collection("events");
    if (req.query.cause) {
      query = query.where("cause", "==", String(req.query.cause));
    }
    const snapshot = await query.orderBy("dateTime", "asc").limit(100).get();

    let events = snapshot.docs.map((doc) => ({ eventId: doc.id, ...doc.data() }));

    // Simple in-memory text search on title/cause (Firestore has no
    // native full-text search); fine for the scale of a student prototype.
    if (req.query.q) {
      const q = String(req.query.q).toLowerCase();
      events = events.filter((e: any) =>
        e.title?.toLowerCase().includes(q) || e.cause?.toLowerCase().includes(q)
      );
    }

    res.json(events);
  } catch (err) {
    res.status(500).json({ error: "Failed to list events" });
  }
});

// ---------------------------------------------------------------------
// GET /events/:id - full event details plus a live registered count.
// ---------------------------------------------------------------------
app.get("/events/:id", async (req: Request, res: Response) => {
  try {
    const doc = await db.collection("events").doc(req.params.id).get();
    if (!doc.exists) return res.status(404).json({ error: "Event not found" });

    const registrations = await db.collection("registrations")
      .where("eventId", "==", req.params.id)
      .where("status", "==", "confirmed")
      .get();

    res.json({ eventId: doc.id, ...doc.data(), volunteersGoing: registrations.size });
  } catch (err) {
    res.status(500).json({ error: "Failed to fetch event" });
  }
});

// ---------------------------------------------------------------------
// POST /events - organiser creates an event. Requires the caller's
// Firestore user profile to have role == "organiser".
// ---------------------------------------------------------------------
app.post("/events", requireAuth, async (req: AuthedRequest, res: Response) => {
  try {
    const userDoc = await db.collection("users").doc(req.uid!).get();
    if (userDoc.data()?.role !== "organiser") {
      return res.status(403).json({ error: "Only organisers can create events" });
    }

    const { title, cause, description, dateTime, location, slotsAvailable } = req.body;
    if (!title || !cause || !dateTime || !location || !slotsAvailable) {
      return res.status(400).json({ error: "Missing required fields" });
    }

    const ref = await db.collection("events").add({
      title, cause, description: description || "", dateTime, location,
      slotsAvailable, organiserId: req.uid, organiserName: userDoc.data()?.displayName || "Organiser",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    res.status(201).json({ eventId: ref.id });
  } catch (err) {
    res.status(500).json({ error: "Failed to create event" });
  }
});

// ---------------------------------------------------------------------
// POST /events/:id/register - individual or group registration.
// Checks remaining slots before confirming (business rule enforced
// server-side, not trusted from the client).
// ---------------------------------------------------------------------
app.post("/events/:id/register", requireAuth, async (req: AuthedRequest, res: Response) => {
  try {
    const eventRef = db.collection("events").doc(req.params.id);
    const eventDoc = await eventRef.get();
    if (!eventDoc.exists) return res.status(404).json({ error: "Event not found" });

    const { mode, groupCode } = req.body; // mode: "individual" | "group"
    let groupId: string | null = null;

    if (mode === "group") {
      if (!groupCode) return res.status(400).json({ error: "groupCode is required for group mode" });
      const groupSnap = await db.collection("groups")
        .where("eventId", "==", req.params.id)
        .where("inviteCode", "==", groupCode)
        .limit(1).get();
      if (groupSnap.empty) return res.status(404).json({ error: "Invalid invite code" });
      const groupDoc = groupSnap.docs[0];
      groupId = groupDoc.id;
      await groupDoc.ref.update({
        memberIds: admin.firestore.FieldValue.arrayUnion(req.uid),
      });
    }

    const confirmedCount = (await db.collection("registrations")
      .where("eventId", "==", req.params.id)
      .where("status", "==", "confirmed")
      .get()).size;

    if (confirmedCount >= (eventDoc.data()?.slotsAvailable || 0)) {
      return res.status(409).json({ error: "No slots available for this event" });
    }

    const regRef = await db.collection("registrations").add({
      eventId: req.params.id,
      userId: req.uid,
      groupId,
      status: "confirmed",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    res.status(201).json({ registrationId: regRef.id });
  } catch (err) {
    res.status(500).json({ error: "Failed to register for event" });
  }
});

// ---------------------------------------------------------------------
// POST /events/:id/group - create a named group with a shareable invite
// code for this event (TogetherWeServe's key differentiator).
// ---------------------------------------------------------------------
app.post("/events/:id/group", requireAuth, async (req: AuthedRequest, res: Response) => {
  try {
    const { groupName } = req.body;
    if (!groupName) return res.status(400).json({ error: "groupName is required" });

    const inviteCode = randomInviteCode();
    const groupRef = await db.collection("groups").add({
      groupName,
      inviteCode,
      eventId: req.params.id,
      memberIds: [req.uid],
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    res.status(201).json({ groupId: groupRef.id, groupCode: inviteCode });
  } catch (err) {
    res.status(500).json({ error: "Failed to create group" });
  }
});

// ---------------------------------------------------------------------
// GET /users/:id/schedule - the caller's own upcoming registrations.
// A user may only read their own schedule.
// ---------------------------------------------------------------------
app.get("/users/:id/schedule", requireAuth, async (req: AuthedRequest, res: Response) => {
  try {
    if (req.uid !== req.params.id) return res.status(403).json({ error: "Forbidden" });

    const regSnap = await db.collection("registrations")
      .where("userId", "==", req.params.id)
      .get();

    const results = await Promise.all(regSnap.docs.map(async (doc) => {
      const data = doc.data();
      const eventDoc = await db.collection("events").doc(data.eventId).get();
      return {
        registrationId: doc.id,
        eventId: data.eventId,
        eventTitle: eventDoc.data()?.title || "Unknown event",
        eventDateTime: eventDoc.data()?.dateTime || 0,
        groupId: data.groupId || null,
        status: data.status,
      };
    }));

    res.json(results);
  } catch (err) {
    res.status(500).json({ error: "Failed to fetch schedule" });
  }
});

// ---------------------------------------------------------------------
// PATCH /users/:id/settings - update language / notification preferences.
// ---------------------------------------------------------------------
app.patch("/users/:id/settings", requireAuth, async (req: AuthedRequest, res: Response) => {
  try {
    if (req.uid !== req.params.id) return res.status(403).json({ error: "Forbidden" });

    const { language, eventRemindersEnabled, chatMessagesEnabled } = req.body;
    const update: Record<string, unknown> = {};
    if (language !== undefined) update.language = language;
    if (eventRemindersEnabled !== undefined) update.eventRemindersEnabled = eventRemindersEnabled;
    if (chatMessagesEnabled !== undefined) update.chatMessagesEnabled = chatMessagesEnabled;

    await db.collection("users").doc(req.params.id).set(update, { merge: true });
    res.status(200).json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: "Failed to update settings" });
  }
});

// ---------------------------------------------------------------------
// Firestore trigger - when a new event is created, push a notification
// to volunteers who follow that cause (FR9, real-time notifications).
// ---------------------------------------------------------------------
export const onEventCreated = functions.firestore
  .document("events/{eventId}")
  .onCreate(async (snap) => {
    const event = snap.data();
    // Topic delivery makes the notification feature reliable for every
    // subscribed volunteer installation without exposing device tokens.
    // DATA-ONLY message: the Android service always receives it in
    // onMessageReceived (foreground AND background), so it can both show the
    // system notification and store it in the in-app Notifications list.
    return admin.messaging().send({
      topic: "new_events",
      android: { priority: "high" },
      data: {
        title: "New volunteering opportunity",
        body: `${event.title} - ${event.cause}`,
        eventId: snap.id,
        cause: String(event.cause || ""),
      },
    });
  });

export const api = functions.https.onRequest(app);
