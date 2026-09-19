package vflab;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import com.google.gson.*;

/** Calls the original client's creative item enumerators; never derives items from block states. */
public final class CreativeInventoryDump {
    public static void main(String[] args) throws Exception {
        Class<?> bootstrap = Class.forName(args[0]);
        for (Method m : bootstrap.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())
                    && m.getReturnType() == void.class && m.getParameterTypes().length == 0) m.invoke(null);
        }
        Class<?> item = Class.forName(args[1]);
        Method enumerate = null;
        for (Method m : item.getDeclaredMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (m.getReturnType() == void.class && p.length >= 2 && p.length <= 3
                    && List.class.isAssignableFrom(p[p.length - 1])) enumerate = m;
        }
        if (enumerate == null) throw new IllegalStateException("Creative item enumerator missing");
        Class<?>[] params = enumerate.getParameterTypes();
        Class<?> tabs = params[params.length - 2];
        Method tabGetter = null;
        for (Method m : item.getDeclaredMethods()) {
            if (m.getParameterTypes().length == 0 && m.getReturnType() == tabs) tabGetter = m;
        }
        if (tabGetter == null) throw new IllegalStateException("Creative tab getter missing");
        Iterable<?> registry = null;
        for (Field f : item.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && Iterable.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true); registry = (Iterable<?>)f.get(null); break;
            }
        }
        if (registry == null) throw new IllegalStateException("Item registry missing");
        Set<String> seen = new HashSet<String>();
        JsonArray result = new JsonArray();
        for (Object value : registry) {
            Object tab = tabGetter.invoke(value);
            if (tab == null) continue; // Operator-only / internal items have no normal creative tab.
            List<?> stacks;
            Class<?> listType = params[params.length - 1];
            if (listType == List.class) stacks = new ArrayList<Object>();
            else {
                Constructor<?> ctor = listType.getDeclaredConstructor(); ctor.setAccessible(true);
                stacks = (List<?>)ctor.newInstance();
            }
            if (params.length == 3) enumerate.invoke(value, value, tab, stacks);
            else enumerate.invoke(value, tab, stacks);
            for (Object stack : stacks) {
                Method serialize = null;
                for (Method m : stack.getClass().getDeclaredMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 1 && p[0] == m.getReturnType() && p[0] != stack.getClass()
                            && !p[0].isPrimitive() && !Modifier.isStatic(m.getModifiers())) serialize = m;
                }
                if (serialize == null) throw new IllegalStateException("Stack serializer missing");
                Class<?> compound = serialize.getReturnType();
                Object nbt = serialize.invoke(stack, compound.getConstructor().newInstance());
                String snbt = nbt.toString();
                if (!seen.add(snbt)) continue;
                Method stringGetter = null, shortGetter = null;
                for (Method m : compound.getDeclaredMethods()) {
                    if (Arrays.equals(m.getParameterTypes(), new Class<?>[]{String.class})) {
                        if (m.getReturnType() == String.class && !Modifier.isStatic(m.getModifiers())) stringGetter = m;
                        if (m.getReturnType() == short.class) shortGetter = m;
                    }
                }
                JsonObject entry = new JsonObject();
                entry.addProperty("name", (String)stringGetter.invoke(nbt, "id"));
                entry.addProperty("metadata", (Short)shortGetter.invoke(nbt, "Damage"));
                entry.addProperty("snbt", snbt);
                result.add(entry);
            }
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(args[2]), "UTF-8")) {
            new GsonBuilder().setPrettyPrinting().create().toJson(result, w);
        }
        if (args.length >= 5) {
            Class<?> block = Class.forName(args[3]);
            Method getId = null;
            for (Method m : block.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() == int.class
                        && Arrays.equals(m.getParameterTypes(), new Class<?>[]{block})) getId = m;
            }
            if (getId == null) throw new IllegalStateException("Block ID getter missing");
            JsonObject ids = new JsonObject();
            for (Field f : block.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) || !Iterable.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true); Object reg = f.get(null);
                Method key = reg.getClass().getMethod("b", Object.class);
                for (Object value : (Iterable<?>)reg) ids.addProperty(key.invoke(reg, value).toString(), (Integer)getId.invoke(null, value));
                break;
            }
            if (ids.entrySet().size() < 100) throw new IllegalStateException("Block ID registry missing");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(args[4]), "UTF-8")) {
                new GsonBuilder().setPrettyPrinting().create().toJson(ids, w);
            }
        }
    }
}
