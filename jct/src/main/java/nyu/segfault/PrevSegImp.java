// package nyu.segfault;

// /**
//  * Java SE library import statements
//  */
// import java.io.*;
// import java.util.*;

// /**
//  * xtc import statements
//  */

// import xtc.lang.JavaFiveParser;

// import xtc.parser.ParseException;
// import xtc.parser.Result;

// import xtc.util.SymbolTable;
// import xtc.util.Tool;

// import xtc.tree.GNode;
// import xtc.tree.Node;
// import xtc.tree.Printer;
// import xtc.tree.Visitor;



// /**
//  * SegImp Visitor  handles classes without inheritance and virtual methods
//  */
// public class SegImp extends Visitor{
//     private String[] files; // args passed from the translator
//     private String fileName; // name of the file to be translated

//     private int count;

//     public PrintWriter cWriter; // prints to the method body
//     public PrintWriter hWriter; // prints to the header
//     public Printer impWriter;
//     public Printer headWriter;

//     public final SegNode<String> inhTree=new SegNode<String>((String)"Object");
//     public String _super;

//     String cc_name; /**@var current class name (this) */
//     String className;

//     HashSet<String> publicHPP;  // Fields that fall under the "public:" tag of the header file.
//     HashSet<String> privateHPP;  // Fields that fall under the "private:" tag of the header file.

//     HashSet<String> publicHPPmethods;  // Methods that fall under the "public:" tag of the header file.
//     HashSet<String> privateHPPmethods;  // Methods that fall under the "private:" tag of the header file.

//     SymbolTable table; /**@var node symbols*/

//     String method_return_type = "";

//     public SegImp() {  }

//     public void visitCompilationUnit(GNode n) {
//         SegHelper.setFileName(files[0]);
//         visit(n);
//         impWriter.flush();
//         headWriter.flush();
//     }


//     public String constructorProp; //global variable to store constructor property in class declaration and to use to assign arguments in struct initialization in visitFieldDeclaration -Jeff

//     public void visitClassDeclaration(GNode n) {
//         SegHelper.getGlobalVariables(n);
//         for (String gVar : SegHelper.currentClassGlobalVariables) SegHelper.cpp_pln(gVar + ";");
//     }

//     public void visitExtension(GNode n){
//         //retrieve explicit extension
//         _super=n.getNode(0).getNode(0).getString(0); /**@var name of super class */
//     }
//     public void visitAdditiveExpression(GNode n){ }
//     public void visitBlock(GNode n){
//         visit(n);
//     }
//     public void visitMethodDeclaration(GNode n){
//         System.out.println("visitMethodDeclaration");
//         System.out.println(n + "\n");
//         final boolean isPrivate = false;

//         final GNode root=n;
//         final String return_type=n.getNode(2).toString();
//         //		method_VT_buffer=new ArrayList<String>();
//         //		method_only_VT=new ArrayList<String>();
//         //		method_only_VT.add(root.getString(3));
//         try{
//             method_return_type = n.getNode(2).getNode(0).getString(0);
//         }
//         catch (Exception e) {}
//         new Visitor(){
//             String fp="";
//             int numTabs = 0;  // The number of tabs to include before a statement.
//             public void visitFormalParameters(GNode n){
//                 fp+=root.getString(3)+"(";
//                 if( n.size() == 0 ) fp+=")";
//                 ArrayList<String> arg_types=new ArrayList<String>();
//                 for(int i=0; i< n.size(); i++){
//                     Node fparam=n.getNode(i);

//                     //retrieve argument type
//                     fp+= j2c(fparam.getNode(1).getNode(0).getString(0))+" ";
//                     arg_types.add(fparam.getNode(1).getNode(0).getString(0)+" ");

//                     //retrieve argument name
//                     fp+=fparam.getString(3);

//                     if(i+1 < n.size()) fp+=",";
//                     else fp+=")";
//                 }
//                 String rType="";

//                 // Method Return Types
//                 if(return_type.equals("VoidType()")) rType="void";
//                 else if(method_return_type.equals("String")) rType="string";
//                 else if(method_return_type.equals("Integer")) rType = "int";

