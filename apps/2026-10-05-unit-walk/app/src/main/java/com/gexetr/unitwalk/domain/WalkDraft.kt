package com.gexetr.unitwalk.domain

import java.util.UUID

/**
 * The pure draft mutations of a walk.
 *
 * Every function here is a pure transformation: it takes the current [Walk] and returns the **next
 * durable snapshot** — a new value, never a mutation in place. Nothing in this file imports the
 * Android framework or performs I/O, so it is exercised directly by JVM unit tests.
 *
 * ## The crash-safety contract (load-bearing)
 *
 * A caller advancing a walk **MUST persist the value returned by each mutation with
 * [com.gexetr.unitwalk.data.WalkStore.save] before opening the next room.** The order is the
 * feature, not an implementation detail:
 *
 * 1. `openRoom` → persist, so the new room is durable before the landlord starts filling it;
 * 2. `addPhoto` → persist, so the bytes are already on disk (the photo file is written before the
 *    document row that names it);
 * 3. `setNote` → persist, so a mis-tapped back gesture or a process kill cannot lose the note;
 * 4. `finishRoom` → persist, so the completed room is committed before the next one opens.
 *
 * A process killed between a mutation and its `save` loses only the un-persisted step; a process
 * killed after a `save` sees the walk exactly as it was. That is the whole reason the store writes
 * atomically (temp file + rename) and why the caller must not batch several rooms into one save.
 */

/**
 * Starts a new draft walk for [unitId].
 *
 * [state] is used to mint an id that is unique among the walks already in it and to leave the rest
 * of the portfolio untouched; the returned walk is added to [state] by the caller. The walk starts
 * empty, in [WalkStatus.DRAFT], with [startedAtIso] stamped.
 */
fun startWalk(state: WalkState, unitId: String, kind: WalkKind, nowIso: String): Walk =
    Walk(
        id = uniqueWalkId(state),
        unitId = unitId,
        kind = kind,
        startedAtIso = nowIso,
        status = WalkStatus.DRAFT,
        rooms = emptyList(),
        differences = emptyList(),
        signatures = emptyList(),
    )

/**
 * Opens a new room named [roomName] at the end of the walk. The room starts empty; the caller
 * persists the returned snapshot before the landlord begins filling it.
 */
fun openRoom(walk: Walk, roomName: String): Walk =
    walk.copy(
        rooms = walk.rooms + Room(
            id = newId("room"),
            name = roomName,
            note = "",
            photos = emptyList(),
        ),
    )

/**
 * Attaches [photo] to the room identified by [roomId].
 *
 * The photo's bytes must already be durable — [com.gexetr.unitwalk.data.WalkStore.putPhoto] writes
 * them before this row is committed — so a crash after this snapshot cannot leave a document
 * naming a file that does not exist.
 */
fun addPhoto(walk: Walk, roomId: String, photo: Photo): Walk =
    updateRoom(walk, roomId) { room -> room.copy(photos = room.photos + photo) }

/** Replaces the note of the room identified by [roomId]. */
fun setNote(walk: Walk, roomId: String, note: String): Walk =
    updateRoom(walk, roomId) { room -> room.copy(note = note) }

/**
 * Marks the room identified by [roomId] as complete by returning the snapshot that commits it.
 *
 * The model stores completion as *the persisted snapshot itself* rather than a per-room flag: the
 * caller persists this value before opening the next room, so "finished" and "durable" are the same
 * fact and cannot disagree. The value returned is always a fresh copy, even when nothing changed,
 * so the caller has an unambiguous commit point.
 */
fun finishRoom(walk: Walk, roomId: String): Walk =
    updateRoom(walk, roomId) { room -> room.copy() }

/**
 * Finishes the walk: flips [WalkStatus] to [WalkStatus.FINISHED] and stamps [finishedAtIso].
 *
 * A finished walk is the only thing the comparison will accept as a prior move-in report
 * ([priorMoveInFor]).
 */
fun finishWalk(walk: Walk, nowIso: String): Walk =
    walk.copy(status = WalkStatus.FINISHED, finishedAtIso = nowIso)

/**
 * Records [delta] as the difference for its room. At most one delta is kept per room, so recording
 * a second difference for the same room replaces the first rather than stacking rows.
 */
fun setDelta(walk: Walk, delta: RoomDelta): Walk =
    walk.copy(
        differences = walk.differences.filterNot { it.roomId == delta.roomId } + delta,
    )

/** Appends a landlord/tenant [signature] to the walk in the order it was entered. */
fun addSignature(walk: Walk, signature: Signature): Walk =
    walk.copy(signatures = walk.signatures + signature)

/** Applies [transform] to the matching room, returning a new [Walk]; a missing id is a no-op copy. */
private fun updateRoom(walk: Walk, roomId: String, transform: (Room) -> Room): Walk =
    walk.copy(
        rooms = walk.rooms.map { room -> if (room.id == roomId) transform(room) else room },
    )

/** Mints an id that is not already used by a walk in [state]. */
private fun uniqueWalkId(state: WalkState): String {
    val existing = state.walks.mapTo(HashSet()) { it.id }
    var id = newId("walk")
    while (id in existing) id = newId("walk")
    return id
}

private fun newId(prefix: String): String = "$prefix-${UUID.randomUUID()}"
