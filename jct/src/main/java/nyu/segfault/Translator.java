package nyu.segfault;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import xtc.lang.JavaAstSimplifier;
import xtc.lang.JavaEntities;
import xtc.lang.JavaExternalAnalyzer;
import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.util.SymbolTable;
import xtc.util.Tool;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;




public class Translator extends xtc.util.Tool {
	public static String[] files; // an array used to store the files - args


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
	/**
	 * Send top-level node to each respective visitor method
	 * 1) SegHead visitor - tree generate struct and struct_vt info for each class
	 * 2) SegImp visitor - generate implementation for classes
	 *
	 * @param node compilation unit node
	 */
	public void process(Node node) {
        String fileArgument = files[0];
        String[] filePath = fileArgument.split("/");
        int indexOfFileName = filePath.length - 1;

	    SegHelper.setFileName(filePath[indexOfFileName]);
        System.out.println("File name: " + filePath[indexOfFileName]);

        SymbolTable table = new SymbolTable();
        // Do some simplifications on the AST.
        new JavaAstSimplifier().dispatch(node);
        // Construct the symbol table.
        new SymbolTableBuilder(runtime, table).dispatch(node);
        // Set SegHelper's symbol table to the newly-constructed.
        SegHelper.symbolTable = table;

	    new SegHead().dispatch(node);
	    new SegImp().dispatch(node);

        // A convenient way to print the symbol table.
        table.current().dump(runtime.console());
        runtime.console().flush();
	}

	public static void main(String[] args) {
		files = args;
		new Translator().run(args);
	}
}