//                 /**
//                  * generate the function ptr in struct <class_name>_VT
//                  * <return_type> (*function name)(arg_type 1, arg_type 2)
//                  */

//                 String function_ptr=rType+"(*"+root.getString(3)+")";
//                 if(arg_types.size() == 0) function_ptr+="()";
//                 else{
//                     for(int k=0; k< arg_types.size(); k++){
//                         function_ptr+=arg_types.get(k);
//                         if(k < arg_types.size() -1) function_ptr+=",";
//                     }
//                 }
//                 //				method_VT_buffer.add(function_ptr);
//                 String hpp_prototype= rType +" "+ fp;
//                 // String cpp_prototype= rType+" "+cc_name+ "::" + fp+" {";
//                 String cpp_prototype= "int main() {";
//                 if(!className.equals(fileName.substring(0, 1).toUpperCase() + fileName.substring(1))) cpp_prototype = rType+" "+className+ "::" + fp+" {";

//                 System.out.println(root.getString(3));
//                 if(!root.getString(3).equals("main")){
//                     //write function prototype to hpp file within struct <cc_name>
//                     // <return_type> <function_name>(arg[0]...arg[n]);

//                     /* Add the method signature to the correct section of the header. */
//                     if (isPrivate) {
//                         privateHPPmethods.add(hpp_prototype);
//                     } else {
//                         publicHPPmethods.add(hpp_prototype);
//                     }
//                 }
//                 //write function prototype to cpp file
//                 // <return type> <class name> :: <function name> (arg[0]...arg[n]){

//                 impWriter.pln(cpp_prototype);
//             }

//             public void visitForStatement(GNode n) {
//                 impWriter.p("\t" + "for " + "(");

//                 new Visitor() {

//                     public void visitBasicForControl(GNode n) {
//                         new Visitor() {
//                             public void visitRelationalExpression(GNode n) {
//                                 impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0) + "; ");
//                             }
//                             public void visitDeclarators(GNode n) {
//                                 impWriter.p(n.getNode(0).getString(0) + " = " + n.getNode(0).getNode(2).getString(0) + "; ");
//                             }
//                             public void visitExpressionList(GNode n) {
//                                 new Visitor() {
//                                     public void visitPostfixExpression(GNode n) {
//                                         impWriter.p(n.getNode(0).getString(0) + n.getString(1));
//                                     }
//                                     public void visitUnaryExpression(GNode n) {
//                                         impWriter.p(n.getString(0) + n.getNode(1).getString(0));
//                                     }
//                                     public void visit(Node n){
//                                         for (Object o : n) if(o instanceof Node) dispatch((Node)o);
//                                     }
//                                 }.dispatch(n);
//                             }
//                             public void visitStringLiteral(GNode n) {
//                                 impWriter.p(n.getString(0));
//                             }

//                             public void visitIntegerLiteral(GNode n) {
//                                 impWriter.p(n.getString(0));
//                             }

//                             public void visitFloatingPointLiteral(GNode n) {
//                                 impWriter.p(n.getString(0));
//                             }

//                             public void visitCharacterLiteral(GNode n) {
//                                 impWriter.p(n.getString(0));
//                             }

//                             public void visitBooleanLiteral(GNode n) {
//                                 impWriter.p(n.getString(0));
//                             }

//                             public void visitNullLiteral(GNode n) {
//                                 impWriter.p("null");
//                             }

//                             public void visitPrimaryIdentifier(GNode n) {
//                                 impWriter.p(n.getString(0));
//                             }
//                             public void visitType(GNode n) {
//                                 impWriter.p(n.getNode(0).getString(0) + " ");
//                             }

//                             public void visitAdditiveExpression(GNode n) {
//                                 impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
//                             }


//                             public void visit(Node n){
//                                 for (Object o : n) if(o instanceof Node) dispatch((Node)o);
//                             }


//                         }.dispatch(n);
//                         impWriter.pln(") {");
//                     }

//                     public void visitBlock(GNode n) {


//                         new Visitor() {
//                             public void visitExpressionStatement(GNode n) {
//                                 impWriter.p("\t");

