package nyu.segfault;

import java.util.ArrayList;
import java.util.List;

import xtc.Constants;

import xtc.lang.JavaEntities;

import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;
import xtc.tree.Attribute;
import xtc.tree.Printer;
import xtc.util.SymbolTable;
import xtc.util.Runtime;
import xtc.type.*;

public class SymbolTableBuilder extends Visitor {
  SymbolTable table = new SymbolTable("Root");

  public SymbolTableBuilder(GNode node){
    buildSymbols(node);
  }

  public void buildSymbols(GNode node){
    new Visitor() {

      public void visitMethodDeclaration(GNode n){
        String name = n.getString(3)+ "Scope";
        table.enter(table.freshName(name));
        table.mark(n);
              visit(n);
              table.exit();            
            }
            public void visitFormalParameter(GNode n){
              String name = n.getString(3);
              String type = n.getNode(1).getNode(0).getString(0);
              table.current().addDefinition(name, type);
              visit(n);
            }
            public void visitFieldDeclaration(GNode n) {

                String type = n.getNode(1).getNode(0).getString(0);
                for (int i = 0 ; i < n.getNode(2).size(); i++ ){
                  String name = n.getNode(2).getNode(i).getString(0);
                  table.current().addDefinition(name, type);
                }               
                visit(n);
            } 


      public void visit(GNode n) {
                for (Object o : n) if (o instanceof GNode) dispatch((GNode)o);
            }
    }.dispatch(node);
  }

}
