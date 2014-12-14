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

    public GNode stackNode;
	int childCount = 3;
	String packageName = null;

   	SegDataLayoutHandler dataLayout = new SegDataLayoutHandler();
   	SegVTableHandler vTable = new SegVTableHandler();

	public SegInheritanceBuilder(LinkedList<GNode> nodeList) {

		root = GNode.create("Object");
		GNode headerNode = GNode.create("HeaderDeclaration");
		GNode stringNode = GNode.create("String");
		GNode classNode = GNode.create("Class");

		root.add(headerNode);

		headerNode.add("null");
		headerNode.add("Object");
		headerNode.add(dataLayout.getObjectDataLayout());
		headerNode.add(vTable.getObjectVTable());

		root.add(stringNode);
		GNode stringHeader = GNode.create("HeaderDeclaration");
		stringHeader.add("null");
		stringHeader.add("String");
		stringNode.add(stringHeader);
		stringHeader.add(dataLayout.getStringDataLayout());
		stringHeader.add(vTable.getStringVTable());

		GNode classHeader = GNode.create("HeaderDeclaration");
		classHeader.add("null");
		classHeader.add("Class");
		classHeader.add(dataLayout.getClassDataLayout());
		classHeader.add(vTable.getClassVTable());
		classNode.add(classHeader);
		root.add(classNode);

		root.setProperty("type", "CompilationUnit");
		stringNode.setProperty("type", "CompilationUnit");
		classNode.setProperty("type", "CompilationUnit");

		stackNode = GNode.create("FirstStackNode");

		for (int i = 0; i < nodeList.size(); i++) {
			buildTree(nodeList.get(i));
		}

		for (GNode s=stackNode;s.getName()!="FirstStackNode";s=(GNode)s.getNode(0)) {
		    GNode parent = findParentNode(root, (String)s.getProperty("parentString"));
		    if (parent == null) {
				System.out.println("NO PARENT FOUND IN THE TREE!");
				continue;
		    }

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

	public void buildTree(GNode node) {
		    new Visitor() {

		    public void visitPackageDeclaration(GNode n){
		    	packageName = n.getNode(1).getString(0);
		    }

		    public void visitClassDeclaration(GNode n) {
				String classname = n.getString(1);
				GNode classNode = GNode.create(classname);
				classNode.setProperty("type", "CompilationUnit"); //all class nodes should have type compilationunit so we can easily identify them.
				root.add(classNode);
				childCount++;
				classNode.setProperty("javaAST", n);

				visit(n);
				return;
			}

			public void visitExtension(GNode n) {
			    GNode thisNode = (GNode)root.getNode(childCount-1);
			    String parentString = n.getNode(0).getNode(0).getString(0);
			    GNode copiedNode = copyNode(thisNode);
			    copiedNode.setProperty("numberInRoot", childCount-1);
			    copiedNode.setProperty("parentString", parentString);
			    copiedNode.add(stackNode);
			    stackNode = copiedNode;
			    return;
			}

			public void visitClassBody(GNode n) {
			    if (childCount > root.size()) {
					return;
			    }

			    childCount--;
			    GNode node = (GNode)root.getNode(childCount);
			    node.setProperty("parent", null);
			    childCount++;
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
		header.add(packageName);
		header.add(astNode.getString(1));
		header.add(dataLayout.getNodeDataLayout(astNode, getParentDataLayout(parentNode)));
		header.add(vTable.getNodeVTable(astNode, getParentVTable(parentNode)));
		return header;
	}

	public GNode getRoot() {
		return root;
	}

	public LinkedList<String> getVTableForNode(String name) {
		GNode node = searchForNode(root, name);
		LinkedList<String> vTableList = new LinkedList<String>();
		if (node==null) {
			vTableList.add("Node not found");
			return vTableList;
		}
		for (int i=0;i<node.getNode(0).getNode(3).size();i++) {
			vTableList.add(node.getNode(0).getNode(3).getNode(i).getString(2));
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

    private GNode searchForNode(GNode node, String name) {
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
				packageName = n.getNode(1).getString(0);
			}

			public void visitClassDeclaration(GNode n){
				GNode targetNode = (GNode) n;
				GNode classNode = searchForNode(root, targetNode.getString(1));
				GNode returnNode = GNode.create(targetNode.getString(1));
				returnNode.add((GNode)classNode.getNode(0));

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
			GNode temp = dataLayout.getObjectDataLayout();
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
			GNode temp = vTable.getObjectVTable();
			vTableNode = copyNode(temp);
		} else {
			GNode oldvTable = (GNode) parent.getNode(0).getNode(3);
			vTableNode = copyNode(oldvTable);
		}
		return vTableNode;
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
