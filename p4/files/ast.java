import java.io.*;
import java.util.*;

// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a C-- program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a list (for nodes that may have a variable number of
// children) or as a fixed set of fields.
//
// The nodes for literals and ids contain line and character number
// information; for string literals and identifiers, they also contain a
// string; for integer literals, they also contain an integer value.
//
// Here are all the different kinds of AST nodes and what kinds of children
// they have.  All of these kinds of AST nodes are subclasses of "ASTnode".
// Indentation indicates further subclassing:
//
//     Subclass            Children
//     --------            ----
//     ProgramNode         DeclListNode
//     DeclListNode        linked list of DeclNode
//     DeclNode:
//       VarDeclNode       TypeNode, IdNode, int
//       FnDeclNode        TypeNode, IdNode, FormalsListNode, FnBodyNode
//       FormalDeclNode    TypeNode, IdNode
//       StructDeclNode    IdNode, DeclListNode
//
//     FormalsListNode     linked list of FormalDeclNode
//     FnBodyNode          DeclListNode, StmtListNode
//     StmtListNode        linked list of StmtNode
//     ExpListNode         linked list of ExpNode
//
//     TypeNode:
//       IntNode           -- none --
//       BoolNode          -- none --
//       VoidNode          -- none --
//       StructNode        IdNode
//
//     StmtNode:
//       AssignStmtNode      AssignNode
//       PostIncStmtNode     ExpNode
//       PostDecStmtNode     ExpNode
//       ReadStmtNode        ExpNode
//       WriteStmtNode       ExpNode
//       IfStmtNode          ExpNode, DeclListNode, StmtListNode
//       IfElseStmtNode      ExpNode, DeclListNode, StmtListNode,
//                                    DeclListNode, StmtListNode
//       WhileStmtNode       ExpNode, DeclListNode, StmtListNode
//       RepeatStmtNode      ExpNode, DeclListNode, StmtListNode
//       CallStmtNode        CallExpNode
//       ReturnStmtNode      ExpNode
//
//     ExpNode:
//       IntLitNode          -- none --
//       StrLitNode          -- none --
//       TrueNode            -- none --
//       FalseNode           -- none --
//       IdNode              -- none --
//       DotAccessNode       ExpNode, IdNode
//       AssignNode          ExpNode, ExpNode
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode
//         MinusNode
//         TimesNode
//         DivideNode
//         AndNode
//         OrNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         GreaterNode
//         LessEqNode
//         GreaterEqNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with linked lists of children, or
// internal nodes with a fixed number of children:
//
// (1) Leaf nodes:
//        IntNode,   BoolNode,  VoidNode,  IntLitNode,  StrLitNode,
//        TrueNode,  FalseNode, IdNode
//
// (2) Internal nodes with (possibly empty) linked lists of children:
//        DeclListNode, FormalsListNode, StmtListNode, ExpListNode
//
// (3) Internal nodes with fixed numbers of children:
//        ProgramNode,     VarDeclNode,     FnDeclNode,     FormalDeclNode,
//        StructDeclNode,  FnBodyNode,      StructNode,     AssignStmtNode,
//        PostIncStmtNode, PostDecStmtNode, ReadStmtNode,   WriteStmtNode
//        IfStmtNode,      IfElseStmtNode,  WhileStmtNode,  RepeatStmtNode,
//        CallStmtNode
//        ReturnStmtNode,  DotAccessNode,   AssignExpNode,  CallExpNode,
//        UnaryExpNode,    BinaryExpNode,   UnaryMinusNode, NotNode,
//        PlusNode,        MinusNode,       TimesNode,      DivideNode,
//        AndNode,         OrNode,          EqualsNode,     NotEqualsNode,
//        LessNode,        GreaterNode,     LessEqNode,     GreaterEqNode
//
// **********************************************************************

// **********************************************************************
// ASTnode class (base class for all other kinds of nodes)
// **********************************************************************

abstract class ASTnode {
    // every subclass must provide an unparse operation
    abstract public void unparse(PrintWriter p, int indent);

    // this method can be used by the unparse methods to do indenting
    protected void addIndentation(PrintWriter p, int indent) {
        for (int k = 0; k < indent; k++)
            p.print(" ");
    }
}

// **********************************************************************
// ProgramNode, DeclListNode, FormalsListNode, FnBodyNode,
// StmtListNode, ExpListNode
// **********************************************************************

