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
  	private LinkedList<String> staticMethods;
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

	public String visitCallExpression(GNode n){
		boolean overloaded = false;
	    String methodName = n.getString(2);
		//Checks if there's overloading
		for (String o : overloadedNames) { 
		  if (o.equals(n.getString(2))) {
		    overloaded=true;
		    break;
		  }
		}
	    String nameOfClass = className; 
	    if (n.getNode(0) != null && n.getNode(0).hasName("PrimaryIdentifier")){
	      String variableName = n.getNode(0).getString(0);
	      if (table.current().isDefined(variableName)) {
	      Type type = (Type) table.current().lookup(variableName);
	      nameOfClass = type.toAlias().getName();
	      }
	    }

	    if (overloaded==false) {
	      String returntype;
	      if (inheritanceTree.getReturnType(methodName, nameOfClass) != null){
	        returntype = inheritanceTree.getReturnType(methodName, nameOfClass);
	        if (returntype != null){
	          return returntype;
	        }
	      }
	      
	      return null;
	    }

	    if (overloaded){
	      LinkedList<String> argumentList = new LinkedList<String>();
	      String actual_method = n.getString(2);
	      LinkedList<String> methods = inheritanceTree.getVTableForNode(nameOfClass);
	      if (methods.isEmpty()){
	        if (n.getNode(0).hasName("PrimaryIdentifier")){
	          nameOfClass = n.getNode(0).getString(0);
	        }
	        methods = inheritanceTree.getVTableForNode(nameOfClass);
	      }
	      if (n.getNode(0).hasName("CallExpression")){
	        actual_method = actual_method + visitCallExpression((GNode) n.getNode(0));
	        if (methods.contains(actual_method)){
	          n.set(2,actual_method);
	          return inheritanceTree.getReturnType(actual_method,nameOfClass);
	        }
	        else if (staticMethods.contains(actual_method)){
	          n.set(2,actual_method);
	          return inheritanceTree.getReturnType(actual_method,nameOfClass);
	        }
	        return null;
	      }
	      argumentList = visitArguments((GNode)n.getNode(3));
	      for (int i = 0; i < argumentList.size(); i++){
	        actual_method = actual_method + "_" + argumentList.get(i);
	      }
	      if (methods.contains(actual_method)){
	        n.set(2,actual_method);
	      }
	      else if (staticMethods.contains(actual_method)){
	        n.set(2,actual_method);
	      }
	      else{
	        String suitable_method = findSuitableMethod(n, methods, argumentList, actual_method);
	        if (suitable_method != null){
	          n.set(2,suitable_method);
	          return inheritanceTree.getReturnType(suitable_method,nameOfClass);
	        }
	      }
	    }
	    return null;
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
	      if (n.getNode(i).hasName("CallExpression")){
	        answer.add(visitCallExpression((GNode)n.getNode(i)));
	      }
	      if (n.getNode(i).hasName("FloatingPointLiteral")){
	        answer.add(visitFloatingPointLiteral((GNode)n.getNode(i)));
	      }
	    }
	    return answer;
	}  

	public String visitFloatingPointLiteral(GNode n){
    	return "double";
 	}

	private int getArgumentCount(String s){
		int count = 0;
		for (int i = 0; i<s.length();i++){
		  if (s.charAt(i) == '_'){
		    count++;
		  }
		}
		return count;
	}

  private LinkedList<String> getArguments(String s){
    LinkedList<String> foundArgs = new LinkedList<String>();
    for(int i = 0; i<s.length();i++){
      if (s.charAt(i) == '_'){
        int nextUnderscore = s.indexOf("_",i+1);
        if (nextUnderscore == -1){
          foundArgs.add(s.substring(i+1));
        }
        else{
          foundArgs.add(s.substring(i+1, nextUnderscore));          
        }
      }
    }
    return foundArgs;
  }

  private LinkedList<String> removeByArgumentCount( LinkedList<String> methods, LinkedList<String> argumentList, String idealMethod){
	LinkedList<String> newMethods = new LinkedList<String>();
	int size = argumentList.size();
	int mSize = 0;
	for(int i = 0; i < methods.size(); i++){
	  String m = methods.get(i);
	  mSize = getArgumentCount(m);
	  if (size == mSize){
	    newMethods.add(methods.get(i));
	  }
	}
	return newMethods;
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
	        answer = "int";
	      }
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

	private int getDistance(String start, String target){
		if(start.equals(target)) return 0;
		if (start.equals("byte") && (target.equals("int") || target.equals("double")) ){
		  return 0;
		}
		if (start.equals("int") && (target.equals("double") || target.equals("long")) ){
		  return 0;
		}
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
		  return -1; 
		}
	}

  private LinkedList<String> duplicateLinkedList(LinkedList<String> old){
    LinkedList<String> newLL = new LinkedList<String>();
    for(String s : old){
      newLL.add(s);
    }
    return newLL;
  }

  private LinkedList<String> removeByRelationship(  LinkedList<String> methods,
                                      LinkedList<String> idealArgs,
                                      String idealMethod){
    LinkedList<String> mArgs = new LinkedList<String>();
    LinkedList<String> newMethods = new LinkedList<String>();
    newMethods = duplicateLinkedList(methods);

    for (int i = 0; i<methods.size(); i++){
      String m = methods.get(i);
      mArgs = getArguments(m);

      innerloop:
      for(int j = 0; j<mArgs.size(); j++){
        if (isPrim(idealArgs.get(j))){
          if(isPrim(mArgs.get(j))){
            if (idealArgs.get(j).equals("byte")){
            }
          }
          else{
            newMethods.remove(methods.get(i));
            break innerloop;
          }
        }
        String arg = mArgs.get(j);
        if (idealArgs.get(j) != arg){
          int dist = getDistance(idealArgs.get(j),arg);
          if (dist == -1){
            newMethods.remove(methods.get(i));
            break innerloop;
          }
        }
      }
    }

    return newMethods;
  }

  
  private String rankSelect( LinkedList<String> methods, LinkedList<String> argumentList, String idealMethod){
    String bestMatchName = "";
    int bestMatchValue = 999; //arbitrary large number
    int distance = -1;
    LinkedList<String> mArgs;
    for(String m : methods){
      mArgs = getArguments(m);
      distance = 0;
      for (int j = 0; j<mArgs.size(); j++){
        distance += getDistance(argumentList.get(j),mArgs.get(j));
      }
      if (distance < bestMatchValue){
        bestMatchValue = distance;
        bestMatchName = m;
      }
    }
    if (bestMatchValue == -1){
    }
    return bestMatchName;
  }

  private String findSuitableMethod(  GNode n, LinkedList<String> methods, LinkedList<String> argumentList, String idealMethod){
    methods = removeByArgumentCount(methods,argumentList,idealMethod);
    methods = removeByRelationship(methods,argumentList,idealMethod);
    String suitableMethod = rankSelect(methods,argumentList,idealMethod);
    if (suitableMethod.length() == 0){
      return null;
    }
    else{
      return suitableMethod;
    }
  }

}
