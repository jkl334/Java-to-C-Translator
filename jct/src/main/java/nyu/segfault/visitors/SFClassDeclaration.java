package nyu.segfault;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import javax.swing.tree.DefaultTreeModel;

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

public class SFClassDeclaration extends Visitor {

	private GNode n;
	private Printer impWriter;
	private Printer headWriter;

	public SFClassDeclaration(Printer impWriter, Printer headWriter, Visitor sfVisitor, GNode n) {
		this.n = n;
		this.impWriter = impWriter;
		this.headWriter = headWriter;

		headWriter.pln("struct " + getClassName() + " {");

		sfVisitor.dispatch(n.getNode(5));
		headWriter.pln();
		headWriter.pln("}");
	}

	public String getClassName() {
		return n.getString(1);
	}

}
