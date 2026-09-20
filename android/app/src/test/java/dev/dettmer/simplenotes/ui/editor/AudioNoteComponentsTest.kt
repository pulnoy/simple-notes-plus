package dev.dettmer.simplenotes.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioNoteComponentsTest {
    @Test fun `extracts audio links without including images`() {
        assertEquals(
            listOf("voice.m4a", "other.ogg"),
            audioAssetNames("[audio](.assets/voice.m4a)\n![](.assets/photo.webp)\n[audio](.assets/other.ogg)\n[audio](.assets/voice.m4a)")
        )
    }

    @Test fun `creates markdown audio reference`() {
        assertEquals("[audio](.assets/voice.m4a)", audioMarkdown("voice.m4a"))
    }
}
