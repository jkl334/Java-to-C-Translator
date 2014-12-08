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
                System.out.println("in cppMacro loop:" + cppMacro);
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
                       /* public void visitExpression(GNode n) {
                            if (n.getNode(2).getName().equals("AdditiveExpression")) {
                                mBod.append(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getNode(0).getString(0) + " " + n.getNode(2).getString(1) + " " + n.getNode(2).getNode(2).getString(0));
                            } else {
                                mBod.append(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
                            }
                        } */
                        public void visitStringLiteral(GNode n) { mBod.append("new __String(" + n.getString(0) + ")"); }
                        public void visitIntegerLiteral(GNode n) { mBod.append(n.getString(0)); }
                        public void visitFloatingPointLiteral(GNode n) { mBod.append(n.getString(0)); }
                        public void visitCharacterLiteral(GNode n) { mBod.append(n.getString(0)); }
                        public void visitBooleanLiteral(GNode n) { mBod.append(n.getString(0)); }
                        public void visitNullLiteral(GNode n) { mBod.append("null"); }
                        public void visitPrimaryIdentifier(GNode n) { mBod.append(n.getString(0)); }
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
                                    method += n.getNode(0).getString(0);
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
                                System.out.println(n.getString(2));
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
        //System.out.println(bod);
        mBod.setLength(0);
        return bod.substring(0, bod.length() - 1);  // Remove the final new line.
            // Node body = n.getNode(7);
            // if (null != body) visit(body);
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
		System.out.println(n.toString());
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
	public static void genVTable() {

		hppWriter.pln("struct __" + getCurrClass() + "_VT {");

        String[] methodPointers = SegHelper.getObjectVtableMethodPointers();
        for (String pointer : methodPointers) {
            hppWriter.pln("\t" + pointer + ";");
        }
        hppWriter.pln("");

        hppWriter.pln("\t// The virtual table constructor for class " + getCurrClass() + ".");
        hppWriter.pln("\t__" + getCurrClass() + "_VT()");
        String[] methodInitializers = SegHelper.getObjectVtableMethodInitializers();
        hppWriter.pln("\t: " + methodInitializers[0]);
        for (int initializer = 1; initializer < methodInitializers.length; initializer++) {
            String suffixCharacter;
            if (initializer == methodInitializers.length - 1) {
                suffixCharacter = " {";
            } else {
                suffixCharacter = ",";
            }
            hppWriter.pln("\t  " + methodInitializers[initializer] + suffixCharacter);
        }
        hppWriter.pln("\t}");

        hppWriter.pln("};\n");

//		if(mbuffer.size() == 0) return;
//        <return_type> (*<method name>)(<parameter type>);
//        /**
//         * produce struct <class_name>_VT data fields (function pointers)
//         */
//		for (int k=0; k<mbuffer.size(); k++){
//			String fptr="\t"+rbuffer.get(k)+"(*"+mbuffer.get(k)+")(";
//			int Q=0;
//			if((pbuffer.size() == 0) || (k >= pbuffer.size())  ) fptr+=");";
//			else{
//				for(String param : pbuffer.get(k)){
//					if ( Q != pbuffer.get(k).size()-1)
//						fptr+=param+",";
//					else
//						fptr+=param+");";
//					Q++;
//				}
//			}
//			hppWriter.pln(fptr);
//
//			/**
//			 * add function pointer string to node in inheritance tree
//			 */
//			//SegNode<CppClass> node=SegHelper.Root.dfs(Root,new CppClass(getCurrClass()));
//			//node.data.functionPtrs.add(fptr);
//		}
//		/**
//		 * produce struct <class_name>_VT constructor and initialized
//		 * initialized function form <function_name>(&<class_name>::<function_name>)
//		 */
//		hppWriter.pln("\t"+getCurrClass()+"_VT"+"():");
//		for(int n=0; n<mbuffer.size();n++){
//			String fref="\t\t"+mbuffer.get(n)+"(&"+getCurrClass()+"::"+mbuffer.get(n)+")";
//			if(n == mbuffer.size() -1) fref+="{};";
//			else fref+=",";
//			hppWriter.pln(fref);
//		}
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
			System.out.println(arg);

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

		//System.out.println(sh_yfunc+" : "+sh_xfunc);

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

    /**
     * Returns a function pointer String corresponding to the given method declaration.
     *
     * @param methodDeclarationNode	A generic node representing a method declaration.
     * @return A string representing the method declaration’s corresponding function pointer.
     */
    public static String getPointerFromMethodDeclaration(GNode methodDeclarationNode) {
        return convertDeclarationToPointer(methodDeclarationNode, currClass);
    }


    /**
     * Returns a function pointer String corresponding to the given method
     * declaration. Assumes that the given className is correct.
     *
     * @param methodDeclarationNode	A generic node representing a method declaration.
     * @param className				The name of the class to which this methodDeclaration belongs.
     *
     * @return A string representing the method declaration’s corresponding
     * function pointer.
     */
    public static String getPointerFromMethodDeclaration(GNode methodDeclarationNode, String className) {
        return convertDeclarationToPointer(methodDeclarationNode, className);
    }

    public static String convertDeclarationToPointer(GNode methodDeclarationNode, String className) {
        GNode n = methodDeclarationNode;
        if (getMethodName(n).equals("main")) {
            return "Function pointer not necessary for main method.";
        }

        String methodName = getMethodName(n);
        String formalParameters = getFormalParameters((GNode)n.getNode(4));
        final StringBuilder returnType = new StringBuilder();

        /* Determine the return type. */
        if (j2c(n.getNode(2).toString()).equals("void")) {
            returnType.append("void");
        } else {
            new Visitor() {
                public void visitQualifiedIdentifier(GNode n) {
                    returnType.append(SegHelper.j2c(n.getString(0)));
                }

                public void visit(GNode n) {
                    for (Object o : n) if (o instanceof Node) dispatch((Node) o);
                }
            }.dispatch(n.getNode(2));
        }

        boolean inHeader = false;
        boolean inImplementation = false;
        final StackTraceElement[] stackTraceElement = Thread.currentThread().getStackTrace();
        if(stackTraceElement[3].getClassName().contains("SegHead")) {
            inHeader = true;
        } else if (stackTraceElement[3].getClassName().contains("SegImp")) {
            inImplementation = true;
        }

        String functionPointer = methodName + (inHeader ? ")(" : "(");
        if(n.getNode(4).size() == 0) {
            functionPointer += className + ");";
        } else {
            functionPointer += className + ", " + formalParameters;
        }

        if(inHeader) {
            return returnType.toString() + " (*" + functionPointer;
        } else if(inImplementation) {
            return returnType.toString() + " " + className + "::" + functionPointer;
        } else {
            return "****Error in getPointerFromMethodDeclaration*****";
        }
    }


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
                        "__isa(__Object::__class()),",
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
}
