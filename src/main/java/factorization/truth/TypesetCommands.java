package factorization.truth;

import factorization.truth.api.ITypesetCommand;

import java.util.HashMap;

public class TypesetCommands {
    public static final HashMap<String, ITypesetCommand> client = new HashMap<String, ITypesetCommand>();
    public static final HashMap<String, ITypesetCommand> html = new HashMap<String, ITypesetCommand>();

    private static <T> void put(HashMap<String, T> map, String name, T cmd) {
        if (!name.startsWith("\\")) {
            name = "\\" + name;
        }
        if (map.put(name, cmd) != null) throw new IllegalArgumentException("name conflict: " + name);
    }

    public static void registerClient(String name, ITypesetCommand cmd) {
        put(client, name, cmd);
    }

    public static void registerHtml(String name, ITypesetCommand cmd) {
        put(html, name, cmd);
    }

    public static void register(String name, ITypesetCommand cmd) {
        put(client, name, cmd);
        put(client, name, cmd);
    }

}
