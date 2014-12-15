package nyu.segfault;

import java.lang.*;
import java.util.Iterator;
import java.util.LinkedList;
import xtc.Constants;
import xtc.lang.JavaEntities;
import xtc.tree.LineMarker;
import xtc.tree.Attribute;
import xtc.tree.Node;
import xtc.tree.GNode;
import xtc.tree.Pragma;
import xtc.tree.Printer;
import xtc.tree.SourceIdentity;
import xtc.tree.Token;
import xtc.tree.Visitor;
import xtc.type.*;
import xtc.util.SymbolTable;
import xtc.util.SymbolTable.Scope;

public class SegImp extends Visitor {
  final private SymbolTable table;
  SegInheritanceHelper inheritanceTree;

  protected Printer printer;
  protected Printer header;
  public GNode root;
  private LinkedList<String> staticMethods;

  private String packageName;
  private String className;     // c++ style
  private String javaClassName;
  private boolean visitedConstructor;
  private boolean visitedNewClassExp;
  private boolean visitedConstructorFormalParam;
  private boolean createdInitMethod;
  private boolean insideConstBlock;
  private boolean inForControl = false;

  LinkedList<GNode> classFields = new LinkedList<GNode>();
  LinkedList<String> constructorArgs = new LinkedList<String>();

	public SegImp(Printer p, SymbolTable table, Inheritance inh, LinkedList<String> sNAmes){
    this.printer = p;
    this.table = table;
    printer.register(this);
    this.inheritanceTree = inh;
    this.staticMethods = sNAmes;

	}

  public void visitSegFieldDeclaration(GNode n){
    classFields.add(n);
  }

	public void visitCompilationUnit(GNode n) {
    if (null == n.get(0))
      visitPackageDeclaration(null);
    else
      dispatch(n.getNode(0));

    table.enter(n);
    
    for (int i = 1; i < n.size(); i++) {
      GNode child = n.getGeneric(i);
      dispatch(child);
    }

    table.setScope(table.root());
  }

	public void visitClassDeclaration(GNode n) {
    table.enter(n);
    className = n.getString(1);
    javaClassName = className.substring(2);
    visit(n);
    table.exit();
  }

  public void visitPackageDeclaration(GNode n) {
    if (! (n == null)){
      table.enter(n);
      GNode qid  = n.getGeneric(1);
      int   size = qid.size();
      packageName = fold(qid,size);
    }

  }
  public void visitImportDeclaration(GNode n) {
    printer.p("using namespace ");
    visit(n);
    printer.pln(";");
    printer.pln("");
  }

	public void visitClassBody(GNode n) {
    printer.incr();
    printlnUnlessNull("namespace " + packageName + " {", packageName);
    visit(n);
    if (visitedConstructor == false) {
      printSecondinit();
    }
    printClassMethod(); 

    printlnUnlessNull("}",packageName);
    printer.decr();
  }

  public void visitMethodDeclaration(GNode n){
    table.enter(n);
    String methodName = n.getString(3);
    if (methodName.equals("main")) {
      printer.pln("int main(void){");
      printer.p(n.getNode(7));
      printer.pln("return 0;");
      printer.pln("}");
    }
    else {
    printer.p(n.getNode(2));
    if (n.getNode(2).hasName("VoidType")){
      printer.p("void");
    }
    printer.p(" ");

    printer.p(className + "::" + methodName);
    if (n.getNode(4).size() !=0) {
      printer.p(n.getNode(4)).p(" {");
      printer.incr();
    }
    else {
      printer.p("(" + javaClassName + " __this)" + " {");
    }
    printer.pln();
    printer.p(n.getNode(7));
    printer.pln("}");
  }
  table.exit();
  }

  /** Visit the specified type. */
  public void visitType(GNode n) {
    printer.p(n.getNode(0));
  }

  /** Visit the specified primitive type. */
  public void visitPrimitiveType(GNode n) {
    if(n.getString(0).equals("int")){
      printer.p("int32_t");
    }
    else{
      printer.p(n.getString(0));
    }
  } 

  public void visitStringLiteral(GNode n){
    printer.p(n.getString(0));
  }
  public void visitCastS(GNode n){
    printer.p(n.getString(1));
  }

  public void visitBlock(GNode n){
    table.enter(n);
    printer.pln("{");
    visit(n);
    printer.decr();
    printer.pln("}");
    printer.pln();
    table.exit();
  }

