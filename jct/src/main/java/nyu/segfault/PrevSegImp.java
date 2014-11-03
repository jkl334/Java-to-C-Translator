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
 public class PrevSegImp extends Visitor{
     private String[] files; // args passed from the translator
     private String fileName; // name of the file to be translated

     private int count;

     String cc_name; /**@var current class name (this) */
     String className;

     public PrevSegImp() {  }

     public void visitCompilationUnit(GNode n) {
         SegHelper.writeMacros();
         SegHelper.getCallExpression(n);
         visit(n);
     }

     public String constructorProp; //global variable to store constructor property in class declaration and to use to assign arguments in struct initialization in visitFieldDeclaration -Jeff

     public void visitClassDeclaration(GNode n) {
         String className = SegHelper.getCurrClass();
         SegHelper.cpp_pln("class " + className + " {");
         SegHelper.getGlobalVariables(n);
         for (String gVar : SegHelper.currentClassGlobalVariables) SegHelper.cpp_pln(gVar + ";");
         visit(n);
         SegHelper.cpp_pln("}");
         SegHelper.cpp_flush();
     }

     public void visitMethodDeclaration(GNode n){
         String declaration = SegHelper.getMethodDeclaration(n, "SegImp");
         SegHelper.cpp_pln(declaration + " {");
         String body = SegHelper.getMethodBody(n);
         SegHelper.cpp_pln("}");
         SegHelper.cpp_flush();
     }
     
     public void visit(Node n) { for (Object o : n) if (o instanceof Node) dispatch((Node)o); }
 }
