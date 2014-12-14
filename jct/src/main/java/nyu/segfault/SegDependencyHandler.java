package nyu.segfault;

import java.lang.*;
import java.io.IOException;
import xtc.parser.ParseException;
import xtc.lang.JavaFiveParser;
import xtc.parser.Result;
import java.io.File;
import java.io.Reader;

import java.util.Iterator;

import xtc.tree.LineMarker;
import xtc.tree.Node;
import xtc.tree.GNode;
import xtc.tree.Pragma;
import xtc.tree.Printer;
import xtc.tree.SourceIdentity;
import xtc.tree.Token;
import xtc.tree.Visitor;
import xtc.util.Tool;

import java.util.LinkedList;

import java.util.logging.Logger;
import java.util.logging.Level;

/* Handles dependency checking and adding */

public class SegDependencyHandler extends Tool {

  private final static Logger LOGGER = Logger.getLogger(SegDependencyHandler.class .getName());

  LinkedList<GNode> depList = new LinkedList<GNode>();

  public SegDependencyHandler(LinkedList<GNode> ll){
    depList = ll;
  }

  public String getName(){
    return "SegDependencyHandler";
  }

  /* fills the addresslist with the addresses of the dependencies */
  public void makeAddressList() {
    LOGGER.setLevel(Level.INFO);
    for (int i = 0; i < depList.size(); i++) {
      if (depList.get(i) != null) {
        LOGGER.info("Looping through the dependencies");
        new Visitor() {
          /* visit the package and get all the files and then their nodes */
          public void visitPackageDeclaration(GNode n) {
            String rootDir = System.getProperty("user.dir") + "/examples";
            String packageName;
            String path = getRelativePath(n);
            packageName = path.substring(1).replace("/",".");
            LOGGER.info("Package name: " + packageName);
            LOGGER.info("Package path: " + rootDir + path);

            processDirectory(rootDir + path, packageName);

            /* Pass a path to processPath() */

          }
          /* Visit the import statements and either get a single file or
             an entire folder */
          public void visitImportDeclaration(GNode n) {
            LOGGER.setLevel(Level.INFO);
            LOGGER.info("Visiting import declaration");
            if (n.getString(2) == null){
            LOGGER.info("Importing a file");
              String path = getNodeLoc(n) + getRelativePath(n);
              File file = new File(path + ".java");
              LOGGER.info("got file " + file.getAbsoluteFile());
              processFile(file);
            }
            else {
              LOGGER.info("Importing an entire folder");
              String path = "examples/" + getRelativePath(n);

              processDirectory(path);
            }
          }

          public void visit(Node n) {
            for (Object o : n) if (o instanceof Node) dispatch((Node) o);
          }

        }.dispatch(depList.get(i));
      }
    }
  }

  public LinkedList<GNode> makeNodeList() {
    return depList;
  }

  public GNode parse(Reader in, File file) throws IOException, ParseException {
    JavaFiveParser parser =
      new JavaFiveParser(in, file.toString(), (int)file.length());
    Result result = parser.pCompilationUnit(0);
    return (GNode)parser.value(result);
  }

  /* Returns the name of the packge given the package declaration GNode */
  private String getPackageName(GNode n){
    GNode qualId = (GNode)n.getNode(1);
    String name = "";
    for (int i = 0; i<qualId.size(); i++){
      if (i > 1){
        name += ".";
      }
      name += qualId.get(i).toString();
    }
    return name;
  }

  /* Returns the path of the package or import statement given a GNode */
  private String getRelativePath(GNode n){
    String path = "";
    Node qualId = n.getNode(1);
    for (int i = 0; i<qualId.size(); i++){
      path += "/" + qualId.get(i).toString();
    }
    return path;
  }

  /* Gets the filepath of the node */
  private String getNodeLoc(GNode n){
    String nodeLoc = n.getLocation().toString();
    nodeLoc = nodeLoc.substring(0, nodeLoc.lastIndexOf("/"));
    return nodeLoc;
  }

  /* Calls processDirectory with an empty
     package declaration to import an entire folder */
  private void processDirectory(String path){
    processDirectory(path, "");
  }

  /* Get all the files in the directory and obtain their Java AST's  and then
     put them in the deplist by calling processNode(node)*/
  private void processDirectory(String path, String packageName){
    File folder = new File(path);
    File[] files = folder.listFiles();
    if (files == null) {LOGGER.warning("Found no files in directory path");}
    LOGGER.info("Scanning for java files in " + folder.toString());

    for (int i = 0; i < files.length; i++) {
      if (files[i].isFile() && files[i].getName().endsWith(".java")) {
        try {
          Reader in = runtime.getReader(files[i]);
          GNode node = parse(in, files[i]);
          if (node.getNode(0) != null){
            if (packageName.length() > 0) {
              if (getPackageName((GNode)node.getNode(0)).equals(packageName)){
                processNode(node);
              }
            }
            else{
              processNode(node);
            }
          }
        }
        catch (IOException e){
          LOGGER.warning("IO Exception on " + files[i].getPath());
        }
        catch (ParseException e){
          LOGGER.warning("Parse Exception");
        }
      }
    }
  }

  /* Used for a single import file */
  private void processFile(File file){
    try {
          Reader in = runtime.getReader(file);
          GNode node = parse(in, file);
          processNode(node);
        }
        catch (IOException e){
          LOGGER.warning("IO Exception on " + file.getPath());
        }
        catch (ParseException e){
          LOGGER.warning("Parse Exception");
        }
  }

  /* Check to see if the node is already in the list. if not then add it */
  private void processNode(GNode n){
    if (!depList.contains(n)){
      depList.add(n);
    }
  }
}