  public void visitBasicForControl(GNode n){
    inForControl = true;
    printer.p("(");
    visit(n);
    inForControl = false;
    printer.p(")");
    printer.pln();
  }

  public void visitForStatement(GNode n){
    table.enter(n); 
    printer.p("for");
    visit(n);
    table.exit();
  }

  public void visitPostfixExpression(GNode n){
    visit(n);
    printer.p(n.getString(1));
  }

  public void visitRelationalExpression(GNode n){
    dispatch(n.getNode(0));
    printer.p(n.getString(1));
    dispatch(n.getNode(2));
    if(inForControl){
      printer.p(";");
    }
  }

  public void visitSubscriptExpression(GNode n){
    dispatch(n.getNode(0));
    printer.p("[");
    dispatch(n.getNode(1));
    printer.p("]");
  }

  public void visitConstructorDeclaration(GNode n){
    visitedConstructor = true;
    printSecondConstructor();
    String constructorName = n.getString(2);
    if (n.getNode(3).size() == 0){
      printer.p(javaClassName + " " + className + "::init(" +javaClassName + " __this" );
      createdInitMethod = true;
    }
    else {
      visitedConstructorFormalParam = true;
      printer.p(javaClassName + " " + className + "::init(" +javaClassName + " __this, " );
    }
    printer.p(n.getNode(3));
    for (int i = 0; i < n.getNode(3).size(); i++){
      constructorArgs.add(n.getNode(3).getNode(i).getString(3));
    }
    printer.pln("){");
    String parent = inheritanceTree.getParentOfNode(javaClassName);
    if (parent != null){
      printer.pln("__"+ parent + "::init(__this);");
    }
    insideConstBlock = true;
    visit(n.getNode(5));
    insideConstBlock = false;
    for (int i = 0; i < classFields.size(); i++){
      printSegField(classFields.get(i));
    }
    printer.pln("return __this;");
    printer.p("}");
    printer.pln();
    printer.pln();
  }
  boolean visitedSelectionExpression = false;
  public void visitSelectionExpression(GNode n){
    visit(n);
    printer.p("->" + n.getString(1));
    visitedSelectionExpression = true;
  }

  private void printSegField(GNode n){
    if (n.getNode(2).hasName("Declarators")){
      GNode declaration = (GNode)n.getNode(2).getNode(0);
      if (declaration.getNode(2) != null){
        printer.p("__this->" + declaration.getString(0));
        printer.p(" = ");
        if (declaration.getNode(2).hasName("StringLiteral")){
          printer.p("__String::init(new __String(");
          printer.p(declaration.getNode(2));
          printer.p("));");
          printer.pln();  
        }
      }
    }
  }

  private void printSecondinit(){
    printer.pln();
    printSecondConstructor();
    printer.pln(javaClassName + " " + className + "::init(" +javaClassName + " __this){" );
    String parent = inheritanceTree.getParentOfNode(javaClassName);
    if (parent != null){
      printer.pln("__"+ parent + "::init(__this);");
    }
    printer.pln("return __this;");
    printer.p("}");
    printer.pln();
    printer.pln();

  }

  public void visitFieldDeclaration(GNode n){
	  visit(n);
    printer.p(";");
    printer.pln();
    if (visitedNewClassExp || visitedSelectionExpression){
      printer.p("__rt::checkNotNull(");
      String id = n.getNode(2).getNode(0).getString(0);
      printer.p(id+");");
      printer.pln();
      visitedNewClassExp = false;
    }
  }
  public void visitReturnStatement(GNode n){
    printer.p("return ");
    if (n.getNode(0).getName().equals("StringLiteral")){
      printer.p("new __String(");
      visit(n);
      printer.p(")");
    }
    else{
      visit(n);
    }
    printer.p(";").pln();
  }

  public void visitExpressionStatement(GNode n){
    visit(n);
    printer.pln(";");
  }

