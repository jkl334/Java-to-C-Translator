package nyu.segfault;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import javax.swing.tree.DefaultTreeModel;

import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.util.SymbolTable;
import xtc.util.Tool;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;

public class VisitFormalParameters extends Visitor {

	private Printer impWriter;
	private Printer headWriter;

	public VisitFormalParameters(Printer impWriter, Printer headWriter) {
		this.impWriter = impWriter;
		this.headWriter = headWriter;

	}

	public void visitFormalParameters(GNode n){
		fp+=root.getString(3)+"(";
		if( n.size() == 0 ) fp+=")";
		ArrayList<String> arg_types=new ArrayList<String>();
		for(int i=0; i< n.size(); i++){
			Node fparam=n.getNode(i);

			//retrieve argument type
			fp+= j2c(fparam.getNode(1).getNode(0).getString(0))+" ";
			arg_types.add(fparam.getNode(1).getNode(0).getString(0)+" ");					

			//retrieve argument name
			fp+=fparam.getString(3);

			if(i+1 < n.size()) fp+=",";
			else fp+=")";
		}
		String rType="";

		// Method Return Types
		if(return_type.equals("VoidType()")) rType="void";
		else if(method_return_type.equals("String")) rType="string";
		else if(method_return_type.equals("Integer")) rType = "int";

		/**
		 * generate the function ptr in struct <class_name>_VT
		 * <return_type> (*function name)(arg_type 1, arg_type 2)
		 */

		String function_ptr=rType+"(*"+root.getString(3)+")";
		if(arg_types.size() == 0) function_ptr+="()";
		else{
			for(int k=0; k< arg_types.size(); k++){
				function_ptr+=arg_types.get(k);
				if(k < arg_types.size() -1) function_ptr+=",";
			}
		}
//				method_VT_buffer.add(function_ptr);
		String hpp_prototype= rType +" "+ fp;
		// String cpp_prototype= rType+" "+cc_name+ "::" + fp+" {";
		String cpp_prototype= "int main() {";
		if(!className.equals(fileName.substring(0, 1).toUpperCase() + fileName.substring(1))) cpp_prototype = rType+" "+className+ "::" + fp+" {";
		
		System.out.println(root.getString(3));
		if(!root.getString(3).equals("main")){
			//write function prototype to hpp file within struct <cc_name>
			// <return_type> <function_name>(arg[0]...arg[n]);

			/* Add the method signature to the correct section of the header. */
			if (isPrivate) {
				privateHPPmethods.add(hpp_prototype);
			} else {
				publicHPPmethods.add(hpp_prototype);
			}
		}
		//write function prototype to cpp file
		// <return type> <class name> :: <function name> (arg[0]...arg[n]){

		impWriter.pln(cpp_prototype);
	}
}