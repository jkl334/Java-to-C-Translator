package nyu.segfault;

import java.io.*;
import java.util.*;

import java.lang.reflect.*;
import java.lang.Thread;

import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.util.SymbolTable;
import xtc.util.Tool;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;

public class SegHelper {

	private static  String file_name; /**@var name of input java source code */

	private static File impFile; /**@var cpp file */
	private static File headFile; /**@var hpp file */

	private static PrintWriter cWriter; /**@var printstream to cpp file */
	private static PrintWriter hWriter; /**@var printstream to hpp file */

	private static Printer cppWriter; /**@var xtc cpp printstream wrapper class */
	private static Printer hppWriter; /**@var xtc hpp printstream wrapper class */

    /**@var List of current class's global variables/assignments */
    public static final ArrayList<String> currentClassGlobalVariables = new ArrayList<String>();

	/**@var class inheritance tree */
	public static final SegNode<CppClass> Root=new SegNode<CppClass>(new CppClass("Object"));

	/**@var current class name*/
	public static String currClass;

	/**@var communication buffer between nested anonymous visitor classes and SegHelper */
	public static String buffer;

	/**@var method buffer for generating functions pointers in vtable */
	public static ArrayList<String> mbuffer;

	/**@var return_type buffer for generate function pointers in vtable */
	public static ArrayList<String> rbuffer;

	/**@var parameter buffer to generate functionpointers in vtable */
	public static ArrayList<ArrayList<String>> pbuffer;

    /**@var global variable to store constructor property in class declaration and to use to assign arguments in struct initialization in visitFieldDeclaration -Jeff */
    public static String constructorProp;

    /**@var The symbol table corresponding to the Java input file. */
    public static SymbolTable symbolTable;

    /**@var A list of all class names that are declared in the Java input file. */
    public static ArrayList<String> allDeclaredClassNames;

    /**@var A list of all class names that are declared in the Java input file. */
    public static HashMap<String, ArrayList<String>> classNameToMethodDeclarations;

    /**@var A hashmap of class names to superclass names. */
    public static HashMap<String, String> classToSuperclass;

    /**@var A hashmap of class names to the method declarations of all methods it has access to. */
    public static HashMap<String, ArrayList<String>> classToAllAvailableMethodDeclarations;


	/**
	 * set the file_name data field and create files
	 */
	public static void setFileName(String fn){
        System.out.println("SegHelper.setFileName: " + fn);
		file_name=fn.replace(".java", "");
		try{
			impFile=new File(getFileName()+".cpp");
			headFile=new File(getFileName()+".hpp");

			cWriter=new PrintWriter(impFile);
			hWriter=new PrintWriter(headFile);

			cppWriter=new Printer(cWriter);
			hppWriter=new Printer(hWriter);
		} catch(Exception e) {
			System.out.println("SegHelper.setFileName: " + e);
		}
	}
	/**
	 * get file_name
	 * @return name of file
	 */
	public static String getFileName(){
		return file_name;
	}
	/**
	 * set the current class
	 */
	private static void setCurrClass(String cs){
		currClass=cs;
	}
	/**
	 * @return string name of current class
	 */
	public static String getCurrClass(){
		return currClass;
	}

	/**
	 * write macros to both cpp and hpp files
	 */
	public static void writeMacros(){

		/**@var STL macros for hpp file */

		final String[] stlMacros=new String[]{"\"java_lang.h\""};

		/**@var cpp macro definitions  */
		final String [] cppMacros=new String[]{"#include \""+getFileName()+".hpp\"", "using namespace java::lang;"};

		final StackTraceElement [] stack=Thread.currentThread().getStackTrace();

		if(stack[2].getClassName().contains("SegHead")){
			/**
			 * write header guard and namespaces
			 */
			SegHelper.hpp_pln("#ifndef SEGFAULT_HPP");

			for (String stlMacro : stlMacros )
				hppWriter.pln("#include "+stlMacro);
		}

		else if(stack[2].getClassName().contains("SegImp")){
			for(String cppMacro : cppMacros) {
                cppWriter.pln(cppMacro);
            }
		}
        cppWriter.pln();

		cppWriter.flush(); hppWriter.flush();
	}
	public static  void endMacroScopes(){

		/** end macro guard */
		hppWriter.pln("#endif");
	}

