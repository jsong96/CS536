public class TSym {
    private String type;

    public TSym(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String toString() {
        return type;
    }
}

class fTSym extends TSym {
    private String paramType;

    public fTSym() {
        super();
    }
}