//                                 if (n.getNode(0).getName().equals("Expression")) { // checks if regular expression is being made
//                                     impWriter.p("\t");


//                                     new Visitor() {  // Visit assigned value if any

//                                         public void visitExpression(GNode n) {
//                                             if (n.getNode(2).getName().equals("AdditiveExpression")) {
//                                                 impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getNode(0).getString(0) + " " + n.getNode(2).getString(1) + " " + n.getNode(2).getNode(2).getString(0));
//                                             } else {
//                                                 impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
//                                             }
//                                         }

//                                         public void visitStringLiteral(GNode n) {
//                                             impWriter.p(n.getString(0));
//                                         }

//                                         public void visitIntegerLiteral(GNode n) {
//                                             impWriter.p(n.getString(0));
//                                         }

//                                         public void visitFloatingPointLiteral(GNode n) {
//                                             impWriter.p(n.getString(0));
//                                         }

//                                         public void visitCharacterLiteral(GNode n) {
//                                             impWriter.p(n.getString(0));
//                                         }

//                                         public void visitBooleanLiteral(GNode n) {
//                                             impWriter.p(n.getString(0));
//                                         }

//                                         public void visitNullLiteral(GNode n) {
//                                             impWriter.p("null");
//                                         }

//                                         public void visitPrimaryIdentifier(GNode n) {

//                                             impWriter.p(n.getString(0));
//                                         }

//                                         public void visitAdditiveExpression(GNode n) {
//                                             //	Currently only works for 2 vars in expression. hard-coded
//                                             //	System.out.println(n.toString());
//                                             /*String add = "";
//                                              System.out.println(n.size());
//                                              for (int i = 0,j=1; i < n.size(); i+=2,j+=2) {
//                                              if(i < n.size()) {
//                                              add += n.getNode(i).getString(0);
//                                              add += n.getString(j);
//                                              }
//                                              }
//                                              impWriter.p(add);
//                                              */
//                                             impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
//                                             //	if (n.getNode(0) != null) visit(n.getNode(0));
//                                         }
//                                         public void visit(Node n){
//                                             for (Object o : n) if(o instanceof Node) dispatch((Node)o);
//                                         }
//                                     }.dispatch(n);
//                                     impWriter.pln(";");
//                                 }
//                                 //System.out.println(n.getNode(0).toString());
//                                 boolean isEndLine = false; // used to check if the print statement has an ln
//                                 if (n.getNode(0).toString().contains("println")) isEndLine = true;
//                                 if (n.toString().contains("CallExpression") && n.toString().contains("SelectionExpression") && n.toString().contains("System") && n.toString().contains("out")) { // checks if a call expression is being made
//                                     // String method_called = n.getNode(0).getNode(3).getNode(0).getString(2);
//                                     // impWriter.p("\tprintf("+method_called+"()");
//                                     //System.out.println(n.toString());
//                                     impWriter.p("\tcout");
//                                     //System.out.println(n.toString());
//                                     final ArrayList<String> vars = new ArrayList<String>();
//                                     new Visitor() {

//                                         public void visitCallExpression(GNode n){
//                                             // If a method is called

//                                             Node arguments = n.getNode(3);
//                                             new Visitor() {
//                                                 public void visitStringLiteral(GNode n) {
//                                                     impWriter.p(" << " + n.getString(0));
//                                                 }

//                                                 public void visitIntegerLiteral(GNode n) {
//                                                     impWriter.p(" << " + n.getString(0));
//                                                 }

//                                                 public void visitFloatingPointLiteral(GNode n) {
//                                                     impWriter.p(" << " + n.getString(0));
//                                                 }

//                                                 public void visitCharacterLiteral(GNode n) {
//                                                     impWriter.p(" << " + n.getString(0));
//                                                 }

//                                                 public void visitBooleanLiteral(GNode n) {
//                                                     impWriter.p(" << " + n.getString(0));
//                                                 }

//                                                 public void visitNullLiteral(GNode n) {
//                                                     impWriter.p(" << " + "null");
//                                                 }

