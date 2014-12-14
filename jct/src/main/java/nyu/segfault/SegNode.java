package nyu.segfault;

import java.util.ArrayList;

/**
 * nonbinary unbalanced tree
 * pre-order traversal
 */
public class SegNode<T>{
	public T data;
	public SegNode<T> parent;
	public ArrayList<SegNode<T>> children;
	/**
	 * @param data create node with specified data
	 */
	public SegNode(T data){
		this.data=data;
		children=new ArrayList<SegNode<T>>();
	}
	/**
	 * @param data add child node to Tree with specified data
	 */
	public void addChild(T data){
		SegNode<T> child=new SegNode<T>(data);
		child.parent=this;
		this.children.add(child);
	}
	/**
	 * @param n root node of subtree to traverse through
	 * @param data data to find in tree
	 */
	public SegNode<T> dfs(SegNode<T> n, T data){
		SegNode<T> found=n;
		if(n.data.equals(data))  return n;
		if(n.children.size() > 0)
			for (SegNode<T> sn : n.children){
				found=dfs(sn,data);
				if(found != null) break;
			}
		return found;
	}
}
