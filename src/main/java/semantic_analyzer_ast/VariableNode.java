package semantic_analyzer_ast;

import lexical_analyzer.IToken;

public class VariableNode extends OperandNode {
    private ChainedNode chainedNode;

    public VariableNode(IToken token, String line, int row, int column) {
        super(token, line, row, column);
    }

    public ChainedNode getChainedNode() {
        return chainedNode;
    }

    public void setChainedNode(ChainedNode chainedNode) {
        this.chainedNode = chainedNode;
    }

    @Override
    public void validate() {

    }
}