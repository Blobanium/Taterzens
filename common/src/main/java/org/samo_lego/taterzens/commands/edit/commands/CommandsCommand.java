package org.samo_lego.taterzens.commands.edit.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.commands.NpcCommand;
import org.samo_lego.taterzens.npc.commands.AbstractTaterzenCommand;
import org.samo_lego.taterzens.npc.commands.BungeeCommand;
import org.samo_lego.taterzens.npc.commands.CommandGroups;
import org.samo_lego.taterzens.npc.commands.MinecraftCommand;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.command.CommandSource;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.taterzens.Taterzens.MOD_ID;
import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.commands.NpcCommand.noSelectedTaterzenError;
import static org.samo_lego.taterzens.npc.commands.BungeeCommand.AVAILABLE_SERVERS;
import static org.samo_lego.taterzens.util.TextUtil.errorText;
import static org.samo_lego.taterzens.util.TextUtil.joinText;
import static org.samo_lego.taterzens.util.TextUtil.successText;
import static org.samo_lego.taterzens.util.TextUtil.translate;

public class CommandsCommand {
    private static final SuggestionProvider<ServerCommandSource> BUNGEE_COMMANDS;
    private static final SuggestionProvider<ServerCommandSource> PLAYERS;

    public static void registerNode(CommandDispatcher<ServerCommandSource> dispatcher, LiteralCommandNode<ServerCommandSource> editNode) {
        LiteralCommandNode<ServerCommandSource> commandsNode = literal("commands")
                .then(literal("setPermissionLevel")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.set_permission_level", config.perms.npcCommandPermissionLevel))
                        .then(argument("level", IntegerArgumentType.integer(0, 4))
                                .executes(CommandsCommand::setPermissionLevel)
                        )
                )
                .then(literal("group")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.group", config.perms.npcCommandPermissionLevel))
                        .then(literal("new").executes(CommandsCommand::newGroup))
                        .then(literal("id")
                                .then(argument("group number", IntegerArgumentType.integer(0))
                                        .then(literal("delete")
                                                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.group.delete", config.perms.npcCommandPermissionLevel))
                                                .executes(CommandsCommand::deleteGroup)
                                        )
                                        .then(literal("removeCommand")
                                                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.remove", config.perms.npcCommandPermissionLevel))
                                                .then(argument("command id", IntegerArgumentType.integer()).executes(CommandsCommand::removeCommandFromGroup))
                                        )
                                        .then(literal("add")
                                                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.add", config.perms.npcCommandPermissionLevel))
                                                .then(literal("minecraft")
                                                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.add.minecraft", config.perms.npcCommandPermissionLevel))
                                                        .redirect(dispatcher.getRoot(), context -> {
                                                            // Really ugly, but ... works :P
                                                            String cmd = addCommand(context);
                                                            throw new SimpleCommandExceptionType(
                                                                    cmd == null ?
                                                                            noSelectedTaterzenError() :
                                                                            joinText("taterzens.command.commands.set", Formatting.GOLD, Formatting.GRAY, "/" + cmd)
                                                            ).create();
                                                        })
                                                )
                                                .then(literal("bungee")
                                                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.add.bungee", config.perms.npcCommandPermissionLevel))
                                                        .then(argument("command", string())
                                                                .suggests(BUNGEE_COMMANDS)
                                                                .then(argument("player", string())
                                                                        .suggests(PLAYERS)
                                                                        .then(argument("argument", string())
                                                                                .suggests((context, builder) -> CommandSource.suggestMatching(AVAILABLE_SERVERS, builder))
                                                                                .executes(CommandsCommand::addBungeeCommand)
                                                                        )
                                                                        .executes(CommandsCommand::addBungeeCommand)
                                                                )
                                                        )
                                                )
                                        )
                                        .then(literal("clear")
                                                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.clear", config.perms.npcCommandPermissionLevel))
                                                .executes(CommandsCommand::clearGroupCommands)
                                        )
                                        .then(literal("list")
                                                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.list", config.perms.npcCommandPermissionLevel))
                                                .executes(CommandsCommand::listGroupCommands)
                                        )
                                )
                        )
                )
                .then(literal("add")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.add", config.perms.npcCommandPermissionLevel))
                        .then(literal("minecraft")
                                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.add.minecraft", config.perms.npcCommandPermissionLevel))
                                .redirect(dispatcher.getRoot(), context -> {
                                    // Really ugly, but ... works :P
                                    String cmd = addCommand(context);
                                    throw new SimpleCommandExceptionType(
                                            cmd == null ?
                                                    noSelectedTaterzenError() :
                                                    joinText("taterzens.command.commands.set", Formatting.GOLD, Formatting.GRAY, "/" + cmd)
                                    ).create();
                                })
                        )
                        .then(literal("bungee")
                                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.add.bungee", config.perms.npcCommandPermissionLevel))
                                .then(argument("command", string())
                                        .suggests(BUNGEE_COMMANDS)
                                        .then(argument("player", string())
                                                .suggests(PLAYERS)
                                                .then(argument("argument", string())
                                                        .suggests((context, builder) -> CommandSource.suggestMatching(AVAILABLE_SERVERS, builder))
                                                        .executes(CommandsCommand::addBungeeCommand)
                                                )
                                        )
                                        .executes(CommandsCommand::addBungeeCommand)
                                )
                        )
                )
                .then(literal("clear")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.clear", config.perms.npcCommandPermissionLevel))
                        .executes(CommandsCommand::clearAllCommands)
                )
                .then(literal("list")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.commands.list", config.perms.npcCommandPermissionLevel))
                        .executes(CommandsCommand::listAllTaterzenCommands)
                )
                .build();

        CooldownCommand.registerNode(commandsNode);
        editNode.addChild(commandsNode);
    }

