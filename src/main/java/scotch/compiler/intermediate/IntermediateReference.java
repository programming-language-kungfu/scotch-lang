package scotch.compiler.intermediate;

import static lombok.AccessLevel.PACKAGE;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.target.BytecodeGenerator;
import scotch.symbol.MethodSignature;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
@ToString
public class IntermediateReference extends IntermediateValue {

    private final DefinitionReference reference;
    private final MethodSignature methodSignature;

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator generator) {
        return methodSignature.reference();
    }
}
