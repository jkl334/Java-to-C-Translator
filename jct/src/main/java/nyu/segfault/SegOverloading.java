package nyu.segfault;

import java.lang.*;
import java.util.LinkedList;

import xtc.lang.JavaEntities;
import xtc.Constants;
import xtc.tree.LineMarker;
import xtc.tree.Attribute;
import xtc.tree.Node;
import xtc.tree.GNode;
import xtc.tree.Pragma;
import xtc.tree.SourceIdentity;
import xtc.tree.Token;
import xtc.tree.Visitor;
import xtc.util.SymbolTable;
import xtc.util.SymbolTable.Scope;
import xtc.type.*;

public class SegOverloading extends Visitor{
	final private SymbolTable table;
	SegInheritanceBuilder inheritanceTree;
	private String className;
  	private String javaClassName;
  	private LinkedList<String> overloadedNames;
	LinkedList<String> primTypes = new LinkedList<String>();

  	public boolean isPrim(String s){
    	return primTypes.contains(s);
  	}

  	public SegOverloading(SymbolTable table, SegInheritanceBuilder inh, LinkedList<String> oNames){
	    this.table = table;
	    this.inheritanceTree = inh;
	    this.overloadedNames = oNames;
	    primTypes.add("int");
	    primTypes.add("byte");
	    primTypes.add("long");
	    primTypes.add("boolean");
	    primTypes.add("double");
	    primTypes.add("float");
	    primTypes.add("short");
	    primTypes.add("char");
	}

	public void visitCompilationUnit(GNode n){
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

	public void visitPackageDeclaration(GNode n){
	    if (! (n == null)){
	      table.enter(n);
	    }
	}

	public void visitImportDeclaration(GNode n){
		visit(n);
	}

	public void visitClassDeclaration(GNode n){
	    table.enter(n);
	    className = n.getString(1);
	    visit(n);
	    table.exit();
	}

	public void visitMethodDeclaration(GNode n){
	    table.enter(n);
	    
	    if (!(n.getNode(4).size() !=0)) {
	      javaClassName = n.getString(5);
	    }
	    visit(n);
	    table.exit();
  	}

  	public void visitBlock(GNode n){
	    table.enter(n);
	    visit(n);
	    table.exit();
  	}


	public void visitClassBody(GNode n){
		visit(n);
	}

	public void visitExpressionStatement(GNode n){
	    visit(n);
	}

	public void visitCallExpression(GNode n){
	boolean overloaded = false;

	for (String o : overloadedNames) { //Checks if there's overloading
	  if (o.equals(n.getString(2))) {
	    overloaded=true;
	    break;
	  }
	}

	if (overloaded==false) {
	  return;
	}

	// name of class is the class name incase of static methods
	String nameOfClass = className;
	// else it is the class of the primary identifier 
	if (n.getNode(0) != null){
	  String variableName = n.getNode(0).getString(0);
	  if (table.current().isDefined(variableName)) {
	  Type type = (Type) table.current().lookup(variableName);
	  nameOfClass = type.toAlias().getName();
	  }
	}
	if (overloaded){
	  LinkedList<String> argumentList = new LinkedList<String>();
	  String actual_method = n.getString(2);
	  LinkedList<String> methods = inheritanceTree.getVTableForNode(nameOfClass);
	  argumentList = visitArguments((GNode)n.getNode(3));
	  for (int i = 0; i < argumentList.size(); i++){
	    actual_method = actual_method + "_" + argumentList.get(i);
	  }
	  if (methods.contains(actual_method)){
	    n.set(2,actual_method);
	  }
	  else{
	    if (argumentList.size() > 0){
	      String parent = inheritanceTree.getParentOfNode(argumentList.get(0));
	      actual_method = n.getString(2);
	      actual_method = actual_method + "_" + parent;
	      if (methods.contains(actual_method)){
	      n.set(2,actual_method);
	      changeArguments((GNode)n.getNode(3), parent);
	      }
	    }
	  }
	}
	}

	public void changeArguments(GNode n, String cast){
	for (int i = 0; i < n.size() ; i++){
	  if (n.getNode(i).hasName("PrimaryIdentifier")){
	    String name = n.getNode(i).getString(0);
	    String nameOfClass = "";
	    if (table.current().isDefined(name)) {
	      Type type = (Type) table.current().lookup(name);
	      if (type.hasAlias()){
	      nameOfClass = type.toAlias().getName();
	      }
	      else {
	      WrappedT wtype = (WrappedT) table.current().lookup(name);
	      nameOfClass = wtype.getType().toString();
	      }
	    }
	    String parent = inheritanceTree.getParentOfNode(nameOfClass);
	    if (parent.equals(cast)){
	      String newname = "("+cast+")" + name;
	      n.getNode(i).set(0, newname);
	    }
	  }
	}
	}

	public LinkedList<String> visitArguments(GNode n){
	LinkedList<String> answer = new LinkedList<String>();
	if (n.size() == 0){
	  return answer;
	}
	for (int i = 0; i < n.size() ; i++){
	  if (n.getNode(i).hasName("AdditiveExpression")){
	    answer.add(visitAdditiveExpression((GNode)n.getNode(i)));
	  }
	  if (n.getNode(i).hasName("NewClassExpression")){
	    answer.add(visitNewClassExpression((GNode)n.getNode(i)));
	  }
	  if (n.getNode(i).hasName("PrimaryIdentifier")){
	    answer.add(visitPrimaryIdentifier((GNode)n.getNode(i)));
	  }
	  if (n.getNode(i).hasName("CastExpression")){
	    answer.add(visitCastExpression((GNode)n.getNode(i)));
	  }
	  if (n.getNode(i).hasName("StringLiteral")){
	    answer.add(visitStringLiteral((GNode)n.getNode(i)));
	  }
	}
	return answer;
	}  

	public String visitAdditiveExpression(GNode n){
	String answer = "";
	LinkedList<String> type = new LinkedList<String>();
	for (int i = 0; i < n.size(); i++){
	  if (!(n.get(i) instanceof String) && n.getNode(i).hasName("PrimaryIdentifier")){
	    type.add(visitPrimaryIdentifier((GNode)n.getNode(i)));
	  }
	}
	if (isPrim(type.get(0))){
	  if (type.contains("long")){
	    answer = "long";
	  }
	  else if (type.contains("double")){
	    answer = "double";
	  }
	  else{
	    answer = "int32_t";
	  }
	}
	else {
	}

	return answer;
	} 


	public String visitPrimaryIdentifier(GNode n) {
	String variableName = n.getString(0);
	String nameOfClass = "";
	if (table.current().isDefined(variableName)) {
	  Type type = (Type) table.current().lookup(variableName);
	  if (type.hasAlias()){
	    nameOfClass = type.toAlias().getName();
	  }
	  else {
	    WrappedT wtype = (WrappedT) table.current().lookup(variableName);
	    nameOfClass = wtype.getType().toString();
	  }
	}
	return nameOfClass;
	} 



	public String visitNewClassExpression(GNode n){
	return n.getNode(2).getString(0);
	}
	public String visitCastExpression(GNode n){
	return n.getNode(0).getNode(0).getString(0);
	}
	public String visitStringLiteral(GNode n){
	return "String";
	}


	public void visit(Node n){
		for (Object o : n) if (o instanceof Node) dispatch((Node) o);
	}
}
