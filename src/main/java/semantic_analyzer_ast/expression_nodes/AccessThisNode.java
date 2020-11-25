package semantic_analyzer_ast.expression_nodes;

import exceptions.SemanticException;
import semantic_analyzer.IAccessMode;
import semantic_analyzer.IType;
import semantic_analyzer.SymbolTable;
import semantic_analyzer_ast.visitors.VisitorExpression;

public class AccessThisNode extends AccessNode {
    public AccessThisNode(String line, int row, int column) {
        super(line, row, column);
    }

    @Override
    public IType getType() {
        return SymbolTable.getInstance().getCurrClass().getConstructor().getReturnType();
    }

    @Override
    public void validateForAssignment() throws SemanticException {
        if (getChainedNode() == null) {
            throw new SemanticException(this, "asignacion a this");
        } else {
            getChainedNode().validateForAssignemnt(getType());
        }
    }

    @Override
    public void acceptVisitor(VisitorExpression visitorExpression) {
        if (getChainedNode() != null) {
            getChainedNode().acceptVisitor(visitorExpression);
        } else {
            visitorExpression.visit(this);
        }
    }

    @Override
    public void validate() throws SemanticException {
        if (isStaticMethod()) {
            throw new SemanticException(this, "acceso this dentro de metodo estatico");
        }
    }

    private boolean isStaticMethod() {
        IAccessMode accessMode = SymbolTable.getInstance().getCurrMethod().getAccessMode();
        return accessMode == null || accessMode.getName().equals("static");
    }
}