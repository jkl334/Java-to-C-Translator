package nyu.segault;

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

	public void visitCompilationUnit(GNode n){
		visit(n);
	}

	public void visitPackageDeclaration(GNode n){
		visit(n);
	}

	public void visitImportDeclaration(GNode n){
		visit(n);
	}

	public void visitClassDeclaration(GNode n){
		visit(n);
	}

	public void visitClassBody(GNode n){
		visit(n);
	}

	public void visit(Node n){
		for (Object o : n) if (o instanceof Node) dispatch((Node) o);
	}
}
