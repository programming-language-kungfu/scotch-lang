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
public class VariableTypeNode extends AstNode {

    @Getter
    private final SourceLocation sourceLocation;
    private final AstNode        name;

    @Override
    public <T> T accept(AstNodeVisitor<T> visitor) {
        return visitor.visitVariableTypeNode(this);
    }
}
