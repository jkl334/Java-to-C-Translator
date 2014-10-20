package nyu.segfault;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import java.util.ArrayList;

import xtc.lang.JavaFiveParser;

import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.tree.ParseTreeStripper;

import xtc.tree.Annotation;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;
import xtc.type.AST;
import xtc.type.JavaAST;
import xtc.type.VariantT;
import xtc.type.TupleT;

/**
 * A tool to generate c++ header files (.hpp)
 */



public class HeaderAST extends xtc.util.Tool {

 int counter=0;
 final ArrayList<JavaAST> class_arr=new ArrayList<JavaAST>();
  
  public interface ICommand {
    public void run();
  }

  @Override
  public void init(){

  }
  @Override
  public String getName() {
    return "HeaderAST";
  }

  public File locate(String name) throws IOException {
    File file = super.locate(name);
    if (Integer.MAX_VALUE < file.length()) {
      throw new IllegalArgumentException(file + ": file too large");
    }
    return file;
  }

  public Node parse(Reader in, File file) throws IOException, ParseException {
    JavaFiveParser parser =
      new JavaFiveParser(in, file.toString(), (int)file.length());
    Result result = parser.pCompilationUnit(0);
    return (Node)parser.value(result);
  }

  public void process(Node node) {
    new Visitor() {
	private int count = 0;
         public void visitCompilationUnit(GNode n) {
        visit(n);
        runtime.console().flush();
      }

      public void visitClassDeclaration(GNode n) {
		ParseTreeStripper pts=new ParseTreeStripper();
		GNode naked=pts.visit(n);
		//runtime.console().pln(naked.toString());	 

		//class_arr.add(new JavaAST());
		//counter++;
      }
      public void visitConstructorDeclaration(GNode n) {
     		//runtime.console().pln(n.toString()); 
      }
      public void visitMethodDeclaration(GNode n) {
	      //runtime.console().pln(n.toString());
      }
      public void visit(Node n) {
        for (Object o : n) {
          // The scope belongs to the for loop!
          if (o instanceof Node) dispatch((Node) o);
        }
      }
      
    }.dispatch(node);
  }
  /**
   * Run the tool with the specified command line arguments.
   *
   * @param args The command line arguments.
   */
  public static void main(String[] args) {
    new HeaderAST().run(args);
  }

}
