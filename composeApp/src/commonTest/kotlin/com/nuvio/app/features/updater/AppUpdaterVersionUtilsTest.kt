package com.nuvio.app.features.updater

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppUpdaterVersionUtilsTest {
    @Test
    fun `release tag with higher build code is newer for same version`() {
        assertTrue(
            VersionUtils.isRemoteNewer(
                remote = "enhanced-v0.2.16-build93",
                local = "0.2.16",
                localBuildCode = 92,
            ),
        )
    }

    @Test
    fun `release tag with same build code is not newer for same version`() {
        assertFalse(
            VersionUtils.isRemoteNewer(
                remote = "enhanced-v0.2.16-build93",
                local = "0.2.16",
                localBuildCode = 93,
            ),
        )
    }

    @Test
    fun `legacy enhanced tag extracts trailing build code`() {
        assertEquals(88, VersionUtils.parseBuildCode("enhanced-v0.2.13-88"))
        assertEquals(listOf(0, 2, 13), VersionUtils.parseVersionParts("enhanced-v0.2.13-88"))
    }

    @Test
    fun `older remote semantic version is not newer even with matching build`() {
        assertFalse(
            VersionUtils.isRemoteNewer(
                remote = "enhanced-v0.2.14-build92",
                local = "0.2.16",
                localBuildCode = 92,
            ),
        )
    }

    @Test
    fun `newer upstream semantic version remains newer`() {
        assertTrue(
            VersionUtils.isRemoteNewer(
                remote = "0.2.17",
                local = "0.2.16",
                localBuildCode = 93,
            ),
        )
    }
}
