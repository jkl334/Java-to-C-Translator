package nyu.segfault;

import java.util.ArrayList;

public class  CppClass{
	public String Parent; /**@var parent node*/
	public String className; /**@var name of class*/
	public ArrayList<String> functionPtrs; /** function pointers*/

	/**
	 * constructor
	 * @param class_name
	 */
	public CppClass(String className){
		this.className=className;
	}
	/**
	 * equals method between to CppClass objects
	 * @return true if both objects have same className
	 */
	public  boolean equals(CppClass other){
		return (this.className.equals(other.className));
	}
}
