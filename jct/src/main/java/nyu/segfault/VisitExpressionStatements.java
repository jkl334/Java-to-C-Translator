package nyu.segfault;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import javax.swing.tree.DefaultTreeModel;

import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.util.SymbolTable;
import xtc.util.Tool;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;


public class VisitExpressionStatements extends Visitor {

				private Printer impWriter;
				private Printer headWriter;

				public VisitExpressionStatements(Printer impWriter, Printer headWriter) {
					this.impWriter = impWriter;
					this.headWriter = headWriter;
				}

	            public void visitExpressionStatement(GNode n) {

            	if (n.getNode(0).getName().equals("Expression")) { // checks if regular expression is being made
            		impWriter.p("\t");


            		new Visitor() {  // Visit assigned value if any

            			public void visitExpression(GNode n) { 
            				if (n.getNode(2).getName().equals("AdditiveExpression")) {
            					impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getNode(0).getString(0) + " " + n.getNode(2).getString(1) + " " + n.getNode(2).getNode(2).getString(0));
            				} else {
            					impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
            				}
            			}

						public void visitStringLiteral(GNode n) {
		                    impWriter.p(n.getString(0));
		                }

						public void visitIntegerLiteral(GNode n) {
							impWriter.p(n.getString(0));
						}

						public void visitFloatingPointLiteral(GNode n) {
							impWriter.p(n.getString(0));
		    	        }

		    	        public void visitCharacterLiteral(GNode n) {
							impWriter.p(n.getString(0));
		    	        }

		    	        public void visitBooleanLiteral(GNode n) {
							impWriter.p(n.getString(0));
		    	        }

		    	        public void visitNullLiteral(GNode n) {
							impWriter.p("null");
		    	        }

		                public void visitPrimaryIdentifier(GNode n) { 

			                impWriter.p(n.getString(0));
		    	        }

		    	        public void visitAdditiveExpression(GNode n) {
		    	        //	Currently only works for 2 vars in expression. hard-coded 
		    	        //	System.out.println(n.toString());
		    	        	/*String add = "";
		    	        	System.out.println(n.size());
		    	        	for (int i = 0,j=1; i < n.size(); i+=2,j+=2) {
		    	        		if(i < n.size()) {
			    	        		add += n.getNode(i).getString(0);
			    	        	    add += n.getString(j);
			    	        	}
		    	        	}
		    	        	impWriter.p(add);
							*/
		    	        	impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
		    	        //	if (n.getNode(0) != null) visit(n.getNode(0));
		    	        }
						public void visit(Node n){
							for (Object o : n) if(o instanceof Node) dispatch((Node)o);
						}
					}.dispatch(n);
					impWriter.pln(";");
            	}
                //System.out.println(n.getNode(0).toString());
                boolean isEndLine = false; // used to check if the print statement has an ln
                if (n.getNode(0).toString().contains("println")) isEndLine = true;
                if (n.toString().contains("CallExpression") && n.toString().contains("SelectionExpression") && n.toString().contains("System") && n.toString().contains("out")) { // checks if a call expression is being made
					// String method_called = n.getNode(0).getNode(3).getNode(0).getString(2);
					// impWriter.p("\tprintf("+method_called+"()");
					//System.out.println(n.toString());
					impWriter.p("\tcout");
					//System.out.println(n.toString());
                    final ArrayList<String> vars = new ArrayList<String>();
                    new Visitor() {

                        public void visitCallExpression(GNode n){
                        	// If a method is called
                        	
		                    Node arguments = n.getNode(3);
		                    new Visitor() {
		                    	public void visitStringLiteral(GNode n) {
                            		impWriter.p(" << " + n.getString(0));
		                        }

		                        public void visitIntegerLiteral(GNode n) {
		                            impWriter.p(" << " + n.getString(0));
		                        }

		                        public void visitFloatingPointLiteral(GNode n) {
		                            impWriter.p(" << " + n.getString(0));
		                        }

		                        public void visitCharacterLiteral(GNode n) {
		                            impWriter.p(" << " + n.getString(0));
		                        }

		                        public void visitBooleanLiteral(GNode n) {
		                            impWriter.p(" << " + n.getString(0));
		                        }

		                        public void visitNullLiteral(GNode n) {
		                            impWriter.p(" << " + "null");
		                        }

		                        public void visitPrimaryIdentifier(GNode n) {
		                            impWriter.p(" << " + n.getString(0));
		                        }
		                    	public void visitCallExpression(GNode n) {
		                    		String method = "";
		                    		method += n.getNode(0).getString(0);
		                    		if (n.getString(2).isEmpty()) {
		                    			method += "()";
		                    		}
		                    		else if (n.getString(2).equals("toString")) {
	                					method = "std::to_string(" + method + ")";
	                				}
		                    		else {
		                    			method += "." + n.getString(2) + "(";
		                    				if (n.getNode(3).isEmpty()){
		                    					method += ")";
		                    				}
		                    		}
		                    		vars.add(method);
                            		impWriter.p(" << " + method);
		                    	}
		                    	public void visit(GNode n) {
                            		for (Object o : n) if (o instanceof Node) dispatch((Node) o);
                        		}
		                    }.dispatch(arguments);
                        }

                        public void visit(GNode n) {
                            for (Object o : n) if (o instanceof Node) dispatch((Node) o);
                        }

                    }.dispatch(n.getNode(0));

                    if (isEndLine) {
                        impWriter.p(" << endl");
                    }
                    /*
                    if (!vars.isEmpty()){
                        for (String var : vars) {
                            impWriter.p(", " + "to_string(" + var + ")");
                        }
                    }
                    */
                    impWriter.pln(";");
            	} 
	            else if (n.toString().contains("CallExpression")) {
	            	impWriter.p("\t");
	                new Visitor() {
	                	public void visitCallExpression(GNode n) {              		
	                		String method = "";
	                		method += n.getNode(0).getString(0);
	                		if (n.getString(2).isEmpty()) {
	                			method += "()";
	                		}
	                		else if (n.getString(2).equals("toString")) {
	                			method = "std::to_string(" + method + ")";
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
	                						if (i == 0) {
	                							method += arguments.getNode(0).getString(i);
	                						}
	                						else {
	                							method += ", " + arguments.getNode(0).getString(i);
	                						}
	                					}
	                					method += ")";

	                				}
	                		}
	                		
	                		impWriter.p(method);
	                	}
	                	public void visit(GNode n) {
	                		for (Object o : n) if (o instanceof Node) dispatch((Node) o);
	            		}
	                }.dispatch(n);
	                impWriter.pln(";");
	            }
        	}
}