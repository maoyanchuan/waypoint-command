package com.waypointcommand;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaypointNameTest {
    @Test
    void usesPlainDefaultNameWithoutCoordinates() {
        assertEquals("路径点", WaypointCommandClient.normalizeName(null));
        assertEquals("路径点", WaypointCommandClient.normalizeName("   "));
    }

    @Test
    void preservesCustomNamesAndTrimsWhitespace() {
        assertEquals("基地", WaypointCommandClient.normalizeName("  基地  "));
        assertEquals("a".repeat(64), WaypointCommandClient.normalizeName("a".repeat(65)));
    }
}
