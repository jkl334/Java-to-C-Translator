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
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.LinkedList;

public class SegOverloadASTHelper extends Visitor {

	public String className;
	private LinkedList<GNode> methodList = new LinkedList<GNode>();
	private LinkedList<String> parameterList = new LinkedList<String>();
	private LinkedList<String> overloadedList = new LinkedList<String>();
	private Boolean ready = false;

	public SegOverloadASTHelper() {

	}

	public String getName() {
		return className;
	}

	public LinkedList<String> getOverloadedList() {
		return overloadedList;
	}

	public void visitCompilationUnit(GNode n) {
		visit(n);
		
		for (int i=0;i<methodList.size();i++) {
			executeOverloading((GNode)methodList.get(i));
		}
	}

	public void visitClassBody(GNode n) {
		visit(n);

		sortList();
	}

	//Removes elements from the list that aren't overloaded.
	public void sortList() {
		boolean match = false;

		for (int k=methodList.size()-1;k>-1;k--) {
			GNode endNode = methodList.get(k);
			for (int i=0;i<methodList.size()-1;i++) {
				if (endNode.get(3).equals(methodList.get(i).get(3))) {
					match = true;
					break;
				}
			}
			if (!match) {
				methodList.remove(k);
			}
			match = false;
		}
	}

	public void visitMethodDeclaration(GNode n) {
		if (n.size()>3 && n.get(3) instanceof String) {
			methodList.add(n); //creates a list with all the method nodes.
		}

		visit(n);
	}

	public void visitQualifiedIdentifier(GNode n) {
		if (!ready) {return;}
		parameterList.add(n.getString(0));
	}

	public void visitPrimitiveType(GNode n) {
		if (!ready) {return;}
		parameterList.add(n.getString(0));
	}

	public void visit(Node n) {
    	for (Object o : n) if (o instanceof Node) dispatch((Node) o);
  	}

	protected void executeOverloading(GNode overload) {	
      	String newNodeString = overload.getString(3);
      	addElementToOverloadedList(newNodeString);
      	if (overload.getNode(4).size() > 0) {
      		ready=true;
        	visit(overload.getNode(4));
        	ready=false;
        	for (int i=0;i<parameterList.size();i++) {
        		if (parameterList.get(i).equals("int")) {
        			parameterList.set(i, "int32_t");
        		}
       			newNodeString = newNodeString+"_"+parameterList.get(i);
       		}
       		parameterList = new LinkedList<String>();
      	}
      	overload.set(3, newNodeString);
    }

    protected void addElementToOverloadedList(String a) {
    	for (int i=0;i<overloadedList.size();i++) {
    		if (overloadedList.get(i).equals(a)) {
    			return;
    		}
    	}
    	overloadedList.add(a);
    	return;
    }

}