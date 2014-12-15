 package nyu.segfault;

import xtc.tree.GNode;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import xtc.lang.JavaFiveParser;
import xtc.lang.JavaPrinter;

import xtc.parser.ParseException;
import xtc.parser.Result;

import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;

import xtc.util.Tool;
import java.util.LinkedList;

//Builds inheritance tree

public class SegInheritanceBuilder {
	public GNode root;
	public GNode nodeStack;
	int children = 3;
	String pck = null;
   	SegVTableHandler segVTable = new SegVTableHandler();
   	SegDataLayoutHandler segDataLayout = new SegDataLayoutHandler();
   	LinkedList<String> staticMethods = new LinkedList<String>();

	public SegInheritanceBuilder(LinkedList<GNode> nodeList) {

		GNode hNode = GNode.create("HeaderDeclaration");
		GNode cNode = GNode.create("Class");
		GNode nodeStr = GNode.create("String");
		root = GNode.create("Object");

		root.add(hNode);
		root.setProperty("type", "CompilationUnit");
		cNode.setProperty("type", "CompilationUnit");
		nodeStr.setProperty("type", "CompilationUnit");

		hNode.add("null");
		hNode.add("Object");
		hNode.add(segDataLayout.getObjectDataLayout());
		hNode.add(segVTable.getObjectVTable());

		root.add(nodeStr);
		GNode strHead = GNode.create("HeaderDeclaration");
		strHead.add("null");
		strHead.add("String");
		nodeStr.add(strHead);
		strHead.add(segDataLayout.getStringDataLayout());
		strHead.add(segVTable.getStringVTable());

		GNode cHead = GNode.create("HeaderDeclaration");
		cHead.add("null");
		cHead.add("Class");
		cHead.add(segDataLayout.getClassDataLayout());
		cHead.add(segVTable.getClassVTable());
		cNode.add(cHead);
		root.add(cNode);

		nodeStack = GNode.create("FirstStackNode");

		for (int i = 0; i < nodeList.size(); i++) {
			buildTree(nodeList.get(i));
		}

		for (GNode s=nodeStack;s.getName()!="FirstStackNode";s=(GNode)s.getNode(0)) {
		    GNode parent = findParentNode(root, (String)s.getProperty("parentString"));
		    GNode thisNode = (GNode)root.getNode((Integer)s.getProperty("numberInRoot"));
		    thisNode.setProperty("parent", parent);
		    parent.add(thisNode);
		    root.remove((Integer)s.getProperty("numberInRoot"));
		}
		// Builds headers
		for (int i=1;i<root.size();i++) {
		    buildTreeHeaders((GNode)root.getNode(i));
		}

	}

	public LinkedList<GNode> getNodeList() {
		LinkedList<GNode> nodeList = new LinkedList<GNode>();
		if (root.size() < 2) {
			return nodeList;
		}

		for (int i=3;i<root.size();i++) {
			getNodeList(nodeList, (GNode)root.getNode(i));
		}

		return nodeList;
	}

	private void getNodeList(LinkedList<GNode> nodeList, GNode nodeToAdd) {
		nodeList.add((GNode)nodeToAdd.getProperty("javaAST"));
		if (nodeToAdd.size() < 2) {
			return;
		}
		for (int i=1;i<nodeToAdd.size();i++) {
			getNodeList(nodeList, (GNode)nodeToAdd.getNode(i));
		}
	}

	private GNode findParentNode(GNode startNode, String name) {
		if (startNode.getName().equals(name)) {
			return startNode;
		} else if (!startNode.hasProperty("type")) {
			return null;
		} else if (startNode.size() == 0) {
			return null;
		} else {
			for (int i = 0; i < startNode.size(); i++) {
			     GNode solution = findParentNode((GNode) startNode.getNode(i),
						name);
				if (solution != null) {
					return solution;
				}
			}
			return null;
		}
	}

