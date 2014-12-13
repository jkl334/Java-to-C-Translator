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

public class SegDataLayoutHandler extends SegNode {

  private GNode objectDataLayout;
  private GNode stringDataLayout;
  private GNode classDataLayout;

  public SegDataLayoutHandler() {

		objectDataLayout = setObjectDataLayout();
		stringDataLayout = setStringDataLayout();
		classDataLayout = setClassDataLayout();
  }

  //Function that creates a datalayout from a javaAST below:
  public GNode getNodeDataLayout(GNode astNode, GNode parentNode) {
		GNode dataLayout = parentNode;
		String parent = astNode.getString(1);
		dataLayout.setProperty("parent", parent);
		String type = "__" + astNode.getString(1) + "_VT";
		dataLayout.set(0, createDataFieldEntry(null, type + "*", "__vptr", null));
		dataLayout.set(1, createDataFieldEntry("static", type, "__vtable", null));

		for (int i = 0; i < dataLayout.size(); i++) {
	    if (dataLayout.get(i) != null && dataLayout.get(i) instanceof Node) {
				Node child = dataLayout.getNode(i);

				if (child.hasName("ConstructorDeclaration")) {
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

  private GNode setObjectDataLayout() {
		// Create the Data Layout here for Object Class
		boolean isVTable = false;
		GNode objectDataLayout = GNode.create("DataLayout");
		objectDataLayout.add(createDataFieldEntry(null, "__Object_VT*",
					"__vptr", null));
		objectDataLayout.add(createDataFieldEntry("static", "__Object_VT",
					"vtable", null));
		objectDataLayout.add(createConstructor("Object", null));
		String arg[] = { "__Object" };
		String modifier[] = { "static" };
		objectDataLayout.add(createMethod(modifier, "toString", arg, "String",
					"Object", isVTable));
		objectDataLayout.add(createMethod(modifier, "hashCode", arg, "int32_t",
					"Object",isVTable));
		objectDataLayout.add(createMethod(modifier, "getClass", arg, "Class",
					"Object",isVTable));
		objectDataLayout.add(createMethod(modifier, "equals", new String[] {
					"Object", "Object" }, "bool", "Object",isVTable));
		objectDataLayout.add(createMethod(modifier, "__class", null, "Class",
					"Object",isVTable));
		return objectDataLayout;
  }

  private GNode setStringDataLayout() {
		// Create the Data Layout for the String Class
		boolean isVTable = false;
		GNode stringDataLayout = GNode.create("DataLayout");
		stringDataLayout.add(createDataFieldEntry(null, "__String_VT*",
					"__vptr", null));
		stringDataLayout.add(createDataFieldEntry("static", "__String_VT",
					"vtable", null));
		stringDataLayout.add(createConstructor("String", null));
		String arg[] = { "__String" };
		String modifier[] = { "static" };
		stringDataLayout.add(createMethod(modifier, "toString", arg, "String",
					"String",isVTable));
		stringDataLayout.add(createMethod(modifier, "hashCode", arg, "int32_t",
					"String",isVTable));
		stringDataLayout.add(createMethod(modifier, "getClass", arg, "Class",
					"String",isVTable));
		stringDataLayout.add(createMethod(modifier, "equals", new String[] {
					"__String", "__Object" }, "bool", "String",isVTable));
		stringDataLayout.add(createMethod(modifier, "__class", null, "Class",
					"String",isVTable));
		stringDataLayout.add(createMethod(new String[] { "static" }, "length",
					new String[] { "__String" }, "int32_t", "String",isVTable));
		stringDataLayout.add(createMethod(new String[] { "static" }, "charAt",
					new String[] { "__String", "int32_t" }, "int32_t", "String",isVTable));
		return stringDataLayout;
  }

  private GNode setClassDataLayout() {
		// Create the Data Layout for the java.lang.Class class
		boolean isVTable = false;
		GNode classDataLayout = GNode.create("DataLayout");
		classDataLayout.add(createDataFieldEntry(null, "__Class_VT*", "__vptr",
					null));
		classDataLayout.add(createDataFieldEntry(null, "String", "name", null));
		classDataLayout.add(createDataFieldEntry(null, "Class", "parent", null));
		classDataLayout.add(createDataFieldEntry("static", "__Class_VT",
					"vtable", null));
		classDataLayout.add(createConstructor("Class", new String[] { "name",
					"parent" }));
		String arg[] = { "__Object" };
		String modifier[] = { "static" };
		classDataLayout.add(createMethod(modifier, "toString", arg, "String",
					"Class",isVTable));
		classDataLayout.add(createMethod(modifier, "getName", null, "String",
					"Class",isVTable));
		classDataLayout.add(createMethod(modifier, "getSuperclass", null,
					"Class", "Class",isVTable));
		classDataLayout.add(createMethod(new String[] { "Class", "Object" },
					"isInstance", null, "bool", "Class",isVTable));
		classDataLayout.add(createMethod(modifier, "__class", null, "Class",
					"Class",isVTable));
		return classDataLayout;
  }

  public GNode getObjectDataLayout() {
		return objectDataLayout;
  }

  public GNode getStringDataLayout() {
		return stringDataLayout;
	}

  public GNode getClassDataLayout() {
		return classDataLayout;
  }
}