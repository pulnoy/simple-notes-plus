package dev.dettmer.simplenotes.ui.main

import dev.dettmer.simplenotes.ui.main.components.HomeAttachments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeAttachmentsTest {
    @Test fun localPreviewsRejectRemoteUrlsAndDirectoryTraversal() {
        assertEquals("photo.jpg", HomeAttachments.firstImage("![vacances](.assets/photo.jpg)"))
        assertNull(HomeAttachments.firstImage("![photo](https://example.com/photo.jpg)"))
        assertNull(HomeAttachments.firstImage("![photo](.assets/../private.jpg)"))
        assertNull(HomeAttachments.firstAudio("[audio](.assets/../private.m4a)"))
    }

    @Test fun mixedMediaKeepsTextAndSelectsFirstAttachmentOfEachType() {
        val content = "Souvenirs\n![photo](.assets/photo.jpg)\n[audio](.assets/memo.m4a)\n![dessin](.assets/dessin.png)"
        assertEquals("photo.jpg", HomeAttachments.firstImage(content))
        assertEquals("memo.m4a", HomeAttachments.firstAudio(content))
        assertEquals("Souvenirs", HomeAttachments.previewText(content))
    }

    @Test fun recordingDurationUsesMinutesAndSeconds() {
        assertEquals("0:24", HomeAttachments.durationLabel(24_999))
        assertEquals("1:05", HomeAttachments.durationLabel(65_000))
        assertEquals("0:00", HomeAttachments.durationLabel(-1000))
    }
}
