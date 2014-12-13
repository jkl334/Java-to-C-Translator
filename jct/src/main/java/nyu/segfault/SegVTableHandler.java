package nyu.segfault;

import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;

import xtc.util.Tool;


public class SegVTableHandler extends SegNode {

    private GNode objectVTable;
    private GNode stringVTable;
    private GNode classVTable;

    public SegVTableHandler() {
	objectVTable = setObjectVTable();
	stringVTable = setStringVTable();
	classVTable = setClassVTable();
    }

    // Creates the node's VTable node
    public GNode getNodeVTable(GNode astNode, GNode parentNode) {
	GNode vTable = parentNode;
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

    private GNode setObjectVTable() {
	// Create the VTable here for Object Class
	boolean isVTable = true;
	GNode objectVTable = GNode.create("VTable");
	String arg[] = { "__Object" };
	objectVTable.add(createMethod(null, "__isa", null, "Class", "Object", isVTable));
	objectVTable.add(createMethod(null, "toString", arg, "String", "Object", isVTable));
	objectVTable.add(createMethod(null, "hashCode", arg, "int32_t",
				      "Object", isVTable));
	objectVTable.add(createMethod(null, "getClass", arg, "Class", "Object", isVTable));
	objectVTable.add(createMethod(null, "equals", new String[] { "Object",
				"Object" }, "bool", "Object", isVTable));

	return objectVTable;
    }

    private GNode setStringVTable() {
	boolean isVTable = true;
	GNode stringVTable = setObjectVTable();
	stringVTable.add(createMethod(null, "length",
				      new String[] { "__Object" }, "int32_t", "String",isVTable));
	stringVTable.add(createMethod(null, "charAt",
				      new String[] { "__Object" }, "int32_t", "String",isVTable));
	return stringVTable;
    }

    private GNode setClassVTable() {
	boolean isVTable = true;
	GNode classVTable = setObjectVTable();
	classVTable.add(createMethod(null, "getName",
				     new String[] { "__Class" }, "String", "Class",isVTable));
	classVTable.add(createMethod(null, "getSuperclass",
				     new String[] { "__Class" }, "Class", "Class",isVTable));
	classVTable.add(createMethod(null, "isInstance", new String[] {
		    "__Class", "__Object" }, "bool", "Class",isVTable));

	return classVTable;
    }


    public GNode getObjectVTable() {
	return objectVTable;
    }

    public GNode getStringVTable() {
	return stringVTable;
    }

    public GNode getClassVTable() {
	return classVTable;
    }
}