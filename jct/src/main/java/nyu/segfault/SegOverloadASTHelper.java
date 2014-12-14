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

	private Boolean finished = false;
	public String cName;
	private LinkedList<String> listOfParameters = new LinkedList<String>();
	private LinkedList<GNode> listOfMethods = new LinkedList<GNode>();
	private LinkedList<String> listOfOverloadedMethods = new LinkedList<String>();

	public SegOverloadASTHelper() {

	}

	public String getName() {
		return cName;
	}

	public LinkedList<String> getOverloadedList() {
		return listOfOverloadedMethods;
	}

	public void visitCompilationUnit(GNode n) {
		visit(n);

		for (int i=0;i<listOfMethods.size();i++)
			executeOverloading((GNode)listOfMethods.get(i));
	}

	public void visitClassBody(GNode n) {
		visit(n);
		sortList();
	}

	// If an element in the list isn't being overloaded we remove it.
	public void sortList() {
		boolean match = false;

		for (int k=listOfMethods.size()-1;k>-1;k--) {
			GNode endNode = listOfMethods.get(k);
			for (int i=0;i<listOfMethods.size()-1;i++) {
				if (endNode.get(3).equals(listOfMethods.get(i).get(3))) {
					match = true;
					break;
				}
			}
			if (!match)
				listOfMethods.remove(k);
			match = false;
		}
	}

	public void visitMethodDeclaration(GNode n) {
		if (n.size()>3 && n.get(3) instanceof String)
			listOfMethods.add(n);

		visit(n);
	}

	public void visitQualifiedIdentifier(GNode n) {
		if (!finished)
			return;
		listOfParameters.add(n.getString(0));
	}

	public void visitPrimitiveType(GNode n) {
		if (!finished)
			return;
		listOfParameters.add(n.getString(0));
	}

	public void visit(Node n) {
    	for (Object o : n) if (o instanceof Node) dispatch((Node) o);
  	}

	protected void executeOverloading(GNode node) {
      	String nodeStr = node.getString(3);
      	addElementToOverloadedList(nodeStr);
      	if (node.getNode(4).size() > 0) {
      		finished=true;
        	visit(node.getNode(4));
        	finished=false;
        	for (int i=0;i<listOfParameters.size();i++) {
        		if (listOfParameters.get(i).equals("int")) {
        			listOfParameters.set(i, "int32_t");
        		}
       			nodeStr = nodeStr+"_"+listOfParameters.get(i);
       		}
       		listOfParameters = new LinkedList<String>();
      	}
      	node.set(3, nodeStr);
    }

    protected void addElementToOverloadedList(String ele) {
    	for (int i=0;i<listOfOverloadedMethods.size();i++) {
    		if (listOfOverloadedMethods.get(i).equals(ele)) {
    			return;
    		}
    	}
    	listOfOverloadedMethods.add(ele);
    	return;
    }

}