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

public class VisitFieldDeclarations extends Visitor {

	private Printer impWriter;
	private Printer headWriter;
	private String constructorProp;

	public VisitFieldDeclarations(Printer impWriter, Printer headWriter, String constructorProp) {
		this.impWriter = impWriter;
		this.headWriter = headWriter;
		this.constructorProp = constructorProp;
	}

	public void visitFieldDeclaration(GNode n) {  // Need to add visitMethodDeclaration() to visitor for advanced FieldDeclarations.
		//System.out.println(n);
	/* Determine and print the declarator type. */
	    impWriter.p("\t");
	    String declarationType = n.getNode(1).getNode(0).getString(0);
	    if (declarationType.equals("boolean")) {
	        impWriter.p("boolean ");
	    } else if (declarationType.equals("char")) {
	        impWriter.p("char ");
	    } else if (declarationType.equals("double")) {
	        impWriter.p("double ");
	    } else if (declarationType.equals("float")) {
	        impWriter.p("float ");
	    } else if (declarationType.equals("int")) {
	        impWriter.p("int ");
	    } else if (declarationType.equals("String")) {
	        impWriter.p("string ");
	    }
	    else if (n.getNode(1).getNode(0).getName().equals("QualifiedIdentifier")) {
	    	/*
	    	if (n.getNode(1).getNode(0).getString(0).equals("String")) {
	    		impWriter.p("string");
	    	} 
	    	else {
	    		*/
	    		impWriter.p(n.getNode(1).getNode(0).getString(0) + " ");
	    	//}
	    }

		/* Print the name of the field. */
		String fieldName = n.getNode(2).getNode(0).getString(0);
		impWriter.p(fieldName);

		/* Potentially visit the assigned value (if any). */
		new Visitor() {
			// initializing struct
			
			public void visitDeclarators(GNode n) { //added 
				
				try {
					boolean a = n.getNode(0).getNode(2).getString(0).equals("null");
					impWriter.p(" = " + n.getNode(0).getNode(2).getString(0));
				} catch(Exception e) {
				}
				
				new Visitor() {
					public void visitNewClassExpression(GNode n) {
						if (n.getNode(3).size() > 0) {  // if arguments exist for object initializing
							impWriter.p(" = " + "(" + n.getNode(2).getString(0) + ")" + " {" + "." + constructorProp + " = " + n.getNode(3).getNode(0).getString(0) + "}");  //arguments passed only 1 argument works for now
						} else { // if arguments do not exist
							if (!n.getNode(3).toString().equals("Arguments()")) {
								impWriter.p(" = " + "(" + n.getNode(2).getString(0) + ")" + " {" + " }");
							}
						}
					}
					public void visit(GNode n) {
						for (Object o : n) if(o instanceof Node) dispatch((Node)o);
					}
				}.dispatch(n);
			}


			public void visitStringLiteral(GNode n) {
	            impWriter.p(" = " + n.getString(0));
	        }

			public void visitIntegerLiteral(GNode n) {
				impWriter.p(" = " + n.getString(0));
			}

			public void visitFloatingPointLiteral(GNode n) {
				impWriter.p(" = " + n.getString(0));
	        }

	        public void visitCharacterLiteral(GNode n) {
				impWriter.p(" = " + n.getString(0));
	        }

	        public void visitBooleanLiteral(GNode n) {
				impWriter.p(" = " + n.getString(0));
	        }

			public void visit(GNode n) {
				for (Object o : n) if(o instanceof Node) dispatch((Node)o);
			}
		}.dispatch(n);
	    impWriter.pln(";");
	}

}