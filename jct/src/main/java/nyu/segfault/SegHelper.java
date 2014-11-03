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

	/**
	 * set the file_name data field and create files
	 * @param file_name
	 */
	public static void setFileName(String fn){
		file_name=fn.replace(".java", "");
		try{
			impFile=new File(getFileName()+".cpp");
			headFile=new File(getFileName()+".hpp");

			cWriter=new PrintWriter(impFile);
			hWriter=new PrintWriter(headFile);

			cppWriter=new Printer(cWriter);
			hppWriter=new Printer(hWriter);

		} catch(Exception e) {
			System.out.println(e);
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
		final String[] stlMacros=new String[]{"<sstream>", "<iostream>", "<string>"};


		/**@var cpp macro definitions  */
		final String [] cppMacros=new String[]{"#include <"+getFileName()+".hpp", "using namespace std;"};

		final StackTraceElement [] stack=Thread.currentThread().getStackTrace();

		if(stack[2].getClassName().contains("SegHead")){
			for (String stlMacro : stlMacros )
				hppWriter.pln("#include "+stlMacro);
			hppWriter.pln("using namespace std;");
		}

		else if(stack[2].getClassName().contains("SegImp")){
			for(String cppMacro : cppMacros)
				cppWriter.pln(cppMacro);
		}

		cppWriter.flush(); hppWriter.flush();
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

		gCallExpression += "\tcout";
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
		return  "struct "+ n.getString(1);
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
		if(getMethodName(n).equals("main")) return null;
		
		String return_type="";

		if(j2c(n.getNode(2).toString()).equals("void")) return_type="void";

		else{
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
			return "static "+return_type+" "+fp;
		}

		else if((s[2].getClassName().contains("SegImp")))
			return return_type+" "+className+"::"+fp;
		
		return null;
	}
	/**
	 * create the vtable for the coressponding class 
	 * and save function pointer to tree
	 * which includes function pointers and class information
	 * @param n method declaration node 
	 */
	public static void genVTable(){
		if(mbuffer.size() == 0) return;
		// <return_type> (*<method name>)(<parameter type>);
		hppWriter.pln("struct "+getCurrClass()+"_VT");
		hppWriter.pln("{");

		/**
		 * produce struct <class_name>_VT data fields (function pointers)
		 */
		for (int k=0; k<mbuffer.size(); k++){
			String fptr="\t"+rbuffer.get(k)+"(*"+mbuffer.get(k)+")(";
			int Q=0;
			if((pbuffer.size() == 0) || (k >= pbuffer.size())  ) fptr+=");";
			else{
				for(String param : pbuffer.get(k)){
					if ( Q != pbuffer.get(k).size()-1)
						fptr+=param+",";
					else
						fptr+=param+");";
					Q++;
				}
			}
			hppWriter.pln(fptr);
			
			/**
			 * add function pointer string to node in inheritance tree
			 */
			//SegNode<CppClass> node=SegHelper.Root.dfs(Root,new CppClass(getCurrClass()));
			//node.data.functionPtrs.add(fptr);
		}
		/**
		 * produce struct <class_name>_VT constructor and initialized  
		 * initialized function form <function_name>(&<class_name>::<function_name>)
		 */
		hppWriter.pln("\t"+getCurrClass()+"_VT"+"():");
		for(int n=0; n<mbuffer.size();n++){
			String fref="\t\t"+mbuffer.get(n)+"(&"+getCurrClass()+"::"+mbuffer.get(n)+")";
			if(n == mbuffer.size() -1) fref+="{}";
			else fref+=",";
			hppWriter.pln(fref);
		}
		hppWriter.pln("};"); 
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
                else if (declarationType.equals("String")) { gVar.append("string "); }
                else { gVar.append(declarationType + " "); }  // For non-primitive, non-String objects.

                /* Get the name of the field. */
                String fieldName = n.getNode(2).getNode(0).getString(0);
                gVar.append(fieldName);

                /* Potentially visit the assigned value (if any). */
                new Visitor() {
                    public void visitStringLiteral(GNode n) { gVar.append(" = " + n.getString(0));}
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
		String cType="";
		if (jType.equals("String")) cType="string";
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

		for (int k=0;k<comp.length;k++) {
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
}
