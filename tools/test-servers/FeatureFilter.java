package vflabinspect;

import java.io.*;
import java.lang.reflect.*;

/** Uses the original server's DEFAULT_FLAGS rather than guessing when experimental content ships. */
public final class FeatureFilter {
    public static void main(String[] args) throws Exception {
        Class.forName(args[0]).getMethod(args[1]).invoke(null);
        Class.forName(args[2]).getMethod(args[3]).invoke(null);
        Class<?> element = Class.forName(args[4]);
        Method required = element.getMethod(args[5]);
        Class<?> set = Class.forName(args[6]);
        Method subset = set.getMethod(args[7], set);
        Object defaults = Class.forName(args[8]).getField(args[9]).get(null);
        Method name = Class.forName(args[10]).getMethod(args[11], Object.class);
        Class<?> registries = Class.forName(args[12]);
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(args[16]), "UTF-8"))) {
            String[] kinds = {"blocks", "items", "mobs"};
            for (int i = 0; i < 3; i++) {
                Object registry = registries.getField(args[13 + i]).get(null);
                for (Object entry : (Iterable<?>) registry) {
                    if (element.isInstance(entry) && !(Boolean)subset.invoke(required.invoke(entry), defaults)) {
                        out.println(kinds[i] + "\t" + name.invoke(registry, entry));
                    }
                }
            }
        }
    }
}
