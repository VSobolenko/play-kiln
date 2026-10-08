package com.gexetr.unitwalk.domain

import kotlinx.serialization.Serializable

/**
 * The whole durable model for the app, as immutable values.
 *
 * Every type here is a plain `data class` / `enum class` with a generated kotlinx.serialization
 * serializer, so the same shapes are the draft store's document, the unit-test fixtures and the
 * report's input. Nothing in this package imports the Android framework: the domain is testable on
 * the JVM, and only [com.gexetr.unitwalk.pdf.renderReportPdf] touches the platform.
 *
 * ## The crash-safety contract
 *
 * A walk is only ever advanced by handing a *new* snapshot to
 * [com.gexetr.unitwalk.data.WalkStore.save]. The caller must persist the snapshot returned by a
 * mutation **before** opening the next room; that ordering is what makes a process kill cost the
 * current room rather than the whole walk (see [WalkDraft]).
 */

/** One named unit in the landlord's local portfolio. */
@Serializable
data class Unit(
    val id: String,
    val name: String,
    val address: String = "",
    val notes: String = "",
    val createdAt: String = "",
)

/** Why the walk is being run. The kind drives the comparison the report offers. */
@Serializable
enum class WalkKind {
    MOVE_IN,
    MOVE_OUT,
    SINGLE,
}

/**
 * Where a walk sits in its life.
 *
 * A walk is created [DRAFT] and only [WalkStatus.FINISHED] once the landlord has signed off on it.
 * The comparison looks for a finished `MOVE_IN`; a draft is not a baseline.
 */
@Serializable
enum class WalkStatus {
    DRAFT,
    FINISHED,
}

/** Where a room's photo came from. Both paths store the bytes in app-private storage. */
@Serializable
enum class PhotoSource {
    CAMERA,
    PICKER,
}

/**
 * A condition photo attached to a room.
 *
 * [fileName] is a name inside the store's own `photos/` directory, never a path: the store owns
 * where bytes live, and a name keeps the JSON document relocatable.
 */
@Serializable
data class Photo(
    val id: String,
    val fileName: String,
    val capturedAtIso: String = "",
    val source: PhotoSource = PhotoSource.CAMERA,
)

/** One room of the walk, with its note and its photos. */
@Serializable
data class Room(
    val id: String,
    val name: String,
    val note: String = "",
    val photos: List<Photo> = emptyList(),
)

/** The closed condition vocabulary. Every `when` over it is exhaustive and has no `else`. */
@Serializable
enum class RoomCondition {
    GOOD,
    WORN,
    DAMAGED,
    MISSING,
}

/** A recorded difference for one room, keyed by the room it belongs to. */
@Serializable
data class RoomDelta(
    val roomId: String,
    val condition: RoomCondition = RoomCondition.GOOD,
    val note: String = "",
)

/** A name the landlord or tenant entered on the device to close out the report. */
@Serializable
data class Signature(
    val name: String = "",
    val signedAtIso: String = "",
)

/** One room-by-room walk. Rooms are ordered; [differences] carries at most one delta per room. */
@Serializable
data class Walk(
    val id: String,
    val unitId: String,
    val kind: WalkKind = WalkKind.SINGLE,
    val startedAtIso: String = "",
    val finishedAtIso: String? = null,
    val status: WalkStatus = WalkStatus.DRAFT,
    val rooms: List<Room> = emptyList(),
    val differences: List<RoomDelta> = emptyList(),
    val signatures: List<Signature> = emptyList(),
)

/** The whole persisted document: every unit and every walk, in one versioned value. */
@Serializable
data class WalkState(
    val units: List<Unit> = emptyList(),
    val walks: List<Walk> = emptyList(),
    val version: Int = 1,
)