  public void visitCallExpression(GNode n){
    if (n.getNode(0).hasName("SuperExpression")){
      String parent = inheritanceTree.getParentOfNode(javaClassName);
      printer.pln(parent +" tmp = __"+ parent + "::init(new __"+ parent+"());");
    }
    String variableName = "";
    if (n.getNode(0).hasName("PrimaryIdentifier")){
      variableName = n.getNode(0).getString(0);
    }
    boolean static_name = false;
    boolean using_static_class_name = false;
    if (staticMethods.contains(n.getString(2))){
      static_name = true;
      if (!(table.current().isDefined(variableName))){
        using_static_class_name = true;
      }
    }
    if (static_name){
      if(using_static_class_name){
        printer.p("__"+n.getNode(0).getString(0)+"::");
      }
      else{
        printer.p(n.getNode(0));
        printer.p("->");
      }
    }
    else{
      printer.p(n.getNode(0));
      printer.p("->__vptr->");
    }
    printer.p(n.getString(2) + "(");
    if (using_static_class_name){
      printer.p("__" + variableName + "::init(new __" + variableName + "())");
    }
    else{
      printer.p(n.getNode(0));
    }
    if(n.getNode(3).size() > 0)
    printer.p(", ");	
    printer.p(n.getNode(3));    
    printer.p(")");
  }

  public void visitSuperExpression(GNode n){
    String parent = inheritanceTree.getParentOfNode(javaClassName);
    printer.p("tmp");
  }

  public void visitInstanceOfExpression(GNode n){
    String variableName = n.getNode(0).getString(0);
    String start = null;
    if (table.current().isDefined(variableName)) {
      Type type = (Type) table.current().lookup(variableName);
      if (type.hasAlias()){
        start = type.toAlias().getName();
      }
    }
    String target =  n.getNode(1).getNode(0).getString(0);
    if (start != null){
      if (getDistance(start,target) >= 0){
        printer.p("\"true\"");
      }
    }
  }

  public void visitFloatingPointLiteral(GNode n){
    printer.p(n.getString(0));
  }


  // gets distance from node to parent or root node in its heirchy
  private int getDistance(String start, String target){

    if(start.equals(target)) return 0;
   
    int distance = 0;
    boolean found = false;
    String parent = start;

    while (!parent.equals(target)){
      parent = inheritanceTree.getParentOfNode(parent);
      if (parent.equals(target)){
        found = true;
      }
      distance++;

      if (parent.equals("Object")){
        break;
      }
      if (parent.equals("No Parent Found")){
        found = false;
        break;
      }
    }

    if (found) {
      return distance;
    }
    else{
      return -1; //could not find
    }
  }

  public void visitArguments(GNode n){

      for (int i = 0; i < n.size() ; i++){
        if (n.getNode(i).hasName("StringLiteral")){
          printer.p("__String::init(new __String(");
          printer.p(n.getNode(i));
          printer.p("))");
        }
        else{
          printer.p(n.getNode(i));
        }
        if (! (i==n.size()-1)){
          printer.p(",");
        }
      }

  }  

  public void visitAdditiveExpression(GNode n){
    if (n.getNode(0).hasName("visitAdditiveExpression")){
      visit(n);
    }
    else {
      printer.p(n.getNode(0));
    }
    printer.p(n.getString(1));
    printer.p(n.getNode(2));
  } 

  public void visitCoutExpression(GNode n){
    printer.p("cout << ");
    visit(n);
    printer.p(" <<endl");
  }

  public void visitCoutCallExpression(GNode n){
    printer.p(n.getNode(0));
    printer.p("->__vptr->");
    String functionName = n.getString(1);
    printer.p(functionName);
    printer.p("(");
    visit(n);
    printer.p(")");

  }
  public void visitCoutAdditiveExpression(GNode n){
    String variableName = n.getNode(2).getString(0);
    if (n.getNode(0).hasName("visitCoutAdditiveExpression")){
      visit(n);
    }
    else {
      printer.p(n.getNode(0));
    }
    printer.p(" << ");
    boolean primT = false;
    boolean castAsInt = false;

    if (table.current().isDefined(variableName)) {
      Type type = (Type) table.current().lookup(variableName);
      if (!type.hasAlias()){
        primT = true;
      }
      String typeAsString = type.toString();
      if (typeAsString.contains("(byte,")){
        castAsInt = true;
      }
    }
    if (castAsInt) printer.p("(int)");
    printer.p(n.getNode(2));
    if ((!primT) && n.getNode(2).hasName("PrimaryIdentifier")){
      printer.p("->__vptr->toString(");
      printer.p(n.getNode(2));
      printer.p(")");
    }
  }

  public void visitExpression(GNode n) {
    printer.p(n.getNode(0));
    if (n.get(1) != null) printer.p(" = ");
    if (n.get(2) != null) printer.p(n.getNode(2));
  }

