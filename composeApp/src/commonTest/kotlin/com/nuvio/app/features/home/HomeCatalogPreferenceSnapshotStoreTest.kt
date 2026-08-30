package com.nuvio.app.features.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class HomeCatalogPreferenceSnapshotStoreTest {
    @Test
    fun `publishing an update preserves the previous complete snapshot`() {
        val originalPreference = preference(key = "first", order = 0)
        val source = mutableMapOf(originalPreference.key to originalPreference)
        val store = HomeCatalogPreferenceSnapshotStore(source)
        val beforeUpdate = store.value

        source["partial"] = preference(key = "partial", order = 1)
        val updatedPreference = originalPreference.copy(
            customTitle = "Enhanced title",
            enabled = false,
        )
        store.publish(beforeUpdate + (updatedPreference.key to updatedPreference))

        assertEquals(setOf("first"), beforeUpdate.keys)
        assertEquals("", beforeUpdate.getValue("first").customTitle)
        assertTrue(beforeUpdate.getValue("first").enabled)
        assertEquals(setOf("first"), store.value.keys)
        assertEquals("Enhanced title", store.value.getValue("first").customTitle)
        assertFalse(store.value.getValue("first").enabled)
        assertTrue(store.value.getValue("first").heroSourceEnabled)
        assertNotSame(beforeUpdate, store.value)
    }

    @Test
    fun `reorder publishes all new orders at once without changing captured snapshot`() {
        val store = HomeCatalogPreferenceSnapshotStore(
            linkedMapOf(
                "first" to preference(key = "first", order = 0, heroSourceEnabled = true),
                "second" to preference(key = "second", order = 1, heroSourceEnabled = false),
                "third" to preference(key = "third", order = 2, heroSourceEnabled = true),
            ),
        )
        val beforeReorder = store.value
        val reordered = beforeReorder.toMutableMap()

        listOf("third", "first", "second").forEachIndexed { order, key ->
            reordered[key] = reordered.getValue(key).copy(order = order)
        }
        store.publish(reordered)

        assertEquals(listOf(0, 1, 2), beforeReorder.values.map { it.order })
        assertEquals(
            listOf("third", "first", "second"),
            store.value.values.sortedBy { it.order }.map { it.key },
        )
        assertTrue(store.value.getValue("third").heroSourceEnabled)
        assertFalse(store.value.getValue("second").heroSourceEnabled)
    }

    @Test
    fun `profile reset publishes an empty snapshot and leaves enhanced settings defaults intact`() {
        val store = HomeCatalogPreferenceSnapshotStore(
            mapOf("first" to preference(key = "first", order = 0)),
        )
        val profileSnapshot = store.value

        store.publish(emptyMap())
        val resetState = HomeCatalogSettingsUiState()

        assertEquals(setOf("first"), profileSnapshot.keys)
        assertTrue(store.value.isEmpty())
        assertTrue(resetState.heroEnabled)
        assertTrue(resetState.heroAutoScrollEnabled)
        assertFalse(resetState.heroMotionPreviewEnabled)
        assertTrue(resetState.showCatalogType)
        assertFalse(resetState.hideUnreleasedContent)
        assertFalse(resetState.hideCatalogUnderline)
    }

    @Test
    fun `preference snapshot replacement preserves enhanced-only settings`() {
        val beforeUpdate = HomeCatalogSettingsSnapshot(
            heroEnabled = false,
            heroAutoScrollEnabled = false,
            heroMotionPreviewEnabled = true,
            showCatalogType = false,
            hideUnreleasedContent = true,
            hideCatalogUnderline = true,
            preferences = mapOf(
                "first" to HomeCatalogPreference(
                    customTitle = "Original",
                    enabled = true,
                    heroSourceEnabled = false,
                    order = 0,
                ),
            ),
        )

        val afterUpdate = beforeUpdate.copy(
            preferences = beforeUpdate.preferences + (
                "first" to beforeUpdate.preferences.getValue("first").copy(customTitle = "Updated")
            ),
        )

        assertFalse(afterUpdate.heroEnabled)
        assertFalse(afterUpdate.heroAutoScrollEnabled)
        assertTrue(afterUpdate.heroMotionPreviewEnabled)
        assertFalse(afterUpdate.showCatalogType)
        assertTrue(afterUpdate.hideUnreleasedContent)
        assertTrue(afterUpdate.hideCatalogUnderline)
        assertFalse(afterUpdate.preferences.getValue("first").heroSourceEnabled)
        assertEquals("Updated", afterUpdate.preferences.getValue("first").customTitle)
    }

    private fun preference(
        key: String,
        order: Int,
        heroSourceEnabled: Boolean = true,
    ) = StoredHomeCatalogPreference(
        key = key,
        heroSourceEnabled = heroSourceEnabled,
        order = order,
    )
}
