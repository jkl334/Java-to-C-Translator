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

import xtc.util.SymbolTable;
import xtc.util.Tool;

import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;


/**
 * SegImp Visitor  handles classes without inheritance and virtual methods
 */
public class SegImp extends Visitor{

	/**
	 * constructor for SegImp
	 */
	public SegImp(){
	}

	public void visitCompilationUnit(GNode n) {
		SegHelper.writeMacros();
		SegHelper.cpp_pln("");
		SegHelper.getCallExpression(n);
		visit(n);
		//SegHelper.just_testing(n);
		SegHelper.flush();
	}

	public void visitClassDeclaraion(GNode n) {
		visit(n);
	}

	public void visitMethodDeclaration(GNode n){
		String method_decl=SegHelper.getMethodDeclaration(n,SegHelper.getCurrClass());
		if(method_decl != null){
			SegHelper.cpp_pln("\t"+method_decl);
		}
	}

	public void visit(GNode n){for (Object o : n) if (o instanceof Node) dispatch((Node) o);}
}
