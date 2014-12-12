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
    public GNode stackNode; //This is the top node of the stack of the nodes
	public String class_name;
	int childCount = 3;
	String packageName = null;
	GNode targetNode;

	public SegInheritanceBuilder(LinkedList<GNode> nodeList) {

		root = GNode.create("Object");
		GNode headerNode = GNode.create("HeaderDeclaration");
		GNode stringNode = GNode.create("String");
		GNode classNode = GNode.create("Class");

		root.add(headerNode);

		headerNode.add(getObjectDataLayout());
		headerNode.add(getObjectVTable());

		root.add(stringNode);
		GNode stringHeader = GNode.create("HeaderDeclaration");
		stringNode.add(stringHeader);
		stringHeader.add(getStringDataLayout());
		stringHeader.add(getStringVTable());

		GNode classHeader = GNode.create("HeaderDeclaration");
		classHeader.add(getClassDataLayout());
		classHeader.add(getClassVTable());
		classNode.add(classHeader);
		root.add(classNode);

		root.setProperty("type", "CompilationUnit");
		stringNode.setProperty("type", "CompilationUnit");
		classNode.setProperty("type", "CompilationUnit");

		for (int i = 0; i < nodeList.size(); i++) {
			buildTree(nodeList.get(i));
		}

		for (GNode s=stackNode;s!=null;s=(GNode)s.getNode(0)) {
		    GNode parent = findParentNode(root, (String)s.getProperty("parentString"));
		    parent.add(s);
		    root.remove((Integer)s.getProperty("numberInRoot"));
		    s.setProperty("parent", parent);
		}

		// Builds headers
		for (int i=3;i<root.size();i++) {
		    buildTreeHeaders((GNode)root.getNode(i));
		}

	}

	public void buildTree(GNode node) {

		     // childCount keeps track of the location of the most recent child in the tree

		    new Visitor() {

		    public void visitPackageDeclaration(GNode n){
		    	packageName = n.getNode(1).getString(0);
		    }

		    public void visitClassDeclaration(GNode n) {
				String classname = n.getString(1);
				GNode classNode = GNode.create(classname);
				classNode.setProperty("type", "CompilationUnit"); // all class nodes should have type CompilationUnit
				root.add(classNode);
				childCount++;
				classNode.setProperty("javaAST", n);
				visit(n);
				return;
			}

			public void visitExtension(GNode n) {
			    childCount--;
			    GNode thisNode = (GNode)root.getNode(childCount);
			    String parent = n.getNode(0).getNode(0).getString(0);
			    GNode parentNode = findParentNode(root, parent);
			    if (parentNode == null) {
				System.out.println("Did not find parent node for " + n.getLocation().toString());
				thisNode.setProperty("parentString", parent);
				thisNode.setProperty("numberInRoot", childCount);
				childCount++;
				thisNode.add(stackNode);
				stackNode = thisNode;
				return;
			    }
			    thisNode.setProperty("parent", parentNode);
			    parentNode.add(thisNode);
			    root.remove(childCount);
			    childCount++;
			    return;
			    }

			public void visitClassBody(GNode n) {
			    if (childCount > root.size()) {
				return;
			    }

			    childCount--;
			    GNode node = (GNode)root.getNode(childCount);
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

	private GNode findParentNode(GNode startNode, String name) {
		// Depth first search through the tree
		// returns the GNode of the parent or null if it doesn't find it
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
	    startNode.add(0, buildHeader((GNode)startNode.getProperty("javaAST"), (GNode)startNode.getProperty("parent")));
	    if (startNode.size()<=1) {
		return;
	    }
	    for (int i=1;i<startNode.size();i++) {
		buildTreeHeaders((GNode)startNode.getNode(i));
	    }
	    return;
	}

	public GNode getRoot() {
		return root;
	}

	private GNode getObjectVTable() {
		// Creates a vtable for the object class
		GNode objectVTable = GNode.create("VTable");
		String arg[] = { "__Object" };
		objectVTable.add(createMethod(null, "__isa", null, "Class", "Object"));
		objectVTable
				.add(createMethod(null, "toString", arg, "String", "Object"));
		objectVTable.add(createMethod(null, "hashcode", arg, "int32_t",
				"Object"));
		objectVTable
				.add(createMethod(null, "getClass", arg, "Class", "Object"));
		objectVTable.add(createMethod(null, "equals", new String[] { "Object",
				"Object" }, "bool", "Object"));

		return objectVTable;
	}

	private GNode getObjectDataLayout() {
		// Creates datalayout for the object class
		GNode objectDataLayout = GNode.create("DataLayout");
		objectDataLayout.add(createDataFieldEntry(null, "__Object_VT*",
				"__vptr", null));
		objectDataLayout.add(createDataFieldEntry("static", "__Object_VT",
				"vtable", null));
		objectDataLayout.add(createConstructor("Object", null));
		String arg[] = { "__Object" };
		String modifier[] = { "static" };
		objectDataLayout.add(createMethod(modifier, "toString", arg, "String",
				"Object"));
		objectDataLayout.add(createMethod(modifier, "hashcode", arg, "int32_t",
				"Object"));
		objectDataLayout.add(createMethod(modifier, "getClass", arg, "Class",
				"Object"));
		objectDataLayout.add(createMethod(modifier, "equals", new String[] {
				"Object", "Object" }, "bool", "Object"));
		objectDataLayout.add(createMethod(modifier, "__class", null, "Class",
				"Object"));
		return objectDataLayout;
	}

	private GNode getStringDataLayout() {
		// Create the data layout for the String Class
		GNode stringDataLayout = GNode.create("DataLayout");
		stringDataLayout.add(createDataFieldEntry(null, "__String_VT*",
				"__vptr", null));
		stringDataLayout.add(createDataFieldEntry("static", "__String_VT",
				"vtable", null));
		stringDataLayout.add(createConstructor("String", null));
		String arg[] = { "__String" };
		String modifier[] = { "static" };
		stringDataLayout.add(createMethod(modifier, "toString", arg, "String",
				"String"));
		stringDataLayout.add(createMethod(modifier, "hashcode", arg, "int32_t",
				"String"));
		stringDataLayout.add(createMethod(modifier, "getClass", arg, "Class",
				"String"));
		stringDataLayout.add(createMethod(modifier, "equals", new String[] {
				"__String", "__Object" }, "bool", "String"));
		stringDataLayout.add(createMethod(modifier, "__class", null, "Class",
				"String"));
		stringDataLayout.add(createMethod(new String[] { "static" }, "length",
				new String[] { "__String" }, "int32_t", "String"));
		stringDataLayout.add(createMethod(new String[] { "static" }, "charAt",
				new String[] { "__String", "int32_t" }, "int32_t", "String"));
		return stringDataLayout;

	}

	private GNode getStringVTable() {
		GNode stringVTable = getObjectVTable();
		stringVTable.add(createMethod(null, "length",
				new String[] { "__Object" }, "int32_t", "String"));
		stringVTable.add(createMethod(null, "charAt",
				new String[] { "__Object" }, "int32_t", "String"));
		return stringVTable;
	}

	private GNode getClassDataLayout() {
		// create the data layout for the java.lang.Class class
		GNode classDataLayout = GNode.create("DataLayout");
		classDataLayout.add(createDataFieldEntry(null, "__Class_VT*", "__vptr",
				null));
		classDataLayout.add(createDataFieldEntry(null, "String", "name", null));
		classDataLayout
				.add(createDataFieldEntry(null, "Class", "parent", null));
		classDataLayout.add(createDataFieldEntry("static", "__Class_VT",
				"vtable", null));
		classDataLayout.add(createConstructor("Class", new String[] { "name",
				"parent" }));
		String arg[] = { "__Object" };
		String modifier[] = { "static" };
		classDataLayout.add(createMethod(modifier, "toString", arg, "String",
				"Class"));
		classDataLayout.add(createMethod(modifier, "getName", null, "String",
				"Class"));
		classDataLayout.add(createMethod(modifier, "getSuperclass", null,
				"Class", "Class"));
		classDataLayout.add(createMethod(new String[] { "Class", "Object" },
				"isInstance", null, "bool", "Class"));
		classDataLayout.add(createMethod(modifier, "__class", null, "Class",
				"Class"));
		return classDataLayout;

	}

	private GNode getClassVTable() {
		GNode classVTable = getObjectVTable();
		classVTable.add(createMethod(null, "getName",
				new String[] { "__Class" }, "String", "Class"));
		classVTable.add(createMethod(null, "getSuperclass",
				new String[] { "__Class" }, "Class", "Class"));
		classVTable.add(createMethod(null, "isInstance", new String[] {
				"__Class", "__Object" }, "bool", "Class"));

		return classVTable;
	}

	private GNode createMethod(String modifiers[], String name, String[] args,
			String returnType, String className) {
		// Create a GNode with method arguments and the returnType as children.
		GNode methodDeclaration = GNode.create("MethodDeclaration");
		GNode modifierDeclaration = GNode.create("Modifiers");
		GNode parameters = GNode.create("Parameters");

		if (modifiers != null) {
			for (String mod : modifiers) {
				modifierDeclaration.add(mod);
			}
		}
		methodDeclaration.add(modifierDeclaration);
		methodDeclaration.add(returnType);
		methodDeclaration.add(name);
		methodDeclaration.add(className);

		if (args != null) {
			for (String arg : args) {
				parameters.add(arg);
			}
		}
		methodDeclaration.add(parameters);

		return methodDeclaration;
	}

	private GNode createDataFieldEntry(String modifier, String type,
			String name, String declarator) {
		GNode node = GNode.create("FieldDeclaration");
		GNode modifiers = GNode.create("Modifiers");
		GNode declarators = GNode.create("Declarators");

		if (modifier != null) {
			modifiers.add(modifier);
		}

		node.add(modifiers);
		node.add(type);
		node.add(name);

		if (declarator != null) {
			declarators.add(declarator);
		}

		node.add(declarators);
		return node;
	}

	private GNode createMethodWithModifier(String modifier, String returnType,
			String[] args, String name) {
		GNode basic = GNode.create(name);
		basic.add(modifier);
		basic.add(returnType);
		if (args == null) {
			return basic;
		}

		for (int i = 0; i < args.length; i++) {
			basic.add(args[i]);
		}
		return basic;
	}

	private GNode createConstructor(String classname, String parameters[]) {
		GNode constructor = GNode.create("ConstructorDeclaration");
		GNode constructorParameters = GNode.create("Parameters");
		constructor.add(classname);

		if (parameters != null) {
			for (String param : parameters) {
				constructorParameters.add(param);
			}
		}
		constructor.add(constructorParameters);
		return constructor;
	}

	// Builds the header in the tree
    public GNode buildHeader(GNode astNode, GNode parentNode) {
		GNode header = GNode.create("HeaderDeclaration");
		header.add(packageName);
		header.add(astNode.getString(1));
		header.add(getNodeDataLayout(astNode, parentNode));
		header.add(getNodeVTable(astNode, parentNode));
		return header;
	}

	public GNode parseNodeToInheritance(GNode n){
		new Visitor(){
			public void visitPackageDeclaration(GNode n){
				packageName = n.getNode(1).getString(0);
			}

			public void visitClassDeclaration(GNode n){
				targetNode = (GNode) n;
			}

			public void visit(GNode n) {
				for (Object o : n) {
					if (o instanceof Node)
						dispatch((Node) o);
				}
			}
		}.dispatch(n);

		GNode returnNode = GNode.create(targetNode.getString(1));
		returnNode.add(buildHeader(targetNode, null));
		return returnNode;
	}

	// Create the node's DataLayout node
    private GNode getNodeDataLayout(GNode astNode, GNode parentNode) {
	GNode dataLayout = getParentDataLayout(parentNode);
		String parent = astNode.getString(1);
		dataLayout.setProperty("parent", parent);
		String type = "__" + astNode.getString(1) + "_VT";
		dataLayout.set(0,
				createDataFieldEntry(null, type + "*", "__vptr", null));
		dataLayout.set(1,
				createDataFieldEntry("static", type, "__vtable", null));
		for (int i = 0; i < dataLayout.size(); i++) {
			if (dataLayout.get(i) != null && dataLayout.get(i) instanceof Node) {
				Node child = dataLayout.getNode(i);
				if (child.hasName("ConstructorDeclaration")) { 
				// removes the parent constructor
					dataLayout.remove(i);
				}
			}
		}
		for (int i = 0; i < astNode.size(); i++) {
			if (astNode.get(i) != null && astNode.get(i) instanceof Node) {
				Node child = astNode.getNode(i);
				if (child.hasName("ClassBody")) {
					handleClassBody(dataLayout, (GNode) child, false);
				}
			}
		}
		return dataLayout;
	}

	// Creates the node's VTable node
    private GNode getNodeVTable(GNode astNode, GNode parentNode) {
	GNode vTable = getParentVTable(parentNode);
		String parent = astNode.getString(1);
		vTable.setProperty("parent", parent);
		for (int i = 0; i < astNode.size(); i++) {
			if (astNode.get(i) != null && astNode.get(i) instanceof Node) {
				GNode child = (GNode) astNode.getNode(i);
				if (child.hasName("ClassBody")) {
					handleClassBody(vTable, child, true);
				}
			}
		}
		return vTable;
	}

	// Handles the ClassBody node into children of the class root 
	private GNode handleClassBody(GNode inheritNode, GNode astNode,
			boolean isVTable) {
		for (int i = 0; i < astNode.size(); i++) {
			if (astNode.get(i) != null && astNode.get(i) instanceof Node) {
				Node child = astNode.getNode(i);
				if (child.hasName("FieldDeclaration") && !isVTable) {
					handleFieldDeclaration(inheritNode, (GNode) child);
				} else if (child.hasName("MethodDeclaration")) {
					handleMethodDeclaration(inheritNode, (GNode) child,
							isVTable);
					if (isVTable) { 
						for (int j = 0; j < inheritNode.size() - 1; j++) {
							String searchName = (String) inheritNode.getNode(j)
									.get(2);
							String checkName = (String) inheritNode.getNode(
									inheritNode.size() - 1).get(2);
							if (inheritNode.getNode(j).getNode(4).size() != 0) {
								inheritNode
										.getNode(j)
										.getNode(4)
										.set(0,
												inheritNode
														.getProperty("parent"));
							}
							if (searchName.equals(checkName)) {
								inheritNode.remove(j);
								break;
							}
						}
					}
				} else if (child.hasName("ConstructorDeclaration") && !isVTable) {
					handleConstructorDeclaration(inheritNode, (GNode) child);
				}
			}
		}
		return inheritNode;
	}

	// Handles a FieldDeclaration from JavaAST to a similar one
	private GNode handleFieldDeclaration(GNode inheritNode, GNode astNode) {
		String modifier = null, type = null, name = null, declarator = null;
		for (int i = 0; i < astNode.size(); i++) {
			if (astNode.get(i) != null && astNode.get(i) instanceof Node) {
				Node child = astNode.getNode(i);
				if (child.hasName("Type")) { 
					type = convertType(((GNode) child.get(0)).getString(0));
				} else if (child.hasName("Declarators")) {
					GNode dec = (GNode) child.getNode(0);
					name = dec.getString(0);
					if (dec.getNode(2) != null)
						declarator = dec.getNode(2).getString(0);
				}
			}
		}
		inheritNode.add(createDataFieldEntry(modifier, type, name, declarator));
		return inheritNode;
	}

	// Handles a MethodDeclaration from JavaAST to a similar one
	private GNode handleMethodDeclaration(GNode inheritNode, GNode astNode,
			boolean isVTable) {
		String[] parameters = null, modifiers = null;
		if (!isVTable) {
			modifiers = new String[1];
			modifiers[0] = "static";
		}
		String name = null, returnType = null;
		if (astNode.getString(3) != null) {
			name = astNode.getString(3);
		}
		String className = (String) inheritNode.getProperty("parent");
		for (int i = 0; i < astNode.size(); i++) {
			if (astNode.get(i) != null && astNode.get(i) instanceof Node) {
				Node child = astNode.getNode(i);
				if (child.hasName("Type")) {
					returnType = convertType(((GNode) child.get(0))
							.getString(0));
				} else if (child.hasName("FormalParameters")) {
					Node param = child;
					if (param.size() > 0)
						parameters = new String[param.size()];
					for (int j = 0; j < param.size(); j++) {
						if (param.get(j) != null
								&& param.get(j) instanceof Node) {
							Node paramChild = param.getNode(j);
							if (paramChild.hasName("FormalParameter")) {
								parameters[j] = convertType(paramChild
										.getNode(1).getNode(0).getString(0));
							}
						}
					}
				}
			}
		}
		inheritNode.add((createMethod(modifiers, name, parameters, returnType,
				className)));
		return inheritNode;
	}

	// Handles a ConstructorDeclaration from JavaAST to a similar one
	private GNode handleConstructorDeclaration(GNode inheritNode, GNode astNode) {
		String name = null;
		String parameters[] = null;
		if (astNode.getString(2) != null) {
			name = astNode.getString(2);
		}
		for (int i = 0; i < astNode.size(); i++) {
			if (astNode.get(i) != null && astNode.get(i) instanceof Node) {
				Node child = astNode.getNode(i);
				if (child.hasName("FormalParameters")) {
					Node param = child;
					if (param.size() > 0)
						parameters = new String[param.size()];
					for (int j = 0; j < param.size(); j++) {
						if (param.get(j) != null
								&& param.get(j) instanceof Node) {
							Node paramChild = param.getNode(j);
							if (paramChild.hasName("FormalParameter")) {
								parameters[j] = convertType(paramChild
										.getNode(1).getNode(0).getString(0));
							}
						}
					}
				}
			}
		}
		inheritNode.add(createConstructor(name, parameters));
		return inheritNode;
	}

	// Converts the java type to the corresponding C++ Type
	private String convertType(String javaType) {
		String cppType = javaType;
		if (javaType.equals("int"))
			cppType = "int32_t";
		return cppType;
	}

	// Returns the parent node Data Layout for inheritance
	private GNode getParentDataLayout(GNode parent) {
		GNode dataLayout = null;
		if (parent == null) {
			dataLayout = getObjectDataLayout();
		} else {
			dataLayout = (GNode) parent.getNode(0).getNode(2);
		}
		return dataLayout;
	}

	// Returns the parent node VTable for inheritance
	private GNode getParentVTable(GNode parent) {
		GNode vTable = null;
		if (parent == null) {
			vTable = getObjectVTable();
		} else {
			vTable = (GNode) parent.getNode(0).getNode(3);
		}
		return vTable;
	}

}
