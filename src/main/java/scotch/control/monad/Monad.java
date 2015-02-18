package scotch.control.monad;

import static scotch.compiler.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.compiler.symbol.type.Types.fn;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.compiler.symbol.type.Types.var;
import static scotch.runtime.RuntimeUtil.applicable;
import static scotch.runtime.RuntimeUtil.flatCallable;

import scotch.compiler.symbol.TypeClass;
import scotch.compiler.symbol.TypeParameter;
import scotch.compiler.symbol.Value;
import scotch.compiler.symbol.ValueType;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.type.Types;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@TypeClass(memberName = "Monad", parameters = {
    @TypeParameter(name = "m"),
})
public interface Monad<Ma> {

    @Value(memberName = ">>=", fixity = LEFT_INFIX, precedence = 1)
    public static <Ma, A, Mb> Applicable<Monad<Ma>, Applicable<Ma, Applicable<Applicable<A, Mb>, Mb>>> bind() {
        return applicable(
            monad -> applicable(
                ma -> applicable(
                    function -> flatCallable(
                        () -> monad.call().bind(ma, function.call())))));
    }

    @ValueType(forMember = ">>=")
    public static Type bind$type() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Value(memberName = "fail")
    public static <Ma> Applicable<Monad<Ma>, Applicable<String, Ma>> fail() {
        return applicable(
            monad -> applicable(
                message -> flatCallable(
                    () -> monad.call().fail(message))));
    }

    @ValueType(forMember = "fail")
    public static Type fail$type() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Value(memberName = "then", fixity = LEFT_INFIX, precedence = 1)
    public static <Ma, Mb> Applicable<Monad<Ma>, Applicable<Ma, Applicable<Mb, Mb>>> then() {
        return applicable(
            monad -> applicable(
                ma -> applicable(
                    mb -> mb)));
    }

    @ValueType(forMember = "then")
    public static Type then$type() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Value(memberName = "return")
    public static <Ma, A> Applicable<Monad<Ma>, Applicable<A, Ma>> wrap() {
        return applicable(
            monad -> applicable(
                a -> flatCallable(
                    () -> monad.call().wrap(a))));
    }

    @ValueType(forMember = "return")
    public static Type wrap$type() {
        throw new UnsupportedOperationException(); // TODO
    }

    <A, Mb> Callable<Mb> bind(Callable<Ma> ma, Applicable<A, Mb> function);

    Callable<Ma> fail(Callable<String> message);

    default <Mb> Callable<Mb> then(Callable<Ma> ma, Callable<Mb> next) {
        return bind(ma, applicable(arg -> flatCallable(() -> next)));
    }

    <A> Callable<Ma> wrap(Callable<A> a);
}