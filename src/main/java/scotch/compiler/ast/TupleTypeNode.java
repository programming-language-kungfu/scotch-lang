package scotch.compiler.ast;

import java.util.List;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import scotch.compiler.text.SourceLocation;

@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceLocation")
public class TupleTypeNode extends AstNode {

    @Getter
    private final SourceLocation sourceLocation;
    private final AstNode        openParen;
    private final List<AstNode>  fieldsAndCommas;
    private final AstNode        closeParen;

    TupleTypeNode(SourceLocation sourceLocation, AstNode openParen, List<AstNode> fieldsAndCommas, AstNode closeParen) {
        super();
        this.sourceLocation = sourceLocation;
        this.openParen = openParen;
        this.fieldsAndCommas = ImmutableList.copyOf(fieldsAndCommas);
        this.closeParen = closeParen;
    }

    @Override
    public <T> T accept(AstNodeVisitor<T> visitor) {
        return visitor.visitTupleTypeNode(this);
    }
}
