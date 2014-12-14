package nyu.segfault;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import javax.swing.tree.DefaultTreeModel;

import nyu.segfault.SFVariable;
import nyu.segfault.SFVisitor;

import xtc.lang.JavaFiveParser;
import xtc.parser.ParseException;
import xtc.parser.Result;
import xtc.util.SymbolTable;
import xtc.util.Tool;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;

import java.util.ArrayList;

public class SFMethodDeclaration extends Visitor {

	private GNode n;
	private Printer impWriter;
	private Printer headWriter;

	public SFMethodDeclaration(Printer impWriter, Printer headWriter, Visitor sfVisitor, GNode n) {
		this.n = n;
		this.impWriter = impWriter;
		this.headWriter = headWriter;
		
		printMethod();

	}

	public String getMethodName() {
		return n.getString(3);
	}

	public void printMethod() {
		SFVariable var = new SFVariable();
		headWriter.p(var.get(n.getNode(0))); // Modifier
		headWriter.p(var.get(n.getNode(2))); // Type
		headWriter.p(getMethodName()); // MethodName
	}

}
