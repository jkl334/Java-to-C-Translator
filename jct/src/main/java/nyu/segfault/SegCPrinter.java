package nyu.segfault;

import java.lang.*;

import java.util.Iterator;

import java.util.LinkedList;
import xtc.tree.LineMarker;
import xtc.tree.Node;
import xtc.tree.GNode;
import xtc.tree.Pragma;
import xtc.tree.Printer;
import xtc.tree.SourceIdentity;
import xtc.tree.Token;
import xtc.tree.Visitor;

/* Based off of src/xtc/lang/CPrinter.java */

public class SegCPrinter extends Visitor {
  private static final boolean VERBOSE = false;
  LinkedList<String> classFields = new LinkedList<String>();

  protected Printer printer;
  protected Printer header;
  public GNode root;


  private String packageName;
  private String className;
  private String javaClassName;

	public SegCPrinter(Printer p){
    this.printer = p;
    printer.register(this);
	}

  public void visitLoveFieldDeclaration(GNode n){
    StringBuilder sb = new StringBuilder();
    sb.append(n.getNode(1).getString(0));
    classFields.add(sb.toString());
  }

	public void visitCompilationUnit(GNode n) {
    visit(n);
  }

	public void visitClassDeclaration(GNode n) {
    className = n.getString(1);
    visit(n);
  }

  /* TRICKY need to have the namespace scope */
  public void visitPackageDeclaration(GNode n) {
    GNode qid  = n.getGeneric(1);
    int   size = qid.size();
    packageName = fold(qid,size);

  }
  public void visitImportDeclaration(GNode n) {
    printer.p("using namespace ");
    visit(n);
    printer.pln(";");
    printer.pln("");
  }

	public void visitClassBody(GNode n) {
    /* Begin the namespace. */
    printer.incr();
    printlnUnlessNull("namespace " + packageName + " {", packageName);
    visit(n);

    printClassMethod();

    printlnUnlessNull("}",packageName); //Closing namespace
    printer.decr();
  }

  public void visitMethodDeclaration(GNode n){
    String methodName = n.getString(3);
    if (methodName.equals("main")) {
      printer.pln("int main(void){");
      printer.p(n.getNode(7));
      printer.pln("return 0;");
      printer.pln("}");
    }
    else {
    printer.p(n.getNode(2));
    printer.p(" ");
    // NEED: formal parameters
    printer.p(className + "::" + methodName);
    if (n.getNode(4).size() !=0) {
      //printer.p("(").p(n.getNode(4)).p(") {");
      printer.p(n.getNode(4)).p(" {");
      //printer.p("(parameters please)" + " {");
      printer.incr();
    }
    else {
      javaClassName = n.getString(5);
      printer.p("(" + javaClassName + " __this)" + " {");
    }
    printer.pln();
    printer.p(n.getNode(7));
    printer.pln("}");
  }
  }
  /** Visit the specified type. */
  public void visitType(GNode n) {
    printer.p(n.getNode(0));
  }

  /** Visit the specified primitive type. */
  public void visitPrimitiveType(GNode n) {
    printer.p(n.getString(0));
  }

  public void visitStringLiteral(GNode n){
    printer.p(n.getString(0));
  }

  public void visitBlock(GNode n){
    visit(n);
    printer.decr();
    printer.pln();
  }

  public void visitConstructorDeclaration(GNode n){
    String constructorName = n.getString(2);
    /* TODO: Allow constructor to accept parameters */
    printer.p(className + "::" + constructorName + "()" );
    printer.p(" : ");
    printer.p("__vptr(&__vtable), ");
    visit(n);
    printer.p("{");
    printer.p("}");
    printer.pln();
    printer.pln();
  }
  public void visitConstructorExpression(GNode n){
    printer.p(n.getString(0) + "(" +n.getString(1) +") ");
  }

  public void visitFieldDeclaration(GNode n){
	  visit(n);
    printer.p(";");
    printer.pln();
  }
  public void visitReturnStatement(GNode n){
    printer.p("return ");
    visit(n);
    printer.p(";").pln();
  }