	private void buildTreeHeaders(GNode startNode) {
	    // adds all the headers
	    if (startNode.size()<=0 || !startNode.getNode(0).getName().equals("HeaderDeclaration")) {
			startNode.add(0, buildHeader((GNode)startNode.getProperty("javaAST"), (GNode)startNode.getProperty("parent"))); //this builds the header
	    }

	    if (startNode.size()<=1) {
			return;
	    }

	    //If the node has any children, it builds the header for it's children
	    for (int i=1;i<startNode.size();i++) {
			buildTreeHeaders((GNode)startNode.getNode(i));
	    }
	    return;
	}

    public GNode buildHeader(GNode astNode, GNode parentNode) {
		GNode header = GNode.create("HeaderDeclaration");
		header.add(pck);
		header.add(astNode.getString(1));
		header.add(segDataLayout.getNodeDataLayout(astNode, getParentDataLayout(parentNode)));
		header.add(segVTable.getNodeVTable(astNode, getParentVTable(parentNode)));
		return header;
	}

	public GNode getRoot() {
		return root;
	}

	public LinkedList<String> getVTableForNode(String name) {
		GNode node = searchForNode(root, name);
		LinkedList<String> vTableList = new LinkedList<String>();
		if (node==null) {
			vTableList.add("Didn't find a node with that name");
			return vTableList;
		}
		for (int i=0;i<node.getNode(0).getNode(2).size();i++) {
		  	GNode entry = (GNode)node.getNode(0).getNode(2).getNode(i);
		  	if (entry.hasProperty("typeOfNode") && entry.getProperty("typeOfNode").equals("method")) {
	      		if (entry.getString(6).equals("Overloaded")) {
 	        		vTableList.add(entry.getString(2));
 	        	}
 	      	}
		}
		return vTableList;
	}

	public String getParentOfNode(String childName) {
		for (int i=1;i<root.size();i++) {
			if (root.getNode(i).getName().equals(childName)) {
				return root.getName();
			}
			else {
				String temp = getParentOfNode(childName, (GNode)root.getNode(i));
				if (temp != null) {
					return temp;
				}
			}
		}
		return "Parent not found";
	}

	private String getParentOfNode(String childName, GNode node) {
		if (node.size()<1) {
			return null;
		}

		for (int i=1;i<node.size();i++) {
			if (node.getNode(i).getName().equals(childName)) {
				return node.getName();
			}
			else {
				String temp = getParentOfNode(childName, (GNode)node.getNode(i));
				if (temp != null) {
					return temp;
				}
			}
		}
		return null;
	}

	public String getReturnType(String methodName, String className) {
		GNode a = (GNode)searchForNode(root, className);
		GNode dataLayout = (GNode)a.getNode(0).getNode(2);
		for (int i=0;i<dataLayout.size();i++) {
			if (dataLayout.get(i) instanceof Node && dataLayout.getNode(i).hasProperty("typeOfNode")  && dataLayout.getNode(i).getProperty("typeOfNode").equals("method")) {
				String name = dataLayout.getNode(i).getString(2);
				if (name.equals(methodName)) {
					return dataLayout.getNode(i).getString(1);
				}
			}
		}
		return "";
	}

    private GNode searchForNode(GNode node, String name) {
    	for (int i=0;i<name.length();i++) {
    		if (name.charAt(i)=='_') {
    			name=name.substring(i+1,name.length());
    		}
    		else {
    			break;
    		}
    	}

		if (node.getNode(0).getString(1).equals(name)) {
	    	return node;
		}
		else if (node.size() == 1) {
	    	return null;
		}
		else {
	    	for (int i=1;i<node.size();i++) {
				GNode foundNode = searchForNode((GNode)node.getNode(i), name);
				if (foundNode != null) {
		    		return foundNode;
				}
	    	}
		}
		return null;
    }

