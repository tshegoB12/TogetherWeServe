package com.togetherweserve.app.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationCodecTest {
    private fun item(id: Long, eventId: String? = "e$id") =
        NotificationItem(id, "Title $id", "Body $id", eventId, id * 1000)

    @Test fun `encode then decode returns the same items`() {
        val items = listOf(item(1), item(2, eventId = null))
        assertThat(NotificationCodec.decode(NotificationCodec.encode(items))).isEqualTo(items)
    }

    @Test fun `null blank and corrupt json decode to empty list`() {
        assertThat(NotificationCodec.decode(null)).isEmpty()
        assertThat(NotificationCodec.decode("   ")).isEmpty()
        assertThat(NotificationCodec.decode("{not json")).isEmpty()
    }

    @Test fun `prepend puts newest first`() {
        val result = NotificationCodec.prepend(listOf(item(1)), item(2))
        assertThat(result.map { it.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test fun `history is capped so storage cannot grow forever`() {
        var list = emptyList<NotificationItem>()
        for (i in 1L..(NotificationCodec.MAX_ITEMS + 10L)) list = NotificationCodec.prepend(list, item(i))
        assertThat(list).hasSize(NotificationCodec.MAX_ITEMS)
        assertThat(list.first().id).isEqualTo(NotificationCodec.MAX_ITEMS + 10L)
    }
}
