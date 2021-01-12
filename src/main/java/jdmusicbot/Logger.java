package jdmusicbot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;

public class Logger {

	/* ------------------------ STATIC ------------------------ */

	private static LinkedList<PrintStream>	out			= new LinkedList<PrintStream>();
	private static Calendar					cal			= new GregorianCalendar();
	private static Loglevel					loglevel	= Loglevel.DEBUG;

	private static Logger LOG = new Logger("Logger");

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> Logger.out.forEach(i -> i.close())));
		Logger.out.add(System.out);
	}

	/**
	 * Adds a printing stream to log to
	 *
	 * @param outStream
	 *            the PrintStream to log to
	 */
	public static void addPrintStream(PrintStream outStream) {
		if (outStream != null)
			synchronized (Logger.out) {
				Logger.out.add(outStream);
			}
	}

	/**
	 * Removes a printing stream from the internal list of output streams
	 *
	 * @param outStream
	 *            the PrintStream to remove
	 */
	public static void removePrintStream(PrintStream outStream) {
		if (outStream != null)
			synchronized (Logger.out) {
				Logger.out.remove(outStream);
			}
	}

	/**
	 * Adds a FileOutputStream for the given File to the list of output streams
	 *
	 * @param file
	 *            the file to log to
	 */
	public static void addLogFile(File file) {
		Logger.addLogFile(file, 3);
	}

	/**
	 * Adds a FileOutputStream for the given File to the list of output streams
	 *
	 * @param file
	 *            the file to log to
	 * @param keep
	 *            how many old logs to keep
	 */
	public static void addLogFile(File file, int keep) {
		// backing up old log files
		if (keep > 0) {
			String fs = file.getAbsolutePath() + ".";
			for (int i = keep - 1; i > 0; i--)
				try {
					if (new File(fs + (i - 1)).exists())
						Files.move(new File(fs + (i - 1)).toPath(), new File(fs + i).toPath(),
								StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					Logger.LOG.error("Failed to move backup logs", e);
				}
			if (file.exists())
				try {
					Files.move(file.toPath(), new File(fs + "0").toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					Logger.LOG.error("Failed to move backup logs", e);
				}
		}
		// setup current log file
		try {
			if (!file.exists()) { // create file if not exist
				File parent = file.getParentFile();
				if (parent != null)
					Files.createDirectories(parent.toPath());
				Files.createFile(file.toPath());
			}
			PrintStream stream = new PrintStream(new FileOutputStream(file));
			Logger.addPrintStream(stream);
		} catch (IOException e) {
			Logger.LOG.error("Failed to open file for writing", e);
		}
	}

	/**
	 * Sets the minimum logging level. Everything with a logging level lower than the minimum will not be logged
	 *
	 * @param loglevel
	 *            the minimum logging level
	 */
	public static void setLoglevel(Loglevel loglevel) {
		if (loglevel != null)
			Logger.loglevel = loglevel;
	}

	/**
	 * Flushes all underlying output streams
	 */
	public static void flush() {
		Logger.out.forEach(i -> i.flush());
	}

	/**
	 * Prints the given String to all underlying PrintStreams
	 *
	 * @param s
	 *            the tring to print
	 */
	private static void print(String s) {
		Logger.cal.setTimeInMillis(System.currentTimeMillis());
		String o = String.format("[%02d:%02d:%02d] %s", Logger.cal.get(Calendar.HOUR_OF_DAY),
				Logger.cal.get(Calendar.MINUTE), Logger.cal.get(Calendar.SECOND), s);
		synchronized (Logger.out) {
			Logger.out.forEach(i -> i.print(o));
		}
	}

	/**
	 * Prints the given String to all underlying Printstreams and appends '\n'
	 *
	 * @param s
	 *            the String to print
	 */
	private static void println(String s) {
		Logger.print(s + '\n');
	}

	/* ---------------------- END STATIC ---------------------- */

	private String name;

	/**
	 * Creates a new Logger instance with the given name
	 *
	 * @param name
	 *            the name of this Logger
	 */
	public Logger(String name) {
		this.name = name;
	}

	public void debug(Object msg) {
		this.debug(msg, null);
	}

	public void debug(Throwable t) {
		this.debug(null, t);
	}

	public void debug(Object msg, Throwable t) {
		if (Logger.loglevel.id <= Loglevel.DEBUG.id) {
			if (msg != null)
				Logger.println(String.format("[DEBUG] [%s] %s", this.name, msg));
			if (t != null) {
				StringWriter sw = new StringWriter();
				PrintWriter ps = new PrintWriter(sw, true);
				t.printStackTrace(ps);
				Logger.print(String.format("[DEBUG] [%s] %s", this.name, sw));
			}
		}
	}

	public void info(Object msg) {
		this.info(msg, null);
	}

	public void info(Throwable t) {
		this.info(null, t);
	}

	public void info(Object msg, Throwable t) {
		if (Logger.loglevel.id <= Loglevel.INFO.id) {
			if (msg != null)
				Logger.println(String.format("[INFO] [%s] %s", this.name, msg));
			if (t != null) {
				StringWriter sw = new StringWriter();
				PrintWriter ps = new PrintWriter(sw, true);
				t.printStackTrace(ps);
				Logger.print(String.format("[INFO] [%s] %s", this.name, sw));
			}
		}
	}

	public void warn(Object msg) {
		this.warn(msg, null);
	}

	public void warn(Throwable t) {
		this.warn(null, t);
	}

	public void warn(Object msg, Throwable t) {
		if (Logger.loglevel.id <= Loglevel.WARNING.id) {
			if (msg != null)
				Logger.println(String.format("[WARN] [%s] %s", this.name, msg));
			if (t != null) {
				StringWriter sw = new StringWriter();
				PrintWriter ps = new PrintWriter(sw, true);
				t.printStackTrace(ps);
				Logger.print(String.format("[WARN] [%s] %s", this.name, sw));
			}
		}
	}

	public void error(Object msg) {
		this.error(msg, null);
	}

	public void error(Throwable t) {
		this.error(null, t);
	}

	public void error(Object msg, Throwable t) {
		if (Logger.loglevel.id <= Loglevel.ERROR.id) {
			if (msg != null)
				Logger.println(String.format("[ERROR] [%s] %s", this.name, msg));
			if (t != null) {
				StringWriter sw = new StringWriter();
				PrintWriter ps = new PrintWriter(sw, true);
				t.printStackTrace(ps);
				Logger.print(String.format("[ERROR] [%s] %s", this.name, sw));
			}
		}
	}

	public void fatal(Object msg) {
		this.fatal(msg, null);
	}

	public void fatal(Throwable t) {
		this.fatal(null, t);
	}

	public void fatal(Object msg, Throwable t) {
		if (Logger.loglevel.id <= Loglevel.FATAL.id) {
			if (msg != null)
				Logger.println(String.format("[FATAL] [%s] %s", this.name, msg));
			if (t != null) {
				StringWriter sw = new StringWriter();
				PrintWriter ps = new PrintWriter(sw, true);
				t.printStackTrace(ps);
				Logger.print(String.format("[FATAL] [%s] %s", this.name, sw));
			}
		}
	}

	public enum Loglevel {
		DEBUG(0), INFO(1), WARNING(2), ERROR(3), FATAL(4);

		public final int id;

		Loglevel(int id) {
			this.id = id;
		}
	}

}
