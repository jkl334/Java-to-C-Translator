package nyu.segfault;

import java.lang.*;
import xtc.lang.JavaEntities;


import xtc.Constants;
import java.util.LinkedList;
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

import java.util.logging.Logger;

/**
 * A visitor that goes through all the CallExpressions and tries to find 
 * the right method to call based on the arguments.
 */
public class SegOverloader extends Visitor {

	public final static Logger LOGGER = Logger.getLogger(SegDependency.class .getName());

	final private SymbolTable table;

	private String className;
	private String javaClassName;
	private LinkedList<String> overloadedNames;
	private LinkedList<String> staticMethods;

	  /** 
   *  A linked list of primitive types for personal purposes.
   */
  	LinkedList<String> primitives = new LinkedList<String>();


  /** visiting compilation unit. */
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

  /** visiting class declaration. */
	public void visitClassDeclaration(GNode n) {
	    table.enter(n);
	    className = n.getString(1);
	    visit(n);
	    table.exit();
  	}

  /** visiting package declaration. */
  	public void visitPackageDeclaration(GNode n) {
	    if (! (n == null)){
	      table.enter(n);
	    }
  	}
	  /** visiting import declaration. */
	public void visitImportDeclaration(GNode n) {
		visit(n);
	}
	  /** visiting class body. */
	public void visitClassBody(GNode n) {
	    visit(n);
	}
  	/** visiting method declaration. */
  	public void visitMethodDeclaration(GNode n){
    	table.enter(n);
    
    	if (!(n.getNode(4).size() !=0)) {
      		javaClassName = n.getString(5);
    	}
    	visit(n);
    	table.exit();
  	}

  	/** visiting block declaration. */
  	public void visitBlock(GNode n){
    	table.enter(n);
    	visit(n);
    	table.exit();
  	}
  	/** visiting For statement. */
 	public void visitForStatement(GNode n){
    	table.enter(n);
    	visit(n);
    	table.exit();
  	}
  	/** visiting Expression Statement. */
  	public void visitExpressionStatement(GNode n){
    	visit(n);
  	}

