package nyu.segfault;

import java.io.*;
import java.util.*;

import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.util.SymbolTable;
import xtc.util.Tool;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;
	
public class SegHelper {
	
	/**
	 * extract class name from node
	 * @param node node from java parse tree
	 * @return formated String represented class name
	 */
	public static  String getClassName(GNode n){ 
		validCall();
		return n.getString(1); 
	}
	/**
	 * extract class declaration from node
	 * @param node node from java parse tree
	 * @return formated string  c++ struct declaration
	 */
	public static  String getClassDeclaration(GNode n){
		validCall();
		return  "struct "+ n.getString(1);
	}
	/**
	 * extract method declaration from node
	 * @param node node from java parse tree
	 * @return formated function prototype c++
	 */
	public static String getMethodDeclaration(GNode n,String className){
		validCall();
		final StackTraceElements s=Thread.currentThread.getStackTrace();
		if((s[1].getClassName().equals("SegHead")) || (s[1].getClassName().equals("SegImp"))
			||(s[1].getClassName().equals("SegCVT"))){
			
			String return_type=j2c(n.getNode(2).toString());
			String fp=n.getString(3)+"("; 
			if( n.size() == 0 ) fp+=")";
			else fp+=getFormalParameters(n.getNode(4)); //replace with correct child index


			if(stack[1].getClassName.equals("SegHead"))
				return return_type+" "+fp;

			else if((stack[1].getClassName.equals("SegImp")) || stack[1].getClassName.equals("SegCVT"))
				return return_type+" "+className+"::"+fp;

			else if((stack[1].getClassName.equals("SegHVT"))){
				//construct function pointers
				// construct vtable 
			}
		}
	}
	private static  String getFormalParameters(GNode n){
		for(int i=0; i< n.size(); i++){
			Node fparam=n.getNode(i);

			//retrieve argument type
			fp+= j2c(fparam.getNode(1).getNode(0).getString(0))+" ";

			//retrieve argument name
			fp+=fparam.getString(3);

			if(i+1 < n.size()) fp+=",";
			else fp+=")";
		}

	}
	private static String j2c(String jType){
		String cType;
		if (jType.equals("String")) cType="string";
		else if(jType.equals("VoidType()")) cType="void";
		else if (jType.equals("Integer")) cType="int";

		return cType;
	}
	/**
	 *  check if visit object methods calls appropriate Helper function
	 */
	private static  void  validCall(){
		final int x=2; final int y=1;
		final StackTraceElements ste=Thread.currentThread.getStackTrace();

		sh_yfunc=ste[y].getMethodName();
		sh_xfunc=ste[x].getMethodName();

		final String[] comp=new String[]{"Class","Method"};
		
		for (int k=0;k<comp.length;k++) {
			if(sh_xfunc.contains(comp[0]) &&  sh_yfunc.contains(comp[0])) return; 
		}
		throw new RuntimeException("visit function  to not correspond to helper function");
	}
}
