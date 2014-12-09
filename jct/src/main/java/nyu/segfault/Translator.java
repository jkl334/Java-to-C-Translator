package nyu.segfault;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import xtc.lang.JavaEntities;
import xtc.lang.JavaExternalAnalyzer;
import xtc.lang.JavaFiveParser;
import xtc.lang.JavaPrinter;
import xtc.lang.JavaAstSimplifier;

import xtc.parser.ParseException;
import xtc.parser.Result;

import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;
import xtc.tree.Printer;

import xtc.util.Tool;
import java.util.LinkedList;
import xtc.util.SymbolTable;

import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import java.util.logging.ConsoleHandler;




public class Translator extends Tool {
	public final static Logger LOGGER = Logger.getLogger(SegDependency.class .getName());
	public static String[] files; // an array used to store the files - args
	
	/** Create a new translator. */
  	public Translator() {
    	// Nothing to do.
  	}

  	/* Returns program description */
	public String getName() {
	    return "SegFault - Java to C++ Translator";
	}

  	public void init() {
    	super.init();

    	/* Declare command line arguments */
    	runtime.
    		bool("translate", "translate", false, "translate java to cpp");
    }

	public interface ICommand {
	    public void run();
	}
  	public void prepare() {
  		/* Perform consistency checks on command line arguments.*/
    	super.prepare();
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

        SegHelper.allDeclaredClassNames = new ArrayList<String>();
        new ClassInformation().dispatch(node);

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

    class ClassInformation extends Visitor {
        public void visitClassDeclaration(GNode n) { SegHelper.allDeclaredClassNames.add(n.getString(1)); visit(n); }
        public void visit(GNode n) { for (Object o : n) if (o instanceof Node) dispatch((Node) o); } ;
    }
}
