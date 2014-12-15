package nyu.segfault;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import xtc.lang.JavaFiveParser;
import xtc.lang.JavaPrinter;
import xtc.lang.JavaAstSimplifier;

import xtc.parser.ParseException;
import xtc.parser.Result;

import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;
import xtc.tree.Printer;

import xtc.util.Tool;
import java.util.LinkedList;
import xtc.util.SymbolTable;

public class Translator extends Tool {

  public static String[] files; // an array used to store the files - args
  private static String file_name; /**@var name of input java source code */
  private static File impFile; /**@var cpp file */
  private static File headFile; /**@var hpp file */

  public Translator() {
    // Nothing to do.
  }

  public String getName() {
    return "SegFault - Java to C++ Translator";
  }

  public void init() {
    super.init();
  }

  public void prepare() {
    super.prepare();
  }

  public File locate(String name) throws IOException {
    File file = super.locate(name);
    if (Integer.MAX_VALUE < file.length()) {
      throw new IllegalArgumentException(file + ": file too large");
    }
    return file;
  }

  /**
   * set the file_name data field and create files
   */
  public static void setFileName(String fn){
    System.out.println("Translator.setFileName: " + fn);
    file_name=fn.replace(".java", "");
    try{
      impFile=new File(getFileName()+".cpp");
      headFile=new File(getFileName()+".hpp");
    } catch(Exception e) {
      System.out.println("setFileName: " + e);
    }
  }
  /**
   * get file_name
   * @return name of file
   */
  public static String getFileName(){
    return file_name;
  }

  public Node parse(Reader in, File file) throws IOException, ParseException {
    JavaFiveParser parser =
      new JavaFiveParser(in, file.toString(), (int)file.length());
    Result result = parser.pCompilationUnit(0);
    return (Node)parser.value(result);
  }

  public void process(Node node) {
    String fileArgument = files[0];
    String[] filePath = fileArgument.split("/");
    int indexOfFileName = filePath.length - 1;
    this.setFileName(filePath[indexOfFileName]);

    LinkedList<GNode> nodeList = new LinkedList<GNode>();
      nodeList.add((GNode)node);
      SegDependencyHandler dep = new SegDependencyHandler(nodeList);
      dep.makeAddressList();
      // Store dependencies on the AST
      nodeList = dep.makeNodeList();
      for (int i=0; i<nodeList.size();i++){
        System.out.println(" -> " + nodeList.get(i).getLocation().toString());
      }

      //Builds inheritance tree
      SegInheritanceBuilder inheritanceTree = new SegInheritanceBuilder(nodeList);
      //Writes VTables
      writeInheritance(inheritanceTree,  inheritanceTree.getNodeList());
      SymbolTable table = new SymbolTable();
      //Builds AST
      for (GNode listNode : nodeList){
        SegOverloadASTHelper oModifier = new SegOverloadASTHelper();
        oModifier.dispatch(listNode);
        LinkedList<String> overloadedNames = oModifier.getOverloadedList();
        //Builds the Symbol Table
        new SymbolTableBuilder(runtime, table).dispatch(listNode);
        new SegASTHelper().dispatch(listNode);
        new SegOverloading(table, inheritanceTree, overloadedNames).dispatch(listNode);
      }

        writeTree(nodeList, table, inheritanceTree);
  }

  private void writeInheritance(SegInheritanceBuilder inheritanceTree, LinkedList<GNode> nodeList){
    Writer outH = null;
    try {
      outH = new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream("output/" + headFile), "utf-8"));
      Printer pH = new Printer(outH);
      initOutputHFile(pH);
      for (GNode listNode : nodeList){
        //Runs SegTreePrinter on nodes
        LinkedList<GNode> listNodeTree = inheritanceTree.parseNodeToInheritance(listNode);
        for (GNode node : listNodeTree) {
          new SegTreePrinter(pH).dispatch(node);
        }
      }

    } catch (IOException ex){

    } finally {
       try {outH.close();} catch (Exception ex) {LOGGER.warning("IO Exception");}
    }
  }

  private void writeTree(LinkedList<GNode> nodeList,  SymbolTable table, SegInheritanceBuilder inh){
    Writer outCC = null;

    try {
      outCC = new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream("output/" + impFile), "utf-8"));
      Printer pCC = new Printer(outCC);

      initOutputCCFile(pCC);
      initMainFile(pCC);

      for (GNode listNode : nodeList){
        // most of the implementation printing done here
        LinkedList<String> staticMethods = inh.getStaticMethods(listNode);
        new SegImp(pCC, table, inh, staticMethods).dispatch(listNode);
      }

    } catch (IOException ex){

    } finally {
       try {outCC.close();} catch (Exception ex) {}
    }
  }

  private void initOutputCCFile(Printer p){
    p.pln("#include \"" + headFile + "\"");
    p.pln("#include <sstream>");
  }
  private void initOutputHFile(Printer p){
    p.pln("#pragma once");
    p.pln("#include <stdint.h>");
    p.pln("#include <string>");
    p.pln("#include \"java_lang.h\"");
    p.pln("");
    p.pln("using namespace java::lang;");
    p.pln("using namespace std;");
  }
  private void initMainFile(Printer p){
    p.pln("#include <iostream>");
    p.pln("#include \"java_lang.h\"");
    p.pln("#include \"" + headFile + "\"");
    p.pln("");
    p.pln("using namespace java::lang;");
    p.pln("using namespace std;");
    p.pln("");
  }

  public static void main(String[] args) {
  try {
    SegFaultLogger.setup();
    } catch (IOException e){
      e.printStackTrace();
    }
    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setLevel(Level.WARNING);
    LOGGER.addHandler(consoleHandler);
    Translator t = new Translator();
    t.files = args;
    t.run(args);
  }

}
