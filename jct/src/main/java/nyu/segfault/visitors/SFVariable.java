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

public class SFVariable extends Visitor {

	private String var;

	public String get(Node n) {
		this.dispatch(n);
		return var;
	}

	public void visitStringLiteral(GNode n) {
        var = n.getString(0);
    }

	public void visitIntegerLiteral(GNode n) {
		var = n.getString(0);
	}

	public void visitFloatingPointLiteral(GNode n) {
		var = n.getString(0);
    }

    public void visitCharacterLiteral(GNode n) {
		var = n.getString(0);
    }

    public void visitBooleanLiteral(GNode n) {
		var = n.getString(0);
    }

    public void visitNullLiteral(GNode n) {
		var = n.getString(0);
    }

    public void visitPrimaryIdentifier(GNode n) { 
        var = n.getString(0);
    }

    public void visitQualifiedIdentifier(GNode n) {
    	var = j2c(n.getString(0)) + " ";
    }

    public void visitType(GNode n){
    	visit(n);
    }

    public void visitModifier(GNode n){
    	var = n.getString(0) + " ";
    }

    public void visit(Node n) {
		for (Object o : n) if (o instanceof Node) dispatch((Node)o);
	}

	public String j2c(String javaType) {
		String cType;
		if (javaType.equals("String")) {
	        cType = "string";
        }
        else {
        	cType = javaType;
        }
        return cType;
	}
}