package org.samo_lego.taterzens.commands.edit;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.commands.NpcCommand;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.util.TextUtil.errorText;
import static org.samo_lego.taterzens.util.TextUtil.successText;
import static org.samo_lego.taterzens.util.TextUtil.translate;

public class SoundCommand {

    public static void registerNode(LiteralCommandNode<ServerCommandSource> editNode)
    {
        LiteralCommandNode<ServerCommandSource> soundNode = literal("sounds")
            .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.sounds", config.perms.npcCommandPermissionLevel))
            .then(literal("list")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzen.npc.edit.sounds.list", config.perms.npcCommandPermissionLevel))
                .then(literal("all")
                    .executes(SoundCommand::listAllSounds)
                )
                .then(literal("ambient")
                    .executes(SoundCommand::listAmbientSounds)
                )
                .then(literal("hurt")
                    .executes(SoundCommand::listHurtSounds)
                )
                .then(literal("death")
                    .executes(SoundCommand::listDeathSounds)
                )
            )
            .then(literal("add")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzen.npc.edit.sounds.add", config.perms.npcCommandPermissionLevel))
                    .then(literal("ambient")
                        .then(argument("ambientSound", IdentifierArgumentType.identifier())
                            .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                            .executes(SoundCommand::addAmbientSound)
                        )
                    )
                    .then(literal("hurt")
                        .then(argument("hurtSound", IdentifierArgumentType.identifier())
                            .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                            .executes(SoundCommand::addHurtSound)
                        )
                    )
                    .then(literal("death")
                        .then(argument("deathSound", IdentifierArgumentType.identifier())
                            .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                            .executes(SoundCommand::addDeathSound)
                        )
                    )
            )
            .then(literal("remove")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzen.npc.edit.sounds.remove", config.perms.npcCommandPermissionLevel))
                .then(literal("all")
                    .executes(SoundCommand::removeAllSounds)
                )
                .then(literal("ambient")
                    .then(literal("all")
                        .executes(SoundCommand::removeAllAmbientSounds)
                    )
                    .then(literal("index")
                        .then(argument("index", IntegerArgumentType.integer(1))
                            .suggests((context, builder) -> CommandSource.suggestMatching(getAvailableAmbientSoundIndices(context), builder))
                            .executes(SoundCommand::removeAmbientSoundByIndex)
                        )
                    )
                    .then(literal("resource")
                        .then(argument("resource", IdentifierArgumentType.identifier())
                            .suggests((context, builder) -> CommandSource.suggestMatching(getAvailableAmbientSounds(context), builder))
                            .executes(SoundCommand::removeAmbientSoundByResource)
                        )
                    )
                )
                .then(literal("hurt")
                    .then(literal("all")
                        .executes(SoundCommand::removeAllHurtSounds)
                    )
                    .then(literal("index")
                        .then(argument("index", IntegerArgumentType.integer(1))
                            .suggests((context, builder) -> CommandSource.suggestMatching(getAvailableHurtSoundIndices(context), builder))
                            .executes(SoundCommand::removeHurtSoundByIndex)
                        )
                    )
                    .then(literal("resource")
                        .then(argument("resource", IdentifierArgumentType.identifier())
                            .suggests((context, builder) -> CommandSource.suggestMatching(getAvailableHurtSounds(context), builder))
                            .executes(SoundCommand::removeHurtSoundByResource)
                        )
                    )
                )
                .then(literal("death")
                    .then(literal("all")
                        .executes(SoundCommand::removeAllDeathSounds)
                    )
                    .then(literal("index")
                        .then(argument("index", IntegerArgumentType.integer(1))
                            .suggests((context, builder) -> CommandSource.suggestMatching(getAvailableDeathSoundIndices(context), builder))
                            .executes(SoundCommand::removeDeathSoundByIndex)
                        )
                    )
                    .then(literal("resource")
                        .then(argument("resource", IdentifierArgumentType.identifier())
                            .suggests((context, builder) -> CommandSource.suggestMatching(getAvailableDeathSounds(context), builder))
                            .executes(SoundCommand::removeDeathSoundByResource)
                        )
                    )
                )
            )
            .build()
        ;
        editNode.addChild(soundNode);
    }

