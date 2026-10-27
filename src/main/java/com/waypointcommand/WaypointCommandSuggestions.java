package com.waypointcommand;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public final class WaypointCommandSuggestions {
    private static ClientPacketListener cachedConnection;
    private static CommandDispatcher<ClientSuggestionProvider> cachedSource;
    private static CommandDispatcher<ClientSuggestionProvider> cachedResult;

    private WaypointCommandSuggestions() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static CommandDispatcher<ClientSuggestionProvider> forSuggestions(ClientPacketListener connection) {
        CommandDispatcher<ClientSuggestionProvider> source = connection.getCommands();
        if (source == null) {
            return null;
        }
        if (connection == cachedConnection && source == cachedSource) {
            return cachedResult;
        }

        CommandDispatcher<ClientSuggestionProvider> filtered = new CommandDispatcher<>();
        for (CommandNode<ClientSuggestionProvider> child : source.getRoot().getChildren()) {
            if (!"waypoint".equals(child.getName())) {
                filtered.getRoot().addChild(child);
            }
        }

        CommandDispatcher<FabricClientCommandSource> clientDispatcher = ClientCommands.getActiveDispatcher();
        if (clientDispatcher != null) {
            ClientCommandInternals.addCommands(
                    (CommandDispatcher) filtered,
                    (FabricClientCommandSource) connection.getSuggestionsProvider()
            );
        }

        cachedConnection = connection;
        cachedSource = source;
        cachedResult = filtered;
        return filtered;
    }
}


