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
  final private SymbolTable table;
  final private Runtime runtime;

  public SymbolTableBuilder(final Runtime runtime, final SymbolTable table) {
    this.runtime = runtime;
    this.table = table;
  }
      
  public void visitCompilationUnit(GNode n) {
    if (null == n.get(0))
      visitPackageDeclaration(null);
    else
      dispatch(n.getNode(0));

    table.enter(JavaEntities.fileNameToScopeName(n.getLocation().file));
    table.mark(n);
    
    for (int i = 1; i < n.size(); i++) {
      GNode child = n.getGeneric(i);
      dispatch(child);
    }
    
    table.setScope(table.root());
  }

  public void visitPackageDeclaration(final GNode n) {
    String canonicalName = null == n ? "" : (String) dispatch(n.getNode(1));
    //PackageT result = JavaEntities.canonicalNameToPackage(table, canonicalName);
    table.enter(JavaEntities.packageNameToScopeName(canonicalName));
    if (n != null) table.mark(n);
  }
  
  public void visitClassDeclaration(GNode n) {
    String className = n.getString(1);
    table.enter(className);
    table.mark(n);
    visit(n);
    table.exit();
  }

  public void visitMethodDeclaration(GNode n) {
    String methodName = JavaEntities.methodSymbolFromAst(n);
    table.enter(methodName);
    table.mark(n);
    visit(n);
    table.exit();
  }

  public void visitBlock(GNode n) {
    table.enter(table.freshName("block"));
    table.mark(n);
    visit(n);
    table.exit();
  }

  public void visitForStatement(GNode n) {
    table.enter(table.freshName("forStatement"));
    table.mark(n);
    visit(n);
    table.exit();
  }
      
  public void visit(GNode n) {
    for (Object o : n) {
      if (o instanceof Node) dispatch((Node) o);
    }
  }

  public final List<Type> visitFieldDeclaration(final GNode n) {
    @SuppressWarnings("unchecked")
    final List<Attribute> modifiers = (List<Attribute>) dispatch(n.getNode(0));
    Type type = (Type) dispatch(n.getNode(1));
    return processDeclarators(modifiers, type, n.getGeneric(2));
  }
      
  /** Visit a Modifiers = Modifier*. */
  public final List<Attribute> visitModifiers(final GNode n) {
    final List<Attribute> result = new ArrayList<Attribute>();
    for (int i = 0; i < n.size(); i++) {
      final String name = n.getGeneric(i).getString(0);
      final Attribute modifier = JavaEntities.nameToModifier(name);
      if (null == modifier)
        runtime.error("unexpected modifier " + name, n);
      else if (result.contains(modifier))
        runtime.error("duplicate modifier " + name, n);
      else
        result.add(modifier);
    }
    return result;
  }

  public final List<Type> processDeclarators(final List<Attribute> modifiers,
                                             final Type type, final GNode declarators) {
    final List<Type> result = new ArrayList<Type>();
    boolean isLocal = JavaEntities.isScopeLocal(table.current().getQualifiedName());
    for (final Object i : declarators) {
      GNode declNode = (GNode) i;
      String name = declNode.getString(0);
      Type dimType = JavaEntities.typeWithDimensions(type, 
                           countDimensions(declNode.getGeneric(1)));
      Type entity = isLocal ? VariableT.newLocal(dimType, name) : 
                              VariableT.newField(dimType, name);
      for (Attribute mod : modifiers)
        entity.addAttribute(mod);
      if (null == table.current().lookupLocally(name)) {
        result.add(entity);
        table.current().define(name, entity);
        //entity.scope(table.current().getQualifiedName());
      }
    }
    return result;
  }

  public final Type visitFormalParameter(final GNode n) {
    assert null == n.get(4) : "must run JavaAstSimplifier first";
    String id = n.getString(3);
    Type dispatched = (Type) dispatch(n.getNode(1));
    Type result = VariableT.newParam(dispatched, id);
    if (n.getGeneric(0).size() != 0)
      result.addAttribute(JavaEntities.nameToModifier("final"));
    if (null == table.current().lookupLocally(id)) {
      table.current().define(id, result);
      result.scope(table.current().getQualifiedName());
    } else
      runtime.error("duplicate parameter declaration " + id, n);
    assert JavaEntities.isParameterT(result);
    return result;
  }

  public final Type visitPrimitiveType(final GNode n) {
    final Type result = JavaEntities.nameToBaseType(n.getString(0));
    return result;
  }

  public final Type visitType(final GNode n) {
    final boolean composite = n.getGeneric(0).hasName("QualifiedIdentifier");
    final Object dispatched0 = dispatch(n.getNode(0));
    assert dispatched0 != null;
    final Type componentT = composite ? new AliasT((String) dispatched0) : (Type) dispatched0;
    final int dimensions = countDimensions(n.getGeneric(1));
    final Type result = JavaEntities.typeWithDimensions(componentT, dimensions);
    return result;
  }

  public static int countDimensions(final GNode dimNode) {
    return null == dimNode ? 0 : dimNode.size();
  }

  public final Type visitVoidType(final GNode n) {
    return JavaEntities.nameToBaseType("void");
  }

  public final String visitQualifiedIdentifier(final GNode n) {
    final StringBuffer b = new StringBuffer();
    for (int i = 0; i < n.size(); i++) {
      if (b.length() > 0)
        b.append(Constants.QUALIFIER);
      b.append(n.getString(i));
    }
    return b.toString();
  }

}
