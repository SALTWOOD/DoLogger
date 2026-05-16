package top.saltwood.dologger.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import top.saltwood.dologger.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LanguageResolver {

    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final String LANGUAGE_PATH = "/assets/dologger/lang/%s.json";
    private static final Gson GSON = new Gson();

    private static String loadedLanguage;
    private static Map<String, String> translations = Map.of();

    private LanguageResolver() {
    }

    public static MutableComponent component(String key, Object... args) {
        return Component.literal(resolve(key, args));
    }

    public static String resolve(String key, Object... args) {
        ensureLoaded();
        String pattern = translations.getOrDefault(key, key);
        if (args.length == 0) {
            return pattern;
        }
        Object[] readableArgs = new Object[args.length];
        for (int index = 0; index < args.length; index++) {
            Object arg = args[index];
            readableArgs[index] = arg instanceof Component component ? component.getString() : arg;
        }
        try {
            return String.format(Locale.ROOT, pattern, readableArgs);
        } catch (IllegalArgumentException exception) {
            return pattern + " " + String.join(" ", toStrings(readableArgs));
        }
    }

    public static void reload() {
        loadedLanguage = null;
        ensureLoaded();
    }

    private static synchronized void ensureLoaded() {
        String language = normalizeLanguage(Config.language);
        if (language.equals(loadedLanguage)) {
            return;
        }

        Map<String, String> loaded = new HashMap<>(load(DEFAULT_LANGUAGE));
        if (!DEFAULT_LANGUAGE.equals(language)) {
            loaded.putAll(load(language));
        }
        translations = Map.copyOf(loaded);
        loadedLanguage = language;
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        return language.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static Map<String, String> load(String language) {
        String path = LANGUAGE_PATH.formatted(language);
        try (InputStream stream = LanguageResolver.class.getResourceAsStream(path)) {
            if (stream == null) {
                return Map.of();
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, String> values = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    JsonElement value = entry.getValue();
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                        values.put(entry.getKey(), GSON.fromJson(value, String.class));
                    }
                }
                return values;
            }
        } catch (IOException | IllegalStateException exception) {
            return Map.of();
        }
    }

    private static String[] toStrings(Object[] args) {
        String[] strings = new String[args.length];
        for (int index = 0; index < args.length; index++) {
            strings[index] = String.valueOf(args[index]);
        }
        return strings;
    }
}