    private static String[] getAvailableAmbientSoundIndices(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        AtomicReference<ArrayList<String>> ambientSounds = new AtomicReference<>();

        int result = NpcCommand.selectedTaterzenExecutor(player,
            taterzen -> ambientSounds.set(taterzen.getAmbientSoundData()));

        if (result == 1)
        {
            String[] availableIndices = new String[ambientSounds.get().size()];
            for (int i = 0; i < ambientSounds.get().size(); i++) {
                availableIndices[i] = Integer.toString(i + 1);
            }
            return availableIndices;
        }
        else
        {
            return new String[0];
        }
    }

    private static String[] getAvailableHurtSoundIndices(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        AtomicReference<ArrayList<String>> hurtSounds = new AtomicReference<>();

        int result = NpcCommand.selectedTaterzenExecutor(player,
            taterzen -> hurtSounds.set(taterzen.getHurtSoundData()));

        if (result == 1)
        {
            String[] availableIndices = new String[hurtSounds.get().size()];
            for (int i = 0; i < hurtSounds.get().size(); i++) {
                availableIndices[i] = Integer.toString(i + 1);
            }
            return availableIndices;
        }
        else
        {
            return new String[0];
        }
    }

    private static String[] getAvailableDeathSoundIndices(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        AtomicReference<ArrayList<String>> deathSounds = new AtomicReference<>();

        int result = NpcCommand.selectedTaterzenExecutor(player,
            taterzen -> deathSounds.set(taterzen.getDeathSoundData()));

        if (result == 1)
        {
            String[] availableIndices = new String[deathSounds.get().size()];
            for (int i = 0; i < deathSounds.get().size(); i++) {
                availableIndices[i] = Integer.toString(i + 1);
            }
            return availableIndices;
        }
        else
        {
            return new String[0];
        }
    }

    private static String[] getAvailableAmbientSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        AtomicReference<ArrayList<String>> ambientSounds = new AtomicReference<>();

        NpcCommand.selectedTaterzenExecutor(player,
            taterzen -> ambientSounds.set(taterzen.getAmbientSoundData()));