//                                                 public void visitPrimaryIdentifier(GNode n) {
//                                                     impWriter.p(" << " + n.getString(0));
//                                                 }
//                                                 public void visitCallExpression(GNode n) {
//                                                     String method = "";
//                                                     method += n.getNode(0).getString(0);
//                                                     if (n.getString(2).isEmpty()) {
//                                                         method += "()";
//                                                     }
//                                                     else {
//                                                         method += "." + n.getString(2) + "(";
//                                                         if (n.getNode(3).isEmpty()){
//                                                             method += ")";
//                                                         }
//                                                     }
//                                                     vars.add(method);
//                                                     impWriter.p(" << " + method);
//                                                 }
//                                                 public void visit(GNode n) {
//                                                     for (Object o : n) if (o instanceof Node) dispatch((Node) o);
//                                                 }
//                                             }.dispatch(arguments);
//                                         }

//                                         public void visit(GNode n) {
//                                             for (Object o : n) if (o instanceof Node) dispatch((Node) o);
//                                         }

//                                     }.dispatch(n.getNode(0));

//                                     if (isEndLine) {
//                                         impWriter.p(" << \"\\n\"");
//                                     }
//                                     /*
//                                      if (!vars.isEmpty()){
//                                      for (String var : vars) {
//                                      impWriter.p(", " + "to_string(" + var + ")");
//                                      }
//                                      }
//                                      */
//                                     impWriter.pln(";");
//                                 }
//                                 else if (n.toString().contains("CallExpression")) {
//                                     impWriter.p("\t");
//                                     new Visitor() {
//                                         public void visitCallExpression(GNode n) {
//                                             String method = "";
//                                             method += n.getNode(0).getString(0);
//                                             if (n.getString(2).isEmpty()) {
//                                                 method += "()";
//                                             }
//                                             else {
//                                                 method += "." + n.getString(2) + "(";
//                                                 if (n.getNode(3).isEmpty()){
//                                                     method += ")";
//                                                 }
//                                                 else {
//                                                     Node arguments = n.getNode(3);

//                                                     for (int i = 0; i < arguments.size(); i++) {
//                                                         if (i == 0) {
//                                                             method += arguments.getNode(0).getString(i);
//                                                         }
//                                                         else {
//                                                             method += ", " + arguments.getNode(0).getString(i);
//                                                         }
//                                                     }
//                                                     method += ")";

//                                                 }
//                                             }
//                                             impWriter.p(method);
//                                         }
//                                         public void visit(GNode n) {
//                                             for (Object o : n) if (o instanceof Node) dispatch((Node) o);
//                                         }
//                                     }.dispatch(n);
//                                     impWriter.pln(";");
//                                 }
//                                 else {
//                                     visit(n);
//                                 }
//                             }
//                             public void visit(GNode n) {
//                                 for (Object o : n) if (o instanceof Node) dispatch((Node) o);
//                             }

//                         }.dispatch(n);
//                     }


//                     public void visit(Node n){
//                         for (Object o : n) if(o instanceof Node) dispatch((Node)o);
//                     }

//                 }.dispatch(n);
//                 impWriter.p("\n");
//                 impWriter.p("\t" + "}" + "\n");


//             }



//             public void visitFieldDeclaration(GNode n) {  // Need to add visitMethodDeclaration() to visitor for advanced FieldDeclarations.
//                 //System.out.println(n);
//                 /* Determine and print the declarator type. */
//                 impWriter.p("\t");
//                 String declarationType = n.getNode(1).getNode(0).getString(0);
//                 if (declarationType.equals("boolean")) {
//                     impWriter.p("boolean ");
//                 } else if (declarationType.equals("char")) {
//                     impWriter.p("char ");
//                 } else if (declarationType.equals("double")) {
//                     impWriter.p("double ");
//                 } else if (declarationType.equals("float")) {
//                     impWriter.p("float ");
//                 } else if (declarationType.equals("int")) {
//                     impWriter.p("int ");
//                 } else if (declarationType.equals("String")) {
//                     impWriter.p("string ");
//                 }
//                 else if (n.getNode(1).getNode(0).getName().equals("QualifiedIdentifier")) {
//                     /*
//                      if (n.getNode(1).getNode(0).getString(0).equals("String")) {
//                      impWriter.p("string");
//                      }
//                      else {
//                      */
//                     impWriter.p(n.getNode(1).getNode(0).getString(0) + " ");
//                     //}
//                 }

