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


import static org.junit.Assert.*;

/**
 * Build the inheritance tree by creating nodes for each class and place them as children of Object. 
 * Any class with "extends" gets a copy added to a stack. For everything in the stack, we look in the tree for it's parent and add the node as a child. 
 * Creates the headers for each class node. 
*/
public class SegInheritance {
	public GNode root;
	public GNode stackNode; //A stack of nodes that have the extends keyword and thus must be moved in the tree to their correct place.
	int childCount = 3; // When building the tree, keeps track of which child we're creating.

	String packageName = null;

   	DataLayoutCreator dataLayout = new DataLayoutCreator();
   	VTableCreator vTable = new VTableCreator();

   	LinkedList<String> staticMethods = new LinkedList<String>();
   	LinkedList<String> nodeOrder = new LinkedList<String>();
   	LinkedList<String> finalNodeOrder = new LinkedList<String>();

  /** Create Object and String class nodes */
	public SegInheritance(LinkedList<GNode> nodeList) {

		for (GNode node : nodeList) {
			setNodeOrderToJava(node);
		}

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

		//Places each node in the tree as a child of Object.
		for (int i = 0; i < nodeList.size(); i++) {
			buildTree(nodeList.get(i));
		}

		//Goes through a stack of all nodes with the extends keyword and moves them in three to be a child of their correct parent.
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

		for (int i=1;i<root.size();i++) {
		    buildTreeHeaders((GNode)root.getNode(i));
		}

		while (nodeOrder.size()>0) {
			GNode inherit = searchForNode(root, nodeOrder.peek());
			GNode ast = (GNode)inherit.getProperty("javaAST");
			modifyNodeOrder(ast);
		}
	}

	/** Places a GNode in the inheritance tree */
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
		    //Places the node in the tree and also a copy of the node in a stack on top of stackNode to be looked at later.
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

	public void setNodeOrder(GNode ast) {
		//Sets an initial ordering of nodes to be the order in the java file
		new Visitor () {
			public void visitCompilationUnit(GNode n) {
				visit(n);
			}
			public void visitClassDeclaration(GNode n) {
				nodeOrder.add(n.getString(1));
			}
			public void visit (Node n) {
				for (Object o : n) {
					if (o instanceof Node)
						dispatch((Node) o);
				}
			}
		}.dispatch(ast);
	}
	public void modifyNodeOrder(GNode ast) {
		//Modifies the order of java nodes depending on what is in the AST
		new Visitor() {
			public void visitClassDeclaration(GNode n) {
				nodeOrder.remove(n.getString(1));
				visit(n);
				finalNodeOrder.add(n.getString(1));
			}
			public void visitQualifiedIdentifier(GNode n) {
				if (n.get(0) instanceof String) {
					if (nodeOrder.contains(n.getString(0))) {
						if (!(n.getString(0).equals("Object") || n.getString(0).equals("String") || n.getString(0).equals("Class"))) {
							GNode innerClass = searchForNode(root, n.getString(0));
							GNode innerAST = (GNode)innerClass.getProperty("javaAST");
							modifyNodeOrder(innerAST);
						}
					}
				}
			}
			public void visit (Node n) {
				for (Object o : n) {
					if (o instanceof Node)
						dispatch((Node) o);
				}
			}
		}.dispatch(ast);
	}

	public LinkedList<GNode> getNodeList() {
		LinkedList<GNode> nodeList = new LinkedList<GNode>();

		for (String s : finalNodeOrder) {
			GNode g = searchForNode(root, s);
			GNode gAST = (GNode)g.getProperty("javaAST");
			nodeList.add(gAST);
		}

		return nodeList;
	}

	/** Finds a node with a given name in a tree with the given root. */
	private GNode findParentNode(GNode startNode, String name) {
		// Finds a node of a given name in the tree whose root is startNode.
		if (startNode.getName().equals(name)) {
			return startNode;
		} else if (!startNode.hasProperty("type")) {
			return null;
		} else if (startNode.size() == 0) {
			return null;
		} else { // DEPTH FIRST SEARCH THROUGH THE TREE TO FIND THE PARENT NODE
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

	/** buildTreeHeaders: Runs through the entire tree and builds a header for each node. */
	private void buildTreeHeaders(GNode startNode) {
	    //Builds the header for each node in the inheritance tree.
	    if (startNode.size()<=0 || !startNode.getNode(0).getName().equals("HeaderDeclaration")) {
			startNode.add(0, buildHeader((GNode)startNode.getProperty("javaAST"), (GNode)startNode.getProperty("parent"))); //this builds the header
	    }

	    if (startNode.size()<=1) {
			return;
	    }
	    
	    //If the node has any children, it builds the header for it's children.
	    for (int i=1;i<startNode.size();i++) {
			buildTreeHeaders((GNode)startNode.getNode(i));
	    }
	    return;
	}


		/** Builds the header in the Inheritance tree for a given node */ 
    public GNode buildHeader(GNode astNode, GNode parentNode) {
		GNode header = GNode.create("HeaderDeclaration");
		header.add(packageName);
		header.add(astNode.getString(1));
		header.add(dataLayout.getNodeDataLayout(astNode, getParentDataLayout(parentNode)));
		header.add(vTable.getNodeVTable(astNode, getParentVTable(parentNode)));
		return header;
	}

	/** Returns the root of the entire tree. */
	public GNode getRoot() {
		return root;
	}

	//Only returns overloaded methods.
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
	      		if (!entry.getString(6).equals(entry.getString(2))) {
 	        		vTableList.add(entry.getString(2));
 	        	}
 	      	}
		}
		return vTableList;
	}

	public String getReturnType(String methodName, String className) {
		GNode a = (GNode)searchForNode(root, className);
		if (a==null) {
			System.err.println("Node "+className+" that was searched for was not found");
		}
		GNode dataLayout = (GNode)a.getNode(0).getNode(2);
		for (int i=0;i<dataLayout.size();i++) {
			//For each method in the dataLayout, compare it's name against the methodName we're searching for
			if (dataLayout.get(i) instanceof Node && dataLayout.getNode(i).hasProperty("typeOfNode")  && dataLayout.getNode(i).getProperty("typeOfNode").equals("method")) {
				String name = dataLayout.getNode(i).getString(2);
				if (name.equals(methodName)) {
					return dataLayout.getNode(i).getString(1);
				}
			}
		}
		System.err.println("Error finding returnType for "+methodName+" in "+className);
		return "";
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
		return "No Parent Found";
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

		/** finds a node with the given name in the tree.
		 * 
		 */
    private GNode searchForNode(GNode node, String name) {
		// DOES A DEPTH-FIRST SEARCH THROUGH THE TREE FOR A NODE OF SPECIFIC NAME

    	//Removes extra underscores at beginning of name:
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

	/** Returns the header information for a given node. */
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

	/** Returns a copy of the parent's dataLayout for a given node. */
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

	/** Returns the parent node VTable for inheritance. 
	 * If parent == null, assumes it's java.lang.Object. 
	 */
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


	/** Returns a copy of a node and all it's children and children's children, etc.
	 * Note: this won't copy any properties you've placed on the nodes.
	 */
    private GNode copyNode(GNode oldNode) {

		GNode newNode = GNode.create(oldNode.getName());
		if (oldNode.hasProperty("typeOfNode")) {
			newNode.setProperty("typeOfNode", oldNode.getProperty("typeOfNode"));
		}
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