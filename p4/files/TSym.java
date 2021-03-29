import java.sql.Struct;
import java.util.LinkedList;

public class TSym {
    private String type;
    private StructDeclNode structDecl;

    public TSym(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String toString() {
        return type;
    }

    public void setStructDecl(StructDeclNode structDecl) {
        this.structDecl = structDecl;
    }

    public StructDeclNode getStructDecl () {
        return this.structDecl;
    }
}

class funcDeclTSym extends TSym {
    private LinkedList<String> paramType;
    private String returnType;

    public funcDeclTSym(String rType, LinkedList<String> pType) {
        super("function");
        this.paramType = pType;
        this.returnType = rType;
    }

    public LinkedList<String> getPType() {
        return this.paramType;
    }

    public String getRType() {
        return this.returnType;
    }

    public String toString() {
        String params = String.join(", ", this.paramType);
        if (params.equals("")) {
            params = "void";
        }
        return params + "-> " + this.returnType;
    }
}