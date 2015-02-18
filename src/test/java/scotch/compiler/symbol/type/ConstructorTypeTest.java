package scotch.compiler.symbol.type;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.Symbol.symbol;
import static scotch.compiler.symbol.Unification.mismatch;
import static scotch.compiler.symbol.Unification.unified;
import static scotch.compiler.symbol.type.Types.ctor;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.compiler.symbol.type.Types.var;

import org.junit.Before;
import org.junit.Test;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.syntax.scope.DefaultTypeScope;

public class ConstructorTypeTest {

    private DefaultTypeScope scope;

    @Before
    public void setUp() {
        scope = new DefaultTypeScope(new SymbolGenerator());
    }

    @Test
    public void constructorShouldFlattenIntoSumIfHeadIsSum() {
        assertThat(ctor(sum("Map", var("k")), var("a")).flatten(), is(sum("Map", var("k"), var("a"))));
    }

    @Test
    public void constructorWithVariableHeadShouldNotFlatten() {
        assertThat(ctor(var("m"), var("a")).flatten(), is(ctor(var("m"), var("a"))));
    }

    @Test
    public void sumShouldUnifyIfItImplementsConstructorContext() {
        scope.implement(symbol("Monad"), sum("List"));
        assertThat(ctor(var("m", asList("Monad")), var("a")).unify(sum("List", var("b")), scope), is(unified(sum("List", var("a")))));
    }

    @Test
    public void sumShouldNotUnifyIfItDoesNotImplementConstructorContext() {
        assertThat(ctor(var("m", asList("Monad")), var("a")).unify(sum("List", var("b")), scope),
            is(mismatch(var("m", asList("Monad")), sum("List", var("b")))));
    }

    @Test
    public void constructorShouldUnifyParameterizedSum() {
        scope.implement(symbol("Monad"), sum("Map", var("k")));
        assertThat(ctor(var("m", asList("Monad")), var("a")).unify(sum("Map", var("k"), var("a")), scope),
            is(unified(sum("Map", var("k"), var("a")))));
    }

    @Test
    public void constructorShouldUnifyWhenPartiallyApplied() {
        scope.implement(symbol("Monad"), sum("Map", var("k")));
        assertThat(ctor(sum("Map", var("k")), var("a")).unify(sum("Map", var("x"), var("y")), scope),
            is(unified(sum("Map", var("k"), var("a")))));
    }

    @Test
    public void constructorShouldNotUnifyWhenSumParameterDoesNotImplementContext() {
        scope.implement(symbol("Toast"), sum("Map", var("k", asList("Eq"))));
        assertThat(ctor(var("a", asList("Toast")), var("m")).unify(sum("Map", sum("Int"), var("y")), scope),
            is(mismatch(var("a", asList("Toast")), sum("Map", sum("Int"), var("y")))));
    }
}