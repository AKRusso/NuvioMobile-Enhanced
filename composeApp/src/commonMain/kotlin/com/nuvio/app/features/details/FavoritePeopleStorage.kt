package com.nuvio.app.features.details

internal expect object FavoritePeopleStorage {
    fun loadPayload(profileId: Int): String?
    fun savePayload(profileId: Int, payload: String)
}
