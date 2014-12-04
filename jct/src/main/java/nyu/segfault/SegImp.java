package nyu.segfault;

/**
 * Java SE library import statements
 */
import java.io.*;
import java.util.*;

/**
 * xtc import statements
 */
import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;
import xtc.util.Tool;

/**
 * SegImp Visitor  handles classes without inheritance and virtual methods
 */
public class SegImp extends Visitor{

    private String[] files; // args passed from the translator
    private String fileName; // name of the file to be translated

    private int count;

    String cc_name; /**@var current class name (this) */
    String className;

    public SegImp() {  }

    public void visitCompilationUnit(GNode n) {
        SegHelper.writeMacros();
        SegHelper.getCallExpression(n);
        visit(n);
        SegHelper.cpp_flush();
    }

    public String constructorProp; //global variable to store constructor property in class declaration and to use to assign arguments in struct initialization in visitFieldDeclaration -Jeff

    public void visitClassDeclaration(GNode n) {
        String className = SegHelper.getClassDeclaration(n).split(" ")[1];  // Set the SegHelper's curr_class, and returns the name of the current class.
//        SegHelper.cpp_pln("class " + className + " {");
        SegHelper.getGlobalVariables(n);
        for (String gVar : SegHelper.currentClassGlobalVariables) SegHelper.cpp_pln(gVar + ";");
        visit(n);
//        SegHelper.cpp_pln("}\n");

    }

    public void visitMethodDeclaration(GNode n){
        String declaration = SegHelper.getMethodDeclaration(n, "SegImp");
        declaration = declaration.substring(0, declaration.length() - 1);  // Remove the semi-colon.
        SegHelper.cpp_pln(declaration + " {");
        String body = SegHelper.getMethodBody(n);
        if (SegHelper.getMethodName(n).equals("main")) {
            body = SegHelper.getMainMethodArgumentsAsSmartPointers() + "\n" + body;
        }
        SegHelper.cpp_pln(body);
        SegHelper.cpp_pln("}\n");
        SegHelper.cpp_flush();
    }

    public void visit(Node n) { for (Object o : n) if (o instanceof Node) dispatch((Node)o); }

}
