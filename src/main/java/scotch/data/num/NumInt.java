package scotch.data.num;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.runtime.RuntimeUtil.callable;

import java.util.List;
import scotch.compiler.symbol.InstanceGetter;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.TypeInstance;
import scotch.compiler.symbol.TypeParameters;
import scotch.compiler.symbol.type.Types;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@TypeInstance(typeClass = "scotch.data.num.Num")
public class NumInt implements Num<Integer> {

    private static final Callable<NumInt> INSTANCE = callable(NumInt::new);

    @InstanceGetter
    public static Callable<NumInt> instance() {
        return INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(Types.sum("scotch.data.int.Int"));
    }

    private NumInt() {
        // intentionally empty
    }

    @Override
    public Callable<Integer> abs(Callable<Integer> operand) {
        return callable(() -> Math.abs(operand.call()));
    }

    @Override
    public Callable<Integer> add(Callable<Integer> left, Callable<Integer> right) {
        return callable(() -> left.call() + right.call());
    }

    @Override
    public Callable<Integer> fromInteger(Callable<Integer> integer) {
        return integer;
    }

    @Override
    public Callable<Integer> multiply(Callable<Integer> left, Callable<Integer> right) {
        return callable(() -> left.call() * right.call());
    }

    @Override
    public Callable<Integer> signum(Callable<Integer> operand) {
        return callable(() -> {
            int value = operand.call();
            if (value > 0) {
                return 1;
            } else if (value < 0) {
                return -1;
            } else {
                return 0;
            }
        });
    }

    @Override
    public Callable<Integer> sub(Callable<Integer> left, Callable<Integer> right) {
        return callable(() -> left.call() - right.call());
    }
}
