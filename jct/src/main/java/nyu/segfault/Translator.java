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

        SegHelper.allDeclaredClassNames = new ArrayList<String>();
        SegHelper.classNameToMethodDeclarations = new HashMap<String, ArrayList<String>>();
        SegHelper.classToAllAvailableMethodDeclarations = new HashMap<String, ArrayList<String>>();
        SegHelper.classToSuperclass = new HashMap<String, String>();
        new ClassInformation().dispatch(node);

        // Add Object and String declarations to classNameToMethodDeclarations.
        SegHelper.classNameToMethodDeclarations.put("Object", new ArrayList<String>(Arrays.asList(SegHelper.getObjectMethodDeclarations())));
        SegHelper.classNameToMethodDeclarations.put("String", new ArrayList<String>(Arrays.asList(SegHelper.getStringMethodDeclarations())));

        // Hash String to Object (where Object is String's super class).
        SegHelper.classToSuperclass.put("String", "Object");

        new SegHead().dispatch(node);
	    new SegImp().dispatch(node);

        // A convenient way to print the symbol table.
        // table.current().dump(runtime.console());
        runtime.console().flush();
	}

	public static void main(String[] args) {
		files = args;
		new Translator().run(args);
	}

    class ClassInformation extends Visitor {
        String currentClass = "";

        public void visitClassDeclaration(GNode n) {
            SegHelper.allDeclaredClassNames.add(n.getString(1));
            currentClass = n.getString(1);
            SegHelper.classNameToMethodDeclarations.put(currentClass, new ArrayList<String>());

            // Handle classes that extend other (non-object) classes.
            String superclass = "";
            boolean superclassIsObject = false;
            try {
                superclass = n.getNode(3).getNode(0).getNode(0).getString(0);
            } catch (Exception e) {
                superclassIsObject = true;
                System.out.println("Class " + currentClass + " directly extends class Object.");
            }

            // Hash this class to its correct super class.
            if (superclassIsObject) {
                SegHelper.classToSuperclass.put(currentClass, "Object");
            } else {
                SegHelper.classToSuperclass.put(currentClass, superclass);
            }
            visit(n);
        }

        public void visitMethodDeclaration(GNode n) {
            // Determine the return type.
            String returnType;
            try {
                returnType = n.getNode(2).toString();
                if (returnType.equals("VoidType()")) {
                    returnType = "void";
                } else {
                    returnType = n.getNode(2).getNode(0).getString(0);
                }

                if (returnType.equals("boolean")) {
                    returnType = "bool";
                }
            } catch (NullPointerException e) {  // This will be thrown if there is no return type (i.e. constructor method.)
                return;  // This constructor should not be dealt with now.
            }

            // Determine the method name.
            String methodName = n.getString(3);

            // Determine the class name.
            String className = currentClass;

            // Determine the parameter types.
            ArrayList<String> parameterTypes = new ArrayList<String>();
            parameterTypes.add(currentClass);  // "this" must be the first parameter of the method.
            Node formalParameters = n.getNode(4);
            if (formalParameters != null && formalParameters.size() > 0) {
                int parameterIndex = 0;
                while (formalParameters.size() > parameterIndex) {
                    parameterTypes.add(formalParameters.getNode(parameterIndex++).getNode(1).getNode(0).getString(0));
                }
            }

            // Create the method declaration string.
            String methodDeclaration = "static " + returnType + " " + methodName + "(";

            for (String parameterType : parameterTypes) { methodDeclaration += parameterType + ", "; }
            methodDeclaration = methodDeclaration.substring(0, methodDeclaration.length() - 2);  // Remove the final ", "
            methodDeclaration += ")";

            SegHelper.classNameToMethodDeclarations.get(currentClass).add(methodDeclaration);
        }

        public void visit(GNode n) {
            for (Object o : n) if (o instanceof Node) dispatch((Node) o);
        }

    }
}
