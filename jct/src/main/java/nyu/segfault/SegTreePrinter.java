package nyu.segfault;

import java.lang.*;

/* Imports based on src/xtc/lang/CPrinter.java */
import java.util.Iterator;

import java.util.LinkedList;
import xtc.tree.LineMarker;
import xtc.tree.Node;
import xtc.tree.GNode;
import xtc.tree.Pragma;
import xtc.tree.Printer;
import xtc.tree.SourceIdentity;
import xtc.tree.Token;
import xtc.tree.Visitor;
/* End imports based on src/xtc/lang/CPrinter.java */

public class SegTreePrinter extends Visitor {
  private static final boolean VERBOSE = true;
	/** The printer for this C printer. */
  protected Printer printer;
  public GNode root;
  public GNode dataLayout;

  private String packageName;
  private String className;
  private String javaClassName;

	public SegTreePrinter(Printer p){
    this.printer = p;
    printer.register(this);
	}

  public void visitHeaderDeclaration(GNode n){

  	printer.pln("namespace " + n.getString(0) + " {");
  	className = n.getString(1);
  	visit(n);
  	printer.pln("}").pln();
  }

  public void visitDataLayout(GNode n){
  	dataLayout = n;
  	printer.pln("struct __" + className + ";");
  	printer.p("struct __" + className + "_VT;").pln(); //
  	printer.p("typedef __" + className + "* " + className + ";").pln();
  	printer.p("struct __" + className + " {").pln();
  	visit(n);
  	printer.pln("};").pln();
  }

  public void visitFieldDeclaration(GNode n){
  	visit(n);
  	printer.p(n.getString(1)).p(" ").p(n.getString(2));
  	printer.pln(";");
  }

  public void visitConstructorDeclaration(GNode n){
  	printer.pln(n.getString(0) + "();");
  }

  public void visitMethodDeclaration(GNode n){
  	if (!(n.get(0) == null)) printer.p(n.getNode(0));
  	if (!(n.get(1) == null)) printer.p(n.getString(1)).p(" ");
  	printer.p(n.getString(2));
  	printer.p("(");
		visit(n.getNode(4));
  	printer.pln(");");

  }

  public void visitVTable(GNode n){
  	printer.pln("struct __" + className + "_VT {");
  	printer.pln("Class __isa;");

  	new Visitor(){
  		public void visitMethodDeclaration(GNode n){
  			if(!(n.getString(2).equals("__class"))){
  				printer.p(n.getString(1) + " (*");
  				printer.p(n.getString(2));
  				printer.p(")(");
  				printer.p(className);
  				printer.p(");");
  				printer.pln();
  			}
  		}
  		  public void visit(Node n) {
		    for (Object o : n) if (o instanceof Node) dispatch((Node) o);
		  }
  	}.dispatch(dataLayout);

    visit(n);
  	printer.pln("};");
  	printer.pln();
  }

  public void visitModifiers(GNode n){
  	if (n.size() == 1) printer.p(n.getString(0)).p(" ");
  }
  public void visitParameters(GNode n){
  	if (n.size() == 1) printer.p(n.getString(0)).p(" ");
  }

  public void visit(Node n) {
    for (Object o : n) if (o instanceof Node) dispatch((Node) o);
  }

}
