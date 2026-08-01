package com.example

import com.example.network.UpdateUserRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the assumption UserDto.kt's UpdateUserRequest comment relies on: that
 * Moshi omits null fields when serializing, rather than writing them as `null`.
 * This matters because PATCH /users/:id treats a present-but-null field as "set
 * this to null", not "leave unchanged" — if Moshi wrote nulls, a partial profile
 * edit (e.g. name only) would wipe out phone/ward on the backend.
 */
class MoshiNullSerializationTest {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test
    fun `partial update omits unset fields rather than sending null`() {
        val adapter = moshi.adapter(UpdateUserRequest::class.java)
        val json = adapter.toJson(UpdateUserRequest(name = "New Name", phone = null, ward = null))
        assertEquals("{\"name\":\"New Name\"}", json)
    }
}
