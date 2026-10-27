package com.waypointcommand;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoordinateArgumentTypeTest {
    @ParameterizedTest
    @CsvSource({
            "100,false,100",
            "-20,false,-20",
            "~,true,0",
            "~16,true,16",
            "~-7,true,-7"
    })
    void parsesAbsoluteAndRelativeCoordinates(String input, boolean relative, int value) throws Exception {
        CoordinateArgumentType.Coordinate coordinate = CoordinateArgumentType.coordinate()
                .parse(new StringReader(input));

        assertEquals(relative, coordinate.relative());
        assertEquals(value, coordinate.value());
    }

    @Test
    void rejectsTextThatIsNotACoordinate() {
        assertThrows(CommandSyntaxException.class,
                () -> CoordinateArgumentType.coordinate().parse(new StringReader("abc")));
    }
}
