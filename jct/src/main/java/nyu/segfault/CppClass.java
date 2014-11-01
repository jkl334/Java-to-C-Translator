package nyu.segfault;

import java.util.ArrayList;

public class  CppClass{
	String Parent; /**@var parent node*/
	String className; /**@var name of class*/
	ArrayList<String> functionPtrs; /** function pointers*/

	public CppClass(String className){
		this.className=className;
	}
}
