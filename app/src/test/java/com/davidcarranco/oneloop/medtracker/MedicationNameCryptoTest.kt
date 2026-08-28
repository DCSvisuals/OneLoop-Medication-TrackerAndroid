package com.davidcarranco.oneloop.medtracker

import com.davidcarranco.oneloop.medtracker.data.crypto.MedicationNameCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationNameCryptoTest {

    @Test
    fun roundTripEncryptsAndDecryptsName() {
        val key = MedicationNameCrypto.accountKey("11111111-2222-3333-4444-555555555555")
        val sealed = MedicationNameCrypto.encrypt("Amoxicillin", key)
        assertTrue(sealed.startsWith(MedicationNameCrypto.PREFIX))
        assertFalse(sealed.contains("Amoxicillin"))
        assertEquals("Amoxicillin", MedicationNameCrypto.decrypt(sealed, key))
    }

    @Test
    fun legacyPlaintextIsReturnedUnchanged() {
        val key = MedicationNameCrypto.accountKey("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        assertEquals("Ibuprofen", MedicationNameCrypto.decrypt("Ibuprofen", key))
    }

    @Test
    fun encryptIsIdempotentWhenAlreadySealed() {
        val key = MedicationNameCrypto.accountKey("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        val once = MedicationNameCrypto.encrypt("Metformin", key)
        val twice = MedicationNameCrypto.encrypt(once, key)
        assertEquals(once, twice)
    }

    @Test
    fun accountKeyIsStableAndCaseInsensitive() {
        val a = MedicationNameCrypto.accountKey("E621E1F8-C36C-495A-93FC-0C247A3E6E5F")
        val b = MedicationNameCrypto.accountKey("e621e1f8-c36c-495a-93fc-0c247a3e6e5f")
        assertTrue(a.contentEquals(b))
        val other = MedicationNameCrypto.accountKey("00000000-0000-0000-0000-000000000000")
        assertFalse(a.contentEquals(other))
    }
}
