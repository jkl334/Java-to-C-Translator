package nyu.segfault;

import java.lang.*;
import java.util.Iterator;
import xtc.tree.LineMarker;
import xtc.tree.Node;
import xtc.tree.GNode;
import xtc.tree.Pragma;
import xtc.tree.Printer;
import xtc.tree.SourceIdentity;
import xtc.tree.Token;
import xtc.tree.Visitor;

public class SegASTHelper extends Visitor {
	public String className;
	
	public SegASTHelper(){
	}

	public String getName(){
		return className;
	}

	public void visitCompilationUnit(GNode n) {
		visit(n);
	}

	public void visitPrimitiveType(GNode n){
		if(n.getString(0).equals("byte")){
		  n.set(0,"unsigned char");
		}
	}

	public void visitClassDeclaration(GNode n) {
		className = n.getString(1);
		n.set(1, "__" + className);
		n.set(3, null);
		visit(n);
	}

	public void visitPackageDeclaration(GNode n) {
		visit(n);
	}

	public void visitImportDeclaration(GNode n) {
		visit(n);
	}

	public void visitClassBody(GNode n) {
		visit(n);
		for (int i = 0; i< n.size(); i++) {
		  if(n.getNode(i).hasName("FieldDeclaration")) {
		    GNode segFieldDeclaration = GNode.create("SegFieldDeclaration");
		    for (int x = 0; x < n.getNode(i).size(); x++){
		      segFieldDeclaration.add(n.getNode(i).getNode(x));
		    }
		    n.set(i,segFieldDeclaration);
		  }
		}
	}

	public void visitConstructorDeclaration(GNode n){
		String constructorName = n.getString(2);
		n.set(2, "__" + constructorName);
		visit(n);
	}

	public void visitMethodDeclaration(GNode n){
		if (n.getNode(4).size() == 0){
		  n.set(5, className);
		}

		visit(n);
	}

	public void visitCallExpression(GNode n){
		visit(n);
	}
	public void visitExpressionStatement(GNode n){
		if (n.getNode(0).hasName("CallExpression")){
		  if (n.getNode(0).getString(2).equals("println")){
		    GNode cout = GNode.create("CoutExpression");
		    cout.add("Cout");
		    cout.add(n.getNode(0).getNode(3));
		    n.set(0, cout);
		  }
		}else if (n.getNode(0).hasName("Expression")){
			GNode expressionNode = (GNode) n.getNode(0);
			if(expressionNode.get(2) instanceof Node && expressionNode.getNode(2) != null && expressionNode.getNode(2).hasName("StringLiteral")){
				String literal = expressionNode.getNode(2).getString(0);
				expressionNode.getNode(2).set(0,"__String::init(new __String(" + literal + "))");
			} 
		}
		visit(n);
	}

	public void visitCoutAdditiveExpression(GNode n){
		if (n.getNode(0).hasName("AdditiveExpression")){
		  GNode coutAdd = GNode.create("CoutAdditiveExpression");
		  coutAdd.add(n.getNode(0).getNode(0));
		  coutAdd.add(n.getNode(0).getString(1));
		  coutAdd.add(n.getNode(0).getNode(2));
		  n.set(0,coutAdd);
		}
		visit(n);
	}

	public void visitCoutExpression(GNode n){
		GNode coutArgs = GNode.create("CoutArguments");
		for (int i = 0; i < n.getNode(1).size(); i++){
		  coutArgs.add(n.getNode(1).getNode(i));
		}
		n.set(1, coutArgs);
		visit(n);
	}

	public void visitCoutArguments(GNode n){
		if (n.getNode(0).hasName("AdditiveExpression")){
		  GNode coutAdd = GNode.create("CoutAdditiveExpression");
		  coutAdd.add(n.getNode(0).getNode(0));
		  coutAdd.add(n.getNode(0).getString(1));
		  coutAdd.add(n.getNode(0).getNode(2));
		  n.set(0,coutAdd);
	}
		else if(n.getNode(0).hasName("CallExpression")){
			  GNode coutCall = GNode.create("CoutCallExpression");
			  coutCall.add(n.getNode(0).getNode(0));
			  coutCall.add(n.getNode(0).getString(2));
			  coutCall.add(n.getNode(0).getNode(3));
			  n.set(0,coutCall);
			}
			visit(n);
		}

	public void visitBlock(GNode n){
		  visit(n);
	}

	public void visitFieldDeclaration(GNode n){
		  visit(n);
	}

	public void visitDeclarator(GNode n){
	    for (int i = 0; i < n.size(); i++){
	      if ((n.get(i) instanceof Node) && n.getNode(i).hasName("StringLiteral")){
	        String literal = n.getNode(i).getString(0);
	        String customLiteral = "__rt::literal(" + literal + ")";
	        n.getNode(i).set(0,customLiteral);
	      }
	    }
	  visit(n);
	}

	public void visit(Node n) {
		for (Object o : n) 
			if (o instanceof Node) 
				dispatch((Node) o);
	}
}