class ProgramNode extends ASTnode {
    public ProgramNode(DeclListNode L) {
        myDeclList = L;
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    }

    public void nameAnalysis() {
        // initialize SymTable 
        SymTable table = new SymTable();
        myDeclList.nameAnalysis(table);
    }

    private DeclListNode myDeclList;
}

class DeclListNode extends ASTnode {
    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode) it.next()).unparse(p, indent);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }

    public SymTable nameAnalysis(SymTable table) {
        // from programNode
        for (DeclNode dNode : myDecls) {
            dNode.nameAnalysis(table);
        }
        return table;
    }

    public void nameAnalysisFnbody(SymTable table) {
        HashMap<String, Integer> countDecl = new HashMap<>();
        // from FnBodynode, check multiply declared variables inside a function body
        for (DeclNode d : myDecls) {
            IdNode i = d.getIdNode();
            String idName = i.getStringval();
            //System.out.println("nameAnalysisFnbody " + idName);
            if (countDecl.get(idName) == null) {
                countDecl.put(idName, 1);
                try {
                    if (table.lookupLocal(idName) == null) {
                        d.nameAnalysis(table);
                    }
                } catch (EmptySymTableException ex) {}
                
            } else {
                int count = countDecl.get(idName);
                countDecl.put(idName, count + 1);
                ErrMsg.fatal(i.getLinenum(), i.getCharnum(), "Multiply declared identifier");
            }  
        }
    }
    
    public void nameAnalysisStructBody(SymTable table, SymTable structSymTable) {
        for (DeclNode d : myDecls) {
            VarDeclNode v = (VarDeclNode) d;
            // for non-struct variable declaration
            if (v.getSize() == VarDeclNode.NOT_STRUCT) {
                v.nameAnalysis(structSymTable);
            //for struct variable declaration
            } else {
                v.nameAnalysisStruct(table, structSymTable);
            }
        }
    }

    private List<DeclNode> myDecls;
}

class FormalsListNode extends ASTnode {
    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) { // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        }
    }

    public SymTable nameAnalysis(SymTable table) {
        for (FormalDeclNode f : this.myFormals) {
            f.nameAnalysis(table);
        }
        return table;
    }

    public LinkedList<String> getParamList() {
        LinkedList<String> paramTypes = new LinkedList<>();
        for (FormalDeclNode fdn : this.myFormals) {
            paramTypes.add(fdn.getTypeNode().getType());
        }
        return paramTypes;
    }

    private List<FormalDeclNode> myFormals;
}

class FnBodyNode extends ASTnode {
    public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    }

    public void nameAnalysis(SymTable table) {
        System.out.println("FunctionBody");
        table.print();
        this.myDeclList.nameAnalysisFnbody(table);
        this.myStmtList.nameAnalysis(table);
    }

    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class StmtListNode extends ASTnode {
    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        }
    }

    public void nameAnalysis(SymTable table) {
        for (StmtNode s : this.myStmts) {
            s.nameAnalysis(table);
        }
    }

    private List<StmtNode> myStmts;
}

class ExpListNode extends ASTnode {
    public ExpListNode(List<ExpNode> S) {
        myExps = S;
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) { // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        }
    }
 
    public void nameAnalysis(SymTable table) {
        for (ExpNode e : this.myExps) {
            e.nameAnalysis(table);
        }
    }

    private List<ExpNode> myExps;
}

