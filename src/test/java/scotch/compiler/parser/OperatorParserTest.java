package scotch.compiler.parser;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.symbol.Value.Fixity.RIGHT_INFIX;
import static scotch.compiler.syntax.DefinitionReference.moduleRef;
import static scotch.compiler.syntax.DefinitionReference.operatorRef;

import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.syntax.Definition.OperatorDefinition;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.StubResolver;

public class OperatorParserTest extends ParserTest {

    @Test
    public void shouldParseOperator() {
        parse(
            "module scotch.test",
            "right infix 0 (>>=), (>>)"
        );
        shouldNotHaveErrors();
        shouldBeDefined(moduleRef("scotch.test"), "scotch.test.(>>=)");
        shouldBeDefined(moduleRef("scotch.test"), "scotch.test.(>>)");
        shouldHaveOperator("scotch.test.(>>=)", RIGHT_INFIX, 0);
        shouldHaveOperator("scotch.test.(>>)", RIGHT_INFIX, 0);
    }

    private void shouldHaveOperator(String name, Fixity fixity, int precedence) {
        assertThat(((OperatorDefinition) graph.getDefinition(operatorRef(Symbol.fromString(name))).get()).getOperator(), is(operator(fixity, precedence)));
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        // intentionally empty
    }

    @Override
    protected Function<Compiler, DefinitionGraph> parse() {
        return Compiler::parseOperators;
    }

    @Override
    protected void setUp() {
        // intentionally empty
    }
}
