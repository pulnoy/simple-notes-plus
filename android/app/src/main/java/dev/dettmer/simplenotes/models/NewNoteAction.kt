package dev.dettmer.simplenotes.models

/** Entrées du menu de création rapide de Simple Notes+. */
enum class NewNoteAction(val noteType: NoteType) {
    IMAGE(NoteType.TEXT),
    DRAWING(NoteType.TEXT),
    AUDIO(NoteType.TEXT),
    CHECKLIST(NoteType.CHECKLIST),
    TEXT(NoteType.TEXT)
}