// **********************************************************************
// DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
    abstract public SymTable nameAnalysis(SymTable table);

    abstract public IdNode getIdNode();
}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.println(";");
    }

    public SymTable nameAnalysis(SymTable table) {
        // check if the variable type is void
        if (this.myType.getType().equals("void")) {
            ErrMsg.fatal(myId.getLinenum(), myId.getCharnum(), "Non-function declared void");
            return table;
        }

        // check if the variable starts with struct
        if (this.myType.getType().equals("struct")) {
            try {
                // check multideclared 
                IdNode structId = ((StructNode)this.myType).getId();
                TSym structSym = table.lookupGlobal(structId.getStringval());
                if (structSym == null || checkMultiDeclaredStruct(table) == false) {
                    return table;
                }
                
                // add struct var
                this.nameAnalysisVar(table);
                // get struct var
                TSym mySym = table.lookupGlobal(this.myId.getStringval());
                mySym.setStructType(structId.getStringval());
                // link Struct table
                mySym.setStructTable(structSym.getStructTable());
                //System.out.println( mySym.getType() + " " + this.myId.getStringval() + " " + structSym.getType() + " " + structId.getStringval());
                
                return table;
            } catch (EmptySymTableException e) {
                System.err.println(e);
                System.exit(-1);
            }
        }
        // normal variable
        this.nameAnalysisVar(table);
        table.print();
        return table;
 
    }

    public void nameAnalysisVar(SymTable table) {
        // add normal variable declaration to current table
        TSym tmpSym = new TSym(this.myType.getType());
        try {
            table.addDecl(this.myId.getStringval(), tmpSym);
        } catch (DuplicateSymException e) {
            ErrMsg.fatal(this.myId.getLinenum(), this.myId.getCharnum(), "Multiply declared identifier");
        } catch (EmptySymTableException e) {
            System.err.println("Symtable is empty");
        }
    }

    public void nameAnalysisStruct(SymTable table, SymTable structTable) {
        this.nameAnalysisVar(structTable);
        this.checkMultiDeclaredStruct(table);

        if (this.myType.getType().equals("struct")) {
            try {
                TSym structSym = table.lookupGlobal(((StructNode)this.myType).getId().getStringval());
                TSym mySym = structTable.lookupGlobal(this.myId.getStringval());
                if (structSym != null && mySym != null) {
                    System.out.println("struct field inside a struct field: " + structSym.getType() + " " + mySym.getType() + " " + this.myId.getStringval());
                    //this.myId.setStructDeclnode(structSym.getStructDecl(), mySym);
                }
            } catch (EmptySymTableException e) {
                System.err.println(e);
                System.exit(-1);
            }   
        }
    }

    public boolean checkMultiDeclaredStruct(SymTable table) {
        IdNode structId = ((StructNode)this.myType).getId(); 
        try {
            TSym sym = table.lookupGlobal(structId.getStringval());
            if (sym == null || !sym.getType().equals("structDecl")) {
                ErrMsg.fatal(structId.getLinenum(), structId.getCharnum(), "Invalid name of struct type");
                return false;
            } 
        } catch (EmptySymTableException e) {
            System.err.println(e);
            System.exit(-1);
        }
        return true;
    }

    public IdNode getIdNode() {
        return this.myId;
    }

    public int getSize() {
        return this.mySize;
    }

    private TypeNode myType;
    private IdNode myId;
    private int mySize; // use value NOT_STRUCT if this is not a struct type

    public static int NOT_STRUCT = -1;
}

class FnDeclNode extends DeclNode {
    public FnDeclNode(TypeNode type, IdNode id, FormalsListNode formalList, FnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.print("(");
        myFormalsList.unparse(p, 0);
        p.println(") {");
        myBody.unparse(p, indent + 4);
        p.println("}\n");
    }

    public SymTable nameAnalysis(SymTable table) {
        // cluster function param definitions to a funcDeclTSym and save it to the SymbolTable
        LinkedList<String> paramTypes = this.myFormalsList.getParamList();
        funcDeclTSym funcParamSym = new funcDeclTSym(this.myType.getType(), paramTypes);
        try {
            table.addDecl(this.myId.getStringval(), funcParamSym);
        } catch (DuplicateSymException e) {
            ErrMsg.fatal(this.myId.getLinenum(), this.myId.getCharnum(), "Multiply declared identifier");
        } catch (EmptySymTableException e) {
            System.err.println(e);
            System.exit(-1);
        }

        table.addScope();
        this.myFormalsList.nameAnalysis(table);
        this.myBody.nameAnalysis(table);
        try {
            table.removeScope();
        } catch(EmptySymTableException e) {
            System.err.println(e);
            System.exit(-1);
        }
        return table;
    }

    public IdNode getIdNode() {
        return this.myId;
    }

    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FnBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    }

    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
    }

    public SymTable nameAnalysis(SymTable table) {
        try {
            table.addDecl(this.myId.getStringval(), new TSym(this.myType.getType()));
        } catch (DuplicateSymException e) {
            ErrMsg.fatal(this.myId.getLinenum(), this.myId.getCharnum(), "Multiply declared identifier");
        } catch (EmptySymTableException e) {
            System.err.println(e);
        }
        return table;
    }

    public IdNode getIdNode() {
        return this.myId;
    }

    public TypeNode getTypeNode() {
        return this.myType;
    }

    private TypeNode myType;
    private IdNode myId;
}

