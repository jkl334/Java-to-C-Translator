package nyu.segfault;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import javax.swing.tree.DefaultTreeModel;

import nyu.segfault.SFClassDeclaration;
import nyu.segfault.SFMethodDeclaration;
import nyu.segfault.SFVariable;

import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.util.SymbolTable;
import xtc.util.Tool;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;

import java.util.ArrayList;


public class SFVisitor extends Visitor {
	//structure for inheritance tree
	public class SegNode<T>{
		public T data;
		public SegNode<T> parent;
		public ArrayList<SegNode<T>> children;

		public SegNode(T data){
			this.data=data;
			children=new ArrayList<SegNode<T>>();
		}
		public void addChild(T data){
			SegNode<T> child=new SegNode<T>(data);
			child.parent=this;
			this.children.add(child);
		}
		public SegNode<T> dfs(SegNode<T> n, T data){
			SegNode<T> found=n;
			if(n.data.equals(data))  return n;
			if(n.children.size() > 0)
				for (SegNode<T> sn : n.children){
					found=dfs(sn,data);
					if(found != null) break;
				}
			return found;
		}
	}
	private String[] files; // args passed from the translator
	private String fileName; // name of the file to be translated

	private int count;

	public PrintWriter cWriter; // prints to the method body
	public PrintWriter hWriter; // prints to the header
	public Printer impWriter;
	public Printer headWriter;

	public final SegNode<String> inhTree=new SegNode<String>((String)"Object");
	public String _super;
	
//	//temporarily stores name of encountered class members to construct vtable
//	public String class_VT_buffer; 
//	public ArrayList<String> method_VT_buffer; // modified function definitions as function pointers
//	public ArrayList<String> method_only_VT; // function name only

	ArrayList<GNode> cxx_class_roots=new ArrayList<GNode>(); /**@var root nodes of classes in linear container*/
	int index=-1; /**@var root node of class subtree index*/

	String cc_name; /**@var current class name (this) */
	String className;

	HashSet<String> publicHPP;  // Fields that fall under the "public:" tag of the header file.
	HashSet<String> privateHPP;  // Fields that fall under the "private:" tag of the header file.

	HashSet<String> publicHPPmethods;  // Methods that fall under the "public:" tag of the header file.
	HashSet<String> privateHPPmethods;  // Methods that fall under the "private:" tag of the header file.

	SymbolTable table; /**@var node symbols*/

	String method_return_type = "";

	public SFVisitor(String[] files) {
		this.files = files;
	}
//	public void initialize_vtable(){
//		//open vtable struct
//		headWriter.pln("struct " + class_VT_buffer+" {");
//		
//		//write function pointers
//		for (String func_VT : method_VT_buffer ){
//			System.out.println(func_VT);
//			if(func_VT.contains("main")) continue;
//			headWriter.pln("\t"+func_VT+";");	 
//		}
//		
//		//write constructor and function pointer initialization (grab address of functions)
//		headWriter.pln("\t" + class_VT_buffer+"()");
//		int i=0;
//		//boolean colonRequired = true;
//		for (String func_name : method_only_VT ){ 
//		//	if (colonRequired) {
//		//		headWriter.p(":");
//		//		colonRequired = false;
//		//	}
//			if(func_name.equals("main")){ i++; continue; }
//			headWriter.pln("\t" + func_name+"(&"+className+"::"+func_name+")");
//			if((i+1) < method_only_VT.size())  headWriter.pln(","); i++;
//		 }
//		
//		//close struct
//		headWriter.pln("};\n");
//	}
	public void visitCompilationUnit(GNode n) {
		//creates the new output files to be written to
		fileName = files[0];
		fileName = fileName.replace(".java", "");
		File impFile = null;
		File headFile = null;
		try {
	        String hppName = fileName + ".hpp";
	        String cppName = fileName + ".cpp";
	        impFile = new File(cppName);
	        impFile.createNewFile();
	        headFile = new File(hppName);
	        headFile.createNewFile();
	        cWriter = new PrintWriter(impFile);
	        hWriter = new PrintWriter(headFile);
	        impWriter = new Printer(cWriter);
	        headWriter = new Printer(hWriter);
	        impWriter.pln("/**\n * Team: SegFault\n */");
	        impWriter.pln();

	        headWriter.pln("/**\n * Team: SegFault\n */");
	        headWriter.pln();

		    impWriter.pln("#include <sstream> ");
		    impWriter.pln("#include <iostream>"); 
		    impWriter.pln("#include <string>");
		    impWriter.pln(); 
		    impWriter.pln("#include " + "\"" + hppName + "\"");
		    impWriter.pln(); 
		    impWriter.pln("using namespace std;");
			impWriter.pln(); 
			
		    headWriter.pln("#include <string>");
		    headWriter.pln("using namespace std;");
		    headWriter.pln();
	    } catch (Exception e) {}
    	visit(n);
    	impWriter.flush();
	    headWriter.flush();
    }
    /*
    * Class Declaration
    */
    public void visitClassDeclaration(GNode n) {
    	new SFClassDeclaration(impWriter, headWriter, this, n);
    }
    /*
    * Method Declaration
    */
	public void visitMethodDeclaration(GNode n) {
		new SFMethodDeclaration(impWriter, headWriter, this, n);
	}
	/*
    * Constructor Declaration
    */
	public void visitConstructorDeclaration(GNode n) {

	}
    /*
    *
    */
	public void visitFormalParameters(GNode n) {

	}
    /*
    *
    */
	public void visitExtension(GNode n) {
		//retrieve explicit extension 
		_super=n.getNode(0).getNode(0).getString(0); /**@var name of super class */
	}
    /*
    *
    */
	public void visitBlock(GNode n) {
		visit(n);
	}
    /*
    *
    */
	public void visitFieldDeclaration(GNode n) {

	}
    /*
    *
    */
    public void visitExpressionStatement(GNode n) {

    }
    /*
    *
    */
    public void visitCallExpression(GNode n) {

    }
    /*
    *
    */
	public void visitAdditiveExpression(GNode n) {
	
	}
    /*
    *
    */
	public void visitForStatement(GNode n) {

	}
    /*
    * Visit method
    */
    public void visit(Node n) {
		for (Object o : n) if (o instanceof Node) dispatch((Node)o);
	}


}