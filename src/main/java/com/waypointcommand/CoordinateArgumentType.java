package com.waypointcommand;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CoordinateArgumentType implements ArgumentType<CoordinateArgumentType.Coordinate> {
    private static final Collection<String> EXAMPLES = List.of("100", "-20", "~", "~16");
    private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(
            Component.literal("坐标必须是整数、~ 或 ~<偏移>")
    );

    private CoordinateArgumentType() {
    }

    public static CoordinateArgumentType coordinate() {
        return new CoordinateArgumentType();
    }

    @Override
    public Coordinate parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        boolean relative = reader.canRead() && reader.peek() == '~';
        if (relative) {
            reader.skip();
        }

        if (!reader.canRead() || !StringReader.isAllowedNumber(reader.peek())) {
            if (relative) {
                return new Coordinate(true, 0);
            }
            reader.setCursor(start);
            throw ERROR_INVALID.createWithContext(reader);
        }

        int offset = reader.readInt();
        return new Coordinate(relative, offset);
    }

    public static int resolve(CommandContext<FabricClientCommandSource> context, String name)
            throws CommandSyntaxException {
        Coordinate coordinate = context.getArgument(name, Coordinate.class);
        if (!coordinate.relative()) {
            return coordinate.value();
        }

        FabricClientCommandSource source = context.getSource();
        if (source.getPlayer() == null) {
            throw new SimpleCommandExceptionType(Component.literal("相对坐标 ~ 只能在玩家上下文中使用"))
                    .create();
        }

        double base = switch (name) {
            case "x" -> source.getPlayer().getX();
            case "y" -> source.getPlayer().getY();
            case "z" -> source.getPlayer().getZ();
            default -> throw new IllegalArgumentException("Unknown coordinate: " + name);
        };
        return (int) Math.floor(base + coordinate.value());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        if (remaining.isEmpty() || remaining.startsWith("~")) {
            builder.suggest("~");
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public record Coordinate(boolean relative, int value) {
    }
}