    /**
     * Returns the body of the method declaration represented by the given GNode. Must be called
     * on a visitMethodDeclaration GNode.
     *
     * @param n The node in the Java AST
     * @return  The entire body of the method declaration.
     */
    public static String getMethodBody(GNode n) {

//        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
       // if (!stack[2].getClassName().contains("SegImp")) return;

        final StringBuilder mBod = new StringBuilder();  // The method body.
        String method_return_type = "";
        final boolean isPrivate = false;

        final GNode root = n;
        final String return_type = n.getNode(2) == null ? "" : n.getNode(2).toString();
        try {
            method_return_type = n.getNode(2).getNode(0).getString(0);
        }
        catch (Exception e) { }
        new Visitor(){
            public void visitFieldDeclaration(GNode n) {  // Need to add visitMethodDeclaration() to visitor for advanced FieldDeclarations.
                /* Determine and print the declarator type. */
                mBod.append("\t");  // Indent the field once (because it is within a method body).
                String declarationType = n.getNode(1).getNode(0).getString(0);
                if (declarationType.equals("boolean")) { mBod.append("boolean "); }
                else if (declarationType.equals("char")) { mBod.append("char "); }
                else if (declarationType.equals("double")) { mBod.append("double "); }
                else if (declarationType.equals("float")) { mBod.append("float "); }
                else if (declarationType.equals("int")) { mBod.append("int "); }
                else if (declarationType.equals("String")) { mBod.append("String "); }
                else if (declarationType.equals("Object")) { mBod.append("Object " ); }
                /* CREATE THE SMART POINTER */
                else if (n.getNode(1).getNode(0).getName().equals("QualifiedIdentifier")) { mBod.append("__rt::Ptr<" + n.getNode(1).getNode(0).getString(0) + "> "); }

                /* Print the name of the field. */
                String fieldName = n.getNode(2).getNode(0).getString(0);
                mBod.append(fieldName);

                /* Potentially visit the assigned value (if any). */
                new Visitor() {
                    public void visitDeclarators(GNode n) {
                        try {
                            String s = n.getNode(0).getNode(2).getString(0);
                            if (s != null) {
                                boolean isJavaString = s.length() >=2
                                        && s.substring(0,1).equals("\"")
                                        && s.substring(s.length() - 1).equals("\"");
                                if (isJavaString) {
                                    mBod.append(" = __rt::literal(" + s + ")");
                                }
                                else{
                                    mBod.append(" = " + s);
                                }
                            }
                        } catch(Exception e) { }
                        new Visitor() {
                            public void visitNewClassExpression(GNode n) {
                                if (n.getNode(3).size() > 0) {  // if arguments exist for object initializing
                                    mBod.append(" = (" + n.getNode(2).getString(0) + ")" + " {" + "." + constructorProp + " = " + n.getNode(3).getNode(0).getString(0) + "}");  //  only 1 argument works for now
                                } else if (n.getNode(3).toString().equals("Arguments()"))  // Arguments do not exist.
                                    ;// mBod.append("(" + n.getNode(2).getString(0) + ")" + " {" + " }");

                            }
                            public void visit(GNode n) { for (Object o : n) if(o instanceof Node) dispatch((Node)o); }
                        }.dispatch(n);
                    }

                    public void visitStringLiteral(GNode n) { mBod.append(" = new __String(" + n.getString(0) + ")"); }
                    public void visitIntegerLiteral(GNode n) { mBod.append(" = " + n.getString(0)); }
                    public void visitFloatingPointLiteral(GNode n) { mBod.append(" = " + n.getString(0)); }
                    public void visitCharacterLiteral(GNode n) { mBod.append(" = " + n.getString(0)); }
                    public void visitBooleanLiteral(GNode n) { mBod.append(" = " + n.getString(0)); }
                    public void visit(GNode n) { for (Object o : n) if(o instanceof Node) dispatch((Node)o); }
                }.dispatch(n);
                mBod.append(";").append("\n");
            }

            public void visitReturnStatement(GNode n) {
                mBod.append("\t");  // Return statements will generally be indented (since they are located in the method body).
                if (n.getNode(0) != null) mBod.append("return ");
                new Visitor() {  // Visit assigned value if any
                    public void visitStringLiteral(GNode n) { mBod.append(n.getString(0)); }  // Should not be returned as a smart pointer.
                    public void visitIntegerLiteral(GNode n) { mBod.append(n.getString(0)); }
                    public void visitFloatingPointLiteral(GNode n) { mBod.append(n.getString(0)); }
                    public void visitCharacterLiteral(GNode n) { mBod.append(n.getString(0)); }
                    public void visitBooleanLiteral(GNode n) { mBod.append(n.getString(0)); }
                    public void visitNullLiteral(GNode n) { mBod.append("null"); }
                    public void visitPrimaryIdentifier(GNode n) { mBod.append(n.getString(0)); }
                    public void visitThisExpression(GNode n) { }
                    public void visitAdditiveExpression(GNode n) { mBod.append(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0)); }
                    public void visit(Node n) { for (Object o : n) if(o instanceof Node) dispatch((Node)o); }
                }.dispatch(n);
                mBod.append(";").append("\n");
            }
            public void visit(Node n) { for (Object o : n) if(o instanceof Node) dispatch((Node)o); }

            public void visitExpressionStatement(GNode n) {
                mBod.append("\t");  // Indent the expression statement once.
                if (n.getNode(0).getName().equals("Expression")) { // checks if regular expression is being made
                    new Visitor() {  // Visit assigned value if any
                        public void visitStringLiteral(GNode n) { mBod.append("new __String(" + n.getString(0) + ")"); }
                        public void visitIntegerLiteral(GNode n) { mBod.append(n.getString(0)); }
                        public void visitFloatingPointLiteral(GNode n) { mBod.append(n.getString(0)); }
                        public void visitCharacterLiteral(GNode n) { mBod.append(n.getString(0)); }
                        public void visitBooleanLiteral(GNode n) { mBod.append(n.getString(0)); }
                        public void visitNullLiteral(GNode n) { mBod.append("null"); }
                        public void visitPrimaryIdentifier(GNode n) { mBod.append(" = " + n.getString(0)); }
                        public void visitAdditiveExpression(GNode n) { mBod.append(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0)); }
                        public void visit(Node n){ for (Object o : n) if(o instanceof Node) dispatch((Node)o); }
                    }.dispatch(n);
                    mBod.append(";").append("\n");
                }
                boolean isEndLine = false; // used to check if the print statement has an ln
                if (n.getNode(0).toString().contains("println")) isEndLine = true;
                if (n.toString().contains("CallExpression") && n.toString().contains("SelectionExpression") && n.toString().contains("System") && n.toString().contains("out")) { //
                    mBod.append("std::cout");
                    final ArrayList<String> vars = new ArrayList<String>();
                    new Visitor() {

                        public void visitCallExpression(GNode n) { // If a method is called
                            Node arguments = n.getNode(3);
                            new Visitor() {
                                public void visitStringLiteral(GNode n) { mBod.append(" << " + n.getString(0) + ")"); }
                                public void visitIntegerLiteral(GNode n) { mBod.append(" << " + n.getString(0)); }
                                public void visitFloatingPointLiteral(GNode n) { mBod.append(" << " + n.getString(0)); }
                                public void visitCharacterLiteral(GNode n) { mBod.append(" << " + n.getString(0)); }
                                public void visitBooleanLiteral(GNode n) { mBod.append(" << " + n.getString(0)); }
                                public void visitNullLiteral(GNode n) { mBod.append(" << " + "null"); }
                                public void visitPrimaryIdentifier(GNode n) {
                                    mBod.append(" << " + n.getString(0));
                                }
                                public void visitCallExpression(GNode n) {
                                    String method = "";
                                    method += n.getNode(0) == null ? n.getString(2) : n.getNode(0).getString(0);
                                    if (n.getString(2).isEmpty()) {
                                        method += "()";
                                    } else if (n.getString(2).equals("toString")) {
                                        method = "toString(" + method + ")";
                                    } else {
                                        method += "->__vptr->" + n.getString(2) + "(";
                                        if (n.getNode(3).isEmpty()) method += ")";
                                    }
                                    vars.add(method);
                                    mBod.append(" << " + method);
                                }
                                public void visit(GNode n) { for (Object o : n) if (o instanceof Node) dispatch((Node) o); }
                            }.dispatch(arguments);
                        }

                        public void visit(GNode n) { for (Object o : n) if (o instanceof Node) dispatch((Node) o); }
                    }.dispatch(n.getNode(0));

                    if (isEndLine) mBod.append(" << \"\\n\"");
                    mBod.append(";").append("\n");
                }
                else if (n.toString().contains("CallExpression")) {
                    mBod.append("\t");
                    new Visitor() {
                        public void visitCallExpression(GNode n) {
                            String method = "";
                            method += n.getNode(0).getString(0);
                            if (n.getString(2).isEmpty()) {
                                method += "()";
                            }
                            else if (n.getString(2).equals("toString")) {
                                method = "toString(" + method + ")";
                            }
                            else {
                                method += "." + n.getString(2) + "(";
                                if (n.getNode(3).isEmpty()){
                                    method += ")";
                                }
                                else {
                                    Node arguments = n.getNode(3);
                                    for (int i = 0; i < arguments.size(); i++) {
                                        if (i == 0) method += arguments.getNode(0).getString(i);
                                        else method += ", " + arguments.getNode(0).getString(i);
                                    }
                                    method += ")";
                                }
                            }
                            mBod.append(method);
                        }
                        public void visit(GNode n) { for (Object o : n) if (o instanceof Node) dispatch((Node) o); }
                    }.dispatch(n);
                    mBod.append(";").append("\n");
                }
                else {
                    visit(n);
                }
            }
        }.dispatch(n);
        String bod = mBod.toString();
        mBod.setLength(0);
        return bod.length() == 0 ? "" : bod.substring(0, bod.length() - 1);  // Remove the final new line.
        }


	public void visitCompilationUnit(GNode n) {
    }

	/**
	* Get the name of the method in visitMethodDeclaration.
	*
	* @param n	The node from the Java AST.
	* @return	The name of the method.
	*/
	public static String getMethodName(GNode n) {
		return n.getString(3);
	}

	static String gCallExpression;  // Global variable used with getCallExpression.
	/**
	* Get the C++ translation of a Java print statement. Must be used in
	* visitExpressionStatment's visitCallExpression.
	*
	* @param n	The node from the Java AST.
	* @return	A String representing the C++ version of a Java print statement.
	*/
	public static String getCallExpression(GNode n) {
		gCallExpression = "";

		boolean isEndLine = false;
		if (n.getNode(1).toString().contains("println")) {
			isEndLine = true;
		}

		gCallExpression += "\tstd::cout";
		final ArrayList<String> vars = new ArrayList<String>();

		new Visitor() {
			public void visitStringLiteral(GNode n) {
				gCallExpression += " << " + n.getString(0);
			}

			public void visitIntegerLiteral(GNode n) {
				gCallExpression += " << " + n.getString(0);
			}

			public void visitFloatingPointLiteral(GNode n) {
				gCallExpression += " << " + n.getString(0);
			}

			public void visitCharacterLiteral(GNode n) {
				gCallExpression += " << " + n.getString(0);
			}

			public void visitBooleanLiteral(GNode n) {
				gCallExpression += " << " + n.getString(0);
			}

			public void visitNullLiteral(GNode n) {
				gCallExpression += " << " + "null";
			}

			public void visitPrimaryIdentifier(GNode n) {
				gCallExpression += " << " + n.getString(0);
			}

			public void visitCallExpression(GNode n) {
				String method = "";
				method +=n.getNode(0).getString(0);
				if (n.getString(2).isEmpty()) {
					method += "()";
				} else {
					method += "." + n.getString(2) + "(";
					if (n.getNode(3).isEmpty()) {
						method += ")";
					}
				}
				vars.add(method);
				gCallExpression += " << " + method;
			}

			public void visit(GNode n) {
				for (Object o : n) {
					if (o instanceof Node) {
						dispatch ((Node) o);
					}
				}
			}
		}.dispatch(n.getNode(0));

		if (isEndLine) {
			gCallExpression += " << \"\\n\"";
		}
		return gCallExpression += ";";
	}

	/**
	* Determines whether the given GNode represents a print statement. Must be used
	* in visitExpressionStatement.
	*
	* @param n	The node from the Java AST.
	* @return	true or false, depending on whether or not the GNode represents
	*		a print statement.
	*/
	public static boolean isPrintStatement(GNode n) {
		return (n.toString().contains("CallExpression") &&
				n.toString().contains("SelectionExpression") &&
				n.toString().contains("System") &&
				n.toString().contains("out"));

	}
	/**
	 * extract class name from node
	 * @param node node from java parse tree
	 * @return formated String represented class name
	 */
	public static  String getClassName(GNode n){
		validCall();
		return n.getString(1);
	}

	/**
	 * extract class declaration from node
	 * @param node node from java parse tree
	 * @return formated string  c++ struct declaration
	 */
	public static  String getClassDeclaration(GNode n){
		validCall();
		mbuffer=new ArrayList<String>();
		rbuffer=new ArrayList<String>();
		pbuffer=new ArrayList<ArrayList<String>>();
		setCurrClass(n.getString(1));
		return  "struct __"+ n.getString(1);
	}
	/**
	 * @param node subclass node
	 * @return String name of superclass
	 */
	public static String getSuperClass(GNode n){
		try{
			return n.getNode(3).getNode(0).getNode(0).getString(0);
		}catch(Exception e){}

		return null;
	}

	/**
	 * extract method declaration from node
	 * @param node node from java parse tree
	 * @return formated function prototype c++
	 */
	public static String getMethodDeclaration(GNode n,String className){
		validCall();

        if(getMethodName(n).equals("main")) {
            if (className.contains("SegImp")) {
                return "static void main(__rt::Ptr<__rt::Array<String> > args);";
            }
            else return null;  // The header doesn't include a main method.
        }
		String return_type="";

		if(n.getNode(2) == null) {  // THIS IS THE CONSTRUCTOR, WHICH HAS NO RETURN TYPE.
            return_type = "";
        } else if (j2c(n.getNode(2).toString()).equals("void")) {
            return_type="void";
        } else {
			new Visitor(){
				public String rtype="";

				/**
				 * extract return type
				 */
				public void visitQualifiedIdentifier(GNode n){
					SegHelper.setBuffer(SegHelper.j2c(n.getString(0)));
				}
				public void visit(GNode n) {
					for (Object o : n) if (o instanceof Node) dispatch((Node) o);

				}
			}.dispatch(n.getNode(2));
			return_type=buffer;
		}

		String fp=getMethodName(n)+"(";
		if(n.getNode(4).size() == 0) fp+=");";
		else fp+=getFormalParameters((GNode)n.getNode(4));

		final StackTraceElement[] s=Thread.currentThread().getStackTrace();

		if(s[2].getClassName().contains("SegHead")){
			mbuffer.add(getMethodName(n));
			rbuffer.add(return_type);

            return "static " + return_type+" "+fp;
        } else if((s[2].getClassName().contains("SegImp"))) {
			return return_type + " " + currClass + "::" + fp;
        }

		return null;
	}
	/**
	 * create the virtual table for the corresponding class
	 * and save function pointer to tree
	 * which includes function pointers and class information
	 * @param n method declaration node
	 */
	public static void generateVtableForClass(String className) {
        hppWriter.pln("struct __" + className + "_VT {");

        // The method declarations of all methods usable by className's class.
        ArrayList<String> methodDeclarations = SegHelper.classToAllAvailableMethodDeclarations.get(className);

        // Write the function pointers.
        hppWriter.pln("\t// The function pointers of class " + className + "'s virtual table..");
        for (String declaration : methodDeclarations) {
            String pointer = SegHelper.getMethodPointerFromDeclaration(declaration);
            hppWriter.pln("\t" + pointer + ";");
        }

        hppWriter.pln("\n\t// The virtual table constructor for class " + className + ".");
        hppWriter.pln("\t__" + className + "_VT()");
        String[] methodInitializers = SegHelper.getObjectVtableMethodInitializers();
        hppWriter.pln("\t: " + methodInitializers[0] + ",");  // CHANGE THIS TO ISA

        /* Iterate through method declarations, and convert the declaration to the pointer value. */
        for (int d = 0; d < methodDeclarations.size(); d++) {
            String suffixCharacter;
            if (d == methodDeclarations.size() - 1) {
                suffixCharacter = " {";
            } else {
                suffixCharacter = ",";
            }
            String pointerValue = SegHelper.getPointerValueFromMethodDeclaration(methodDeclarations.get(d), className);
            hppWriter.pln("\t  " + pointerValue + suffixCharacter);
        }

        hppWriter.pln("\t}");
        hppWriter.pln("};\n");
	}

	/**
	 * buffer to communication between anonymous inner classes and SegHelper
	 * @param s String to be sent from anonymous class to SegHelper
	 */
	public static void setBuffer(String s){
		buffer=s;
	}


	/**
	 * extract function parameters
	 * @param n node from java parse tree
	 * @return formated list of parameter types nad parameter names
	 */
	private static  String getFormalParameters(GNode n){
		String fp="";
		ArrayList<String> param=new ArrayList<String>();
		for(int i=0; i< n.size(); i++){
			Node fparam=n.getNode(i);

			//retrieve argument type
			String arg=j2c(fparam.getNode(1).getNode(0).getString(0));
			param.add(arg);
			fp+=arg+" ";

			//retrieve argument name
			fp+=fparam.getString(3);

			if(i+1 < n.size()) fp+=",";
			else fp+=");";
		}
		pbuffer.add(param); //add Array to List of Arrays to construct vtable
		return fp;
	}

    /**
     * Returns a list of the class's global variables and global variable assignments. Must be called with the GNode
     * of visitClassDeclaration.
     *
     *@param n  The node from the Java AST.
     *@return   An ArrayList of the class's global variables.
     */
    public static void getGlobalVariables(GNode n) {
        new Visitor() {
            public void visitFieldDeclaration(GNode n) {
                final StringBuilder gVar = new StringBuilder();
                /* Determine the variable modifiers (e.g. "static", "private"). */
                boolean isPrivateField = false;
                for (int x = 0; (x < n.getNode(0).size()) && (n.getNode(0).getNode(x).getString(0) != null); x++) {
                    String modifier = n.getNode(0).getNode(x).getString(0);
                    if (modifier.equals("static")) gVar.append("static ");
                    else if (modifier.equals("private")) isPrivateField = true;
                }

                /* Determine the declarator type. */
                String declarationType = n.getNode(1).getNode(0).getString(0);
                if (declarationType.equals("boolean")) { gVar.append("boolean "); }
                else if (declarationType.equals("char")) { gVar.append("char "); }
                else if (declarationType.equals("double")) { gVar.append("double "); }
                else if (declarationType.equals("float")) { gVar.append("float "); }
                else if (declarationType.equals("int")) { gVar.append("int "); }
                else if (declarationType.equals("String")) { gVar.append("String "); }
                else if (declarationType.equals("Object")) {gVar.append("Object "); }
                else { gVar.append(declarationType + " "); }  // For non-primitive, non-String objects.

                /* Get the name of the field. */
                String fieldName = n.getNode(2).getNode(0).getString(0);
                gVar.append(fieldName);

                /* Potentially visit the assigned value (if any). */
                new Visitor() {
                    public void visitDeclarators(GNode n) { constructorProp = n.getNode(0).getString(0); }
                    public void visitStringLiteral(GNode n) { gVar.append(" = new String(" + n.getString(0) + ")");}
                    public void visitIntegerLiteral(GNode n) { gVar.append(" = " + n.getString(0)); }
                    public void visitFloatingPointLiteral(GNode n) { gVar.append(" = " + n.getString(0)); }
                    public void visitCharacterLiteral(GNode n) { gVar.append(" = " + n.getString(0)); }
                    public void visitBooleanLiteral(GNode n) { gVar.append(" = " + n.getString(0)); }
                    public void visit(GNode n) { for (Object o : n) if(o instanceof Node) dispatch((Node)o); }
                }.dispatch(n);
                currentClassGlobalVariables.add(gVar.toString());
            }
            /* To prevent printing local fields, do not visit methodDeclaration nodes. */
            public void visitMethodDeclaration(GNode n) { }
            public void visit(GNode n) { for (Object o : n) if(o instanceof Node) dispatch((Node)o); }
        }.dispatch(n);
    }

	/**
	 *  convert raw type provided by xtc to c++ type
	 *  @param javaType  raw java type from xtc node
	 *  @return formated c++ type
	 */
	private static String j2c(String jType){
		String cType = "";
		if (jType.equals("String")) cType="__String";
        else if (jType.equals("Object")) cType = "__Object";
		else if(jType.equals("VoidType()")) cType="void";
		else if (jType.equals("Integer")) cType="int";
		return cType;
	}

	/**
	 *  check if visit object methods calls appropriate Helper function
	 */
	private static void validCall(){
		final int x=3; final int y=2;
		final StackTraceElement[] s=Thread.currentThread().getStackTrace();

		String sh_yfunc=s[y].getMethodName();
		String sh_xfunc=s[x].getMethodName();

		final String[] comp=new String[]{"Class","Method"};

		for (int k = 0; k < comp.length; k++) {
			if(sh_xfunc.contains(comp[k]) &&  sh_yfunc.contains(comp[k])) return;
		}
		throw new RuntimeException("visit function  to not correspond to helper function");
	}
	/**
	 * print string to new line hpp file
	 */
	public static void hpp_pln(String s){
		hppWriter.pln(s);
	}
	/**
	 * print string to new line in cpp file
	 */
	public static void cpp_pln(String s){
		cppWriter.pln(s);
	}
	/**
	 * print string to hpp file on same line
	 */
	public static void hpp_p(String s){
		hppWriter.p(s);
	}
	/**
	 * print string to cpp file on same line
	 */
	public static void cpp_p(String s){
		cppWriter.p(s);
	}

    /**
     * print string to hpp file on same line
     */
    public static void hpp_flush(){
        hppWriter.flush();
    }
    /**
     * print string to cpp file on same line
     */
    public static void cpp_flush(){
        cppWriter.flush();
    }

    /**
     * Prints the inheritance tree.
     */
    public static void printInheritanceTree() {
        System.out.println("\nInheritance Tree:");
        printInheritanceTree(Root, 0);
    }

    /**
     * Prints the inheritance tree by recurring through the tree.
     *
     * @param treeNode  A node from the inheritance tree.
     * @param numTabs   The number of tabs to print before printing the class's name.
     */
    private static void printInheritanceTree(SegNode<CppClass> treeNode, int numTabs) {
        for (int t = 0; t < numTabs; t++) System.out.print("\t\t");
        if (treeNode.data == null) System.out.println("nullData");
        else if (treeNode.data.className == null) System.out.println("nullClassName");
        else System.out.println(treeNode.data.className);

        for (SegNode<CppClass> child : treeNode.children) printInheritanceTree(child, numTabs + 1);
    }

