package util;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimedPrintStream extends PrintStream {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public TimedPrintStream(PrintStream original) {
        super(original);
    }

    @Override
    public void println(String x) {
        super.println("["+getTimestamp()+"] " + " " + x);
    }

    @Override
    public void println(Object x) {
        super.println("["+getTimestamp()+"] " + " " + x);
    }

    private String getTimestamp() {
        return dateFormat.format(new Date());
    }
}
