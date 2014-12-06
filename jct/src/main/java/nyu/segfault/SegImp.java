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
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;
import xtc.util.Tool;

/**
 * SegImp Visitor  handles classes without inheritance and virtual methods
 */
public class SegImp extends Visitor{

    private String[] files; // args passed from the translator
    private String fileName; // name of the file to be translated

    private int count;

    String cc_name; /**@var current class name (this) */
    String className;

    public SegImp() {  }

    public void visitCompilationUnit(GNode n) {
        SegHelper.writeMacros();
        SegHelper.getCallExpression(n);
        visit(n);
        SegHelper.cpp_flush();
    }

    public String constructorProp; //global variable to store constructor property in class declaration and to use to assign arguments in struct initialization in visitFieldDeclaration -Jeff

    public void visitClassDeclaration(GNode n) {
        String className = SegHelper.getClassDeclaration(n).split(" ")[1];  // Set the SegHelper's curr_class, and returns the name of the current class.
//        SegHelper.cpp_pln("class " + className + " {");
        SegHelper.getGlobalVariables(n);
        for (String gVar : SegHelper.currentClassGlobalVariables) SegHelper.cpp_pln(gVar + ";");
        visit(n);
//        SegHelper.cpp_pln("}\n");

    }

    public void visitMethodDeclaration(GNode n){
        String declaration = SegHelper.getMethodDeclaration(n, "SegImp");
        declaration = declaration.substring(0, declaration.length() - 1);  // Remove the semi-colon.
        SegHelper.cpp_pln(declaration + " {");
        String body = SegHelper.getMethodBody(n);
        if (SegHelper.getMethodName(n).equals("main")) {
            body = SegHelper.getMainMethodArgumentsAsSmartPointers() + "\n" + body;
        }
        SegHelper.cpp_pln(body);
        if(SegHelper.getMethodName(n).equals("main")) {
            SegHelper.cpp_pln("\treturn 0;");
        }
        SegHelper.cpp_pln("}\n");
        SegHelper.cpp_flush();
    }

    public void visit(Node n) { for (Object o : n) if (o instanceof Node) dispatch((Node)o); }

public void visitForStatement(GNode n) {
	SegHelper.cpp_p("\t" + "for " + "(");
	
	new Visitor() {  

		public void visitBasicForControl(GNode n) {
			new Visitor() {
				public void visitRelationalExpression(GNode n) {
					SegHelper.cpp_p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0) + "; ");
				}
				public void visitDeclarators(GNode n) {
					SegHelper.cpp_p(n.getNode(0).getString(0) + " = " + n.getNode(0).getNode(2).getString(0) + "; ");
				}
				public void visitExpressionList(GNode n) {
					new Visitor() {
						public void visitPostfixExpression(GNode n) {
							SegHelper.cpp_p(n.getNode(0).getString(0) + n.getString(1));
						}
						public void visitUnaryExpression(GNode n) {
							SegHelper.cpp_p(n.getString(0) + n.getNode(1).getString(0));
						}
						public void visit(Node n){
							for (Object o : n) if(o instanceof Node) dispatch((Node)o);
						}
					}.dispatch(n);
				}
				public void visitStringLiteral(GNode n) {
			SegHelper.cpp_p(n.getString(0));
		    }

				public void visitIntegerLiteral(GNode n) {
					SegHelper.cpp_p(n.getString(0));
				}

				public void visitFloatingPointLiteral(GNode n) {
					SegHelper.cpp_p(n.getString(0));
		    }

		    public void visitCharacterLiteral(GNode n) {
					SegHelper.cpp_p(n.getString(0));
		    }

		    public void visitBooleanLiteral(GNode n) {
					SegHelper.cpp_p(n.getString(0));
		    }

		    public void visitNullLiteral(GNode n) {
					SegHelper.cpp_p("null");
		    }

		    public void visitPrimaryIdentifier(GNode n) {
			    SegHelper.cpp_p(n.getString(0));
		    }
		    public void visitType(GNode n) {
			SegHelper.cpp_p(n.getNode(0).getString(0) + " ");
		    }

		    public void visitAdditiveExpression(GNode n) {
			SegHelper.cpp_p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
		    }

			
				public void visit(Node n){
					for (Object o : n) if(o instanceof Node) dispatch((Node)o);
				}


			}.dispatch(n);
			SegHelper.cpp_pln(") {");
		}

