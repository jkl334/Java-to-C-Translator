//Java SE packages 
import java.io.*;
import java.util.*;

// xtc packages 
import xtc.tree.*;
import xtc.util.*;

public class cxxAST extends Visitor{
	
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
				index++; cxx_class_roots[index]=new GNode(n.getString(0)); //c++ class root
				class_node=n; 
				table.enter(class_node); visit(n); table.exit();
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
}
