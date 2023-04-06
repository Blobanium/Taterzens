package org.samo_lego.taterzens.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import java.util.Arrays;

import static org.samo_lego.taterzens.Taterzens.lang;
import static org.samo_lego.taterzens.compatibility.ModDiscovery.SERVER_TRANSLATIONS_LOADED;

public class TextUtil {

    /**
     * Inserts colored insertedText in string message.
     */
    public static MutableText joinText(String key, Formatting messageColor, Formatting insertedTextColor, String... insertedString) {
        Object[] texts = Arrays.stream(insertedString).map(s -> Text.literal(s).formatted(insertedTextColor)).toArray();
        return translate(key, texts).copyContentOnly().formatted(messageColor);
    }

    public static MutableText successText(String key, String... insertedText) {
        return joinText(key, Formatting.GREEN, Formatting.YELLOW, insertedText);
    }

    public static MutableText errorText(String key, String... insertedText) {
        return joinText(key, Formatting.RED, Formatting.LIGHT_PURPLE, insertedText);
    }

    /**
     * Converts {@link Text} to {@link NbtElement}.
     * @param text text to convert.
     * @return NbtElement generated from text.
     */
    public static NbtElement toNbtElement(Text text) {
        JsonElement json = JsonParser.parseString(Text.Serializer.toJson(text));
        return JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, json);
    }

    /**
     * Creates a {@link MutableText} from {@link NbtElement}.
     * @param textNbtElement text nbt to convert to text
     * @return mutable text object..
     */
    public static MutableText fromNbtElement(NbtElement textNbtElement) {
        JsonElement json = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, textNbtElement);
        return Text.Serializer.fromJson(json);
    }

    /**
     * Gets the text for the given language key.
     *
     * @param key lang key.
     * @return {@link TranslatableTextContent} or {@link LiteralTextContent} depending on whether SERVER_TRANSLATIONS is loaded.
     */
    public static MutableText translate(String key, Object... args) {
        if(SERVER_TRANSLATIONS_LOADED) {
            return Text.translatable(key, args);
        }

        String translation;
        if(lang.has(key))
            translation = lang.get(key).getAsString();
        else
            translation = key;
        return Text.translatable(translation, args);
    }

}
