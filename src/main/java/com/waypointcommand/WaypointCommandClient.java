package com.waypointcommand;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class WaypointCommandClient implements ClientModInitializer {
    private static final int MAX_NAME_LENGTH = 64;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));
    }

    static void registerCommands(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = createRootCommand();
        root.then(createKindCommand("temp", WaypointKind.TEMPORARY));
        root.then(createKindCommand("temporary", WaypointKind.TEMPORARY));
        root.then(createKindCommand("destination", WaypointKind.DESTINATION));
        root.then(createKindCommand("dest", WaypointKind.DESTINATION));
        dispatcher.register(root);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> createRootCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal("waypoint")
                .executes(context -> showUsage(context.getSource()));

        addCoordinateArguments(root, WaypointKind.NORMAL, null);
        addDimensionArguments(root, WaypointKind.NORMAL);
        return root;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> createKindCommand(
            String literal,
            WaypointKind kind
    ) {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommands.literal(literal);
        addCoordinateArguments(command, kind, null);
        addDimensionArguments(command, kind);
        return command;
    }

    private static void addDimensionArguments(
            LiteralArgumentBuilder<FabricClientCommandSource> parent,
            WaypointKind kind
    ) {
        parent.then(createDimensionCommand("overworld", "主世界", Level.OVERWORLD, kind));
        parent.then(createDimensionCommand("nether", "下界", Level.NETHER, kind));
        parent.then(createDimensionCommand("end", "末地", Level.END, kind));
        parent.then(createCustomDimensionCommand(kind));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> createDimensionCommand(
            String literal,
            String displayName,
            ResourceKey<Level> dimension,
            WaypointKind kind
    ) {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommands.literal(literal);
        addCoordinateArguments(command, kind, new DimensionTarget(displayName, dimension));
        return command;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> createCustomDimensionCommand(
            WaypointKind kind
    ) {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommands.literal("dimension");
        addCoordinateArguments(command, kind, null);

        RequiredArgumentBuilder<FabricClientCommandSource, Identifier> dimensionId = ClientCommands
                .argument("dimension_id", IdentifierArgument.id());
        addCustomCoordinateArguments(dimensionId, kind);
        command.then(dimensionId);
        return command;
    }

    private static void addCoordinateArguments(
            LiteralArgumentBuilder<FabricClientCommandSource> parent,
            WaypointKind kind,
            DimensionTarget target
    ) {
        parent.then(ClientCommands.argument("x", CoordinateArgumentType.coordinate())
                .then(ClientCommands.argument("y", CoordinateArgumentType.coordinate())
                        .then(ClientCommands.argument("z", CoordinateArgumentType.coordinate())
                                .executes(context -> executeWaypoint(context, kind, target, null))
                                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> executeWaypoint(
                                                context,
                                                kind,
                                                target,
                                                StringArgumentType.getString(context, "name")))))));
    }

    private static void addCustomCoordinateArguments(
            RequiredArgumentBuilder<FabricClientCommandSource, Identifier> parent,
            WaypointKind kind
    ) {
        parent.then(ClientCommands.argument("x", CoordinateArgumentType.coordinate())
                .then(ClientCommands.argument("y", CoordinateArgumentType.coordinate())
                        .then(ClientCommands.argument("z", CoordinateArgumentType.coordinate())
                                .executes(context -> executeWithCustomDimension(context, kind, null))
                                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> executeWithCustomDimension(
                                                context,
                                                kind,
                                                StringArgumentType.getString(context, "name")))))));
    }

    private static int executeWaypoint(
            CommandContext<FabricClientCommandSource> context,
            WaypointKind kind,
            DimensionTarget target,
            String requestedName
    ) throws CommandSyntaxException {
        return addWaypoint(
                context.getSource(),
                kind,
                target,
                CoordinateArgumentType.resolve(context, "x"),
                CoordinateArgumentType.resolve(context, "y"),
                CoordinateArgumentType.resolve(context, "z"),
                requestedName
        );
    }

    private static int executeWithCustomDimension(
            CommandContext<FabricClientCommandSource> context,
            WaypointKind kind,
            String requestedName
    ) throws CommandSyntaxException {
        Identifier identifier = context.getArgument("dimension_id", Identifier.class);
        DimensionTarget target = new DimensionTarget(
                identifier.toString(),
                ResourceKey.create(Registries.DIMENSION, identifier)
        );
        return executeWaypoint(context, kind, target, requestedName);
    }

    private static int showUsage(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal("用法:").withStyle(ChatFormatting.YELLOW));
        source.sendFeedback(Component.literal("/waypoint <x> <y> <z> [名称]")
                .withStyle(ChatFormatting.GRAY));
        source.sendFeedback(Component.literal("/waypoint <overworld|nether|end> <x> <y> <z> [名称]")
                .withStyle(ChatFormatting.GRAY));
        source.sendFeedback(Component.literal("/waypoint dimension [维度ID] <x> <y> <z> [名称]")
                .withStyle(ChatFormatting.GRAY));
        source.sendFeedback(Component.literal("/waypoint temp|temporary [维度] <x> <y> <z> [名称]")
                .withStyle(ChatFormatting.GRAY));
        source.sendFeedback(Component.literal("/waypoint destination|dest [维度] <x> <y> <z> [名称]")
                .withStyle(ChatFormatting.GRAY));
        source.sendFeedback(Component.literal("坐标可用 ~ 或 ~偏移，例如 /waypoint 100 ~ 100")
                .withStyle(ChatFormatting.DARK_GRAY));
        return 0;
    }

    private static int addWaypoint(
            FabricClientCommandSource source,
            WaypointKind kind,
            DimensionTarget target,
            int x,
            int y,
            int z,
            String requestedName
    ) {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null || session.getWorldManager() == null) {
            source.sendError(Component.literal("Xaero 小地图尚未就绪，请进入世界后再试。"));
            return 0;
        }

        MinimapWorld world = findWorld(session, target == null ? null : target.dimension());
        if (world == null) {
            String dimensionName = target == null ? "当前维度" : target.displayName();
            source.sendError(Component.literal("找不到 " + dimensionName
                    + " 的 Xaero 世界数据。请先进入该维度，让 Xaero 创建它。"));
            return 0;
        }

        WaypointSet waypointSet = world.getCurrentWaypointSet();
        if (waypointSet == null) {
            source.sendError(Component.literal("Xaero 当前路径点集不可用。"));
            return 0;
        }

        String name = normalizeName(requestedName, x, y, z);
        Waypoint waypoint;
        if (kind == WaypointKind.TEMPORARY) {
            waypoint = createTemporaryWaypoint(session, world, waypointSet, x, y, z);
            if (waypoint == null) {
                source.sendError(Component.literal("Xaero 未创建临时路径点，请确认小地图路径点功能已开启。"));
                return 0;
            }
            waypoint.setName(name);
            waypoint.setInitials(kind.symbol);
            waypoint.setWaypointColor(kind.color);
        } else {
            WaypointPurpose purpose = kind == WaypointKind.DESTINATION
                    ? WaypointPurpose.DESTINATION
                    : WaypointPurpose.NORMAL;
            waypoint = new Waypoint(
                    x,
                    y,
                    z,
                    name,
                    kind.symbol,
                    kind.color,
                    purpose,
                    false,
                    true
            );
            waypointSet.add(waypoint);

            try {
                session.getWorldManagerIO().saveWorld(world);
            } catch (IOException exception) {
                waypointSet.remove(waypoint);
                source.sendError(Component.literal("保存 Xaero 路径点失败: " + exception.getMessage()));
                return 0;
            }
        }

        session.getWaypointSession().setSetChangedTime(System.currentTimeMillis());

        String dimensionName = dimensionName(world);
        StringBuilder message = new StringBuilder("已添加");
        if (kind == WaypointKind.TEMPORARY) {
            message.append("临时");
        } else if (kind == WaypointKind.DESTINATION) {
            message.append("目的地");
        }
        message.append("路径点: ");

        source.sendFeedback(Component.literal(message.toString())
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" [" + dimensionName + "] (" + x + ", " + y + ", " + z + ")")
                        .withStyle(ChatFormatting.GRAY)));

        if (kind == WaypointKind.TEMPORARY) {
            source.sendFeedback(Component.literal("临时点不会写入存档，退出游戏或重载后消失。")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else if (kind == WaypointKind.DESTINATION) {
            source.sendFeedback(Component.literal("抵达目的地 4 格范围内后，Xaero 会自动移除它。")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else if (target != null) {
            source.sendFeedback(Component.literal("目标维度不是当前维度时，请进入该维度查看。")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        return 1;
    }

    private static Waypoint createTemporaryWaypoint(
            MinimapSession session,
            MinimapWorld world,
            WaypointSet waypointSet,
            int x,
            int y,
            int z
    ) {
        Set<Waypoint> before = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Waypoint waypoint : waypointSet.getWaypoints()) {
            before.add(waypoint);
        }

        double targetScale = session.getDimensionHelper().getDimCoordinateScale(world);
        session.getWaypointSession().getTemporaryHandler()
                .createTemporaryWaypoint(world, x, y, z, true, targetScale);

        for (Waypoint waypoint : waypointSet.getWaypoints()) {
            if (!before.contains(waypoint)) {
                return waypoint;
            }
        }
        return null;
    }

    private static MinimapWorld findWorld(MinimapSession session, ResourceKey<Level> dimension) {
        MinimapWorld currentWorld = session.getWorldManager().getCurrentWorld();
        if (dimension == null) {
            return currentWorld;
        }
        if (currentWorld != null && dimension.equals(currentWorld.getDimId())) {
            return currentWorld;
        }

        MinimapWorldRootContainer root = session.getWorldManager().getCurrentRootContainer();
        if (root == null) {
            return null;
        }

        MinimapWorld fallback = null;
        for (MinimapWorld world : root.getAllWorldsIterable()) {
            if (!dimension.equals(world.getDimId())) {
                continue;
            }
            if (currentWorld != null && currentWorld.getNode().equals(world.getNode())) {
                return world;
            }
            if (fallback == null) {
                fallback = world;
            }
        }
        return fallback;
    }

    private static String dimensionName(MinimapWorld world) {
        ResourceKey<Level> dimension = world.getDimId();
        if (Level.OVERWORLD.equals(dimension)) {
            return "主世界";
        }
        if (Level.NETHER.equals(dimension)) {
            return "下界";
        }
        if (Level.END.equals(dimension)) {
            return "末地";
        }
        return dimension == null ? "未知维度" : dimension.identifier().toString();
    }

    private static String normalizeName(String requestedName, int x, int y, int z) {
        String fallback = "路径点 " + x + ", " + y + ", " + z;
        if (requestedName == null) {
            return fallback;
        }

        String name = requestedName.strip().replaceAll("[\\r\\n\\t]", " ");
        if (name.isEmpty()) {
            return fallback;
        }
        return name.length() <= MAX_NAME_LENGTH ? name : name.substring(0, MAX_NAME_LENGTH);
    }

    private record DimensionTarget(String displayName, ResourceKey<Level> dimension) {
    }

    private enum WaypointKind {
        NORMAL("W", WaypointColor.AQUA),
        TEMPORARY("T", WaypointColor.YELLOW),
        DESTINATION("D", WaypointColor.GOLD);

        private final String symbol;
        private final WaypointColor color;

        WaypointKind(String symbol, WaypointColor color) {
            this.symbol = symbol;
            this.color = color;
        }
    }
}




