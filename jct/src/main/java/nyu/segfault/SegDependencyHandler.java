package nyu.segfault;
;
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


/* Handles dependency checking and adding */

public class SegDependencyHandler extends Tool {

  LinkedList<GNode> dependancyList = new LinkedList<GNode>();

  public SegDependencyHandler(LinkedList<GNode> ll){
    dependancyList = ll;
  }

  public String getName(){
    return "SegDependencyHandler";
  }

  public LinkedList<GNode> makeNodeList() {
    return dependancyList;
  }

  public GNode parse(Reader in, File file) throws IOException, ParseException {
    JavaFiveParser parser =
      new JavaFiveParser(in, file.toString(), (int)file.length());
    Result result = parser.pCompilationUnit(0);
    return (GNode)parser.value(result);
  }

    // Fills list the addresses of the dependencies
  public void makeAddressList() {
    for (int i = 0; i < dependancyList.size(); i++) {
      if (dependancyList.get(i) != null) {
        // Looping through the dependencies
        new Visitor() {

          public void visitPackageDeclaration(GNode n) {
            String rootDir = System.getProperty("user.dir") + "/examples";
            String packageName;
            String path = getRelativePath(n);
            packageName = path.substring(1).replace("/",".");
            processDirectory(rootDir + path, packageName);
          }

          public void visitImportDeclaration(GNode n) {
            //Visiting package declarations"
            if (n.getString(2) == null){
            //Importing a file
              String path = getNodeLoc(n) + getRelativePath(n);
              File file = new File(path + ".java");
              processFile(file);
            }
            else {
              //Importing an entire folder
              String path = "examples/" + getRelativePath(n);

              processDirectory(path);
            }
          }

          public void visit(Node n) {
            for (Object o : n) if (o instanceof Node) dispatch((Node) o);
          }

        }.dispatch(dependancyList.get(i));
      }
    }
  }

  /* Returns the name of the packge */
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

  /* Returns the path of the package or import statement */
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

  private void processDirectory(String path){
    processDirectory(path, "");
  }

  /* Gets all the files in the directory and retrieves their Java AST's and then 
  run processNode on it  */
  private void processDirectory(String path, String packageName){
    File folder = new File(path);
    File[] files = folder.listFiles();

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
        }
        catch (ParseException e){
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
        }
        catch (ParseException e){
        }
  }

  /* Check to see if the node is already in the list. if not then add it */
  private void processNode(GNode n){
    if (!dependancyList.contains(n)){
      dependancyList.add(n);
    }
  }
}