		public void visitBlock(GNode n) {
			
			
			new Visitor() {
				 public void visitExpressionStatement(GNode n) {
					SegHelper.cpp_p("\t");

		if (n.getNode(0).getName().equals("Expression")) { // checks if regular expression is being made
			SegHelper.cpp_p("\t");


			new Visitor() {  // Visit assigned value if any

				public void visitExpression(GNode n) { 
					if (n.getNode(2).getName().equals("AdditiveExpression")) {
						SegHelper.cpp_p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getNode(0).getString(0) + " " + n.getNode(2).getString(1) + " " + n.getNode(2).getNode(2).getString(0));
					} else {
						SegHelper.cpp_p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
					}
				}

						public void visitStringLiteral(GNode n) {
				    SegHelper.cpp_p(n.getString(0));
				}

						public void visitIntegerLiteral(GNode n) {
							SegHelper.cpp_p(n.getString(0));
						}

						public void visitFloatingPointLiteral(GNode n) {
							SegHelper.cpp_p(n.getString(0));
				}

				public void visitCharacterLiteral(GNode n) {
							SegHelper.cpp_p(n.getString(0));
				}

				public void visitBooleanLiteral(GNode n) {
							SegHelper.cpp_p(n.getString(0));
				}

				public void visitNullLiteral(GNode n) {
							SegHelper.cpp_p("null");
				}

				public void visitPrimaryIdentifier(GNode n) { 

					SegHelper.cpp_p(n.getString(0));
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
					SegHelper.cpp_p(add);
							*/
					SegHelper.cpp_p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
				//	if (n.getNode(0) != null) visit(n.getNode(0));
				}
						public void visit(Node n){
							for (Object o : n) if(o instanceof Node) dispatch((Node)o);
						}
					}.dispatch(n);
					SegHelper.cpp_pln(";");
		}
		//System.out.println(n.getNode(0).toString());
		boolean isEndLine = false; // used to check if the print statement has an ln
		if (n.getNode(0).toString().contains("println")) isEndLine = true;
		if (n.toString().contains("CallExpression") && n.toString().contains("SelectionExpression") && n.toString().contains("System") && n.toString().contains("out")) { // checks if a call expression is being made
					// String method_called = n.getNode(0).getNode(3).getNode(0).getString(2);
					// SegHelper.cpp_p("\tprintf("+method_called+"()");
					//System.out.println(n.toString());
					SegHelper.cpp_p("\tcout");
					//System.out.println(n.toString());
		    final ArrayList<String> vars = new ArrayList<String>();
		    new Visitor() {

			public void visitCallExpression(GNode n){
				// If a method is called
				
				    Node arguments = n.getNode(3);
				    new Visitor() {
					public void visitStringLiteral(GNode n) {
					SegHelper.cpp_p(" << " + n.getString(0));
					}

					public void visitIntegerLiteral(GNode n) {
					    SegHelper.cpp_p(" << " + n.getString(0));
					}

					public void visitFloatingPointLiteral(GNode n) {
					    SegHelper.cpp_p(" << " + n.getString(0));
					}

					public void visitCharacterLiteral(GNode n) {
					    SegHelper.cpp_p(" << " + n.getString(0));
					}

					public void visitBooleanLiteral(GNode n) {
					    SegHelper.cpp_p(" << " + n.getString(0));
					}

					public void visitNullLiteral(GNode n) {
					    SegHelper.cpp_p(" << " + "null");
					}

					public void visitPrimaryIdentifier(GNode n) {
					    SegHelper.cpp_p(" << " + n.getString(0));
					}
					public void visitCallExpression(GNode n) {
						String method = "";
						method += n.getNode(0).getString(0);
						if (n.getString(2).isEmpty()) {
							method += "()";
						}
						else {
							method += "." + n.getString(2) + "(";
								if (n.getNode(3).isEmpty()){
									method += ")";
								}
						}
						vars.add(method);
					SegHelper.cpp_p(" << " + method);
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
			SegHelper.cpp_p(" << \"\\n\"");
		    }
		    /*
		    if (!vars.isEmpty()){
			for (String var : vars) {
			    SegHelper.cpp_p(", " + "to_string(" + var + ")");
			}
		    }
		    */
		    SegHelper.cpp_pln(";");
	    } 
	    else if (n.toString().contains("CallExpression")) {
		SegHelper.cpp_p("\t");
		new Visitor() {
			public void visitCallExpression(GNode n) {              		
				String method = "";
				method += n.getNode(0).getString(0);
				if (n.getString(2).isEmpty()) {
					method += "()";
				}
				else {
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
				SegHelper.cpp_p(method);
			}
			public void visit(GNode n) {
				for (Object o : n) if (o instanceof Node) dispatch((Node) o);
			}
		}.dispatch(n);
		SegHelper.cpp_pln(";");
	    }
	    else {
		    visit(n);
	    }
	}
			public void visit(GNode n) {
				for (Object o : n) if (o instanceof Node) dispatch((Node) o);
			}

			}.dispatch(n);
		}	


		public void visit(Node n){
			for (Object o : n) if(o instanceof Node) dispatch((Node)o);
		}
		
	}.dispatch(n);
	SegHelper.cpp_p("\n");
	SegHelper.cpp_p("\t" + "}" + "\n");

	}
}