  public void visitPrimaryIdentifier(GNode n) {
    String variableName = n.getString(0);
    if (insideConstBlock){
      printHandleConst(n);
      return;
    }
    if (table.current().isDefined(variableName)) {
      Type type = (Type) table.current().lookup(variableName);
      if (JavaEntities.isFieldT(type)){
        if (table.current().isDefined(variableName)){
          printer.p("__this->" + variableName);
        }
      }
      else {
        printer.p(variableName);
      }
    }
    else{
        printer.p(variableName);
    }
  } 

  private void printHandleConst(GNode n){
    String variableName = n.getString(0);
    if(constructorArgs.size() == 0){
      if (table.current().isDefined(variableName)) {
        Type type = (Type) table.current().lookup(variableName);
        if (JavaEntities.isFieldT(type)){
          printer.p("__this->" + variableName);
        }
        else {
          printer.p(variableName);
        }
      }
      else{
        printer.p(variableName);
      }
    }
    else {
      for (int i = 0; i < constructorArgs.size(); i++){
        if (variableName.equals(constructorArgs.get(i))){
          printer.p(variableName);
        }
        else {
          printer.p("__this->" + variableName);
        }
      }
    }
  }

  public void visitDeclarators(GNode n){
    if (n.size() == 1) {
      visit(n);
    }
	  else {
      for (int i = 0; i < n.size(); i++) {
        printer.p(n.getNode(i));
        if (! (i == n.size()-1)){
          printer.p(",");
        }

      }
    }

    if (inForControl){
      printer.p(";");
    }
  }

  public void visitThisExpression(GNode n){
    printer.p("__this");
  }

  public void visitDeclarator(GNode n){
    printer.p(" " + n.getString(0));
    if (n.getNode(2) != null){
      printer.p(" = ");
      visit(n);
    }
    else{
      visit(n);
    }
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
    visitedNewClassExp = true;
    String className = fold((GNode)n.getNode(2), n.getNode(2).size());
    printer.p("__"+className+"::init(");
    if (className.equals("Exception")){
      printer.p("new " + className + "(");
    }
    else{
      printer.p("new __" + className + "()");
    }
    if (n.getNode(3).size() > 0){
      printer.p(", ");
      printer.p(n.getNode(3)); 
    }      
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
    if (visitedConstructorFormalParam){
      Iterator<Object> iter = n.iterator();
      if (iter.hasNext()){
        for (iter = n.iterator(); iter.hasNext(); ) {
          printer.p((Node)iter.next());
          if (iter.hasNext()) printer.p(", ");
        }
      }
      visitedConstructorFormalParam = false;
    }
    else{
  	Iterator<Object> iter = n.iterator();
  	if(!createdInitMethod){		
        		printer.p('(');
        		printer.p(javaClassName + " __this");			
  	}else{
  		createdInitMethod = false;
  	}
        if (iter.hasNext()){      
  	printer.p(", ");
        for (iter = n.iterator(); iter.hasNext(); ) {
          printer.p((Node)iter.next());
          if (iter.hasNext()) printer.p(", ");
        }
        printer.p(')'); 
      }
    }
  }

  /** Visit the specified qualified identifier. */
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

  //Fold the specified qualified identifier.

  protected String fold(GNode qid, int size) {
    StringBuilder buf = new StringBuilder();
    for (int i=0; i<size; i++) {
      buf.append(qid.getString(i));
      if (i<size-1) buf.append('.');
    }
    return buf.toString();
  }

  /* Unless the string is null, print it out as a line */
  private void printlnUnlessNull(String s){
    printlnUnlessNull(s, s);
  }

  /* Unless the compare string is null, print another string out as a line */
  private void printlnUnlessNull(String s, String compare){
    if (!(compare == null)) printer.pln(s);
    else return;
  }

  private void printClassMethod(){
    printer.pln(className + "_VT " + className + "::__vtable;");
    printer.pln();
    printer.pln("Class " + className + "::__class() {");
    printer.p("static Class k = ");
    printer.p("new __Class(__rt::literal(\"");
    if (packageName != null){
      printer.p(packageName + ".");
    }
    printer.p(javaClassName + "\"), (Class) __rt::null());");
    printer.pln();

    printer.pln("return k;");
    printer.pln("}");
  }

  private void printSecondConstructor(){
      printer.p(className + "::" + className + "()" );
      printer.p(" : ");
      printer.p("__vptr(&__vtable)");
      printer.p("{");
      printer.p("}");
      printer.pln();
      printer.pln();
  }

}
