package nyu.segfault;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.LogManager;
import java.util.logging.ConsoleHandler;

public class SegFaultLogger {
  static private FileHandler fileTxt;
  static private SimpleFormatter formatterTxt;

  static private FileHandler fh;
  static private Formatter formatterHTML;

  static private ConsoleHandler ch;

  static public void setup() throws IOException {
    LogManager.getLogManager().reset();
    Logger logger = Logger.getLogger("");
    logger.setLevel(Level.ALL);
    fh = new FileHandler("SegFault.log");
    SimpleFormatter simpleFormat = new SimpleFormatter();
    fh.setFormatter(simpleFormat);
    logger.addHandler(fh);

    ch = new ConsoleHandler();
    ch.setLevel(Level.WARNING);
    logger.addHandler(ch);
  }
}