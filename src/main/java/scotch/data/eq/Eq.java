package scotch.data.eq;

import static java.util.Arrays.asList;
import static scotch.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.var;
import static scotch.data.bool.BoolModule.not;
import static scotch.runtime.RuntimeSupport.applicable;
import static scotch.runtime.RuntimeSupport.flatCallable;

import scotch.symbol.Member;
import scotch.symbol.TypeClass;
import scotch.symbol.TypeParameter;
import scotch.symbol.Value;
import scotch.symbol.ValueType;
import scotch.symbol.type.Type;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@TypeClass(memberName = "Eq", parameters = {
    @TypeParameter(name = "a"),
})
public interface Eq<A> {

    @Value(memberName = "==", fixity = LEFT_INFIX, precedence = 5)
    static <A> Applicable<Eq<A>, Applicable<A, Applicable<A, Boolean>>> eq() {
        return applicable(instance -> applicable(left -> applicable(right -> instance.call().eq(left, right))));
    }

    @ValueType(forMember = "==")
    static Type eq$type() {
        Type a = var("a", asList("scotch.data.eq.Eq"));
        return fn(a, fn(a, sum("scotch.data.bool.Bool")));
    }

    @Value(memberName = "/=", fixity = LEFT_INFIX, precedence = 5)
    static <A> Applicable<Eq<A>, Applicable<A, Applicable<A, Boolean>>> ne() {
        return applicable(instance -> applicable(left -> applicable(right -> instance.call().ne(left, right))));
    }

    @ValueType(forMember = "/=")
    static Type ne$type() {
        Type a = var("a", asList("scotch.data.eq.Eq"));
        return fn(a, fn(a, sum("scotch.data.bool.Bool")));
    }

    @Member("==")
    default Callable<Boolean> eq(Callable<A> left, Callable<A> right) {
        return flatCallable(() -> not().apply(ne(left, right)));
    }

    @Member("/=")
    default Callable<Boolean> ne(Callable<A> left, Callable<A> right) {
        return flatCallable(() -> not().apply(eq(left, right)));
    }
}
