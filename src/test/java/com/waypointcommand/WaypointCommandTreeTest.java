package com.waypointcommand;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
    void acceptsDimensionShortcutAndCustomDimension() {
        assertParses("waypoint dimension 100 100 100");
        assertParses("waypoint dimension minecraft:the_nether 100 ~ 100");
    }

    private static void assertParses(String command) {
        CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
        WaypointCommandClient.registerCommands(dispatcher);

        ParseResults<FabricClientCommandSource> result = dispatcher.parse(command, null);
        assertTrue(result.getExceptions().isEmpty(), () -> command + " -> " + result.getExceptions());
        assertTrue(!result.getReader().canRead(), () -> command + " left unparsed input: "
                + result.getReader().getRemaining());
    }
}


