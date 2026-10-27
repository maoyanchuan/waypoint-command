# Waypoint Command

一个面向 Minecraft **26.2 Fabric** 的客户端模组。它注册客户端指令，把坐标直接写入 **Xaero's Minimap 26.6.1** 的当前路径点集。

## 指令

默认格式保持不变，名称可以省略：

```text
/waypoint <x> <y> <z>
/waypoint <x> <y> <z> <名称>
```

`x`、`y`、`z` 都支持 `~` 和 `~<偏移>`。例如只指定高度为当前位置：

```text
/waypoint 1000 ~ 1000
/waypoint 1000 ~16 1000
```

示例：

```text
/waypoint 1000 128 1000
/waypoint 1000 128 1000 基地
```

### 指定维度

维度写在坐标前面。普通路径点保存在对应维度的当前路径点集中：

```text
/waypoint overworld <x> <y> <z> [名称]
/waypoint nether <x> <y> <z> [名称]
/waypoint end <x> <y> <z> [名称]
```

示例：

```text
/waypoint nether 100 64 -200 下界传送门
/waypoint end 0 80 0 末地平台
```

模组维度也可以使用维度 ID：

```text
/waypoint dimension [namespace:path] <x> <y> <z> [名称]
```

省略维度 ID 时写入当前维度。

例如：

```text
/waypoint dimension minecraft:the_nether 100 64 -200
```

### 临时路径点

`temp` 创建仅在当前游戏进程中存在的 Xaero 临时路径点，不写入存档：

```text
/waypoint temp <x> <y> <z> [名称]
/waypoint temporary <x> <y> <z> [名称]
/waypoint temp <overworld|nether|end> <x> <y> <z> [名称]
/waypoint temp dimension <维度ID> <x> <y> <z> [名称]
```

示例：

```text
/waypoint temp 1000 ~ 1000 临时集合点
/waypoint temporary 1000 128 1000
```

### 目的地路径点

`destination` 创建 Xaero 目的地路径点。它会正常保存；当玩家进入目的地 4 格范围后，Xaero 会自动移除它：

```text
/waypoint destination <x> <y> <z> [名称]
/waypoint dest <x> <y> <z> [名称]
/waypoint destination <overworld|nether|end> <x> <y> <z> [名称]
/waypoint destination dimension <维度ID> <x> <y> <z> [名称]
```

示例：

```text
/waypoint destination nether 100 64 -200
```

## 默认表现

- 普通路径点颜色为浅蓝，图标为 `W`。
- 临时路径点颜色为黄色，图标为 `T`，不会写入存档；使用 Xaero 官方临时路径点创建流程。
- 目的地路径点颜色为金色，图标为 `D`。
- 名称省略时自动使用 `路径点`，不会把坐标写进名称。
- `x`、`y`、`z` 支持 `~` 和 `~<偏移>`；相对坐标以玩家当前位置为基准。
- 名称最长 64 个字符。
- 默认写入当前维度；指定 `overworld`、`nether`、`end` 或 `dimension <维度ID>` 时写入对应维度。
- 目标维度必须已经由 Xaero 创建。若提示找不到维度数据，请先进入该维度一次。

## 依赖

运行时需要：

- Minecraft 26.2
- Java 25
- Fabric Loader 0.18.4 或更高
- Fabric API 0.161.0+26.2 或兼容版本
- Xaero's Minimap 26.6.1 或兼容版本

Loader 最低要求由 Fabric API `0.161.0+26.2` 的依赖 `fabricloader >= 0.18.4` 决定；本模组自身没有额外要求。

本模组不会打包或重新分发 Xaero's Minimap。

## 构建

默认从仓库同级的 `xaero` 目录读取：

```text
../xaero/xaerominimap-fabric-26.2-26.6.1.jar
```

如果 Xaero JAR 位于其他位置，可通过 Gradle 属性覆盖：

```powershell
.\gradlew.bat build -Pxaero_minimap_jar="C:/path/to/xaerominimap-fabric-26.2-26.6.1.jar"
```

构建：

```powershell
.\gradlew.bat build
```

成品：

```text
build/libs/waypoint-command-1.0.3.jar
```

把该 JAR 和 Xaero's Minimap、Fabric API 一起放入 `mods` 目录即可。

## 实现说明

指令是客户端指令，不要求服务端安装本模组。模组通过 Xaero 26.2 的公开运行期类访问：

- 当前 `MinimapWorld`
- 当前 `WaypointSet`
- `Waypoint`、`WaypointPurpose`、`WaypointColor`
- `MinimapWorldManagerIO.saveWorld`

临时路径点利用 Xaero 的 `temporary` 标记，Xaero 自己会在保存时跳过它。

Minecraft 26.2 自带一个用于追踪实体的原版 `/waypoint` 指令。本模组会让聊天栏语法高亮使用自己的客户端命令树，避免输入 `/waypoint <x> <y> <z>` 时坐标被原版规则标红。

