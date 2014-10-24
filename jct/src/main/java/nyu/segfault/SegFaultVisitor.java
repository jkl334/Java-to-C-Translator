package nyu.segfault;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.util.SymbolTable;
import xtc.util.Tool;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;


public class SegFaultVisitor extends Visitor {
	private String[] files; // args passed from the translator
	private String fileName; // name of the file to be translated

	private int count;

	public PrintWriter cWriter; // prints to the method body
	public PrintWriter hWriter; // prints to the header
	public Printer impWriter;
	public Printer headWriter;


	ArrayList<GNode> cxx_class_roots=new ArrayList<GNode>(); /**@var root nodes of classes in linear container*/
	int index=-1; /**@var root node of class subtree index*/

	String cc_name; /**@var current class name (this) */

	SymbolTable table; /**@var node symbols*/


	public SegFaultVisitor(String[] files) {
		this.files = files;
	}

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
	        impWriter.pln("//SegFault");
	        impWriter.pln();
	        impWriter.pln();
	        impWriter.pln();

	    } catch (Exception e) {System.out.println("Exception");}
    	visit(n);
    	impWriter.flush();
	    headWriter.flush();
    }

  	GNode class_node; /**@var java class node */

  	public void visitClassDeclaration(GNode n){
		index++;
		cxx_class_roots.add(n);
		String cc_name=cxx_class_roots.get(index).getString(3);
		visit(n);
	}
	public void visitAdditiveExpression(GNode n){
	}
	public void visitBlock(GNode n){
	}
	public void visitCallExpression(GNode n){
	}

	public void visitMethodDeclaration(GNode n){

		final GNode root=n;
		final String return_type=n.getNode(2).toString();
		//runtime.console().pln(return_type);
		new Visitor(){
			String fp="";
			public void visitFormalParameters(GNode n){
				fp+=root.getString(3)+"(";
				if( n.size() == 0 ) fp+=")";
				for(int i=0; i< n.size(); i++){
					Node fparam=n.getNode(i);

					//retrieve argument type
					fp+=fparam.getNode(1).getNode(0).getString(0)+" ";

					//retrieve argument name
					fp+=fparam.getString(3);

					if(i+1 < n.size()) fp+=",";
					else fp+=")";
				}
				String rType="";
				if(return_type.equals("VoidType()")) rType="void";
				else if( return_type.equals("String")) rType="string";

				String hpp_prototype= rType +" "+ fp;
				String cpp_prototype= rType+" "+cc_name+ "::" + fp+"{\n";
				//runtime.console().pln(cpp_prototype);
				//write function prototype to hpp file within struct <cc_name>
				// <return_type> <function_name>(arg[0]...arg[n]);

				headWriter.pln(hpp_prototype);
				//write function prototype to cpp file
				// <return type> <class name> :: <function name> (arg[0]...arg[n]){

				impWriter.pln(cpp_prototype);

			}
			public void visitReturnStatement(GNode n) {
				impWriter.pln("return" + " " + n.getNode(0).getString(0) + ";");
			}
			public void visit(Node n){
				for (Object o : n) if(o instanceof Node) dispatch((Node)o);

			}
		}.dispatch(n);
		
		impWriter.pln("}");
		Node body = n.getNode(7);
		if (null != body) visit(body);
	}
	public void visitExpressionStatement(GNode n) {
		//System.out.println(n.getNode(0).toString());
		count = 0;
		boolean isEndLine = false; // used to check if the print statement has an ln
		if (n.getNode(0).toString().contains("println")) isEndLine = true;
  		if (n.getNode(0).getName().equals("CallExpression")) { // checks if a call expression is being made
  			impWriter.pln("printf(");
    		final ArrayList<String> vars = new ArrayList<String>();
        	new Visitor() {
		    	public void visitSelectionExpression(GNode n) {
		    		/*
		    		*
		    		*
		    		*/
			    }

			    public void visitStringLiteral(GNode n) {
			    	if (count > 0) {
			    		impWriter.pln(" + ");
			    	}
			    	else {
			    		count++;
			    	}
                    impWriter.pln(n.getString(0));
                }

				public void visitIntegerLiteral(GNode n) {
					if (count > 0) {
			    		impWriter.pln(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					impWriter.pln(n.getString(0));
					}

					public void visitFloatingPointLiteral(GNode n) {
					if (count > 0) {
			    		impWriter.pln(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					impWriter.pln(n.getString(0));
    	        }

    	        public void visitCharacterLiteral(GNode n) {
					if (count > 0) {
			    		impWriter.pln(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					impWriter.pln(n.getString(0));
    	        }

    	        public void visitBooleanLiteral(GNode n) {
					if (count > 0) {
			    		impWriter.pln(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					impWriter.pln(n.getString(0));
    	        }

    	        public void visitNullLiteral(GNode n) {
					if (count > 0) {
			    		impWriter.pln(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					impWriter.pln("null");
    	        }

                public void visitPrimaryIdentifier(GNode n) { 
	                vars.add(n.getString(0));
	           		if (count > 0) {
			    		impWriter.pln(" + ");
			    	}
			    	else {
			    		count++;
			    	}
	                impWriter.pln("%s");
    	        }

    	        public void visit(GNode n) {
        	        for (Object o : n) if (o instanceof Node) dispatch((Node) o);
            	}

        	}.dispatch(n.getNode(0));

        	if (isEndLine != false) {
        		impWriter.pln(" + " + "\"" + " + " + "\\" + "n" + "\"");
        	}

        	if (!vars.isEmpty()){
        		for (String var : vars) {
        			impWriter.pln(", " + "to_string(" + var + ")");
        		}
        	}

        	impWriter.pln(");");
        	impWriter.pln();
		}

    	else {
        	visit(n);
    	}
	}

	public void visitFieldDeclaration(GNode n){
	}

	public void visitForStatement(GNode n){
	}

	public void visitPrimaryIdentifier(GNode n){
	}

	public void visitBreakStatement(GNode n) {
		impWriter.pln("break;\n");
	}

	public void visitContinueStatement(GNode n) {
		impWriter.pln("continue;\n");
	}

	

	public void visit(Node n) {
		for (Object o : n) if (o instanceof Node) dispatch((Node)o);
	}
}
