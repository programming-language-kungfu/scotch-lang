package scotch.compiler.ast;

import static lombok.AccessLevel.PACKAGE;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import scotch.compiler.text.SourceLocation;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceLocation")
public class ParenthesizedNode extends AstNode {

    @Getter
    private final SourceLocation sourceLocation;
    private final AstNode        openParen;
    private final AstNode        argument;
    private final AstNode        closeParen;

    @Override
    public <T> T accept(AstNodeVisitor<T> visitor) {
        return visitor.visitParenthesizedNode(this);
    }
}