class StructDeclNode extends DeclNode {
    public StructDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
        myDeclList = declList;
        this.type = "structDecl";
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("struct ");
        myId.unparse(p, 0);
        p.println("{");
        myDeclList.unparse(p, indent + 4);
        addIndentation (p, indent);
        p.println("};\n");

    }

    public SymTable nameAnalysis(SymTable table) {
        //StructDeclTSym structSym = new StructDeclTSym(this.type);
        TSym structSym = new TSym(this.type);
        // make a SymTable for each struct name
        this.mySymTable = new SymTable();

        this.myId.setStructDeclnode(this);

        this.myDeclList.nameAnalysisStructBody(table, this.mySymTable);

        structSym.setStructTable(this.mySymTable);
        try {
            table.addDecl(this.myId.getStringval(), structSym);
        } catch (DuplicateSymException e) {
            ErrMsg.fatal(this.myId.getLinenum(), this.myId.getCharnum(), "Multiply declared identifier");
        } catch (EmptySymTableException e) {
            System.err.println(e);
            System.exit(-1);
        }

        return table;
    }

    public IdNode getIdNode() {
        return this.myId;
    }

    public SymTable getSymTable() {
        return this.mySymTable;
    }

    private String type;
    private IdNode myId;
    private DeclListNode myDeclList;
    private SymTable mySymTable;
}

// **********************************************************************
// TypeNode and its Subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {
    abstract public String getType();
}

class IntNode extends TypeNode {
    public IntNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("int");
    }

    public String getType() {
        return "int";
    }
}

class BoolNode extends TypeNode {
    public BoolNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("bool");
    }

    public String getType() {
        return "bool";
    }
}

class VoidNode extends TypeNode {
    public VoidNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }

    public String getType() {
        return "void";
    }
}

class StructNode extends TypeNode {
    public StructNode(IdNode id) {
        myId = id;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("struct ");
        myId.unparse(p, 0);
    }

    public String getType() {
        return "struct";
    }

    public IdNode getId() {
        return this.myId;
    }

    private IdNode myId;
}

// **********************************************************************
// StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
    public abstract void nameAnalysis(SymTable table);
}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(AssignNode assign) {
        myAssign = assign;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        myAssign.unparse(p, -1); // no parentheses
        p.println(";");
    }

    public void nameAnalysis(SymTable table) {
        this.myAssign.nameAnalysis(table);
    }

    private AssignNode myAssign;
}

class PostIncStmtNode extends StmtNode {
    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        myExp.unparse(p, 0);
        p.println("++;");
    }

    public void nameAnalysis(SymTable table) {
        this.myExp.nameAnalysis(table);
    }

    private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        myExp.unparse(p, 0);
        p.println("--;");
    }

    public void nameAnalysis(SymTable table) {
        this.myExp.nameAnalysis(table);
    }

    private ExpNode myExp;
}

