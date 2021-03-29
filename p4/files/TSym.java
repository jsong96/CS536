import java.sql.Struct;
import java.util.LinkedList;

public class TSym {
    private String type;
    private String structType;
    private SymTable table;

    public TSym(String type) {
        this.type = type;
    }

    public void setStructType(String structType) {
        this.structType = structType;
    }

    public String getType() {
        return type;
    }

    public String toString() {
        if (structType != null) {
            return this.structType;
        } else {
            return this.type;
        }   
    }

    public SymTable getStructTable() {
        return this.table;
    }

    public void setStructTable(SymTable table) {
        this.table = table;
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
        return params + "->" + this.returnType;
    }
}