    private static int newGroup(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            int ix = taterzen.getCommandGroups().createGroup();
            source.sendFeedback(successText("taterzens.command.commands.group.created", String.valueOf(ix + 1)), false);
        });
    }

    private static int listGroupCommands(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int groupIndex = IntegerArgumentType.getInteger(context, "group number") - 1;

        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            var commands = taterzen.getGroupCommands(groupIndex);
            final String separator = "\n-------------------------------------------------";

            MutableText response = joinText("taterzens.command.commands.list", Formatting.AQUA, Formatting.YELLOW, taterzen.getName().getString());
            response.append(separator);

            if (!commands.isEmpty()) {
                response.append("\n");
                response.append(
                        joinText("taterzens.command.commands.group", Formatting.LIGHT_PURPLE, Formatting.DARK_RED, "#" + (groupIndex + 1)));
                response.append(separator);


                for (int i = 0; i < commands.size(); i++) {
                    var cmd = commands.get(i);
                    int finalJ = i + 1;
                    response.append(
                            Text.literal("\n" + (i + 1) + "-> ")
                                    .formatted(i % 2 == 0 ? Formatting.YELLOW : Formatting.GOLD)
                                    .append(cmd.toString())
                                    .append("   ")
                                    .append("(" + cmd.getType().toString().toLowerCase() + ")")
                                    .append("   ")
                                    .append(
                                            Text.literal("X")
                                                    .formatted(Formatting.RED)
                                                    .formatted(Formatting.BOLD)
                                                    .styled(style -> style
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate("taterzens.tooltip.delete", finalJ)))
                                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/npc edit commands group id " + (groupIndex + 1) + " removeCommand " + finalJ))
                                                    )
                                    )
                    );
                }
            }
            source.sendFeedback(response, false);
        });
    }

    private static int deleteGroup(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int groupIndex = IntegerArgumentType.getInteger(context, "group number") - 1;

        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            var cmds = taterzen.getCommandGroups();
            if (groupIndex >= cmds.size()) {
                source.sendFeedback(
                        errorText("taterzens.command.commands.error.group.404", String.valueOf(groupIndex + 1)),
                        false
                );
            } else {
                source.sendFeedback(successText("taterzens.command.commands.group.removed", String.valueOf(groupIndex)), false);
                taterzen.clearGroupCommands(groupIndex);
            }
        });
    }


    private static int removeCommandFromGroup(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int groupIndex = IntegerArgumentType.getInteger(context, "group number") - 1;
        int cmdIx = IntegerArgumentType.getInteger(context, "command id") - 1;

        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            var cmds = taterzen.getCommandGroups();
            if (groupIndex >= cmds.size() || cmdIx >= cmds.get(groupIndex).size()) {
                source.sendFeedback(
                        errorText("taterzens.command.commands.error.404", String.valueOf(cmdIx)),
                        false
                );
            } else {
                source.sendFeedback(successText("taterzens.command.commands.removed", cmds.get(groupIndex).get(cmdIx).toString()), false);
                taterzen.removeGroupCommand(groupIndex, cmdIx);
            }
        });
    }

    private static int clearAllCommands(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            source.sendFeedback(successText("taterzens.command.commands.cleared", taterzen.getName().getString()), false);
            taterzen.clearAllCommands();
        });
    }


    private static int clearGroupCommands(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int groupIndex = IntegerArgumentType.getInteger(context, "group number") - 1;

        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            source.sendFeedback(successText("taterzens.command.commands.group.cleared", String.valueOf(groupIndex)), false);
            taterzen.clearGroupCommands(groupIndex);
        });
    }

    private static int listAllTaterzenCommands(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            var commands = taterzen.getCommandGroups();
            final String separator = "\n-------------------------------------------------";

            MutableText response = joinText("taterzens.command.commands.list", Formatting.AQUA, Formatting.YELLOW, taterzen.getName().getString());
            response.append(separator);

            if (!commands.isEmpty()) {
                for (int i = 0; i < commands.size(); i++) {
                    int finali = i + 1;
                    var cmdGrp = commands.get(i);
                    response.append("\n");
                    response.append(
                            joinText("taterzens.command.commands.group", Formatting.LIGHT_PURPLE, Formatting.DARK_RED, "#" + (i + 1)));
                    response.append("   ").append(Text.literal("X")
                            .formatted(Formatting.BOLD)
                            .formatted(Formatting.RED)
                            .styled(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate("taterzens.tooltip.delete", finali)))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/npc edit commands group id " + finali + " delete"))));

                    for (int j = 0; j < cmdGrp.size(); j++) {
                        var cmd = cmdGrp.get(j);
                        int finalJ = j + 1;
                        response.append(
                                Text.literal("\n" + finalJ + "-> ")
                                        .formatted(j % 2 == 0 ? Formatting.YELLOW : Formatting.GOLD)
                                        .append(cmd.toString())
                                        .append("   ")
                                        .append("(" + cmd.getType().toString().toLowerCase() + ")")
                                        .append("   ")
                                        .append(
                                                Text.literal("X")
                                                        .formatted(Formatting.RED)
                                                        .formatted(Formatting.BOLD)
                                                        .styled(style -> style
                                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate("taterzens.tooltip.delete", finalJ)))
                                                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/npc edit commands group id " + finali + " removeCommand " + finalJ))
                                                        )
                                        )
                        );
                    }


                    response.append(separator);
                }
            }
            source.sendFeedback(response, false);
        });
    }

    private static int setPermissionLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int newPermLevel = IntegerArgumentType.getInteger(context, "level");

        if(!config.perms.allowSettingHigherPermissionLevel && !source.hasPermissionLevel(newPermLevel)) {
            source.sendError(errorText("taterzens.error.permission", String.valueOf(newPermLevel)));
            return -1;
        }

        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            source.sendFeedback(successText("taterzens.command.commands.permission.set", String.valueOf(newPermLevel)), false);
            taterzen.setPermissionLevel(newPermLevel);

        });
    }

    private static int addBungeeCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String command = StringArgumentType.getString(context, "command");
        String player = StringArgumentType.getString(context, "player");
        String argument = "";
        int group = 0;
        boolean latest;
        try {
            group = IntegerArgumentType.getInteger(context, "group number") - 1;
            latest = false;
        } catch (IllegalArgumentException ignored) {
            latest = true;
        }

        try {
            argument = StringArgumentType.getString(context, "argument");
        } catch (IllegalArgumentException ignored) {
        }

        String finalArgument = argument;
        int finalGroup = group;
        boolean finalLatest = latest;
        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            boolean added;
            if (finalLatest) {
                added = taterzen.addCommand(new BungeeCommand(BungeeCommand.BungeeMessage.valueOf(command.toUpperCase()), player, finalArgument));
            } else {
                added = taterzen.addCommand(new BungeeCommand(BungeeCommand.BungeeMessage.valueOf(command.toUpperCase()), player, finalArgument), finalGroup);
            }
            Text text;
            if (added) {
                text = joinText("taterzens.command.commands.setBungee",
                        Formatting.GOLD,
                        Formatting.GRAY,
                        "/" + command + " " + player + " " + finalArgument
                );
            } else {
                text = errorText("taterzens.error.enableBungee");
            }
            source.sendFeedback(text, false);
        });
    }

    private static String addCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        int group = 0;
        boolean latest;
        try {
            group = IntegerArgumentType.getInteger(context, "group number") - 1;
            latest = false;
        } catch (IllegalArgumentException ignored) {
            latest = true;
        }

        AtomicReference<String> command = new AtomicReference<>();
        int finalGroup = group;
        boolean finalLatest = latest;
        NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            // Extremely :concern:
            // I know it
            String inputCmd = context.getInput();
            String index = "add minecraft ";
            command.set(inputCmd.substring(inputCmd.indexOf(index) + index.length()));
            if (finalLatest) {
                taterzen.addCommand(new MinecraftCommand(command.get()));
            } else {
                taterzen.addCommand(new MinecraftCommand(command.get()), finalGroup);
            }
        });
        return command.get();
    }


    static {
        BUNGEE_COMMANDS = SuggestionProviders.register(
                new Identifier(MOD_ID, "bungee_commands"),
                (context, builder) ->
                        CommandSource.suggestMatching(Stream.of(BungeeCommand.BungeeMessage.values()).map(cmd -> cmd.toString().toLowerCase()).collect(Collectors.toList()), builder)
        );
        PLAYERS = SuggestionProviders.register(
                new Identifier(MOD_ID, "players"),
                (context, builder) -> {
                    Collection<String> names = context.getSource().getPlayerNames();
                    names.add("--clicker--");
                    return CommandSource.suggestMatching(names, builder);
                }
        );
    }
}