class ReadStmtNode extends StmtNode {
    public ReadStmtNode(ExpNode e) {
        myExp = e;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("cin >> ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    public void nameAnalysis(SymTable table) {
        this.myExp.nameAnalysis(table);
    }

    // 1 child (actually can only be an IdNode or an ArrayExpNode)
    private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("cout << ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    public void nameAnalysis(SymTable table) {
        this.myExp.nameAnalysis(table);
    }

    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent + 4);
        myStmtList.unparse(p, indent + 4);
        addIndentation(p, indent);
        p.println("}");
    }

    public void nameAnalysis(SymTable table) {
        this.myExp.nameAnalysis(table);
        table.addScope();
        this.myDeclList.nameAnalysis(table);
        this.myStmtList.nameAnalysis(table);
        try {
            table.removeScope();
        } catch (EmptySymTableException e) {
            System.err.println(e);
            System.exit(-1);
        }
        
    }

    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1, StmtListNode slist1, DeclListNode dlist2,
            StmtListNode slist2) {
        myExp = exp;
        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myThenDeclList.unparse(p, indent + 4);
        myThenStmtList.unparse(p, indent + 4);
        addIndentation(p, indent);
        p.println("}");
        addIndentation(p, indent);
        p.println("else {");
        myElseDeclList.unparse(p, indent + 4);
        myElseStmtList.unparse(p, indent + 4);
        addIndentation(p, indent);
        p.println("}");
    }

    public void nameAnalysis(SymTable table) {
        this.myExp.nameAnalysis(table);
        table.addScope();
        myThenDeclList.nameAnalysis(table);
        myThenStmtList.nameAnalysis(table);
        try {
            table.removeScope();
        } catch (EmptySymTableException e) {
            System.err.println(e);
            System.exit(-1);
        }

        table.addScope();
        myElseDeclList.nameAnalysis(table);
        myElseStmtList.nameAnalysis(table);
        try {
            table.removeScope();
        } catch (EmptySymTableException e) {
            System.err.println(e);
            System.exit(-1);
        }
    }

    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("while (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent + 4);
        myStmtList.unparse(p, indent + 4);
        addIndentation(p, indent);
        p.println("}");
    }

    public void nameAnalysis(SymTable table) {
        this.myExp.nameAnalysis(table);
        table.addScope();
        this.myDeclList.nameAnalysis(table);
        this.myStmtList.nameAnalysis(table);
        try {
            table.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println(ex);
        }
    }

    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class RepeatStmtNode extends StmtNode {
    public RepeatStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("repeat (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent + 4);
        myStmtList.unparse(p, indent + 4);
        addIndentation(p, indent);
        p.println("}");
    }

    public void nameAnalysis(SymTable table) {
        this.myExp.nameAnalysis(table);
        table.addScope();
        this.myDeclList.nameAnalysis(table);
        this.myStmtList.nameAnalysis(table);
        try {
            table.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println(ex);
        }
        
    }

    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(CallExpNode call) {
        myCall = call;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        myCall.unparse(p, indent);
        p.println(";");
    }

    public void nameAnalysis(SymTable table) {
        this.myCall.nameAnalysis(table); 
    }

    private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        addIndentation(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, 0);
        }
        p.println(";");
    }

    public void nameAnalysis(SymTable table) {
        if (this.myExp != null) {
            this.myExp.nameAnalysis(table);
        }
        
    }

    private ExpNode myExp; // possibly null
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {
    public abstract void nameAnalysis(SymTable table);
}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
    }

    public void nameAnalysis(SymTable table) {
        
    }

    private int myLineNum;
    private int myCharNum;
    private int myIntVal;
}

class StringLitNode extends ExpNode {
    public StringLitNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
    }

    public void nameAnalysis(SymTable table) {
        
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("true");
    }

    public void nameAnalysis(SymTable table) {
        
    }

    private int myLineNum;
    private int myCharNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("false");
    }

    public void nameAnalysis(SymTable table) {
        
    }

    private int myLineNum;
    private int myCharNum;
}

class IdNode extends ExpNode {
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void setTSym(TSym type) {
        this.symType = type;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
        if (this.symType != null) {
            p.print("(");
            p.print(this.symType.toString());
            p.print(")");
        }
    }

    public void nameAnalysis(SymTable table) {
        try {
            this.symType = table.lookupGlobal(this.myStrVal);
            if (this.symType == null) {
                ErrMsg.fatal(myLineNum, myCharNum, "Undeclared identifier");
            }
        } catch (EmptySymTableException e) {
            System.err.println(e);
            System.exit(-1);
        }        
    }

    public int getLinenum() {
        return this.myLineNum;
    }

    public int getCharnum() {
        return this.myCharNum;
    }

    public String getStringval() {
        return this.myStrVal;
    }

    public TSym getSymType() {
        return this.symType;
    }

    public void setStructDeclnode(StructDeclNode myStruct) {
        this.myStruct = myStruct;
    }

    public StructDeclNode getStructDeclNode() {
        return this.myStruct;
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private StructDeclNode myStruct;
    private TSym symType;
}

class DotAccessExpNode extends ExpNode {
    public DotAccessExpNode(ExpNode loc, IdNode id) {
        myLoc = loc;
        myId = id;
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myLoc.unparse(p, 0);
        p.print(").");
        myId.unparse(p, 0);
    }

