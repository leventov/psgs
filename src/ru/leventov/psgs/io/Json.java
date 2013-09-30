package ru.leventov.psgs.io;

import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.ByteOrder;
import java.nio.file.Path;

public final class Json {
    public static void writeJson(Path file, Object object) throws IOException {
        try (PrintWriter writer = new PrintWriter(file.toFile(), "UTF-8")) {
            writer.println(Json.GSON.toJson(object));
        }
    }

    public static <T> T readJson(Path file, Class<T> type) throws IOException {
        try (InputStreamReader metadataReader = new InputStreamReader(new FileInputStream(file.toFile()), "UTF-8")) {
            return Json.GSON.fromJson(metadataReader, type);
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(ByteOrder.class, new ByteOrderSerializer()).create();

    private static class ByteOrderSerializer
            implements JsonSerializer<ByteOrder>, JsonDeserializer<ByteOrder>, InstanceCreator<ByteOrder> {

        @Override
        public ByteOrder createInstance(Type type) {
            return ByteOrder.nativeOrder();
        }

        @Override
        public ByteOrder deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String s = json.getAsString();
            if (s.equals(ByteOrder.LITTLE_ENDIAN.toString())) {
                return ByteOrder.LITTLE_ENDIAN;
            }
            else if (s.equals(ByteOrder.BIG_ENDIAN.toString())) {
                return ByteOrder.BIG_ENDIAN;
            } else {
                throw new JsonParseException("Unknown ByteOrder");
            }
        }

        @Override
        public JsonElement serialize(ByteOrder src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    private Json() {}
}