	public LinkedList<GNode> parseNodeToInheritance(GNode n){
		final LinkedList<GNode> returnList = new LinkedList<GNode>();
		
		new Visitor(){
			public void visitPackageDeclaration(GNode n){
				pck = n.getNode(1).getString(0);
			}

			public void visitClassDeclaration(GNode n){
				GNode targetNode = (GNode) n;
				GNode cNode = searchForNode(root, targetNode.getString(1));
				GNode returnNode = GNode.create(targetNode.getString(1));
				returnNode.add((GNode)cNode.getNode(0));

				returnList.add(returnNode);
			}

			public void visit(GNode n) {
				for (Object o : n) {
					if (o instanceof Node)
						dispatch((Node) o);
				}
			}
		}.dispatch(n);

		return returnList;
	}

	private GNode getParentDataLayout(GNode parent) {
		GNode dataLayoutNode = null;
		if (parent == null) {
			GNode temp = segDataLayout.getObjectDataLayout();
			dataLayoutNode = copyNode(temp);
		} else {
			GNode olddataLayout = (GNode) parent.getNode(0).getNode(2);
			dataLayoutNode = copyNode(olddataLayout);
		}
		return dataLayoutNode;
	}

	private GNode getParentVTable(GNode parent) {
	        GNode vTableNode = null;
		if (parent == null) {
			GNode temp = segVTable.getObjectVTable();
			vTableNode = copyNode(temp);
		} else {
			GNode oldvTable = (GNode) parent.getNode(0).getNode(3);
			vTableNode = copyNode(oldvTable);
		}
		return vTableNode;
	}

    public LinkedList<String> getStaticMethods(GNode javaAST) {
		new Visitor() {

			public void visitCompilationUnit(GNode n) {
				visit(n);
			}

			public void visitMethodDeclaration(GNode n) {
				if (n.get(0)!=null) {
					GNode modifiers = (GNode)n.getNode(0);
					for (int i=0;i<modifiers.size();i++) {
						if (modifiers.get(i) instanceof Node && modifiers.getNode(i).size()>0 && modifiers.getNode(i).get(0) instanceof String) {
							String type = (String)modifiers.getNode(i).getString(0);
							if (type.equals("static") && n.size()>3&&n.get(3) instanceof String) {
								staticMethods.add(n.getString(3));
							}
						}
					}
				}
			}

			public void visit(Node n) {
				for (Object o : n) {
					if (o instanceof Node)
						dispatch((Node) o);
				}
			}
		}.dispatch(javaAST);

		return staticMethods;
	}

	public void buildTree(GNode node) {
		    new Visitor() {

		    public void visitPackageDeclaration(GNode n){
		    	pck = n.getNode(1).getString(0);
		    }

		    public void visitClassDeclaration(GNode n) {
				String classname = n.getString(1);
				GNode cNode = GNode.create(classname);
				cNode.setProperty("type", "CompilationUnit"); //all class nodes should have type compilationunit so we can easily identify them.
				root.add(cNode);
				children++;
				cNode.setProperty("javaAST", n);

				visit(n);
				return;
			}

			public void visitExtension(GNode n) {
			    GNode thisNode = (GNode)root.getNode(children-1);
			    String parentString = n.getNode(0).getNode(0).getString(0);
			    GNode copiedNode = copyNode(thisNode);
			    copiedNode.setProperty("numberInRoot", children-1);
			    copiedNode.setProperty("parentString", parentString);
			    copiedNode.add(nodeStack);
			    nodeStack = copiedNode;
			    return;
			}

			public void visitClassBody(GNode n) {
			    if (children > root.size()) {
					return;
			    }

			    children--;
			    GNode node = (GNode)root.getNode(children);
			    node.setProperty("parent", null);
			    children++;
			    return;
			}


			public void visit(Node n) {
				for (Object o : n) {
					if (o instanceof Node)
						dispatch((Node) o);
				}
			}
		}.dispatch(node);
	}

    private GNode copyNode(GNode oldNode) {
	GNode newNode = GNode.create(oldNode.getName());
	if (oldNode.size() > 0) {
	    for (int i=0;i<oldNode.size();i++) {
		if (oldNode.get(i) instanceof String) {
		    newNode.add(oldNode.get(i));
		}
		else {
		    newNode.add(copyNode((GNode)oldNode.get(i)));
		}
	    }
	}
	return newNode;
    }
}