    public void nameAnalysis(SymTable table) {
        myLoc.nameAnalysis(table);
        if (this.myLoc instanceof IdNode) {
            IdNode locId = ((IdNode)this.myLoc);
            try {
                // find the lhs's TSym
                TSym locSym = table.lookupGlobal(locId.getStringval());
                if (locSym == null) {
                    ErrMsg.fatal(locId.getLinenum(), locId.getCharnum(), "Undeclared identifier");
                }
                // get the Struct Table for the right struct type
                SymTable structTable = locSym.getStructTable();
                if (structTable == null) {
                    ErrMsg.fatal(locId.getLinenum(), locId.getCharnum(), "Dot-access of non-struct type");
                }
                // check if the field exists
                TSym fieldExists = structTable.lookupGlobal(this.myId.getStringval());
                if (fieldExists == null) {
                    ErrMsg.fatal(this.myId.getLinenum(), this.myId.getCharnum(), "Invalid struct field name");
                } else {
                    System.out.println("ID: " + this.myId.getStringval() + " Type: " + fieldExists.getType());
                    this.myId.setTSym(fieldExists);
                }

            } catch (EmptySymTableException e) {
                System.err.println(e);
                System.exit(-1);
            }
        } else {
            System.out.println("loc node not IdNode");
        }
    }

    public StructDeclNode getLhsDeclNode(SymTable table) {
        // case 1
        if (this.myLoc instanceof IdNode) {
            try {
                IdNode locId = ((IdNode)this.myLoc);

                // search lhs id first, if does not exist, then err
                TSym locSym = table.lookupGlobal(locId.getStringval());
                if (locSym == null) {
                    ErrMsg.fatal(locId.getLinenum(), locId.getCharnum(), "Undeclared identifier");
                    return null;
                }
                System.out.println(locId.getStringval() + " type: " + locSym.getType());
                locSym.getStructTable().print();

                SymTable structT = locSym.getStructTable();
                if (structT == null) {
                    ErrMsg.fatal(locId.getLinenum(), locId.getCharnum(), "Dot-access of non-struct type");
                    return null;
                }

                return null;
                
            } catch (EmptySymTableException e) {
                System.err.println(e);
                System.exit(-1);
            }
            return ((IdNode)this.myLoc).getStructDeclNode();
        }
        return null;
    }

    private ExpNode myLoc;
    private IdNode myId;
}

class AssignNode extends ExpNode {
    public AssignNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
    }

    public void unparse(PrintWriter p, int indent) {
        if (indent != -1)
            p.print("(");
        myLhs.unparse(p, 0);
        p.print(" = ");
        myExp.unparse(p, 0);
        if (indent != -1)
            p.print(")");
    }

    public void nameAnalysis(SymTable table) {
        this.myLhs.nameAnalysis(table);
        System.out.println("assign check");
        this.myExp.nameAnalysis(table);
    }

    private ExpNode myLhs;
    private ExpNode myExp;
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
    }

    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    }

    public void unparse(PrintWriter p, int indent) {
        myId.unparse(p, 0);
        p.print("(");
        if (myExpList != null) {
            myExpList.unparse(p, 0);
        }
        p.print(")");
    }

    public void nameAnalysis(SymTable table) {
        this.myId.nameAnalysis(table);
        this.myExpList.nameAnalysis(table);
    }

    private IdNode myId;
    private ExpListNode myExpList; // possibly null
}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalysis(SymTable table) {
        this.myExp.nameAnalysis(table);
    }

    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
    }

    public void nameAnalysis(SymTable table) {
        this.myExp1.nameAnalysis(table);
        this.myExp2.nameAnalysis(table);
    }

    protected ExpNode myExp1;
    protected ExpNode myExp2;
}

// **********************************************************************
// Subclasses of UnaryExpNode
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode {
    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(-");
        myExp.unparse(p, 0);
        p.print(")");
    }
}

class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(!");
        myExp.unparse(p, 0);
        p.print(")");
    }
}

// **********************************************************************
// Subclasses of BinaryExpNode
// **********************************************************************

class PlusNode extends BinaryExpNode {
    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" + ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class MinusNode extends BinaryExpNode {
    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" - ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class TimesNode extends BinaryExpNode {
    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" * ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class DivideNode extends BinaryExpNode {
    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" / ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class AndNode extends BinaryExpNode {
    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" && ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class OrNode extends BinaryExpNode {
    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" || ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class EqualsNode extends BinaryExpNode {
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" == ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class NotEqualsNode extends BinaryExpNode {
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" != ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class LessNode extends BinaryExpNode {
    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" < ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class GreaterNode extends BinaryExpNode {
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" > ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class LessEqNode extends BinaryExpNode {
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" <= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class GreaterEqNode extends BinaryExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" >= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}
