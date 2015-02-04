package scotch.compiler.symbol;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.DataFieldDescriptor.field;
import static scotch.compiler.symbol.MethodSignature.methodSignature;
import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.symbol.Symbol.symbol;
import static scotch.compiler.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.compiler.symbol.type.Types.fn;
import static scotch.compiler.symbol.type.Types.var;
import static scotch.compiler.util.TestUtil.constructor;
import static scotch.compiler.util.TestUtil.dataType;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.typeClass;
import static scotch.compiler.util.TestUtil.typeInstance;

import org.junit.Before;
import org.junit.Test;
import scotch.compiler.symbol.type.Type;
import scotch.data.num.NumInt;

public class ClasspathResolverTest {

    private ClasspathResolver resolver;

    @Before
    public void setUp() {
        resolver = new ClasspathResolver(getClass().getClassLoader());
    }

    @Test
    public void shouldResolveJavaSymbol() {
        Type a = var("a", asList("scotch.data.num.Num"));
        SymbolEntry entry = resolver.getEntry(symbol("scotch.data.num.(+)")).get();

        assertThat(entry.getValue(), is(fn(a, fn(a, a))));
        assertThat(entry.getOperator(), is(operator(LEFT_INFIX, 7)));
        assertThat(entry.getValueMethod(), is(methodSignature("scotch/data/num/Num:add:()Lscotch/runtime/Applicable;")));
        assertThat(entry.getMemberOf(), is(symbol("scotch.data.num.Num")));
    }

    @Test
    public void shouldResolveJavaTypeClass() {
        SymbolEntry entry = resolver.getEntry(symbol("scotch.data.num.Num")).get();
        assertThat(entry.getTypeClass(), is(typeClass("scotch.data.num.Num", asList(var("a")), asList(
            "scotch.data.num.(+)",
            "scotch.data.num.(-)",
            "scotch.data.num.(*)",
            "scotch.data.num.fromInteger",
            "scotch.data.num.signum",
            "scotch.data.num.negate",
            "scotch.data.num.abs"
        ))));
    }

    @Test
    public void shouldResolveJavaTypeInstancesByClass() {
        assertThat(resolver.getTypeInstancesByClass(symbol("scotch.data.num.Num")), hasItem(typeInstance(
            "scotch.data.num",
            "scotch.data.num.Num",
            asList(intType()),
            MethodSignature.fromMethod(NumInt.class, "instance")
        )));
    }

    @Test
    public void shouldResolveJavaTypeInstancesByType() {
        resolver.getEntry(symbol("scotch.data.num.Num")); // force loading of module containing instance
        assertThat(resolver.getTypeInstancesByArguments(asList(intType())), hasItem(typeInstance(
            "scotch.data.num",
            "scotch.data.num.Num",
            asList(intType()),
            MethodSignature.fromMethod(NumInt.class, "instance")
        )));
    }

    @Test
    public void shouldResolveJavaTypeInstanceByClassAndType() {
        assertThat(resolver.getTypeInstances(symbol("scotch.data.num.Num"), asList(intType())), hasItem(typeInstance(
            "scotch.data.num",
            "scotch.data.num.Num",
            asList(intType()),
            MethodSignature.fromMethod(NumInt.class, "instance")
        )));
    }

    @Test
    public void shouldResolveJavaTypeInstanceByModuleName() {
        assertThat(resolver.getTypeInstancesByModule("scotch.data.num"), hasItem(typeInstance(
            "scotch.data.num",
            "scotch.data.num.Num",
            asList(intType()),
            MethodSignature.fromMethod(NumInt.class, "instance")
        )));
    }

    @Test
    public void shouldResolveDataType() {
        assertThat(resolver.getEntry(symbol("scotch.data.maybe.Maybe")).get().getDataType(), is(dataType(
            "scotch.data.maybe.Maybe",
            asList(var("a")),
            asList(
                constructor(
                    "scotch.data.maybe.Maybe",
                    "scotch.data.maybe.Nothing"
                ),
                constructor(
                    "scotch.data.maybe.Maybe",
                    "scotch.data.maybe.Just",
                    asList(field("value", var("a")))
                )
            )
        )));
    }
}
