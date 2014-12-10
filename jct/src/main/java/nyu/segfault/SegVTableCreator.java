package nyu.segfault;


import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;

import xtc.util.Tool;

import static org.junit.Assert.*;

/** SegVTableCreator creates a VTable for any class node.
 * It extends SegNode so that it can use all of it's methods.
 */
public class SegVTableCreator extends SegNode {

    private GNode objectVTable;
    private GNode stringVTable;
    private GNode classVTable;

    public SegVTableCreator() {
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
	String arg[] = { "Object" };
	objectVTable.add(createMethod(null, "__isa", null, "Class", "Object", isVTable));
	objectVTable.add(createMethod(null, "hashCode", null, "int",
				      "Object", isVTable));
	objectVTable.add(createMethod(null, "equals", new String[] {"Object" }, "bool", "Object", isVTable));
	objectVTable.add(createMethod(null, "getClass", null, "Class", "Object", isVTable));
	objectVTable.add(createMethod(null, "toString", null, "String", "Object", isVTable));
	return objectVTable;
    }

    private GNode setStringVTable() {
	boolean isVTable = true;
	GNode stringVTable = setObjectVTable();
	stringVTable.add(createMethod(null, "length",
				      null, "int", "String",isVTable));
	stringVTable.add(createMethod(null, "charAt",
				      null, "int", "String",isVTable));
	return stringVTable;
    }

    private GNode setClassVTable() {
	boolean isVTable = true;
	GNode classVTable = setObjectVTable();
	classVTable.add(createMethod(null, "getName",
				     null, "String", "Class",isVTable));
	classVTable.add(createMethod(null, "getSuperclass",
				     new String[] { "Class" }, "Class", "Class",isVTable));
	classVTable.add(createMethod(null, "isInstance", new String[] {"Object"}, "bool", "Class",isVTable));

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