  public void visitExpressionStatement(GNode n){

    if ((n.getNode(0).getName().equals("CallExpression")) &&
       (n.getNode(0).getNode(0).getString(0).equals("cout"))){
        printer.p("cout << ");
        printer.p(n.getNode(0).getNode(3).getNode(0).getNode(0).getString(0));
        printer.p("->__vptr->");
        String functionName = n.getNode(0).getNode(3).getNode(0).getString(2);
        printer.p(functionName);
        printer.p("(");
        visit(n);
        printer.p(")");
        if (functionName.equals("toString")){
          printer.p("->data");
        }
        printer.p(" <<endl");
    }
    else{
      visit(n);
    }
    printer.pln(";");
  }

  public void visitExpression(GNode n) {
    printer.p(n.getNode(0));
    if (n.get(1) != null) printer.p(" = ");
    if (n.get(2) != null) printer.p(n.getNode(2));
    //visit(n);
  }
  public void visitPrimaryIdentifier(GNode n) {
    //if this is a field, prepend "__this->"
    String variableName = n.getString(0);
    if (classFields.contains(variableName)){
      printer.p("__this->" + variableName);
    }
    else{
        printer.p(variableName);
    }
  }

  public void visitDeclarators(GNode n){
	  visit(n);
  }
  public void visitDeclarator(GNode n){
    printer.p(" " + n.getString(0) + " = ");
    visit(n);
  }
  public void visitIntegerLiteral(GNode n){
    printer.p(n.getString(0));
  }
  public void visitCastExpression (GNode n){
    printer.p("(");
    printer.p(n.getNode(0));
    printer.p(")");
    printer.p(n.getNode(1));
  }

  public void visitNewClassExpression(GNode n){
    String className = fold((GNode)n.getNode(2), n.getNode(2).size());
    printer.p("new __" + className + "(");
    printer.p(n.getNode(3));
    printer.p(")");
  }

  /** Visit the specified formal parameter. */
  public void visitFormalParameter(GNode n) {
    final int size = n.size();
    printer.p(n.getNode(0)).p(n.getNode(1));
    for (int i=2; i<size-3; i++) { // Print multiple catch types.
      printer.p(" | ").p(n.getNode(i));
    }
    if (null != n.get(size-3)) printer.p(n.getString(size-3));
    printer.p(' ').p(n.getString(size-2)).p(n.getNode(size-1));
  }

  public void visitFormalParameters(GNode n) {
    Iterator<Object> iter = n.iterator();
      if (iter.hasNext()){
      printer.p('(');
      for (iter = n.iterator(); iter.hasNext(); ) {
        printer.p((Node)iter.next());
        if (iter.hasNext()) printer.p(", ");
      }
      printer.p(')');
    }
  }

  /** Visit the qualified identifier. */
  public void visitQualifiedIdentifier(GNode n) {
    if (1 == n.size()) {
        printer.p(n.getString(0));
    } else {
      for (Iterator<Object> iter = n.iterator(); iter.hasNext(); ) {
        printer.p(Token.cast(iter.next()));
        if (iter.hasNext()) printer.p('.');
      }
    }
  }

  public void visit(Node n) {
    for (Object o : n) if (o instanceof Node) dispatch((Node) o);
  }

  /**
   * Fold the specified qualified identifier.
   *
   * @param qid The qualified identifier.
   * @param size Its size.
   */
  protected String fold(GNode qid, int size) {
    StringBuilder buf = new StringBuilder();
    for (int i=0; i<size; i++) {
      buf.append(qid.getString(i));
      if (i<size-1) buf.append('.');
    }
    return buf.toString();
  }

  /* Print the string out as a line unless it is null */
  private void printlnUnlessNull(String s){
    printlnUnlessNull(s, s);
  }

  private void printlnUnlessNull(String s, String compare){
    if (!(compare == null)) printer.pln(s);
    else return;
  }

  private void printClassMethod(){
    if (javaClassName != null){
      printer.pln(className + "_VT " + className + "::__vtable;");
      printer.pln();
      printer.pln("Class " + className + "::__class() {");
      printer.pln("static Class k =");
      printer.pln("new __Class(__rt::literal(\"" + packageName + "." + javaClassName + "\"), (Class) __rt::null());");
      printer.pln("return k;");
      printer.pln("}");
    }
  }

}
