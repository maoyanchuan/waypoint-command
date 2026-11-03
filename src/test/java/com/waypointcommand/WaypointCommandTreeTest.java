package com.waypointcommand;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaypointCommandTreeTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void acceptsRelativeYCoordinates() {
        assertParses("waypoint 100 ~ 100");
        assertParses("waypoint 100 ~16 -100");
        assertParses("waypoint temp 100 ~ 100");
        assertParses("waypoint temporary 100 ~ 100");
        assertParses("waypoint temporary nether 100 ~ 100");
        assertParses("waypoint destination 100 ~ 100");
        assertParses("waypoint dest end 100 ~ 100");
        assertParses("waypoint temp dimension minecraft:the_nether 100 ~ 100");
    }

    @Test
    void acceptsTwoArgumentCoordinatesWithCurrentY() {
        assertArguments("waypoint 16591 21695", Set.of("x", "y"));
        assertArguments("waypoint 16591 21695 基地", Set.of("x", "y", "name"));
        assertArguments("waypoint nether 16591 21695", Set.of("x", "y"));
        assertArguments("waypoint temp dimension minecraft:the_nether 16591 21695",
                Set.of("dimension_id", "x", "y"));
    }

    @Test
    void keepsThreeArgumentCoordinatesAsXyz() {
        assertArguments("waypoint 100 64 200", Set.of("x", "y", "z"));
        assertArguments("waypoint 100 64 200 基地", Set.of("x", "y", "z", "name"));
    }

    @Test
    void acceptsDimensionShortcutAndCustomDimension() {
        assertParses("waypoint dimension 100 100 100");
        assertParses("waypoint dimension minecraft:the_nether 100 ~ 100");
    }

    private static void assertParses(String command) {
        assertArguments(command, null);
    }

    private static void assertArguments(String command, Set<String> expectedArguments) {
        CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
        WaypointCommandClient.registerCommands(dispatcher);

        ParseResults<FabricClientCommandSource> result = dispatcher.parse(command, null);
        assertTrue(result.getExceptions().isEmpty(), () -> command + " -> " + result.getExceptions());
        assertTrue(!result.getReader().canRead(), () -> command + " left unparsed input: "
                + result.getReader().getRemaining());
        if (expectedArguments != null) {
            assertEquals(expectedArguments, result.getContext().getArguments().keySet());
        }
    }
}



