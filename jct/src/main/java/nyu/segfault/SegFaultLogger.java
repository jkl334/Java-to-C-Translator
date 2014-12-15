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

  static private FileHandler fileHandler;
  static private Formatter formatterHTML;

  static private ConsoleHandler ch;

  static public void setup() throws IOException {
    LogManager.getLogManager().reset();
    Logger logger = Logger.getLogger("");
    logger.setLevel(Level.ALL);
    fileHandler = new FileHandler("SegFault.log");
    SimpleFormatter simpleFormat = new SimpleFormatter();
    fileHandler.setFormatter(simpleFormat);
    logger.addHandler(fileHandler);
  }
}