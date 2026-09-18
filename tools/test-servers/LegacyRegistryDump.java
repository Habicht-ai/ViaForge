import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import com.google.gson.*;

/** Reads registries from the original 1.9-1.12.2 server, without starting a world. */
public final class LegacyRegistryDump {
    public static void main(String[] args) throws Exception {
        Class<?> bootstrap = Class.forName(args[0]);
        for (Method m : bootstrap.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())
                    && m.getReturnType() == void.class && m.getParameterTypes().length == 0) m.invoke(null);
        }
        JsonObject out = new JsonObject();
        out.add("blocks", dump(args[1], true));
        out.add("items", dump(args[2], false));
        Class<?> living = Class.forName(args[4]);
        JsonArray mobs = new JsonArray();
        for (Map.Entry<String, Object> e : registry(Class.forName(args[3])).entrySet()) {
            if (e.getValue() instanceof Class && living.isAssignableFrom((Class<?>) e.getValue())
                    && !Modifier.isAbstract(((Class<?>)e.getValue()).getModifiers())) mobs.add(new JsonPrimitive(e.getKey()));
        }
        out.add("mobs", mobs);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(args[5]), "UTF-8")) {
            new GsonBuilder().setPrettyPrinting().create().toJson(out, w);
        }
    }

    private static JsonArray dump(String cls, boolean blocks) throws Exception {
        Class<?> type = Class.forName(cls);
        JsonArray rows = new JsonArray();
        Method fromMeta = null, toMeta = null;
        if (blocks) {
            for (Method m : type.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers()) || m.getParameterTypes().length != 1) continue;
                if (m.getParameterTypes()[0] == int.class && m.getReturnType().isInterface()) fromMeta = m;
            }
            if (fromMeta == null) throw new IllegalStateException("No block metadata decoder");
            int bestScore = -1;
            for (Method m : type.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers()) && m.getReturnType() == int.class
                        && Arrays.equals(m.getParameterTypes(), new Class<?>[]{fromMeta.getReturnType()})) {
                    int score = 0;
                    for (Object block : registry(type).values()) for (int i = 0; i < 16; i++) {
                        try {
                            Object state = fromMeta.invoke(block, i);
                            int encoded = (Integer)m.invoke(block, state);
                            if (encoded >= 0 && encoded < 16 && state.equals(fromMeta.invoke(block, encoded))) score++;
                        } catch (InvocationTargetException invalid) { }
                    }
                    if (score > bestScore) { bestScore = score; toMeta = m; }
                }
            }
            if (toMeta == null) throw new IllegalStateException("No block metadata encoder");
        }
        for (Map.Entry<String, Object> e : registry(type).entrySet()) {
            JsonObject row = new JsonObject(); row.addProperty("name", e.getKey());
            JsonArray variants = new JsonArray(); Set<Integer> used = new TreeSet<Integer>();
            if (blocks) {
                for (int i = 0; i < 16; i++) {
                    try { used.add((Integer) toMeta.invoke(e.getValue(), fromMeta.invoke(e.getValue(), i))); }
                    catch (InvocationTargetException invalidMeta) { /* Not a valid state of this block. */ }
                }
            } else used.add(0);
            for (Integer i : used) variants.add(new JsonPrimitive(i));
            row.add("metadata", variants); rows.add(row);
        }
        return rows;
    }

    private static Map<String, Object> registry(Class<?> type) throws Exception {
        for (Field f : type.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true); Object value = f.get(null);
            Map<String, Object> entries = new TreeMap<String, Object>();
            // Before 1.11 EntityList uses a String -> Class map instead of a registry.
            if (value instanceof Map) {
                for (Object entry : ((Map<?, ?>) value).entrySet()) {
                    Map.Entry<?, ?> e = (Map.Entry<?, ?>) entry;
                    if (e.getKey() instanceof String && e.getValue() instanceof Class) entries.put((String)e.getKey(), e.getValue());
                }
            } else if (value instanceof Iterable) {
                Method key;
                try { key = value.getClass().getMethod("b", Object.class); }
                catch (NoSuchMethodException notRegistry) { continue; }
                for (Object obj : (Iterable<?>) value) {
                    Object name = key.invoke(value, obj);
                    if (name != null && name.toString().startsWith("minecraft:")) entries.put(name.toString(), obj);
                }
            }
            if (entries.size() > 20) return entries;
        }
        throw new IllegalStateException("Registry not found: " + type);
    }
}
