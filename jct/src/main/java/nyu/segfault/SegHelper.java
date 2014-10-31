package nyu.segfault;

import java.io.*;
import java.util.*;

import java.lang.reflect.*;
import java.lang.Thread;

import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.util.SymbolTable;
import xtc.util.Tool;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;
	
public class SegHelper {
	/**
	* Get the name of the method in visitMethodDeclaration.
	*
	* @param n	The node from the Java AST.
	* @return	The name of the method.
	*/
	public static String getMethodName(GNode n) {
		return n.getString(3);
	}	



	static String gCallExpression;  // Global variable used with getCallExpression.
	/**
	* Get the C++ translation of a Java print statement. Must be used in
	* visitExpressionStatment's visitCallExpression.
	*
	* @param n	The node from the Java AST.
	* @return	A String representing the C++ version of a Java print statement.
	*/
	public static String getCallExpression(GNode n) {
		gCallExpression = "";

		boolean isEndLine = false;
		if (n.getNode(0).toString().contains("println")) {
			isEndLine = true;
		}

		gCallExpression += "\tcout";
		final ArrayList<String> vars = new ArrayList<String>();

		new Visitor() {
			public void visitStringLiteral(GNode n) {
				gCallExpression += " << " + n.getString(0);
			}
			
			public void visitIntegerLiteral(GNode n) {
				gCallExpression += " << " + n.getString(0);
			}

			public void visitFloatingPointLiteral(GNode n) {
				gCallExpression += " << " + n.getString(0);
			}

			public void visitCharacterLiteral(GNode n) {
				gCallExpression += " << " + n.getString(0);
			}
			
			public void visitBooleanLiteral(GNode n) {
				gCallExpression += " << " + n.getString(0);
			}

			public void visitNullLiteral(GNode n) {
				gCallExpression += " << " + "null";
			}

			public void visitPrimaryIdentifier(GNode n) {
				gCallExpression += " << " + n.getString(0);
			}
			
			public void visitCallExpression(GNode n) {
				String method = "";
				method +=n.getNode(0).getString(0);
				if (n.getString(2).isEmpty()) {
					method += "()";
				} else {
					method += "." + n.getString(2) + "(";
					if (n.getNode(3).isEmpty()) {
						method += ")";
					}
				}
				vars.add(method);
				gCallExpression += " << " + method;
			}

			public void visit(GNode n) {
				for (Object o : n) {
					if (o instanceof Node) { 
						dispatch ((Node) o);
					}
				}
			}	
		}.dispatch(n.getNode(0));

		if (isEndLine) {
			gCallExpression += " << \"\\n\"";
		}		
		return gCallExpression += ";";
	}	

	/**
	* Determines whether the given GNode represents a print statement. Must be used
	* in visitExpressionStatement.
	*
	* @param n	The node from the Java AST.
	* @return	true or false, depending on whether or not the GNode represents
	*		a print statement.
	*/
	public static boolean isPrintStatement(GNode n) {
		return (n.toString().contains("CallExpression") && 
				n.toString().contains("SelectionExpression") && 
				n.toString().contains("System") && 
				n.toString().contains("out"));

	}
	/**
	 * extract class name from node
	 * @param node node from java parse tree
	 * @return formated String represented class name
	 */
	public static  String getClassName(GNode n){ 
		validCall();
		return n.getString(1); 
	}

	/**
	 * extract class declaration from node
	 * @param node node from java parse tree
	 * @return formated string  c++ struct declaration
	 */
	public static  String getClassDeclaration(GNode n){
		validCall();
		return  "struct "+ n.getString(1);
	}

	/**
	 * extract method declaration from node
	 * @param node node from java parse tree
	 * @return formated function prototype c++
	 */
	public static String getMethodDeclaration(GNode n,String className){
		validCall();
		final StackTraceElement[] s=Thread.currentThread().getStackTrace();
		if((s[1].getClassName().equals("SegHead")) || (s[1].getClassName().equals("SegImp"))
			||(s[1].getClassName().equals("SegCVT"))){
			
			String return_type=j2c(n.getNode(2).toString());
			String fp=n.getString(3)+"("; 
			if( n.size() == 0 ) fp+=")";
			else fp+=getFormalParameters((GNode)n.getNode(4)); 


			if(s[1].getClassName().equals("SegHead"))
				return return_type+" "+fp;

			else if((s[1].getClassName().equals("SegImp")) || s[1].getClassName().equals("SegCVT"))
				return return_type+" "+className+"::"+fp;

			else if((s[1].getClassName().equals("SegHVT"))){
				
			}
		}
		return null;
	}
	
	/**
	 * extract function parameters
	 * @param n node from java parse tree
	 * @return formated list of parameter types nad parameter names 
	 */
	private static  String getFormalParameters(GNode n){
		String fp="";
		for(int i=0; i< n.size(); i++){
			Node fparam=n.getNode(i);

			//retrieve argument type
			fp+= j2c(fparam.getNode(1).getNode(0).getString(0))+" ";

			//retrieve argument name
			fp+=fparam.getString(3);

			if(i+1 < n.size()) fp+=",";
			else fp+=")";
		}
		return fp;
	}
	
	/**
	 *  convert raw type provided by xtc to c++ type
	 *  @param javaType  raw java type from xtc node
	 *  @return formated c++ type
	 */
	private static String j2c(String jType){
		String cType="";
		if (jType.equals("String")) cType="string";
		else if(jType.equals("VoidType()")) cType="void";
		else if (jType.equals("Integer")) cType="int";

		return cType;
	}

	/**
	 *  check if visit object methods calls appropriate Helper function
	 */
	private static void validCall(){
		final int x=2; final int y=1;
		final StackTraceElement[] ste=Thread.currentThread().getStackTrace();

		String sh_yfunc=ste[y].getMethodName();
		String sh_xfunc=ste[x].getMethodName();

		final String[] comp=new String[]{"Class","Method"};
		
		for (int k=0;k<comp.length;k++) {
			if(sh_xfunc.contains(comp[k]) &&  sh_yfunc.contains(comp[k])) return; 
		}
		throw new RuntimeException("visit function  to not correspond to helper function");
	}
}
