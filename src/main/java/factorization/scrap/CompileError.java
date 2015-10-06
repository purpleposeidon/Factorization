package factorization.scrap;

import com.google.common.base.Joiner;

import java.util.ArrayList;

public class CompileError extends RuntimeException {
    public CompileError(String msg) {
        super(msg);
    }

    public CompileError(String msg, Throwable cause) {
        super(msg, cause);
    }

    @Override
    public String getMessage() {
        String msg = super.getMessage();
        Throwable here = this;
        final Joiner joiner = Joiner.on("\n");
        while (here != null) {
            if (here instanceof CompileError) {
                CompileError err = (CompileError) here;
                msg += "\n" + joiner.join(err.traces);
            }
            here = here.getCause();
        }
        return msg;
    }

    final ArrayList<String> traces = new ArrayList<String>();
    //final ArrayList<StackTraceElement> traces = new ArrayList<StackTraceElement>();

    public void addTrace(String filename, int lineNumber, String src) {
        traces.add(filename + ":" + lineNumber + "  " + src);
        //traces.add(new StackTraceElement(filename.replace(".scrap", ""), "scrap", "[" + src + "]", lineNumber));
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return super.getStackTrace();
        /*StackTraceElement[] parent = super.getStackTrace();
        final StackTraceElement[] traceArray = traces.toArray(new StackTraceElement[traces.size()]);
        return ArrayUtils.addAll(traceArray, parent);*/
    }
}
