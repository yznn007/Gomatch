package com.gomatch.server.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PhoneNumberNormalizerTest {

    @Test
    void normalizesValidInternationalNumbers() {
        assertEquals("+8613800138000", PhoneNumberNormalizer.normalize("+86 138-0013-8000"));
        assertEquals("+14155552671", PhoneNumberNormalizer.normalize("14155552671"));
    }

    @Test
    void rejectsInvalidInternationalNumbers() {
        assertNull(PhoneNumberNormalizer.normalize("+01234567890"));
        assertNull(PhoneNumberNormalizer.normalize("1234567"));
    }
}
