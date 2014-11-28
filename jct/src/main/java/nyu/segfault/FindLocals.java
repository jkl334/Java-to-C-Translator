package nyu.segfault;

import xtc.lang.JavaEntities;

import xtc.Constants;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;
import xtc.tree.Attribute;
import xtc.tree.Printer;
import xtc.util.SymbolTable;
import xtc.util.SymbolTable.Scope;
import xtc.util.Runtime;
import xtc.type.*;

public class FindLocals extends Visitor {
  final private SymbolTable table;
  final private Runtime runtime;

  public FindLocals(final Runtime runtime, final SymbolTable table) {
    this.runtime = runtime;
    this.table = table;
  }

  public void visitCompilationUnit(GNode n) {
    if (null == n.get(0))
      visitPackageDeclaration(null);
    else
      dispatch(n.getNode(0));

    table.enter(n);
    runtime.console().p("Entered scope ").pln(table.current().getName()).flush();
    
    for (int i = 1; i < n.size(); i++) {
      GNode child = n.getGeneric(i);
      dispatch(child);
    }

    table.setScope(table.root());
  }
  
  public void visitPackageDeclaration(final GNode n) {
    table.enter(n);
    runtime.console().p("Entered scope ").pln(table.current().getName()).flush();
  }

  public void visitClassDeclaration(GNode n) {
    table.enter(n);
    runtime.console().p("Entered scope ").pln(table.current().getName());
    visit(n);
    table.exit();
  }

  public void visitMethodDeclaration(GNode n) {
    table.enter(n);
    runtime.console().p("Entered scope ").pln(table.current().getName());
    visit(n);
    table.exit();
  }

  public void visitBlock(GNode n) {
    table.enter(n);
    runtime.console().p("Entered scope ").pln(table.current().getName());
    visit(n);
    table.exit();
  }

  public void visitForStatement(GNode n) {
    table.enter(n);
    runtime.console().p("Entered scope ").pln(table.current().getName());
    visit(n);
    table.exit();
  }
      
  public void visitPrimaryIdentifier(final GNode n) {
    String name = n.getString(0);

    if (table.current().isDefined(name)) {
      Type type = (Type) table.current().lookup(name);
      if (JavaEntities.isLocalT(type))
        runtime.console().p("Found occurrence of local variable ").pln(name);
    }
  }

  public void visit(GNode n) {
    for (Object o : n) {
      if (o instanceof Node) dispatch((Node) o);
    }
  }

}