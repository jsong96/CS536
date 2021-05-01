import java.util.*;

/**
 * The TSym class defines a symbol-table entry. Each TSym contains a type (a
 * Type).
 */
public class TSym {
    private Type type;
    private int offset = 0;
    private static int localOffset = -8;

    public TSym(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return this.offset;
    }

    public static void setLocalOffset(int offset) {
        TSym.localOffset = offset;
    }

    public static int getLocalOffset() {
        return TSym.localOffset;
    }

    public String toString() {
        return type.toString();
    }
}

/**
 * The FnSym class is a subclass of the TSym class just for functions. The
 * returnType field holds the return type and there are fields to hold
 * information about the parameters.
 */
class FnSym extends TSym {
    // new fields
    private Type returnType;
    private int numParams;
    private List<Type> paramTypes;
    private int sizeParams = 0;
    private int sizeLocals = 0;

    public FnSym(Type type, int numparams) {
        super(new FnType());
        returnType = type;
        numParams = numparams;
    }

    public void addFormals(List<Type> L) {
        paramTypes = L;
    }

    public Type getReturnType() {
        return returnType;
    }

    public int getNumParams() {
        return numParams;
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    public void setSizeParams(int paramSize) {
        this.sizeParams = paramSize;
    }

    public void setSizeLocals(int localSize) {
        this.sizeLocals = localSize;
    }

    public int getSizeParams() {
        return this.sizeParams;
    }

    public int getSizeLocals() {
        return this.sizeLocals;
    }

    public String toString() {
        // make list of formals
        String str = "";
        boolean notfirst = false;
        for (Type type : paramTypes) {
            if (notfirst)
                str += ",";
            else
                notfirst = true;
            str += type.toString();
        }

        str += "->" + returnType.toString();
        return str;
    }
}

/**
 * The StructSym class is a subclass of the TSym class just for variables
 * declared to be a struct type. Each StructSym contains a symbol table to hold
 * information about its fields.
 */
class StructSym extends TSym {
    // new fields
    private IdNode structType; // name of the struct type

    public StructSym(IdNode id) {
        super(new StructType(id));
        structType = id;
    }

    public IdNode getStructType() {
        return structType;
    }
}

/**
 * The StructDefSym class is a subclass of the TSym class just for the
 * definition of a struct type. Each StructDefSym contains a symbol table to
 * hold information about its fields.
 */
class StructDefSym extends TSym {
    // new fields
    private SymTable symTab;

    public StructDefSym(SymTable table) {
        super(new StructDefType());
        symTab = table;
    }

    public SymTable getSymTable() {
        return symTab;
    }
}
