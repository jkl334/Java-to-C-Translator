//Java SE packages 
import java.io.*;
import java.util.*;

// xtc packages 
import xtc.tree.*;
import xtc.util.*;

public class CxxAST extends Visitor {
	
	PrintWriter headWriter;  // The PrintWriter that will write to the C++ header file.
	PrintWriter impWriter;  // The PrintWriter that will write to the C++ implementation file.
	
	StringBuilder headText = new StringBuilder();  // The text that will be writen to the C++ header file.
	StringBuilder impText = new StringBuilder();  // The text that will be written to the C++ implementation file.
	
	ArrayList<GNode> cxx_class_roots=new ArrayList<GNode>();; /**@var root nodes of classes in linear container*/
	int index=-1; /**@var root node of class subtree index*/

	SymbolTable table; /**@var node symbols*/

	

	/**
	 * Create c++ AST from implicit Java AST
	 * @param n is the root of the entire Java AST
	 */
	public GNode CXXTree(GNode n){
		new Visitor(){

			GNode class_node; /**@var java class node */
			
			public void visitAdditiveExpression(GNode n){

			}
			public void visitBlock(GNode n){


			}
			public void visitCallExpression(GNode n){

			}
			/**
			 *
			 *
			 *
			 */
			public void visitClassDeclaration(GNode n){
				
			}
			public void visitExpressionStatement(GNode n){


			}
			public void visitFieldDeclaration(GNode n){


			}
			public void visitForStatement(GNode n){

			}
			public void visitMethodDeclaration(GNode n){


			}
			public void visitPrimaryIdentifier(GNode n){


			}
			public void visit(GNode n) 
			{
				for( Object o : n) if (o instanceof Node) dispatch((GNode)o);
			}
		}
	}
	
	public static void main(String[] args) {
		String fileName = args[0].substring(0, args[0].length() - 5);  // The file name without ".java".
		
		String headFileName = fileName + ".hpp";
		this.headWriter = new PrintWriter(headFileName);
		
		String impFileName = fileName + ".cpp";
		this.impWriter = new PrintWriter(impFileName);
		
		CxxAST cxxast = new CxxAST();
		cxxAST.run(args);
	}
}
