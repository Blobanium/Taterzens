package org.samo_lego.taterzens.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.samo_lego.config2brigadier.util.TranslatedText;
import org.samo_lego.taterzens.Taterzens;
import org.samo_lego.taterzens.util.LanguageUtil;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.samo_lego.taterzens.Taterzens.MOD_ID;
import static org.samo_lego.taterzens.Taterzens.config;
import static org.samo_lego.taterzens.compatibility.ModDiscovery.SERVER_TRANSLATIONS_LOADED;
import static org.samo_lego.taterzens.util.LanguageUtil.LANG_LIST;
import static org.samo_lego.taterzens.util.TextUtil.errorText;
import static org.samo_lego.taterzens.util.TextUtil.successText;
import static org.samo_lego.taterzens.util.TextUtil.translate;

public class TaterzensCommand {
    private static final SuggestionProvider<ServerCommandSource> AVAILABLE_LANGUAGES;

    private static final String DOCS_URL = "https://samolego.github.io/Taterzens/";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // root node
        LiteralCommandNode<ServerCommandSource> taterzensNode = dispatcher.register(literal("taterzens")
                .then(literal("wiki")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.wiki_info", config.perms.taterzensCommandPermissionLevel))
                        .executes(TaterzensCommand::wikiInfo)
                )
        );

        // config node
        LiteralCommandNode<ServerCommandSource> configNode = literal("config")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.config", config.perms.taterzensCommandPermissionLevel))
                .then(literal("reload")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.config.reload", config.perms.taterzensCommandPermissionLevel))
                        .executes(TaterzensCommand::reloadConfig)
                )
                .build();

        LiteralCommandNode<ServerCommandSource> editNode = literal("edit")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.config.edit", config.perms.taterzensCommandPermissionLevel))
                .then(literal("language")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.config.edit.lang", config.perms.taterzensCommandPermissionLevel))
                        .then(argument("language", word())
                                .suggests(AVAILABLE_LANGUAGES)
                                .executes(TaterzensCommand::setLang)
                        )
                )
                .build();


        // Register config edit commands
        config.generateCommand(editNode);

        // Register nodes
        configNode.addChild(editNode);
        taterzensNode.addChild(configNode);
    }

    private static int setLang(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String language = StringArgumentType.getString(context, "language");

        // Set language for config editing system
        TranslatedText.setLang(language);

        if(LANG_LIST.contains(language)) {
            config.language = language;
            config.save();

            LanguageUtil.setupLanguage();
            source.sendFeedback(successText("taterzens.command.language.success", language), false);
            if(SERVER_TRANSLATIONS_LOADED) {
                source.sendFeedback(successText("taterzens.command.language.server_translations_hint.1"), false);
                source.sendFeedback(successText("taterzens.command.language.server_translations_hint.2"), false);
            }

        } else {
            String url = "https://github.com/samolego/Taterzens#translation-contributions";
            source.sendError(
                    errorText("taterzens.command.language.404", language, url)
                    .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                    )
            );
        }

        return 1;
    }

    private static int wikiInfo(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(
                successText("taterzens.command.wiki", DOCS_URL)
                        .formatted(Formatting.GREEN)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, DOCS_URL))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate("taterzens.tooltip.see_docs")))
                        ),
                false
        );
        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        reloadConfig();

        context.getSource().sendFeedback(
                translate("taterzens.command.config.success").formatted(Formatting.GREEN),
                false
        );
        return 1;
    }

    /**
     * Reloads the config and language objects.
     */
    private static void reloadConfig() {
        config.reload();
        LanguageUtil.setupLanguage();
    }


    static {
        AVAILABLE_LANGUAGES = SuggestionProviders.register(
                new Identifier(MOD_ID, "languages"),
                (context, builder) ->
                        CommandSource.suggestMatching(LANG_LIST, builder)
        );
    }
}
