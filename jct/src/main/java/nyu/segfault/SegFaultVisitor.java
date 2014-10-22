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
	
	public PrintWriter impWriter; // prints to the method body
	public PrintWriter headWriter; // prints to the header

	ArrayList<GNode> cxx_class_roots=new ArrayList<GNode>(); /**@var root nodes of classes in linear container*/
	int index=-1; /**@var root node of class subtree index*/

	SymbolTable table; /**@var node symbols*/


	public SegFaultVisitor(String[] files) {
		this.files = files;
	}

	public void visitCompilationUnit(GNode n) {

		//creates the new output files to be written to

		fileName = files[0];
		fileName = fileName.replace(".java", "");
		File impFile; 
		File headFile; 
		try { 

	        // File I/O to folder titled "output"
	        impFile = new File("output", fileName + ".cpp");
	        
	        // create a new file 
	        
	        impFile.createNewFile();

	        headFile = new File("output", fileName + ".hpp"); 

	        headFile.createNewFile(); 

	        impWriter = new PrintWriter(impFile); 
	        headWriter = new PrintWriter(headFile);

	        impWriter.println("//SegFault");
	        impWriter.println();
	        impWriter.println();
	        impWriter.println();

	    } catch (Exception e) { 
		
		}
    	visit(n);
    	impWriter.flush();
    	headWriter.flush();
  	}

  	GNode class_node; /**@var java class node */

  	public void visitClassDeclaration(GNode n){
				
	}
			
	public void visitAdditiveExpression(GNode n){

	}
	public void visitBlock(GNode n){


	}
	public void visitCallExpression(GNode n){

	}

  	public void visitMethodDeclaration(GNode n) {
		//extract function prototype
		final GNode root=n;
		new Visitor(){
			String fp=""; /**@var function prototype*/
			//function return type + function name
			public void visitQualifiedIdentifier(GNode n){
				fp+=n.getString(0)+" "+root.getString(3)+"(";	
			}
			//function parameters
			public void visitFormalParameters(GNode n){
				for(int i=0; i< n.size(); i++){
					Node fparam=n.getNode(i);
						
					//retrieve argument type
					fp+=fparam.getNode(1).getNode(0).getString(0)+" ";

					//retrieve argument name
					fp+=fparam.getString(3);

					if(i+1 < n.size()) fp+=",";
					else fp+=")";
				}
				//runtime.console().pln(fp);
			}
			public void visit(Node n){
				for (Object o : n)
					if(o instanceof Node) dispatch((Node)o);

			}
		}.dispatch(n);
		
		//write function definitions to hpp and cpp files
		
		
		
		//body
		Node body = n.getNode(7);
		if (null != body) visit(body);
				
	}
	public void visitExpressionStatement(GNode n) {
		//System.out.println(n.getNode(0).toString());
		count = 0;
		boolean isEndLine = false; // used to check if the print statement has an ln 
		if (n.getNode(0).toString().contains("println")) isEndLine = true;
  		if (n.getNode(0).getName().equals("CallExpression")) { // checks if a call expression is being made
  			impWriter.print("printf(");
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
			    		impWriter.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
                    impWriter.print(n.getString(0));
                }

				public void visitIntegerLiteral(GNode n) {
					if (count > 0) {
			    		impWriter.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					impWriter.print(n.getString(0));
					}

					public void visitFloatingPointLiteral(GNode n) {
					if (count > 0) {
			    		impWriter.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					impWriter.print(n.getString(0));
    	        }

    	        public void visitCharacterLiteral(GNode n) {
					if (count > 0) {
			    		impWriter.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					impWriter.print(n.getString(0));
    	        }  

    	        public void visitBooleanLiteral(GNode n) {
					if (count > 0) {
			    		impWriter.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					impWriter.print(n.getString(0));
    	        }            

    	        public void visitNullLiteral(GNode n) {
					if (count > 0) {
			    		impWriter.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					impWriter.print("null");
    	        }            	        	                  	        

                public void visitPrimaryIdentifier(GNode n) { 
	                vars.add(n.getString(0));
	           		if (count > 0) {
			    		impWriter.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
	                impWriter.print("%s");
    	        }

    	        public void visit(GNode n) { 
        	        for (Object o : n) if (o instanceof Node) dispatch((Node) o);
            	}

        	}.dispatch(n.getNode(0));

        	if (isEndLine != false) {
        		impWriter.print(" + " + "\"" + " + " + "\\" + "n" + "\"");
        	}

        	if (!vars.isEmpty()){
        		for (String var : vars) {
        			impWriter.print(", " + "to_string(" + var + ")");
        		}
        	}

        	impWriter.print(");");
        	impWriter.println();
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

	public void visit(Node n) {
    	for (Object o : n) {
      	if (o instanceof Node) dispatch((Node)o);
    	}
	}

}