//    /**
//     * Returns a function pointer String corresponding to the given method declaration.
//     *
//     * @param methodDeclarationNode	A generic node representing a method declaration.
//     * @return A string representing the method declaration’s corresponding function pointer.
//     */
//    public static String getPointerFromMethodDeclaration(GNode methodDeclarationNode) {
//        return convertDeclarationToPointer(methodDeclarationNode, currClass);
//    }
//
//
//    /**
//     * Returns a function pointer String corresponding to the given method
//     * declaration. Assumes that the given className is correct.
//     *
//     * @param methodDeclarationNode	A generic node representing a method declaration.
//     * @param className				The name of the class to which this methodDeclaration belongs.
//     *
//     * @return A string representing the method declaration’s corresponding
//     * function pointer.
//     */
//    public static String getPointerFromMethodDeclaration(GNode methodDeclarationNode, String className) {
//        return convertDeclarationToPointer(methodDeclarationNode, className);
//    }
//
//    public static String convertDeclarationToPointer(GNode methodDeclarationNode, String className) {
//        GNode n = methodDeclarationNode;
//        if (getMethodName(n).equals("main")) {
//            return "Function pointer not necessary for main method.";
//        }
//
//        String methodName = getMethodName(n);
//        String formalParameters = getFormalParameters((GNode)n.getNode(4));
//        final StringBuilder returnType = new StringBuilder();
//
//        /* Determine the return type. */
//        if (j2c(n.getNode(2).toString()).equals("void")) {
//            returnType.append("void");
//        } else {
//            new Visitor() {
//                public void visitQualifiedIdentifier(GNode n) {
//                    returnType.append(SegHelper.j2c(n.getString(0)));
//                }
//
//                public void visit(GNode n) {
//                    for (Object o : n) if (o instanceof Node) dispatch((Node) o);
//                }
//            }.dispatch(n.getNode(2));
//        }
//
//        boolean inHeader = false;
//        boolean inImplementation = false;
//        final StackTraceElement[] stackTraceElement = Thread.currentThread().getStackTrace();
//        if(stackTraceElement[3].getClassName().contains("SegHead")) {
//            inHeader = true;
//        } else if (stackTraceElement[3].getClassName().contains("SegImp")) {
//            inImplementation = true;
//        }
//
//        String functionPointer = methodName + (inHeader ? ")(" : "(");
//        if(n.getNode(4).size() == 0) {
//            functionPointer += className + ");";
//        } else {
//            functionPointer += className + ", " + formalParameters;
//        }
//
//        if(inHeader) {
//            return returnType.toString() + " (*" + functionPointer;
//        } else if(inImplementation) {
//            return returnType.toString() + " " + className + "::" + functionPointer;
//        } else {
//            return "****Error in getPointerFromMethodDeclaration*****";
//        }
//    }


    public static String getMainMethodArgumentsAsSmartPointers () {
        String mainMethodArgumentsAsSmartPointers =
                "\t__rt::Ptr<__rt::Array<String> > args = new __rt::Array<String>(argc - 1);\n" +   // Declare smart pointer of type Array(Strings) with size args-1.
                "\tfor (int32_t i = 1; i < argc; i++) {\n" +                                        // Loop through all arguments of the array parameter (except 1st).
                "\t\t(*args)[i - 1] = __rt::literal(argv[i]);\n" +                                  // Dereferenced array contents set to args' contents.
                "\t} ";
        return mainMethodArgumentsAsSmartPointers;
    }





    public static String[] getObjectMethodDeclarations() {
        String[] objectMethodDeclarations =
                new String[] {
                    "static int32_t hashCode(Object)",
                    "static bool equals(Object, Object)",
                    "static Class getClass(Object)",
                    "static String toString(Object)",
                    "static Object init(Object __this) { return __this; }"
                 };
        return objectMethodDeclarations;
    }

    public static String[] getObjectVtableMethodPointers() {
        String[] objectVtableMethodPointers =
                new String[] {
                        "Class __isa",
                        "void (*__delete)(__Object*)",
                        "int32_t (*hashCode)(Object)",
                        "bool (*equals)(Object, Object)",
                        "Class (*getClass)(Object)",
                        "String (*toString)(Object)"
                };
        return objectVtableMethodPointers;
    }

    public static String[] getObjectVtableMethodInitializers() {
        String[] objectVtableMethodInitializers =
                new String[] {
                        "__isa(__Object::__class())",
                        "__delete(&__rt::__delete<__Object>)",
                        "hashCode(&__Object::hashCode)",
                        "equals(&__Object::equals)",
                        "getClass(&__Object::getClass)",
                        "toString(&__Object::toString)"
        };
        return objectVtableMethodInitializers;
    }


    public static String[] getStringMethodDeclarations() {
        String[] stringMethodDeclarations =
                new String[] {
                    "static int32_t hashCode(String)",
                    "static bool equals(String, Object)",
                    "static String toString(String)",
                    "static int32_t length(String)",
                    "static char charAt(String, int32_t)",
                    "static String init(String __this) { return __this; }"
                };
        return stringMethodDeclarations;
    }

    public static String[] getStringVtableMethodPointers() {
        String[] stringVtableMethodPointers =
                new String[] {
                        "Class __isa",
                        "void (*__delete)(__String*)",
                        "int32_t (*hashCode)(String)",
                        "bool (*equals)(String, Object)",
                        "Class (*getClass)(String)",
                        "String (*toString)(String)",
                        "int32_t (*length)(String)",
                        "char (*charAt)(String, int32_t)"
                };
        return stringVtableMethodPointers;
    }

    public static String[] getStringVtableMethodInitializers() {
        String[] stringVtableMethodInitializers =
                new String[] {
                        "__isa(__String::__class())",
                        "__delete(&__rt::__delete<__String>)",
                        "hashCode(&__String::hashCode)",
                        "equals(&__String::equals)",
                        "getClass((Class(*)(String))&__Object::getClass)",
                        "toString(&__String::toString)",
                        "length(&__String::length)",
                        "charAt(&__String::charAt)"
                };
        return stringVtableMethodInitializers;
    }

    /**
     * Given a method declaration and a "this" parameter (i.e. the first parameter in each method's declaration), return
     * a new method declaration, with the original "this" parameter name substituted for the desired one.
     *
     * @param declaration   The original method declaration.
     * @param _this         The desired new "this" parameter.
     * @return              The new, altered method declaration.
     */
    public static String getDeclarationWithNewThisParameter(String declaration, String _this) {
        String requestedDeclaration = "";
        String[] splitDeclaration = declaration.split("\\(");  // Length 2 array, where [1] contains the parameters.
        String parameters = splitDeclaration[1];
        if (parameters.indexOf(',') == -1) {  // There is only one parameter (i.e., the 'this' parameter).
            requestedDeclaration += splitDeclaration[0] + "(" + _this + ")";
        } else {  // There is more than one parameter.
            String[] splitParameters = parameters.split(", ");  // Length ? array, where [0] contains original _this parameter.
            requestedDeclaration += splitDeclaration[0] + "(" + _this + ", ";
            for (int p = 1; p < splitParameters.length; p++) {
                requestedDeclaration += splitParameters[p] + ", ";
            }
            requestedDeclaration = requestedDeclaration.substring(0, requestedDeclaration.length() - 2);  // Remove last ", ";
        }
        return requestedDeclaration;
    }

    /**
     * Given a class name, this method returns a list of all of this class's superclasses, starting from the closest
     * superclass to the farthest superclass (Object).
     *
     * @param className The name of the class for which the user desires to get all super classes.
     * @return  An array list of all super classes.
     */
    public static ArrayList<String> getListOfSuperclasses(String className) {
        ArrayList<String> allSuperclasses = new ArrayList<String>();
        while (SegHelper.classToSuperclass.get(className) != null) {
            String currentSuperclass = SegHelper.classToSuperclass.get(className);
            allSuperclasses.add(currentSuperclass);
            className = currentSuperclass;
        }
        return allSuperclasses;
    }


    /**
     * Given a method declaration, returns the corresponding method pointer.
     *
     * @param declaration   The given method declaration.
     * @return              The corresponding method pointer.
     */
    public static String getMethodPointerFromDeclaration(String declaration) {
        // Remove "static ".
        declaration = declaration.substring(7);

        String[] splitDeclaration = declaration.split(" ");
        String returnType = splitDeclaration[0];
        String methodName = splitDeclaration[1].split("\\(")[0];
        String parameterList = "(" + declaration.split("\\(")[1];
        return returnType + " (*" + methodName + ")" + parameterList;
    }

    /**
     * Given a method declaration and a class name, returns the corresponding method pointer.
     *
     * @param declaration   The given method declaration.
     * @param className     The class to which the declaration belongs.
     *
     * @return              The corresponding method pointer.
     */
    public static String getPointerValueFromMethodDeclaration(String declaration, String className) {
        // Remove "static ".
        declaration = declaration.substring(7);

        String[] splitDeclaration = declaration.split(" ");
        String returnType = splitDeclaration[0];
        String methodName = splitDeclaration[1].split("\\(")[0];
        String parameterList = "(" + declaration.split("\\(")[1];

        String address = "&__";  // The prefix of each address in the virtual table's initializer list.
        /* Determine whether the given method is originally declared in the given class. */
        boolean originallyDeclaredInAnotherClass = true;
        ArrayList<String> declarationsOfTheGivenClass = SegHelper.classNameToMethodDeclarations.get(className);
        for (String d : declarationsOfTheGivenClass) {
            if (d.contains(methodName + "(")) {
                address += className + "::" + methodName;
                originallyDeclaredInAnotherClass = false;
                break;
            }
        }

        /* Iterate through superclasses to determine which class originally declared the declaration. */
        if (originallyDeclaredInAnotherClass) {
            boolean foundOriginalClassWhereMethodIsDeclared = false;
            while (!foundOriginalClassWhereMethodIsDeclared) {
                String superclass = SegHelper.classToSuperclass.get(className);
                ArrayList<String> superclassDeclarations = SegHelper.classNameToMethodDeclarations.get(superclass);
                for (String d : superclassDeclarations) {
                    if (d.contains(methodName + "(")) {
                        foundOriginalClassWhereMethodIsDeclared = true;
                        address += superclass + "::" + methodName;
                        break;
                    }
                }
                className = superclass;
            }
        }

        return methodName + "((" + returnType + "(*)" + parameterList +  address + ")";
    }
}
/**
 For method overloading:
    For every class
        For each method declaration of each class
            If the name of a method appears more than once
                Change the name of the method to "methodName" + "1" (or 2 or 3 etc) in the method declaration
                Change the name of the method to "methodName" + "1" (or 2 or 3 etc) in the vtable method pointer
                Change the name of the method to "methodName" + "1" (or 2 or 3 etc) in the vtable method initializer

    When a method is called (i.e., visitCallExpression(GNode n))
        if there exists only one method with its name
            call that method
        else
            get the static types of the method parameters via SegHelper's Symbol Table
            get the number of parameters via SegHelper's Symbol Table

 CHECK IF IS SUBCLASS OF AND NAME MANGLED
 */

/**
 For inheritance:
    Data Structures
        Class name -> vector of declarations            // DONE
        Class name -> vector of function pointers
        Class name -> vector of function initializers

    To get the parent class of smart pointer a:
        use classToSuperClass
        need a new method that overwrites first parameter of declaration with a given class
        same for method pointer
        same for initializer
        method for casting parameters

    STILL NEED TO FIX CONSTRUCTOR
    FIX INIT, ISA, DELETE METHODS
 */