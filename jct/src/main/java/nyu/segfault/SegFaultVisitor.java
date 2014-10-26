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

import java.util.ArrayList;


public class SegFaultVisitor extends Visitor {
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

	ArrayList<GNode> cxx_class_roots=new ArrayList<GNode>(); /**@var root nodes of classes in linear container*/
	int index=-1; /**@var root node of class subtree index*/

	String cc_name; /**@var current class name (this) */
	String className;

	SymbolTable table; /**@var node symbols*/

	String method_return_type = "";

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
	        impWriter.pln("/**\n * Team: SegFault\n */");
	        impWriter.pln();

		    impWriter.pln("#include <sstream> ");
		    impWriter.pln("#include <iostream>"); 
		    impWriter.pln("#include <string>");
		    impWriter.pln(); 
		    impWriter.pln("#include " + "\"" + hppName + "\"");
		    impWriter.pln(); 
		    impWriter.pln("using namespace std;");
			impWriter.pln(); 
			headWriter.pln("using namespace std;");
		    headWriter.pln("#include <string>");
		    headWriter.pln();
	    } catch (Exception e) {}
    	visit(n);
    	impWriter.flush();
	    headWriter.flush();
    }

  	GNode class_node; /**@var java class node */

  	public String constructorProp; //global variable to store constructor property in class declaration and to use to assign arguments in struct initialization in visitFieldDeclaration -Jeff

  	public void visitClassDeclaration(GNode n) {
		className = n.getString(1);
		headWriter.pln("struct " + className + " {");

		index++;
		cxx_class_roots.add(n);
		//cc_name=cxx_class_roots.get(index).getString(3);

		//check if class explicitly inherits from another class other than Object
		// if not add class as child of Object
		if(n.getNode(3) == null)
			inhTree.addChild(className);  // Add a child to the parent with the class' name.

		//otherwise add class a child of explicit super class
		else{

			visit(n.getNode(3));
			SegNode<String> super_class=inhTree.dfs(inhTree,_super);
			super_class.addChild(className);
		}
        new Visitor() {
            /* This takes care of global variables */
            public void visitFieldDeclaration(GNode n) {
            	headWriter.p("\t");
                /* Returns if n is a local field declaration, in which case it is taken care of by visitMethodDeclaration. */
                System.out.println("Class body field: " + n + "\n");


                /* Determine and print the variable modifiers (e.g. "static", "private"). */
                for (int x = 0; (x < n.getNode(0).size()) && (n.getNode(0).getNode(x).getString(0) != null); x++) {
                    String modifier = n.getNode(0).getNode(x).getString(0);
                    if (modifier.equals("static")) {
                        System.out.println(n);
                        headWriter.p("static ");
                    } else if (modifier.equals("private")) {
                        // Do something.
                    }
                }

                /* Determine and print the declarator type. */
                String declarationType = n.getNode(1).getNode(0).getString(0);
                if (declarationType.equals("boolean")) {
                    headWriter.p("boolean ");
                } else if (declarationType.equals("char")) {
                    headWriter.p("char ");
                } else if (declarationType.equals("double")) {
                    headWriter.p("double ");
                } else if (declarationType.equals("float")) {
                    headWriter.p("float ");
                } else if (declarationType.equals("int")) {
                    headWriter.p("int ");
                } else if (declarationType.equals("String")) {
                    headWriter.p("string ");
                } else {
		    headWriter.p(declarationType + " ");  // For non-primitive, non-String objects.
		}
                

                /* Print the name of the field. */
                String fieldName = n.getNode(2).getNode(0).getString(0);
                headWriter.p(fieldName);

                /* Potentially visit the assigned value (if any). */
                new Visitor() {
                	public void visitDeclarators(GNode n) { // method to visit Declarator and grab constructor properties to store to constructorProp
                		constructorProp = n.getNode(0).getString(0); //grabbing property
                	}


                    public void visitStringLiteral(GNode n) {
                        headWriter.p(" = " + n.getString(0));
                    }

                    public void visitIntegerLiteral(GNode n) {
                        headWriter.p(" = " + n.getString(0));
                    }

                    public void visitFloatingPointLiteral(GNode n) {
                        headWriter.p(" = " + n.getString(0));
                    }

                    public void visitCharacterLiteral(GNode n) {
                        headWriter.p(" = " + n.getString(0));
                    }

                    public void visitBooleanLiteral(GNode n) {
                        headWriter.p(" = " + n.getString(0));
                    }

                    public void visit(GNode n) {
                        for (Object o : n) if(o instanceof Node) dispatch((Node)o);
                    }
                }.dispatch(n);
                headWriter.pln(";");
            }

            public void visitConstructorDeclaration(GNode n) {
    			headWriter.p("\t");
    			headWriter.p(n.getString(2) + "(");
				new Visitor() {

					// check the modifier
					public void visitModifiers(GNode n) {
						new Visitor() {
							// check the modifier
							public void visitModifier(GNode n) {
								//impWriter.pln(n.getString(0) + ":");
							}

							public void visit(GNode n) {
								for (Object o : n) if(o instanceof Node) dispatch((Node)o);
							}
						}.dispatch(n);
					}

					public void visitFormalParameters(GNode n) {
						if (!n.getNode(0).getNode(0).toString().equals("Modifiers()"))	{
							headWriter.p(n.getNode(0).getNode(0).getString(0) + " ");
						}
						headWriter.p(n.getNode(0).getNode(1).getNode(0).getString(0).toLowerCase() + " " + n.getNode(0).getString(3));
						headWriter.pln(")");
						headWriter.p("\t");
						headWriter.pln("{");
					}

					public void visitBlock(GNode n) { //visits the constructor block
						new Visitor() {

							public void visitExpressionStatement(GNode n) {
								System.out.println(n.toString());
								headWriter.p("\t");
				            	if (n.getNode(0).getName().equals("Expression")) { // checks if regular expression is being made
				            		headWriter.p("\t");

				            		new Visitor() {  // Visit assigned value if any

				            			public void visitExpression(GNode n) { 
				            				System.out.println(n.toString());
				            				if (n.getNode(2).getName().equals("AdditiveExpression")) {
				            					headWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getNode(0).getString(0) + " " + n.getNode(2).getString(1) + " " + n.getNode(2).getNode(2).getString(0));
				            				} else {
				            					headWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
				            				}
				            			}

										public void visitStringLiteral(GNode n) {
						                    headWriter.p(n.getString(0));
						                }

										public void visitIntegerLiteral(GNode n) {
											headWriter.p(n.getString(0));
										}

										public void visitFloatingPointLiteral(GNode n) {
											headWriter.p(n.getString(0));
						    	        }

						    	        public void visitCharacterLiteral(GNode n) {
											headWriter.p(n.getString(0));
						    	        }

						    	        public void visitBooleanLiteral(GNode n) {
											headWriter.p(n.getString(0));
						    	        }

						    	        public void visitNullLiteral(GNode n) {
											headWriter.p("null");
						    	        }

						                public void visitPrimaryIdentifier(GNode n) { 

							                headWriter.p(n.getString(0));
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
						    	        	headWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
						    	        //	if (n.getNode(0) != null) visit(n.getNode(0));
						    	        }
										public void visit(Node n){
											for (Object o : n) if(o instanceof Node) dispatch((Node)o);
										}
									}.dispatch(n);
									headWriter.pln(";");
				            	}
				            }

							public void visit(GNode n) {
		                		for (Object o : n) if(o instanceof Node) dispatch((Node)o);
		            		}
						}.dispatch(n);
						headWriter.p("\t");
						headWriter.pln("}");
					}

					public void visit(GNode n) {
                		for (Object o : n) if(o instanceof Node) dispatch((Node)o);
            		}
				}.dispatch(n);
			}

            /* To prevent printing local fields, do not visit methodDeclaration nodes. */
            public void visitMethodDeclaration(GNode n) { }

            public void visit(Node n){
                for (Object o : n) if(o instanceof Node) dispatch((Node)o);
            }
        }.dispatch(n);
		visit(n);
		headWriter.pln("};\n");
	}
	public void visitExtension(GNode n){
		//retrieve explicit extension 
		_super=n.getNode(0).getNode(0).getString(0); /**@var name of super class */
	}
	public void visitAdditiveExpression(GNode n){
	}
	public void visitBlock(GNode n){
		visit(n);
	}
	public void visitMethodDeclaration(GNode n){
		final GNode root=n;
		final String return_type=n.getNode(2).toString();
		try{
			method_return_type = n.getNode(2).getNode(0).getString(0);
		}
				catch (Exception e) {}
		new Visitor(){
			String fp="";
			int numTabs = 0;  // The number of tabs to include before a statement.
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

				// Method Return Types
				if(return_type.equals("VoidType()")) rType="void";
				else if(method_return_type.equals("String")) rType="string";
				else if(method_return_type.equals("Integer")) rType = "int";

				String hpp_prototype= rType +" "+ fp;
				// String cpp_prototype= rType+" "+cc_name+ "::" + fp+" {";
				String cpp_prototype= "int main() {";
				if(!className.equals(fileName.substring(0, 1).toUpperCase() + fileName.substring(1))) cpp_prototype = rType+" "+className+ "::" + fp+" {";
				//runtime.console().pln(cpp_prototype);
				//write function prototype to hpp file within struct <cc_name>
				// <return_type> <function_name>(arg[0]...arg[n]);

				headWriter.p("\t");  // Print a tab for method signatures in head file.
				headWriter.pln(hpp_prototype + ";");
				//write function prototype to cpp file
				// <return type> <class name> :: <function name> (arg[0]...arg[n]){

				impWriter.pln(cpp_prototype);

			}


			public void visitFieldDeclaration(GNode n) {  // Need to add visitMethodDeclaration() to visitor for advanced FieldDeclarations.
				System.out.println(n);
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
                if (n.getNode(1).getNode(0).getName().equals("QualifiedIdentifier")) {
                	impWriter.p(n.getNode(1).getNode(0).getString(0) + " ");
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

			public void visitReturnStatement(GNode n) {
				numTabs++;  // Increment the number of tabs to be printed before the statement.
				if (n.getNode(0) != null) {
					for (int x = 0; x < numTabs; x++) impWriter.p("\t");
					impWriter.p("return "); // print return keyword
				}
				numTabs--;  // Decrement the number of tabs to be printed before the statement.
				new Visitor() {  // Visit assigned value if any

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
			public void visit(Node n){
				for (Object o : n) if(o instanceof Node) dispatch((Node)o);
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
                count = 0;
                boolean isEndLine = false; // used to check if the print statement has an ln
                if (n.getNode(0).toString().contains("println")) isEndLine = true;
                if (n.getNode(0).getName().equals("CallExpression")) { // checks if a call expression is being made
					// String method_called = n.getNode(0).getNode(3).getNode(0).getString(2);
					// impWriter.p("\tprintf("+method_called+"()");
					impWriter.p("\tprintf(");
                    final ArrayList<String> vars = new ArrayList<String>();
                    new Visitor() {

                        public void visitSelectionExpression(GNode n) {
                        }

                        public void visitStringLiteral(GNode n) {
                            if (count > 0) {
                                impWriter.p(" + ");
                            }
                            else {
                                count++;
                            }
                            impWriter.p(n.getString(0));
                        }

                        public void visitIntegerLiteral(GNode n) {
                            if (count > 0) {
                                impWriter.p(" + ");
                            }
                            else {
                                count++;
                            }
                            impWriter.p(n.getString(0));
                        }

                        public void visitFloatingPointLiteral(GNode n) {
                        	if (count > 0) {
                                impWriter.p(" + ");
                            }
                            else {
                                count++;
                            }
                            impWriter.p(n.getString(0));
                        }

                        public void visitCharacterLiteral(GNode n) {
                            if (count > 0) {
                                impWriter.p(" + ");
                            }
                            else {
                                count++;
                            }
                            impWriter.p(n.getString(0));
                        }

                        public void visitBooleanLiteral(GNode n) {
                            if (count > 0) {
                                impWriter.p(" + ");
                            }
                            else {
                                count++;
                            }
                            impWriter.p(n.getString(0));
                        }

                        public void visitNullLiteral(GNode n) {
                            if (count > 0) {
                                impWriter.p(" + ");
                            }
                            else {
                                count++;
                            }
                            impWriter.p("null");
                        }

                        public void visitPrimaryIdentifier(GNode n) {
                            vars.add(n.getString(0));
                            if (count > 0) {
                                impWriter.p(" + ");
                            }
                            else {
                                count++;
                            }
                            impWriter.p("\"%s\"");
                        }

                        public void visitCallExpression(GNode n){
                        	// If a method is called
                        	try{
		                    	if(n.getNode(3).getNode(0).getString(2).isEmpty() == false){
		                    		String method_name = n.getNode(3).getNode(0).getString(2);
		                        	// If arguments are passed in to that method add them to the arraylist
		                        	if(n.getNode(3).getNode(0).getNode(3).isEmpty() == false){
		                        		ArrayList<String> arguments = new ArrayList<String>();
		                        		for(int i = 0; i < n.getNode(3).getNode(0).getNode(3).size(); i++){
		                        			arguments.add(n.getNode(3).getNode(0).getNode(3).getNode(i).getString(0));
		                        		}
		                        		String all_arguments = "";
		                        		for(String item : arguments){
		                        			all_arguments += item + ",";
		                        		}
		                        		impWriter.p(method_name+"("+all_arguments.substring(0,all_arguments.length()-1)+")");
		                        	}
		                    	}
		                    }
		                    catch(Exception e) {}
                        }

                        public void visit(GNode n) {
                            for (Object o : n) if (o instanceof Node) dispatch((Node) o);
                        }

                    }.dispatch(n.getNode(0));

                    if (isEndLine) {
                        impWriter.p(" + \"\\n\"");
                    }

                    if (!vars.isEmpty()){
                        for (String var : vars) {
                            impWriter.p(", " + "to_string(" + var + ")");
                        }
                    }
                    impWriter.pln(");");
            } else {
                    visit(n);
            }
        }
		}.dispatch(n);

		impWriter.pln("}\n");
		Node body = n.getNode(7);
		if (null != body) visit(body);
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
