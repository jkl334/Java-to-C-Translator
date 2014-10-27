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

public class VisitReturnStatements extends Visitor {

	private Printer impWriter;
	private Printer headWriter;

	public VisitReturnStatements(Printer impWriter, Printer headWriter) {
		this.impWriter = impWriter;
		this.headWriter = headWriter;
	}

	public void visitReturnStatement(GNode n) {
		new Visitor() {  // Visit assigned value if any

			public void visitStringLiteral(GNode n) {
                impWriter.p(n.getString(0));
            }

			public void visitIntegerLiteral(GNode n) {
				impWriter.p(n.getString(0));
			}

			public void visitFloatingPointLiteral(GNode n) {
				impWriter.p(n.getString(0));
	        }

	        public void visitCharacterLiteral(GNode n) {
				impWriter.p(n.getString(0));
	        }

	        public void visitBooleanLiteral(GNode n) {
				impWriter.p(n.getString(0));
	        }

	        public void visitNullLiteral(GNode n) {
				impWriter.p("null");
	        }

            public void visitPrimaryIdentifier(GNode n) {
                impWriter.p(n.getString(0));
	        }

	        public void visitThisExpression(GNode n) {

	        }


	        public void visitAdditiveExpression(GNode n) {
	        //	Currently only works for 2 vars in expression. hard-coded
	        //	System.out.println(n.toString());
	        	/*String add = "";
	        	System.out.println(n.size());
	        	for (int i = 0,j=1; i < n.size(); i+=2,j+=2) {
	        		if(i < n.size()) {
    	        		add += n.getNode(i).getString(0);
    	        	    add += n.getString(j);
    	        	}
	        	}
	        	impWriter.p(add);
				*/
	        	impWriter.p(n.getNode(0).getString(0) + " " + n.getString(1) + " " + n.getNode(2).getString(0));
	        //	if (n.getNode(0) != null) visit(n.getNode(0));
	        }
			public void visit(Node n){
				for (Object o : n) if(o instanceof Node) dispatch((Node)o);

			}
		}.dispatch(n);
		impWriter.pln(";");
	}

}