  	/** visiting Call Statement. */
  	public String visitCallExpression(GNode n){
    	boolean overloaded = false;
    	String methodName = n.getString(2);

	    for (String o : overloadedNames) { //Detects if there's overloading
	      	if (o.equals(n.getString(2))) {
	        	overloaded=true;
	        	LOGGER.info("Overloading happening for method " + o);
	        	break;
	      	}
	    }
	    
	    // name of class is the class name incase of static methods
	    String nameOfClass = className;
	    LOGGER.info("nameOfClass is " + nameOfClass);
	    // else it is the class of the primary identifier 
	    if (n.getNode(0) != null && n.getNode(0).hasName("PrimaryIdentifier")){
	      String variableName = n.getNode(0).getString(0);
	      LOGGER.info("variableName " + variableName);
	      if (table.current().isDefined(variableName)) {
	      Type type = (Type) table.current().lookup(variableName);
	      nameOfClass = type.toAlias().getName();
	      LOGGER.info("variableName " + variableName + " of className " + nameOfClass);
	      }
	    }

	    //If there's no overloading going on, we return the return type
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
    	 //TODO 
    	}    
    	return null;
	}
	  private String find_best(GNode n, LinkedList<String> methods, LinkedList<String> argumentList){
	    
	  }

	  /* gets the number of arguments given the method name*/
	  private int getArgumentCount(String s){
	    int count = 0;
		//count it 
	    
	    return count;
	  }

	  /* removes the methods from the linked list based on the # of arguments.*/
	  private LinkedList<String> removeByArgumentCount( LinkedList<String> methods,
	                                      LinkedList<String> argumentList,
	                                      String idealMethod){

	    LinkedList<String> newMethods = new LinkedList<String>();
	    
	    return newMethods;
	  }

	  /* Gets the distance a child is away from the parent.*/
	  private int getDistance(String start, String target){
	    LOGGER.info("start" + start + "target" + target);
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
	      LOGGER.info("getting Distance... " + parent);
	      parent = inheritanceTree.getParentOfNode(parent);
	      LOGGER.info(" is " + parent);
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

	  /* gets the arguments from a method and return a linked list*/
	  private LinkedList<String> getArguments(String s){
	    /*
	     * Args are split from a string like methodName_A_B_C_Object
	     * to a linkedlist like [A->B->C]
	     */

	    LinkedList<String> foundArgs = new LinkedList<String>();
	    for(int i = 0; i<s.length();i++){
	      if (s.charAt(i) == '_'){
	        int nextUnderscore = s.indexOf("_",i+1);
	        if (nextUnderscore == -1){
	          //did not fund an underscore. This occurs at end of file.
	          foundArgs.add(s.substring(i+1));
	        }
	        else{
	          foundArgs.add(s.substring(i+1, nextUnderscore));          
	        }
	      }
	    }
	    return foundArgs;
	  }

	  /* helper method to duplicate a linked list*/
	  private LinkedList<String> duplicateLL(LinkedList<String> old){
	    LinkedList<String> newLL = new LinkedList<String>();
	    for(String s : old){
	      newLL.add(s);
	    }
	    return newLL;
	  }


	  /* Remove any entries in the methods list which are impossible to call. */
	  private LinkedList<String> removeByRelationship(  LinkedList<String> methods,
	                                      LinkedList<String> idealArgs,
	                                      String idealMethod){
	    LOGGER.info("Removing for " + idealMethod);

	    LinkedList<String> mArgs = new LinkedList<String>();
	    LinkedList<String> newMethods = new LinkedList<String>();
	    newMethods = duplicateLL(methods);

	    for (int i = 0; i<methods.size(); i++){
	      String m = methods.get(i);
	      mArgs = getArguments(m);

	      innerloop:
	      for(int j = 0; j<mArgs.size(); j++){
	        /* 
	         * If both the ideal and found params are prim types,
	         * we check for equality and remove the method if its not equal. 
	         */
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

	        /* 
	         * Find whether or not an argument has valid a root path
	        */

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

	  /* calls another helper method to permute the different combinations of the
	     arguments*/
	  private String find_suitable_method(GNode n, LinkedList<String> methods, LinkedList<String> children, LinkedList<String> parent){
	    String actual_method = n.getString(2);
	    boolean found = false;
	    // This loop calls the helper method by giving it the number of arguments to permute on
	    // The first step is to permute 1 arguments and the last step is to permute them all.
	    for (int i = 1; i <= children.size(); i++ ){
	      String answer = permute_combinations(n, i, methods, children, parent);
	      if (answer != null){
	        found = true;
	        actual_method = answer;
	        break;
	      }
	    }
	    if (found){
	      return actual_method;
	    }
	    else 
	      return null;
	  }
	  /* continually check if the method is in the methods list. */
	  private String permute_combinations(GNode n, int m, LinkedList<String> methods, LinkedList<String> children, LinkedList<String> parent){
	    String actual_method = n.getString(2);
	    boolean found1 = false;
	    // builds a method name based on the int m passed to it and checks if it is in methods list.
	    outerloop:
	    for (int i = 0; i < children.size(); i++){
	      actual_method = n.getString(2);
	      for(int j = 0; j < i; j++ ){
	        actual_method = actual_method + "_" + children.get(j);
	      }
	      int x;
	      for (x = i; x < i+m; x++){
	          actual_method = actual_method + "_" + parent.get(i);
	      }
	      for(int k = x; k < children.size(); k++ ){
	        actual_method = actual_method + "_" + children.get(k);
	      }
	      if (methods.contains(actual_method)){
	        found1 = true;
	        break outerloop;
	      }
	    }

	    if(found1){
	      return actual_method;
	    }
	    else return null;
	  }
	  /* removes the method based upon its distance score*/
	  private String selectByPrecision( LinkedList<String> methods,
	                                                LinkedList<String> argumentList,
	                                                String idealMethod){
	    LOGGER.info("Selecting by precision for " + idealMethod);
	    String bestMatchName = "";
	    int bestMatchValue = 999999;
	    int distance = -1;
	    LinkedList<String> mArgs;

	    for(String m : methods){
	      mArgs = getArguments(m);
	      distance = 0;
	      for (int j = 0; j<mArgs.size(); j++){
	        /* See how far away an argument is from its possible candidate */
	        distance += getDistance(argumentList.get(j),mArgs.get(j));
	      }
	      if (distance < bestMatchValue){
	        bestMatchValue = distance;
	        bestMatchName = m;
	      }
	    }

	    if (bestMatchValue == -1){
	      LOGGER.warning("Could not find a suitable method");
	    }

	    return bestMatchName;
	  }

	  /* handler function to control all the steps in overloading resolution*/ 
	  private String findSuitableMethod(  GNode n, LinkedList<String> methods,
	                                        LinkedList<String> argumentList,
	                                        String idealMethod){
	  
	    methods = removeByArgumentCount(methods,argumentList,idealMethod);

	    methods = removeByRelationship(methods,argumentList,idealMethod);

	    String suitableMethod = selectByPrecision(methods,argumentList,idealMethod);

	    if (suitableMethod.length() == 0){
	      return null;
	    }
	    else{
	      return suitableMethod;
	    }
	  }
	  /** 
	   *  Visits the arguments of the Call expression and dispatches on them.
	   *  @param n GNode of Argument
	   *  @return the linked list of arguments.
	   */
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

	  /** 
	   *  just returns a double as a string.
	   *  @param n GNode of Floating Point Literal
	   *  @return the string double.
	   */
	  public String visitFloatingPointLiteral(GNode n){
	    return "double";
	  }
	 /** 
	   *  visits the additive arguments to determine what is being added.
	   *  @param n GNode of Additive Expression
	   *  @return the type of the result.
	   */
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
	    else {
	      LOGGER.info("Adding non primitive");
	    }

	    return answer;
	  } 

	   /** 
	   *  visits the primary identifier.
	   *  @param n GNode of Primary identifier
	   *  @return the string of the type of the primary identifier.
	   */
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
	    LOGGER.info("variableName " + variableName + " of className " + nameOfClass);
	    return nameOfClass;
	  } 

	  
	   /** 
	   *  visits the class expression.
	   *  @param n GNode of Class Expression
	   *  @return the type of the new class.
	   */
	  public String visitNewClassExpression(GNode n){
	    return n.getNode(2).getString(0);
	  }
	   /** 
	   *  visiting cast expression.
	   *  @param n GNode of Cast expression
	   *  @return the type of cast.
	   */
	  public String visitCastExpression(GNode n){
	    if (n.getNode(1).hasName("CallExpression")){
	      visitCallExpression((GNode)n.getNode(1));
	    }
	    return n.getNode(0).getNode(0).getString(0);
	  }
	   /** 
	   *  visits string literal.
	   *  @param n GNode of String Literal
	   *  @return the string.
	   */
	  public String visitStringLiteral(GNode n){
	    return "String";
	  }

	  public void visit(Node n) {
	    for (Object o : n) if (o instanceof Node) dispatch((Node) o);
	  }
}