//                 /* Print the name of the field. */
//                 String fieldName = n.getNode(2).getNode(0).getString(0);
//                 impWriter.p(fieldName);

//                 /* Potentially visit the assigned value (if any). */
//                 new Visitor() {
//                     // initializing struct

//                     public void visitDeclarators(GNode n) { //added

//                         try {
//                             boolean a = n.getNode(0).getNode(2).getString(0).equals("null");
//                             impWriter.p(" = " + n.getNode(0).getNode(2).getString(0));
//                         } catch(Exception e) {
//                         }

//                         new Visitor() {
//                             public void visitNewClassExpression(GNode n) {
//                                 if (n.getNode(3).size() > 0) {  // if arguments exist for object initializing
//                                     impWriter.p(" = " + "(" + n.getNode(2).getString(0) + ")" + " {" + "." + constructorProp + " = " + n.getNode(3).getNode(0).getString(0) + "}");  //arguments passed only 1 argument works for now
//                                 } else { // if arguments do not exist
//                                     if (!n.getNode(3).toString().equals("Arguments()")) {
//                                         impWriter.p(" = " + "(" + n.getNode(2).getString(0) + ")" + " {" + " }");
//                                     }
//                                 }
//                             }
//                             public void visit(GNode n) {
//                                 for (Object o : n) if(o instanceof Node) dispatch((Node)o);
//                             }
//                         }.dispatch(n);
//                     }


//                     public void visitStringLiteral(GNode n) {
//                         impWriter.p(" = " + n.getString(0));
//                     }

//                     public void visitIntegerLiteral(GNode n) {
//                         impWriter.p(" = " + n.getString(0));
//                     }

//                     public void visitFloatingPointLiteral(GNode n) {
//                         impWriter.p(" = " + n.getString(0));
//                     }

//                     public void visitCharacterLiteral(GNode n) {
//                         impWriter.p(" = " + n.getString(0));
//                     }

//                     public void visitBooleanLiteral(GNode n) {
//                         impWriter.p(" = " + n.getString(0));
//                     }

//                     public void visit(GNode n) {
//                         for (Object o : n) if(o instanceof Node) dispatch((Node)o);
//                     }
//                 }.dispatch(n);
//                 impWriter.pln(";");
//             }

//             public void visitReturnStatement(GNode n) {
//                 numTabs++;  // Increment the number of tabs to be printed before the statement.
//                 if (n.getNode(0) != null) {
//                     for (int x = 0; x < numTabs; x++) impWriter.p("\t");
//                     impWriter.p("return "); // print return keyword
//                 }
//                 numTabs--;  // Decrement the number of tabs to be printed before the statement.
//                 new Visitor() {  // Visit assigned value if any

//                     public void visitStringLiteral(GNode n) {
//                         impWriter.p(n.getString(0));
//                     }

//                     public void visitIntegerLiteral(GNode n) {
//                         impWriter.p(n.getString(0));
//                     }

//                     public void visitFloatingPointLiteral(GNode n) {
//                         impWriter.p(n.getString(0));
//                     }

//                     public void visitCharacterLiteral(GNode n) {
//                         impWriter.p(n.getString(0));
//                     }

//                     public void visitBooleanLiteral(GNode n) {
//                         impWriter.p(n.getString(0));
//                     }

//                     public void visitNullLiteral(GNode n) {
//                         impWriter.p("null");
//                     }

//                     public void visitPrimaryIdentifier(GNode n) {
//                         impWriter.p(n.getString(0));
//                     }

//                     public void visitThisExpression(GNode n) {

//                     }


