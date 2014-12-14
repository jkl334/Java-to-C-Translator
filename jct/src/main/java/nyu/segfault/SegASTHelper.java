package nyu.segfault;

import java.lang.*;

import java.util.Iterator;

import xtc.tree.LineMarker;
import xtc.tree.Node;
import xtc.tree.GNode;
import xtc.tree.Pragma;
import xtc.tree.Printer;
import xtc.tree.SourceIdentity;
import xtc.tree.Token;
import xtc.tree.Visitor;

public class SegASTHelper extends Visitor {

  // Based of XTC's CPrinter
  public String className,val,sec_val,third_val;

  public SegASTHelper(){
  }

  public String getName(){
    return className;
  }

  public void visitCompilationUnit(GNode n){
    visit(n);
  }

  public void visitPrimitiveType(GNode n){
    if(n.getString(0).equals("int")){
      n.set(0,"int32_t");
    }
    else if(n.getString(0).equals("byte")){
      n.set(0,"unsigned char");
    }
  }

  public void visitClassDeclaration(GNode n){
    className = n.getString(1);
    n.set(1, "__" + className);
    n.set(3, null);
    visit(n);
  }

  public void visitPackageDeclaration(GNode n){
    visit(n);
  }
  public void visitImportDeclaration(GNode n){
    visit(n);
  }

  public void visitClassBody(GNode n){
    visit(n);
    for (int i = 0; i < n.size(); i++){
      if(n.getNode(i).hasName("FieldDeclaration")){
        GNode fieldDec = GNode.create("fieldDec");
        GNode type = GNode.create("type");
        GNode dec = GNode.create("dec");
        String left_side = n.getNode(i).getNode(1).getNode(0).getString(0);
        String right_side = n.getNode(i).getNode(2).getNode(0).getString(0);
        type.add(left_side);
        dec.add(right_side);
        fieldDec.add(type);
        fieldDec.add(dec);
        n.set(i,fieldDec);
      }
    }
  }

  public void visitConstructorDeclaration(GNode n){
    // Modify Constructor
    n.set(2, "__" + n.getString(2));
    GNode conBlock = GNode.create("conBlock");
    GNode conExpStatement = GNode.create("ConstructorExpression");
    for (int i = 0; i< n.getNode(5).size(); i++){
      if(n.getNode(5).getNode(i).hasName("ExpressionStatement")){
        String left_side = n.getNode(5).getNode(i).getNode(0).getNode(0).getString(0);
        String right_side = n.getNode(5).getNode(i).getNode(0).getNode(2).getString(0);
        conExpStatement.add(left_side);
        conExpStatement.add(right_side);
        conBlock.add(conExpStatement);
      }
    }
    n.set(5,conBlock);
    visit(n);
  }

  public void visitMethodDeclaration(GNode n){
    if (n.getNode(4).size() == 0) {n.set(5, className);}
    visit(n);
  }

  public void visitCallExpression(GNode n){
    visit(n);
  }

  public void visitExpressionStatement(GNode n){
    if (n.getNode(0).hasName("CallExpression")){
      if (n.getNode(0).getString(2).equals("println")){
        GNode cout = GNode.create("CoutExpression");
        cout.add("Cout");
        cout.add(n.getNode(0).getNode(3));
        n.set(0, cout);
      }
    }
    visit(n);
  }

  public void visitCoutExpression(GNode n){
    GNode coutArgs = GNode.create("CoutArguments");
    for (int i = 0; i < n.getNode(1).size(); i++){
      coutArgs.add(n.getNode(1).getNode(i));
    }
    n.set(1, coutArgs);
    visit(n);
  }

  public void visitCoutArguments(GNode n){
    if (n.getNode(0).hasName("AdditiveExpression")){
      GNode coutAdd = GNode.create("CoutAdditiveExpression");
      coutAdd.add(n.getNode(0).getNode(0));
      coutAdd.add(n.getNode(0).getString(1));
      coutAdd.add(n.getNode(0).getNode(2));
      n.set(0,coutAdd);
    }
    else if(n.getNode(0).hasName("CallExpression")){
      GNode coutCall = GNode.create("CoutCallExpression");
      coutCall.add(n.getNode(0).getNode(0));
      coutCall.add(n.getNode(0).getString(2));
      coutCall.add(n.getNode(0).getNode(3));
      n.set(0,coutCall);
    }
    visit(n);
  }

  public void visitCoutAdditiveExpression(GNode n){
    if (n.getNode(0).hasName("AdditiveExpression")){
      GNode coutAdd = GNode.create("CoutAdditiveExpression");
      coutAdd.add(n.getNode(0).getNode(0));
      coutAdd.add(n.getNode(0).getString(1));
      coutAdd.add(n.getNode(0).getNode(2));
      n.set(0,coutAdd);
    }
    visit(n);
  }

  public void visitBlock(GNode n){
    visit(n);
  }

  public void visitFieldDeclaration(GNode n){
    visit(n);
  }

  public void visitDeclarator(GNode n){

    visit(n);
  }

  public void visit(Node n) {
    for (Object o : n) if (o instanceof Node) dispatch((Node) o);
  }
}
