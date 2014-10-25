package nyu.segfault;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;


import xtc.lang.*;
import xtc.parser.*;
import xtc.tree.*;
import xtc.util.*;

import xtc.tree.Visitor;

public class  SegNodeTest{

	//checks SegNodeTree operations  one level in
	
	public static SegFaultVisitor sfv;
	public static String[] data;

	@BeforeClass
	public static void setup(){
		String [] files={""};
		sfv=new SegFaultVisitor(files);
		data=new String[]{"segfault","check","alvin","corey","donato","jeff"};
		for(int i=0; i< data.length; i++)
			sfv.inhTree.addChild(data[i]);
	}
	@Test
	public void segNodeAddChild(){
		for(int i=0; i< data.length; i++)
			assertEquals(sfv.inhTree.children.get(i).data,data[i]);
	}
	@Test
	public void segNodeDFS(){
		for(String s : data){
			assertTrue((sfv.inhTree.dfs(sfv.inhTree, s)) != null);
			assertEquals(sfv.inhTree.dfs(sfv.inhTree,s).data,s);
		}
	}
}
