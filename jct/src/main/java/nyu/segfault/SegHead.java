package nyu.segfault;

/**
 * Java SE library import statements
 */
import java.io.*;
import java.util.*;

/**
 * xtc import statements
 */

import xtc.lang.JavaFiveParser;

import xtc.parser.ParseException;
import xtc.parser.Result;

import xtc.util.SymbolTable;
import xtc.util.Tool;

import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;


/**
 * SegHead Visitor  handles classes without inheritance and virtual methods
 */
public class SegHead extends Visitor{

	HashSet<String> publicHPP; /**@var fields that fall under the public access modifier */
	HashSet<String> privateHPP; /**@var fields that fall under the private access modifer */

	HashSet<String> publicHPPMethods; /**@var methods that fall under public access modifier */
	HashSet<String> privateHPPMethods; /**@var methods that fall under private access modifier */

	/**
	 * default constructor for SegHead
	 */
	public SegHead(){
        this.privateHPP = new HashSet<String>();
        this.publicHPP = new HashSet<String>();
        this.privateHPPMethods = new HashSet<String>();
        this.publicHPPMethods = new HashSet<String>();
	}
	public void visitCompilationUnit(GNode n){
		SegHelper.writeMacros();
		SegHelper.endMacroScopes();
        SegHelper.hpp_pln("");
        SegHelper.hpp_pln("using namespace java::lang;\n");
        SegHelper.hpp_pln("struct __" + SegHelper.getFileName() + ";");
        SegHelper.hpp_pln("struct __" + SegHelper.getFileName() + "_VT;");

        for (String className : SegHelper.allDeclaredClassNames) {
            SegHelper.hpp_pln("typedef __rt::Ptr<__" + className + "> " + className + ";");
        }
        SegHelper.hpp_pln("");

        visit(n);
    }
	public void visitClassDeclaration(GNode n) {
        String className = n.getString(1);

        // Print the virtual table pointer data field.
		SegHelper.hpp_pln(SegHelper.getClassDeclaration(n) + " {");
        SegHelper.hpp_pln("\t__" + SegHelper.getClassName(n) + "_VT* __vptr;  // Virtual table pointer. \n\n");

        // Print the constructor.
        SegHelper.hpp_pln("\t// The constructor.");
        SegHelper.hpp_pln("\n\n");

        // Print the Object superclass method declarations.
        SegHelper.hpp_pln("\t// This class's method declarations.");
        for (String methodDeclaration : SegHelper.getObjectMethodDeclarations()) {
            String tailoredDeclaration = SegHelper.getDeclarationWithNewThisParameter(methodDeclaration, className);
            SegHelper.hpp_pln("\t" + tailoredDeclaration + ";");
        }

        // Print any superclass method declarations.
            // get list of all superclasses in order from closest to furthest (Object)

        // Print this class's method declarations.
        for (String methodDeclaration : SegHelper.classNameToMethodDeclarations.get(className)) {
            SegHelper.hpp_pln("\t" + methodDeclaration + ";");
        }

        // Print the function returning this class's object.
        SegHelper.hpp_pln("\n\t// The function returning the class object representing " + SegHelper.getClassName(n) + ".");
        SegHelper.hpp_pln("\tstatic Class __class();\n");

        // If this class does not explicitly extend a class, use Object's declarations. Else, use the declarations of
        // the first superclass that uses the same

        // Print the virtual table data field.
        SegHelper.hpp_pln("\t// The vtable for " + SegHelper.getClassName(n) + ".");
        SegHelper.hpp_pln("\tstatic __" + SegHelper.getClassName(n) + "_VT __vtable;");

		SegHelper.hpp_pln("};\n\n");

		/**generate vtable for that respective class*/
		SegHelper.genVTable();

//		String super_class=SegHelper.getSuperClass(n);
//		if(super_class == null){
//			SegHelper.Root.addChild(new CppClass(SegHelper.getClassName(n)));
//		} else {
//			SegNode<CppClass> parent=SegHelper.Root.dfs(SegHelper.Root,new CppClass(super_class));
//			parent.addChild(new CppClass(SegHelper.getClassName(n)));
//		}
//		visit(n);
	}


	public void visitMethodDeclaration(GNode n){
        if (n.getNode(2) == null) return;  // This is a constructor

		String method_decl=SegHelper.getMethodDeclaration(n,SegHelper.getCurrClass());
		if(method_decl != null){
			SegHelper.hpp_pln("\t"+method_decl);
		}
        String pointer = SegHelper.getPointerFromMethodDeclaration(n);
        System.out.println(pointer);
	}


	public void visit(GNode n) {
		for (Object o : n) if (o instanceof Node) dispatch((Node) o);
	}
}