        return ambientSounds.get().toArray(new String[0]);
    }

    private static String[] getAvailableHurtSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        AtomicReference<ArrayList<String>> hurtSounds = new AtomicReference<>();

        NpcCommand.selectedTaterzenExecutor(player,
            taterzen -> hurtSounds.set(taterzen.getHurtSoundData()));

        return hurtSounds.get().toArray(new String[0]);
    }

    private static String[] getAvailableDeathSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        AtomicReference<ArrayList<String>> deathSounds = new AtomicReference<>();

        NpcCommand.selectedTaterzenExecutor(player,
            taterzen -> deathSounds.set(taterzen.getDeathSoundData()));

        return deathSounds.get().toArray(new String[0]);
    }
    private static int removeAllSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return removeAllAmbientSounds(context) + removeAllHurtSounds(context) + removeAllDeathSounds(context) == 3 ? 1 : 0;
    }

    private static int removeAllAmbientSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        int result = NpcCommand.selectedTaterzenExecutor(player,
            taterzen -> taterzen.setAmbientSoundData(new ArrayList<>()));

        if (result == 1) {
            source.sendFeedback(successText("taterzens.command.sounds.remove.all.success",
                translate("taterzens.command.sounds.ambient").getString()), false);
        }
        else {
            source.sendError(errorText("taterzens.command.sounds.remove.all.failure",
                translate("taterzens.command.sounds.ambient").getString()));
        }

        return result;
    }

    private static int removeAmbientSoundByIndex(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        int idx = IntegerArgumentType.getInteger(context, "index") - 1;
        AtomicReference<String> soundResource = new AtomicReference<>("");

        AtomicBoolean success = new AtomicBoolean(false);
        NpcCommand.selectedTaterzenExecutor(player, taterzen -> {
            try
            {
                if (!taterzen.getAmbientSoundData().isEmpty())
                {
                    soundResource.set(taterzen.getAmbientSoundData().get(idx));
                    taterzen.removeAmbientSound(idx);
                    source.sendFeedback(successText("taterzens.command.sounds.remove.success", soundResource.get()), false);
                }
                else
                {
                    source.sendFeedback(successText("taterzens.command.sounds.list.empty"), false);
                }

                success.set(true);
            }
            catch (IndexOutOfBoundsException err)
            {
                source.sendError(errorText("taterzens.command.sounds.remove.outofbounds",
                    Integer.toString(idx + 1),
                    "1",
                    Integer.toString(taterzen.getAmbientSoundData().size()))
                );
                success.set(false);
            }
        });

        if (success.get())
        {
            return 1;
        }
        else
        {
            source.sendError(errorText("taterzens.command.sounds.remove.failure", soundResource.get()));
            return 0;
        }
    }

    private static int removeAmbientSoundByResource(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        //String resource = StringArgumentType.getString(context, "resource");
        String resource = IdentifierArgumentType.getIdentifier(context, "resource").toString();

        AtomicReference<ArrayList<String>> soundData = new AtomicReference<>();
        AtomicBoolean success = new AtomicBoolean(false);

        NpcCommand.selectedTaterzenExecutor(player, taterzen -> {

            soundData.set(taterzen.getAmbientSoundData());

            int idx = indexOfStringInArrayList(resource, soundData.get());
            if (idx >= 0)
            {
                taterzen.removeAmbientSound(idx);
                source.sendFeedback(successText("taterzens.command.sounds.remove.success", resource), false);
                success.set(true);
            }
            else
            {
                source.sendError(errorText("taterzens.command.sounds.404",
                    resource,
                    translate("taterzens.command.sounds.ambient").getString())
                );
                success.set(false);
            }
        });

        if (success.get())
        {
            return 1;
        }
        else {
            source.sendError(errorText("taterzens.command.sounds.remove.failure", resource));
            return 0;
        }
    }

    private static int removeAllHurtSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        int result = NpcCommand.selectedTaterzenExecutor(player,
            taterzen -> taterzen.setHurtSoundData(new ArrayList<>()));

        if (result == 1) {
            source.sendFeedback(successText("taterzens.command.sounds.remove.all.success",
                    translate("taterzens.command.sounds.hurt").getString()), false);
        }
        else {
            source.sendError(errorText("taterzens.command.sounds.remove.all.failure",
                    translate("taterzens.command.sounds.hurt").getString()));
        }

        return result;
    }

    private static int removeHurtSoundByIndex(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        int idx = IntegerArgumentType.getInteger(context, "index") - 1;
        AtomicReference<String> soundResource = new AtomicReference<>("");

        AtomicBoolean success = new AtomicBoolean(false);
        NpcCommand.selectedTaterzenExecutor(player, taterzen -> {
            try
            {
                if (!taterzen.getHurtSoundData().isEmpty())
                {
                    soundResource.set(taterzen.getHurtSoundData().get(idx));
                    taterzen.removeHurtSound(idx);
                    source.sendFeedback(successText("taterzens.command.sounds.remove.success", soundResource.get()), false);
                }
                else
                {
                    source.sendFeedback(successText("taterzens.command.sounds.list.empty"), false);
                }

                success.set(true);
            }
            catch (IndexOutOfBoundsException err)
            {
                source.sendError(errorText("taterzens.command.sounds.remove.outofbounds",
                        Integer.toString(idx + 1),
                        "1",
                        Integer.toString(taterzen.getHurtSoundData().size()))
                );
                success.set(false);
            }
        });

        if (success.get())
        {
            return 1;
        }
        else
        {
            source.sendError(errorText("taterzens.command.sounds.remove.failure", soundResource.get()));
            return 0;
        }
    }

    private static int removeHurtSoundByResource(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        String resource = IdentifierArgumentType.getIdentifier(context, "resource").toString();

        AtomicReference<ArrayList<String>> soundData = new AtomicReference<>();
        AtomicBoolean success = new AtomicBoolean(false);

        NpcCommand.selectedTaterzenExecutor(player, taterzen -> {

            soundData.set(taterzen.getHurtSoundData());

            int idx = indexOfStringInArrayList(resource, soundData.get());
            if (idx >= 0)
            {
                taterzen.removeHurtSound(idx);
                source.sendFeedback(successText("taterzens.command.sounds.remove.success", resource), false);
                success.set(true);
            }
            else
            {
                source.sendError(errorText("taterzens.command.sounds.404",
                        resource,
                        translate("taterzens.command.sounds.hurt").getString())
                );
                success.set(false);
            }
        });

        if (success.get())
        {
            return 1;
        }
        else {
            source.sendError(errorText("taterzens.command.sounds.remove.failure", resource));
            return 0;
        }
    }

    private static int removeAllDeathSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        int result = NpcCommand.selectedTaterzenExecutor(player,
            taterzen -> taterzen.setDeathSoundData(new ArrayList<>()));

        if (result == 1) {
            source.sendFeedback(successText("taterzens.command.sounds.remove.all.success",
                    translate("taterzens.command.sounds.death").getString()), false);
        }
        else {
            source.sendError(errorText("taterzens.command.sounds.remove.all.failure",
                    translate("taterzens.command.sounds.death").getString()));
        }

        return result;
    }

    private static int removeDeathSoundByIndex(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        int idx = IntegerArgumentType.getInteger(context, "index") - 1;
        AtomicReference<String> soundResource = new AtomicReference<>("");

        AtomicBoolean success = new AtomicBoolean(false);
        NpcCommand.selectedTaterzenExecutor(player, taterzen -> {
            try
            {
                if (!taterzen.getDeathSoundData().isEmpty())
                {
                    soundResource.set(taterzen.getDeathSoundData().get(idx));
                    taterzen.removeDeathSound(idx);
                    source.sendFeedback(successText("taterzens.command.sounds.remove.success", soundResource.get()), false);
                }
                else
                {
                    source.sendFeedback(successText("taterzens.command.sounds.list.empty"), false);
                }

                success.set(true);
            }
            catch (IndexOutOfBoundsException err)
            {
                source.sendError(errorText("taterzens.command.sounds.remove.outofbounds",
                        Integer.toString(idx + 1),
                        "1",
                        Integer.toString(taterzen.getDeathSoundData().size()))
                );
                success.set(false);
            }
        });

        if (success.get())
        {
            return 1;
        }
        else
        {
            source.sendError(errorText("taterzens.command.sounds.remove.failure", soundResource.get()));
            return 0;
        }
    }

    private static int removeDeathSoundByResource(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        String resource = IdentifierArgumentType.getIdentifier(context, "resource").toString();

        AtomicReference<ArrayList<String>> soundData = new AtomicReference<>();
        AtomicBoolean success = new AtomicBoolean(false);

        NpcCommand.selectedTaterzenExecutor(player, taterzen -> {

            soundData.set(taterzen.getDeathSoundData());

            int idx = indexOfStringInArrayList(resource, soundData.get());
            if (idx >= 0)
            {
                taterzen.removeDeathSound(idx);
                source.sendFeedback(successText("taterzens.command.sounds.remove.success", resource), false);
                success.set(true);
            }
            else
            {
                source.sendError(errorText("taterzens.command.sounds.404",
                        resource,
                        translate("taterzens.command.sounds.death").getString())
                );
                success.set(false);
            }
        });

        if (success.get())
        {
            return 1;
        }
        else {
            source.sendError(errorText("taterzens.command.sounds.remove.failure", resource));
            return 0;
        }
    }

    private static int addAmbientSound(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        String soundResource = IdentifierArgumentType.getIdentifier(context, "ambientSound").toString();
        if (!Identifier.isValid(soundResource))
        {
            source.sendError(errorText("taterzens.command.sounds.invalid"));
            return 0;
        }
        else
        {
            int result = NpcCommand.selectedTaterzenExecutor(player,
                taterzen -> taterzen.addAmbientSound(soundResource));

            if (result == 1) {
                source.sendFeedback(successText("taterzens.command.sounds.add.success", soundResource), false);
            }
            else {// if 0 or anything else
                source.sendError(errorText("taterzens.command.sounds.add.failure", soundResource));
            }

            return result;
        }
    }

    private static int addHurtSound(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        String soundResource = IdentifierArgumentType.getIdentifier(context, "hurtSound").toString();
        if (!Identifier.isValid(soundResource))
        {
            source.sendError(errorText("taterzens.command.sounds.invalid"));
            return 0;
        }
        else
        {
            int result = NpcCommand.selectedTaterzenExecutor(player,
                taterzen -> taterzen.addHurtSound(soundResource));

            if (result == 1) {
                source.sendFeedback(successText("taterzens.command.sounds.add.success", soundResource), false);
            }
            else { // if 0 or anything else
                source.sendError(errorText("taterzens.command.sounds.add.failure", soundResource));
            }

            return result;
        }
    }

    private static int addDeathSound(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        String soundResource = IdentifierArgumentType.getIdentifier(context, "deathSound").toString();
        if (!Identifier.isValid(soundResource))
        {
            source.sendError(errorText("taterzens.command.sounds.invalid"));
            return 0;
        }
        else
        {
            int result = NpcCommand.selectedTaterzenExecutor(player,
                taterzen -> taterzen.addDeathSound(soundResource));

            if (result == 1) {
                source.sendFeedback(successText("taterzens.command.sounds.add.success", soundResource), false);
            }
            else { // if 0 or anything else
                source.sendError(errorText("taterzens.command.sounds.add.failure", soundResource));
            }

            return result;
        }
    }

    private static int listAllSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        return listAmbientSounds(context) + listHurtSounds(context) + listDeathSounds(context) == 3 ? 1 : 0;
    }
    
    private static int listAmbientSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        MutableText response = translate("taterzens.command.sounds.list.ambient").formatted(Formatting.AQUA);

        return NpcCommand.selectedTaterzenExecutor(player, taterzen -> {

            ArrayList<String> ambientSounds = taterzen.getAmbientSoundData();
            if (!ambientSounds.isEmpty()) {
                for (int i = 0; i < ambientSounds.size(); i++)
                {
                    int idx = i + 1;

                    response.append(
                            Text.literal("\n" + idx + ": " + ambientSounds.get(i))
                            .formatted(i % 2 == 0 ? Formatting.DARK_GREEN : Formatting.DARK_BLUE)
                    );
                }
            }
            else
            {
                response.append(
                        Text.literal(" " + translate("taterzens.command.sounds.list.empty").getString())
                            .formatted(Formatting.YELLOW)
                );
            }

            source.sendFeedback(response, false);
        });
    }

    private static int listHurtSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        MutableText response = translate("taterzens.command.sounds.list.hurt").formatted(Formatting.AQUA);

        return NpcCommand.selectedTaterzenExecutor(player, taterzen -> {

            ArrayList<String> hurtSounds = taterzen.getHurtSoundData();
            if (!hurtSounds.isEmpty()) {
                for (int i = 0; i < hurtSounds.size(); i++)
                {
                    int idx = i + 1;

                    response.append(
                            Text.literal("\n" + idx + ": " + hurtSounds.get(i))
                                    .formatted(i % 2 == 0 ? Formatting.DARK_GREEN : Formatting.DARK_BLUE)
                    );
                }
            }
            else
            {
                response.append(
                        Text.literal(" " + translate("taterzens.command.sounds.list.empty").getString())
                                .formatted(Formatting.YELLOW)
                );
            }

            source.sendFeedback(response, false);
        });
    }

    private static int listDeathSounds(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
    {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        MutableText response = translate("taterzens.command.sounds.list.death").formatted(Formatting.AQUA);

        return NpcCommand.selectedTaterzenExecutor(player, taterzen -> {

            ArrayList<String> deathSounds = taterzen.getDeathSoundData();
            if (!deathSounds.isEmpty()) {
                for (int i = 0; i < deathSounds.size(); i++)
                {
                    int idx = i + 1;

                    response.append(
                            Text.literal("\n" + idx + ": " + deathSounds.get(i))
                                    .formatted(i % 2 == 0 ? Formatting.DARK_GREEN : Formatting.DARK_BLUE)
                    );
                }
            }
            else
            {
                response.append(
                        Text.literal(" " + translate("taterzens.command.sounds.list.empty").getString())
                                .formatted(Formatting.YELLOW)
                );
            }

            source.sendFeedback(response, false);
        });
    }

    /** Searches a term in a given String ArrayList.
     * @param term The String which should be searched for.
     * @param list The ArrayList to be searched in.
     * @return -1 if the term was not found in the list. Otherwise, the index of the term within the list.
     * */
    private static int indexOfStringInArrayList(String term, ArrayList<String> list)
    {
        for (int c = 0; c < list.size(); c++)
        {
            if (term.equals(list.get(c)))
            {
                return c;
            }
        }
        return -1;
    }
}
