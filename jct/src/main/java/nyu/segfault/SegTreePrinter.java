package nyu.segfault;

import java.lang.*;

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


public class SegTreePrinter extends Visitor {
  protected Printer printer;
  public GNode root;
  public GNode dataLayout;

  private String packageName;
  private String className;
  private String javaClassName;
  private boolean isFirstVTMethod = true;

  public SegTreePrinter(Printer p){
    this.printer = p;
    printer.register(this);
  }

  public void visitHeaderDeclaration(GNode n){
    packageName = n.getString(0); 
    if (packageName != null){
      printer.pln("namespace " + n.getString(0) + " {");
      className = n.getString(1);
      visit(n);
      printer.pln("}").pln();
    }
    else{
      className = n.getString(1);
      visit(n);
    }
  }

  public void visitDataLayout(GNode n){
    dataLayout = n;
    printer.pln("struct __" + className + ";");
    printer.p("struct __" + className + "_VT;").pln();
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
    printer.pln("__" + n.getString(0) + "();");
  }

  public void visitDataLayoutMethodDeclaration(GNode n){
          if (!(n.get(0) == null)) printer.p(n.getNode(0));
          if (!(n.get(1) == null)) printer.p(n.getString(1)).p(" ");
    String methodName = n.getString(2);
    printer.p(methodName);
    printer.p("(");
    
    printer.p(n.getNode(4));
    if ((n.getNode(4).size()==0) && !(n.getString(2).equals("__class"))){
        printer.p(className);
      }
    
          printer.pln(");");
  }

public void visitVTable(GNode n){
    printer.pln("struct __" + className + "_VT {");
    printer.pln("Class __isa;");

    new Visitor(){
      public void visitDataLayoutMethodDeclaration(GNode n){
        if(!(n.getString(2).equals("__class"))){
          if (n.getString(1) != null) {
            printer.p(n.getString(1) + " (*");
          }
          else {
            printer.p("void" + " (*");
          }
          printer.p(n.getString(2));
          printer.p(")(");
          //printer.p(className);
          printer.p(n.getNode(4));
          if ((n.getNode(4).size()==0) && !(n.getString(2).equals("__class"))){
            printer.p(className);
          }
          printer.p(");");
          printer.pln();
        }
      }

      public void visit(Node n) {
      for (Object o : n) if (o instanceof Node) dispatch((Node) o);
    }
    }.dispatch(dataLayout);
    printer.pln("__" + className + "_VT()");
    printer.p(": ");
    visit(n);
    printer.pln("{}");
    printer.pln("};");
    printer.pln();
  }



  public void visitVTableMethodDeclaration(GNode n){
    if (isFirstVTMethod) {
      printer.p("__isa(__" + className + "::__class())");
      isFirstVTMethod = false;
    }
    else{
      printer.p(",");
      printer.pln();
      printer.p(n.getString(2)).p("(");
      if (n.get(1) != null) {printer.p("(").p(n.getString(1)).p("(*)(").p(n.getNode(4));}
      if (n.getNode(4).size()==0){
        printer.p(className);
      }
      printer.p("))");
      printer.p(" &__" + n.getString(3) + "::" + n.getString(2));
      printer.p(")");
    }
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