//                     public void visitAdditiveExpression(GNode n) {
//                         //	Currently only works for 2 vars in expression. hard-coded
//                         //	System.out.println(n.toString());
//                         /*String add = "";
//                          System.out.println(n.size());
//                          for (int i = 0,j=1; i < n.size(); i+=2,j+=2) {
//                          if(i < n.size()) {
//                          add += n.getNode(i).getString(0);
//                          add += n.getString(j);
//                          }
//                          }
//                          impWriter.p(add);
//                          */
//                         impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
//                         //	if (n.getNode(0) != null) visit(n.getNode(0));
//                     }
//                     public void visit(Node n){
//                         for (Object o : n) if(o instanceof Node) dispatch((Node)o);

//                     }
//                 }.dispatch(n);
//                 impWriter.pln(";");
//             }
//             public void visit(Node n){
//                 for (Object o : n) if(o instanceof Node) dispatch((Node)o);
//             }


//             public void visitExpressionStatement(GNode n) {

//                 if (n.getNode(0).getName().equals("Expression")) { // checks if regular expression is being made
//                     impWriter.p("\t");


//                     new Visitor() {  // Visit assigned value if any

//                         public void visitExpression(GNode n) {
//                             if (n.getNode(2).getName().equals("AdditiveExpression")) {
//                                 impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getNode(0).getString(0) + " " + n.getNode(2).getString(1) + " " + n.getNode(2).getNode(2).getString(0));
//                             } else {
//                                 impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
//                             }
//                         }

//                         public void visitStringLiteral(GNode n) {
//                             impWriter.p(n.getString(0));
//                         }

//                         public void visitIntegerLiteral(GNode n) {
//                             impWriter.p(n.getString(0));
//                         }

//                         public void visitFloatingPointLiteral(GNode n) {
//                             impWriter.p(n.getString(0));
//                         }

//                         public void visitCharacterLiteral(GNode n) {
//                             impWriter.p(n.getString(0));
//                         }

//                         public void visitBooleanLiteral(GNode n) {
//                             impWriter.p(n.getString(0));
//                         }

//                         public void visitNullLiteral(GNode n) {
//                             impWriter.p("null");
//                         }

//                         public void visitPrimaryIdentifier(GNode n) {

//                             impWriter.p(n.getString(0));
//                         }

//                         public void visitAdditiveExpression(GNode n) {
//                             //	Currently only works for 2 vars in expression. hard-coded
//                             //	System.out.println(n.toString());
//                             /*String add = "";
//                              System.out.println(n.size());
//                              for (int i = 0,j=1; i < n.size(); i+=2,j+=2) {
//                              if(i < n.size()) {
//                              add += n.getNode(i).getString(0);
//                              add += n.getString(j);
//                              }
//                              }
//                              impWriter.p(add);
//                              */
//                             impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
//                             //	if (n.getNode(0) != null) visit(n.getNode(0));
//                         }
//                         public void visit(Node n){
//                             for (Object o : n) if(o instanceof Node) dispatch((Node)o);
//                         }
//                     }.dispatch(n);
//                     impWriter.pln(";");
//                 }
//                 //System.out.println(n.getNode(0).toString());
//                 boolean isEndLine = false; // used to check if the print statement has an ln
//                 if (n.getNode(0).toString().contains("println")) isEndLine = true;
//                 if (n.toString().contains("CallExpression") && n.toString().contains("SelectionExpression") && n.toString().contains("System") && n.toString().contains("out")) { // checks if a call expression is being made
//                     // String method_called = n.getNode(0).getNode(3).getNode(0).getString(2);
//                     // impWriter.p("\tprintf("+method_called+"()");
//                     //System.out.println(n.toString());
//                     impWriter.p("\tcout");
//                     //System.out.println(n.toString());
//                     final ArrayList<String> vars = new ArrayList<String>();
//                     new Visitor() {

//                         public void visitCallExpression(GNode n){
//                             // If a method is called

//                             Node arguments = n.getNode(3);
//                             new Visitor() {
//                                 public void visitStringLiteral(GNode n) {
//                                     impWriter.p(" << " + n.getString(0));
//                                 }

//                                 public void visitIntegerLiteral(GNode n) {
//                                     impWriter.p(" << " + n.getString(0));
//                                 }

//                                 public void visitFloatingPointLiteral(GNode n) {
//                                     impWriter.p(" << " + n.getString(0));
//                                 }

