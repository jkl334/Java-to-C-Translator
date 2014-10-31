package nyu.segfault;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import javax.swing.tree.DefaultTreeModel;

import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.util.SymbolTable;
import xtc.util.Tool;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;
	
public static class SegHelper {
	/**
	* Get the name of the method in visitMethodDeclaration.
	*
	* @param n	The node from the Java AST.
	* @return	The name of the method.
	*/
	public static String getMethodName(GNode n) {
		return n.getString(3);
	}	

	/**
	* Get a list of visitCallExpression's parameters. 
	* Must be used in visitExpressionStatement.
	*
	* @param n	The node from the Java AST.
	* @return	An ArrayList<String> of parameters with the format ["Type name", "Type name", ...].
	*/
	public static ArrayList<String> getCallExpression(GNode n) {
		ArrayList<String> parameters = new ArrayList<String>();
		
	
	}	

	/**
	* 
	*
	*
	*/
	public static boolean isPrintStatement(GNode n) {
		return (n.toString().contains("CallExpression") && n.toString().contains("SelectionExpression") && n.toString().contains("System") && n.toString().contains("out"));

	}
}

