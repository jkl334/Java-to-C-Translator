package nyu.segfault;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.util.Tool;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;

public class SegFaultVisitor extends Visitor {
	private String[] files;
	private String fileName; // name of the file to be translated

	private int count;
	
	public PrintWriter p1; // prints to the method body
	public PrintWriter p2; // prints to the header

	public SegFaultVisitor(String[] files) {
		this.files = files;
	}

	public void visitCompilationUnit(GNode n) {

		//creates the new output files to be written to

		fileName = files[0];
		fileName = fileName.replace(".java", "");
		File cppFile; 
		File mainFile; 
		try { 

	        // File I/O 
	        cppFile = new File("output", fileName + "_output.cc");
	        
	        // create a new file 
	        
	        cppFile.createNewFile();

	        mainFile = new File("output", fileName + "_main.cc"); 

	        mainFile.createNewFile(); 

	        p1 = new PrintWriter(cppFile); 
	        p2 = new PrintWriter(mainFile);

	        p1.println("//SegFault");
	        p1.println();
	        p1.println();
	        p1.println();

	    } catch (Exception e) { 
		
		}
    	visit(n);
    	p1.flush();
    	p2.flush();
  	}

  	public void visitMethodDeclaration(GNode n) {
    	Node body = n.getNode(7);
    	if (null != body) visit(body);
	}

	public void visitExpressionStatement(GNode n) {
		//System.out.println(n.getNode(0).toString());
		count = 0;
		boolean isEndLine = false; // used to check if the print statement has an ln 
		if (n.getNode(0).toString().contains("println")) isEndLine = true;
  		if (n.getNode(0).getName().equals("CallExpression")) { // checks if a call expression is being made
  			p1.print("printf(");
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
			    		p1.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
                    p1.print(n.getString(0));
                }

				public void visitIntegerLiteral(GNode n) {
					if (count > 0) {
			    		p1.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					p1.print(n.getString(0));
					}

					public void visitFloatingPointLiteral(GNode n) {
					if (count > 0) {
			    		p1.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					p1.print(n.getString(0));
    	        }

    	        public void visitCharacterLiteral(GNode n) {
					if (count > 0) {
			    		p1.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					p1.print(n.getString(0));
    	        }  

    	        public void visitBooleanLiteral(GNode n) {
					if (count > 0) {
			    		p1.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					p1.print(n.getString(0));
    	        }            

    	        public void visitNullLiteral(GNode n) {
					if (count > 0) {
			    		p1.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
					p1.print("null");
    	        }            	        	                  	        

                public void visitPrimaryIdentifier(GNode n) { 
	                vars.add(n.getString(0));
	           		if (count > 0) {
			    		p1.print(" + ");
			    	}
			    	else {
			    		count++;
			    	}
	                p1.print("%s");
    	        }

    	        public void visit(GNode n) { 
        	        for (Object o : n) if (o instanceof Node) dispatch((Node) o);
            	}

        	}.dispatch(n.getNode(0));

        	if (isEndLine != false) {
        		p1.print(" + " + "\"" + " + " + "\\" + "n" + "\"");
        	}

        	if (!vars.isEmpty()){
        		for (String var : vars) {
        			p1.print(", " + "to_string(" + var + ")");
        		}
        	}

        	p1.print(");");
        	p1.println();
		}

    	else {
        	visit(n);                     
    	}
	}

	public void visit(Node n) {
    	for (Object o : n) {
      	if (o instanceof Node) dispatch((Node)o);
    	}
	}

}