package org.samo_lego.taterzens.fabric.compatibility.carpet;

import carpet.script.value.Value;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.api.TaterzensAPI;
import org.samo_lego.taterzens.api.professions.TaterzenProfession;
import org.samo_lego.taterzens.commands.NpcCommand;
import org.samo_lego.taterzens.interfaces.ITaterzenEditor;
import org.samo_lego.taterzens.npc.TaterzenNPC;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.commands.ProfessionCommand.PROFESSION_COMMAND_NODE;
import static org.samo_lego.taterzens.compatibility.ModDiscovery.CARPETMOD_LOADED;
import static org.samo_lego.taterzens.util.TextUtil.errorText;
import static org.samo_lego.taterzens.util.TextUtil.joinText;
import static org.samo_lego.taterzens.util.TextUtil.successText;
import static org.samo_lego.taterzens.util.TextUtil.translate;

public class ScarpetTraitCommand {
    static {
        TaterzensAPI.registerProfession(ScarpetProfession.ID, ScarpetProfession::new);
    }

    public static void register() {
        LiteralCommandNode<ServerCommandSource> scarpet = literal("scarpetTraits")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.profession.scarpet", config.perms.professionCommandPL))
                .then(literal("add")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.profession.scarpet.add", config.perms.professionCommandPL))
                        .then(argument("id", StringArgumentType.word())
                                .executes(ScarpetTraitCommand::addTrait)
                        )
                )
                .then(literal("list")
                        .executes(ScarpetTraitCommand::listScarpetTraits)
                )
                .then(literal("remove")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.profession.scarpet.remove", config.perms.professionCommandPL))
                        .then(argument("id", StringArgumentType.word())
                                .suggests(ScarpetTraitCommand::suggestRemovableTraits)
                                .executes(ScarpetTraitCommand::removeTrait)
                        )
                )
                .executes(ScarpetTraitCommand::listScarpetTraits)
                .build();


        PROFESSION_COMMAND_NODE.addChild(scarpet);
    }


    private static int listScarpetTraits(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            TaterzenProfession profession = taterzen.getProfession(ScarpetProfession.ID);
            if (profession instanceof ScarpetProfession scarpetProfession) {
                HashSet<Value> traitIds = scarpetProfession.getTraits();

                MutableText response = joinText("taterzens.command.trait.list", Formatting.AQUA, Formatting.YELLOW, taterzen.getName().getString());
                AtomicInteger i = new AtomicInteger();

                traitIds.forEach(trait -> {
                    int index = i.get() + 1;

                    String id = trait.getString();
                    response.append(
                            Text.literal("\n" + index + "-> " + id + " (")
                                    .formatted(index % 2 == 0 ? Formatting.YELLOW : Formatting.GOLD)
                                    .append(
                                            Text.literal("X")
                                                    .formatted(Formatting.RED)
                                                    .formatted(Formatting.BOLD)
                                                    .styled(style -> style
                                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate("taterzens.tooltip.delete", id)))
                                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/trait scarpet remove " + id))
                                                    )
                                    )
                                    .append(Text.literal(")").formatted(Formatting.RESET))
                    );
                    i.incrementAndGet();
                });
                source.sendFeedback(response, false);
            } else {
                source.sendError(errorText("taterzens.profession.lacking", ScarpetProfession.ID.toString()));
            }
        });
    }

    private static int removeTrait(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            TaterzenProfession profession = taterzen.getProfession(ScarpetProfession.ID);
            if (profession instanceof ScarpetProfession scarpetProfession) {
                if (scarpetProfession.removeTrait(id))
                    source.sendFeedback(successText("taterzens.command.trait.remove", id), false);
                else
                    context.getSource().sendError(errorText("taterzens.command.trait.error.404", id));
            } else {
                source.sendError(errorText("taterzens.profession.lacking", ScarpetProfession.ID.toString()));
            }
        });
    }

    private static int addTrait(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrThrow(), taterzen -> {
            TaterzenProfession profession = taterzen.getProfession(ScarpetProfession.ID);
            if (profession instanceof ScarpetProfession scarpetProfession) {
                scarpetProfession.addTrait(id);
                source.sendFeedback(successText("taterzens.command.trait.add", id), false);
            } else {
                source.sendError(errorText("taterzens.profession.lacking", ScarpetProfession.ID.toString()));
            }
        });
    }

    private static CompletableFuture<Suggestions> suggestRemovableTraits(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        HashSet<Value> traits = new HashSet<>();
        try {
            TaterzenNPC taterzen = ((ITaterzenEditor) ctx.getSource().getPlayerOrThrow()).getNpc();
            if(taterzen != null && taterzen.getProfession(ScarpetProfession.ID) instanceof ScarpetProfession scarpetProfession) {
                traits = scarpetProfession.getTraits();
            }
        } catch(CommandSyntaxException ignored) { }
        return CommandSource.suggestMatching(traits.stream().map(Value::getPrettyString), builder);
    }
}
