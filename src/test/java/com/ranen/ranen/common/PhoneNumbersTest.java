package com.ranen.ranen.common;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhoneNumbersTest {

    @Test
    void acceptsCommonFormats() {
        Optional<String> expected = Optional.of("+254711000111");
        assertEquals(expected, PhoneNumbers.normalize("+254711000111"));
        assertEquals(expected, PhoneNumbers.normalize("254711000111"));
        assertEquals(expected, PhoneNumbers.normalize("0711000111"));
        assertEquals(expected, PhoneNumbers.normalize("0711 000 111"));
        assertEquals(expected, PhoneNumbers.normalize("711000111"));
        assertEquals(expected, PhoneNumbers.normalize("+254 711-000-111"));
    }

    @Test
    void rejectsGarbage() {
        assertEquals(Optional.empty(), PhoneNumbers.normalize(null));
        assertEquals(Optional.empty(), PhoneNumbers.normalize(""));
        assertEquals(Optional.empty(), PhoneNumbers.normalize("abc"));
        assertEquals(Optional.empty(), PhoneNumbers.normalize("+255711000111")); // Tanzania
        assertEquals(Optional.empty(), PhoneNumbers.normalize("071100011"));     // too short
    }
}
