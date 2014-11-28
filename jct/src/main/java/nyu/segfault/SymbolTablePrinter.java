package nyu.segfault;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import xtc.lang.JavaEntities;

import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;
import xtc.tree.Attribute;
import xtc.tree.Printer;
import xtc.util.SymbolTable;
import xtc.util.SymbolTable.Scope;
import xtc.util.Runtime;
import xtc.type.Type;
import xtc.type.TypePrinter;

public class SymbolTablePrinter {
  final private SymbolTable table;
  final private Printer printer;
  final private TypePrinter typePrinter;

  public SymbolTablePrinter(final Printer printer, final SymbolTable table) {
    this.printer = printer;
    this.table = table;
    typePrinter = new TypePrinter(printer);
  }

  public void print() {
    Scope curr = table.current();
    
    printer.indent();
    printer.p("scope: ").pln(curr.getName());
    printer.incr();

    // print symbols in current scope
    for (Iterator<String> iter = curr.symbols(); iter.hasNext(); ) {
      String sym = iter.next();
      printer.indent();

      printer.p("symbol: ").p(sym);

      Object value = curr.lookupLocally(sym);
      if (null == value) continue;

      if (value instanceof Type) {
        printer.p(" -> ");
        printer.incr();
        typePrinter.dispatch((Type) value);
        printer.decr();
        printer.pln();
      }
      printer.flush();
    }

    // recurse into all nested scopes of all current scopes
    for (Iterator<String> iter = curr.nested(); iter.hasNext(); ) {
      table.enter(iter.next());
      print();
      table.exit();
    }

    printer.decr();
  }

}
