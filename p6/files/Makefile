###
# This Makefile can be used to make a parser for the C-- language
# (parser.class) and to make a program (P6.class) that tests code generator.
#
# make clean removes all generated files.
#
###

JC = javac
CP = ./deps:.

P6.class: P6.java parser.class Yylex.class ASTnode.class
	$(JC) -g -cp $(CP) P6.java

parser.class: parser.java ASTnode.class Yylex.class ErrMsg.class
	$(JC) -g -cp $(CP) parser.java

parser.java: cminusminus.cup
	java -cp $(CP) java_cup.Main < cminusminus.cup

Yylex.class: cminusminus.jlex.java sym.class ErrMsg.class
	$(JC) -g -cp $(CP) cminusminus.jlex.java

ASTnode.class: ast.java Type.java TSym.class
	$(JC) -g -cp $(CP) ast.java Type.java

cminusminus.jlex.java: cminusminus.jlex sym.class
	java -cp $(CP) JLex.Main cminusminus.jlex

sym.class: sym.java
	$(JC) -g -cp $(CP) sym.java

sym.java: cminusminus.cup
	java java_cup.Main < cminusminus.cup

ErrMsg.class: ErrMsg.java
	$(JC) -g -cp $(CP) ErrMsg.java

TSym.class: TSym.java Type.class ast.java
	$(JC) -g -cp $(CP) TSym.java ast.java

SymTable.class: SymTable.java TSym.class DuplicateSymException.class EmptySymTableException.class
	$(JC) -g -cp $(CP) SymTable.java

Type.class: Type.java ast.java TSym.java
	$(JC) -g -cp $(CP) Type.java ast.java TSym.java

DuplicateSymException.class: DuplicateSymException.java
	$(JC) -g -cp $(CP) DuplicateSymException.java

EmptySymTableException.class: EmptySymTableException.java
	$(JC) -g -cp $(CP) EmptySymTableException.java

###
# test
#
test:
	java -cp $(CP) P6 test.cminusminus test.s

###
# clean
###
clean:
	rm -f *~ *.class parser.java cminusminus.jlex.java sym.java

cleantest:
	rm -f test.s
