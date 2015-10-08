package factorization.scrap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

@Help({"Runs a script",
        "Script extra # runs scraps/extra.scrap"})
public class Script implements IRevertible {
    static final Logger log = ScrapManager.log;

    final ArrayList<IRevertible> actions = new ArrayList<IRevertible>();
    final String filename;
    final String simpleName;
    final File scriptFile;

    static ThreadLocal<HashSet<String>> localActive = new ThreadLocal();

    private static HashSet<String> getLocalActive() {
        HashSet<String> ret = localActive.get();
        if (ret == null) {
            localActive.set(ret = new HashSet<String>());
        }
        return ret;
    }

    public Script(Scanner in) {
        this(in.next(), false);
    }

    public Script(String simpleName, boolean createIfMissing) {
        this.simpleName = simpleName;
        for (String b : new String[] { ".", "\\", ":" }) {
            if (simpleName.contains(b) || simpleName.startsWith("/")) {
                throw new CompileError("Not running script with strange name");
            }
        }
        this.filename = "scraps/" + simpleName + ".scrap";
        final HashSet<String> active = getLocalActive();
        if (active.contains(simpleName)) {
            throw new CompileError("Already compiling: " + simpleName);
        }
        scriptFile = new File(this.filename);
        tryCreate(createIfMissing);
        active.add(simpleName);
        try {
            read();
        } catch (IOException e) {
            throw new CompileError("Error reading script", e);
        } finally {
            active.remove(simpleName);
        }
    }

    void tryCreate(boolean createIfMissing) {
        if (scriptFile.exists()) return;
        if (!createIfMissing) throw new CompileError("File does not exist: " + scriptFile);
        //noinspection ResultOfMethodCallIgnored
        new File(scriptFile.getParent()).mkdirs();
        try {
            if (!scriptFile.createNewFile()) throw new IOException("Could not create " + filename + " file");
        } catch (IOException e) {
            log.log(Level.WARN, "File creation failed", e);
        }
        return;
    }

    private void read() throws IOException {
        log.log(Level.INFO, "Running script: " + filename);
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(scriptFile)));
        int lineNumber = 0;
        while (true) {
            lineNumber++;
            String line = in.readLine();
            if (line == null) break;
            line = line.replaceFirst(" *", "");
            if (line.startsWith("#")) continue;
            if (line.isEmpty()) continue;
            try {
                actions.add(ScrapManager.compile(line));
            } catch (CompileError error) {
                error.addTrace(filename, lineNumber, line);
                throw error;
            }
        }
        log.log(Level.INFO, "Script completed");
    }

    @Override
    public void apply() {
        for (IRevertible action : actions) {
            action.apply();
        }
    }

    @Override
    public void revert() {
        for (int i = actions.size() - 1; i >= 0; i--) {
            IRevertible action = actions.get(i);
            action.revert();
        }
    }

    @Override
    public String info() {
        return "script " + simpleName + " # containing " + actions.size() + " actions";
    }
}