//                                 public void visitCharacterLiteral(GNode n) {
//                                     impWriter.p(" << " + n.getString(0));
//                                 }

//                                 public void visitBooleanLiteral(GNode n) {
//                                     impWriter.p(" << " + n.getString(0));
//                                 }

//                                 public void visitNullLiteral(GNode n) {
//                                     impWriter.p(" << " + "null");
//                                 }

//                                 public void visitPrimaryIdentifier(GNode n) {
//                                     impWriter.p(" << " + n.getString(0));
//                                 }
//                                 public void visitCallExpression(GNode n) {
//                                     String method = "";
//                                     method += n.getNode(0).getString(0);
//                                     if (n.getString(2).isEmpty()) {
//                                         method += "()";
//                                     }
//                                     else if (n.getString(2).equals("toString")) {
//                                         method = "std::to_string(" + method + ")";
//                                     }
//                                     else {
//                                         method += "." + n.getString(2) + "(";
//                                         if (n.getNode(3).isEmpty()){
//                                             method += ")";
//                                         }
//                                     }
//                                     vars.add(method);
//                                     impWriter.p(" << " + method);
//                                 }
//                                 public void visit(GNode n) {
//                                     for (Object o : n) if (o instanceof Node) dispatch((Node) o);
//                                 }
//                             }.dispatch(arguments);
//                         }

//                         public void visit(GNode n) {
//                             for (Object o : n) if (o instanceof Node) dispatch((Node) o);
//                         }

//                     }.dispatch(n.getNode(0));

//                     if (isEndLine) {
//                         impWriter.p(" << \"\\n\"");
//                     }
//                     /*
//                      if (!vars.isEmpty()){
//                      for (String var : vars) {
//                      impWriter.p(", " + "to_string(" + var + ")");
//                      }
//                      }
//                      */
//                     impWriter.pln(";");
//                 }
//                 else if (n.toString().contains("CallExpression")) {
//                     impWriter.p("\t");
//                     new Visitor() {
//                         public void visitCallExpression(GNode n) {
//                             String method = "";
//                             method += n.getNode(0).getString(0);
//                             if (n.getString(2).isEmpty()) {
//                                 method += "()";
//                             }
//                             else if (n.getString(2).equals("toString")) {
//                                 method = "std::to_string(" + method + ")";
//                             }
//                             else {
//                                 System.out.println(n.getString(2));
//                                 method += "." + n.getString(2) + "(";
//                                 if (n.getNode(3).isEmpty()){
//                                     method += ")";
//                                 }
//                                 else {
//                                     Node arguments = n.getNode(3);

//                                     for (int i = 0; i < arguments.size(); i++) {
//                                         if (i == 0) {
//                                             method += arguments.getNode(0).getString(i);
//                                         }
//                                         else {
//                                             method += ", " + arguments.getNode(0).getString(i);
//                                         }
//                                     }
//                                     method += ")";

//                                 }
//                             }

//                             impWriter.p(method);
//                         }
//                         public void visit(GNode n) {
//                             for (Object o : n) if (o instanceof Node) dispatch((Node) o);
//                         }
//                     }.dispatch(n);
//                     impWriter.pln(";");
//                 }
//                 else {
//                     visit(n);
//                 }
//             }



//         }.dispatch(n);

//         impWriter.pln("}\n");
//         Node body = n.getNode(7);
//         if (null != body) visit(body);
//     }

//     public void visitForStatement(GNode n){
//     }

//     public void visitPrimaryIdentifier(GNode n){
//     }

//     public void visitBreakStatement(GNode n) {
//         impWriter.pln("break;\n");
//     }

//     public void visitContinueStatement(GNode n) {
//         impWriter.pln("continue;\n");
//     }

//     public void visit(Node n) {
//         for (Object o : n) if (o instanceof Node) dispatch((Node)o);
//     }

//     public String j2c(String javaType) {
//         String cType;
//         if (javaType.equals("String")) {
//             cType = "string";
//         }
//         else {
//             cType = javaType;
//         }
//         return cType;
//     }
// }