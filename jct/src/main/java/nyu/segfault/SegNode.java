package nyu.segfault;

import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;

import xtc.util.Tool;


public class SegNode {

	public SegNode() {
	}

    // Parses the ClassBody node into children of the class root 
	protected GNode handleClassBody(GNode inheritNode, GNode astNode, boolean isVTable) {
		boolean foundConstructor = false;
			GNode nodesToOverload = GNode.create("NodesToOverload");

		if (inheritNode.size() > 0) {
			for (int k=0;k<inheritNode.size();k++) {
				if (inheritNode.getNode(k).size()==7) {
					inheritNode.getNode(k).set(5, "null");
					if (inheritNode.getNode(k).getNode(4).size() != 0 && inheritNode.getNode(k).getString(3)=="Object") {
						//Renames the parameters in a method to be the classname type
						inheritNode.getNode(k).getNode(4).set(0,inheritNode.getProperty("parent"));
					}
				}
			}
		}		

		for (int i = 0; i < astNode.size(); i++) {
			if (astNode.get(i) != null && astNode.get(i) instanceof Node) {
				Node child = astNode.getNode(i);
				if (child.hasName("FieldDeclaration") && !isVTable) {
					handleFieldDeclaration(inheritNode, (GNode) child);
				} else if (child.hasName("MethodDeclaration")) {
						handleMethodDeclaration(inheritNode, (GNode) child,
							isVTable);
						boolean isOverwritten = false;
						for (int j = 0; j < inheritNode.size() - 1; j++) {
							if (inheritNode.getNode(j).size()==7) {
								if (nodeEquals((GNode)inheritNode.getNode(j), (GNode)inheritNode.getNode(inheritNode.size()-1), true)) {
									inheritNode.set(j, inheritNode.getNode(inheritNode.size()-1));
									inheritNode.getNode(j).set(5, "Overwritten");
									isOverwritten = true;
									break;
								}
							}
						}
						if(isOverwritten){
							inheritNode.remove(inheritNode.size() -1);
						}
						else {
							inheritNode.getNode(inheritNode.size()-1).set(5, "New");
						}
						checkForOverloading(inheritNode, (GNode)inheritNode.getNode(inheritNode.size()-1), nodesToOverload);
					}
					else if (child.hasName("ConstructorDeclaration") && !isVTable) {
					foundConstructor = true;
					handleConstructorDeclaration(inheritNode, (GNode) child);
				}
			}
		}

		if (!isVTable && !foundConstructor){
			String className = inheritNode.getProperty("parent").toString();
			inheritNode.add(2,createConstructor(className, null));	
		}
		executeOverloading(nodesToOverload);
		return inheritNode;
	}

	private boolean nodeEquals(GNode node1, GNode node2, boolean methodOverwriting) {
		if (!node1.getName().equals(node2.getName())) {
			return false;
		}
		else if (node1.size() != node2.size()) {
			return false;
		}
		else {
			boolean temp = true;
			for (int i=0;i<node1.size();i++) {
				if (methodOverwriting && i==3) {
					continue;
				}

				if (methodOverwriting && i==4 && (node1.getNode(i).size()==0 || node2.getNode(i).size() == 0)) {
					continue;
				}

				if (methodOverwriting && i>4) {
					return true;
				}



				if (node1.get(i) instanceof String && node2.get(i) instanceof String) {
					temp = node1.getString(i).equals(node2.getString(i));
				}
				else if (node1.get(i) instanceof Node && node2.get(i) instanceof Node) {
					if (!node1.getName().equals(node2.getName())) {
						return false;
					}
					temp = nodeEquals((GNode)node1.getNode(i), (GNode)node2.getNode(i), false);
				}
				else {
					return false;
				}

				if (temp == false) {
					return false;
				}
			}
		}

		return true;
	}

	/*Runs through each node that is a child of masterNode 
	and compares it to currentNode.  If they have the same name, 
	then switch the names of the two to be the appropriate 
	overloaded names.
	*/
	protected void checkForOverloading(GNode masterNode, GNode currentNode, GNode nodesToOverload) {
		if (masterNode.size() > 0) {
			for (int i=0;i<masterNode.size()-1;i++) {
				if (masterNode.getNode(i).hasProperty("typeIsMethod")) {
					String masterString = masterNode.getNode(i).getString(2);
					String currentString = currentNode.getString(2);
					if (masterString.equals(currentString)) {
						boolean addCurrentNode = true;
						boolean addMasterNode = true;
						if (nodesToOverload.size() > 0) {
							for (int j=0;j<nodesToOverload.size();j++) {
								if (currentNode.equals(nodesToOverload.getNode(j))) {
									addCurrentNode = false;
								}
								if (masterNode.getNode(i).equals(nodesToOverload.getNode(j))) {
									addMasterNode = false;
								}
							}
						}
						if (addCurrentNode) {
							currentNode.set(6, "Overloaded");
							nodesToOverload.add(currentNode);
						}
						if (addMasterNode) {
							masterNode.getNode(i).set(6, "Overloaded");
							nodesToOverload.add(masterNode.getNode(i));
						}
						break;
					}
				}
			}
		}
	}

