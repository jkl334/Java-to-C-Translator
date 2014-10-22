package nyu.segfault;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.util.Tool;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;

import nyu.segfault.SegFaultVisitor;

public class Translator extends xtc.util.Tool {
	public static String[] files; // an array used to store the files - args 

	public Translator() {
		// do Nothing
	}

	public interface ICommand {
	    public void run();
	}

	  // Returns program description
	public String getName() {
	    return "SegFault - Java to C++ Translator";
	}

	  // Locates the file to be parsed
	public File locate(String name) throws IOException {
	    File file = super.locate(name);
	    if (Integer.MAX_VALUE < file.length()) {
	      throw new IllegalArgumentException(file + ": file too large");
	    }
	    return file;
	}

	  // Parses input Java class, returns a Node
	public Node parse(Reader in, File file) throws IOException, ParseException {
	    JavaFiveParser parser =
	      new JavaFiveParser(in, file.toString(), (int)file.length());
	    Result result = parser.pCompilationUnit(0);
	    return (Node)parser.value(result);
	}
	  

	public void process(Node node) {
	    new SegFaultVisitor(files).dispatch(node);
	}

	public static void main(String[] args) {
		files = args;
		new Translator().run(args);
	}

}