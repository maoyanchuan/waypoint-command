package com.waypointcommand.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.waypointcommand.WaypointCommandSuggestions;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Redirect(
            method = "updateCommandInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;getCommands()Lcom/mojang/brigadier/CommandDispatcher;"
            )
    )
    private CommandDispatcher<ClientSuggestionProvider> waypointcommand$useWaypointCommandTree(
            ClientPacketListener connection
    ) {
        return WaypointCommandSuggestions.forSuggestions(connection);
    }
}