	protected void executeOverloading(GNode nodesToOverload) {
		if (nodesToOverload.size()==0) {
			return;
		}

		for (int i=0;i<nodesToOverload.size();i++) {
			String newNodeString = nodesToOverload.getNode(i).getString(2);
			if (nodesToOverload.getNode(i).getNode(4).size() > 0) {
				for (int j=0;j<nodesToOverload.getNode(i).getNode(4).size();j++) {
					String typeToAppend = nodesToOverload.getNode(i).getNode(4).getString(j);
					typeToAppend = typeToAppend.replace(" ", "_");
					newNodeString = newNodeString+"_"+typeToAppend;
				}
				nodesToOverload.getNode(i).set(2, newNodeString);
			}
		}
	}

    // Parses a FieldDeclaration from JavaAST to a similar one 
	protected GNode handleFieldDeclaration(GNode inheritNode, GNode astNode) {
		String modifier = null, type = null, name = null, declarator = null;
		for (int i = 0; i < astNode.size(); i++) {
			if (astNode.get(i) != null && astNode.get(i) instanceof Node) {
				Node child = astNode.getNode(i);
		if (child.hasName("Type")) { // Gets the field type
			type = convertMethodNameType(((GNode) child.get(0)).getString(0));
		} else if (child.hasName("Declarators")) {
			GNode dec = (GNode) child.getNode(0);
			name = dec.getString(0);
		    if (dec.getNode(2) != null)// verifies if there is an
			// initial value to the variable
		    declarator = dec.getNode(2).getString(0);
		  }
		}
	}
	inheritNode.add(createDataFieldEntry(modifier, type, name, declarator));
	return inheritNode;
	}

	    // Parses a MethodDeclaration from JavaAST to a similar one 
	protected void handleMethodDeclaration(GNode inheritNode, GNode astNode,
		boolean isVTable) {
		String[] parameters = null, modifiers = null;
		String classname = null;
		if (!isVTable) {
			modifiers = new String[1];
			modifiers[0] = "static";
		}
		String name = null, returnType = null;
		if (astNode.getString(3) != null) {
			name = astNode.getString(3);
			if (name.equals("main")) return;
		}
		classname = (String) inheritNode.getProperty("parent");
		for (int i = 0; i < astNode.size(); i++) {
			if (astNode.get(i) != null && astNode.get(i) instanceof Node) {
				Node child = astNode.getNode(i);
				if (child.hasName("Type")) {
					returnType = convertMethodNameType(((GNode) child.get(0))
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
							parameters[j] = convertMethodNameType(paramChild
								.getNode(1).getNode(0).getString(0));
						}
					}
				}
			}
		}
	}
	inheritNode.add((createMethod(modifiers, name, parameters, returnType,
		classname, isVTable)));
	}

	    // Parses a ConstructorDeclaration from JavaAST to a similar one
	protected GNode handleConstructorDeclaration(GNode inheritNode, GNode astNode) {
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
							parameters[j] = convertMethodNameType(paramChild
								.getNode(1).getNode(0).getString(0));
						}
					}
				}
			}
		}
	}
	inheritNode.add(2, createConstructor(name, parameters));
	return inheritNode;
	}


	protected GNode createConstructor(String classname, String parameters[]) {
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


	protected GNode createMethod(String modifiers[], String name, String[] args, String returnType, String className, boolean isVTable) {

		GNode methodDeclaration = null;
		if(isVTable)
			methodDeclaration = GNode.create("VTableMethodDeclaration");
		else {
			methodDeclaration = GNode.create("DataLayoutMethodDeclaration");
		}

		methodDeclaration.setProperty("typeIsMethod", true);

		GNode modifierDeclaration = GNode.create("Modifiers");
		GNode parameters = GNode.create("Parameters");
		
		if (modifiers != null) {
			for (String mod : modifiers) {
				modifierDeclaration.add(mod);
			}
		}
		methodDeclaration.add(modifierDeclaration);

		if (returnType==null) {
			returnType="void";
		}
		methodDeclaration.add(returnType);

		if (args != null) {
			for (String arg : args) {
				parameters.add(arg);
			}
		}

		methodDeclaration.add(name);
		if(className != null){
			methodDeclaration.add(className);
		}

		methodDeclaration.add(parameters);
		methodDeclaration.add("null");
		methodDeclaration.add("null");

		return methodDeclaration;
	}

	protected GNode createDataFieldEntry(String modifier, String type, String name, String declarator) {
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

	protected String convertMethodNameType(String javaType) {
		String cppType = javaType;
		if (javaType.equals("int")){
			cppType = "int32_t";
		}
		return cppType;
	}
}