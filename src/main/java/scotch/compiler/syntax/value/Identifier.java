package scotch.compiler.syntax.value;

import static java.util.Arrays.asList;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.reference.DefinitionReference.valueRef;
import static scotch.compiler.syntax.value.Values.arg;
import static scotch.compiler.syntax.value.Values.id;
import static scotch.compiler.syntax.value.Values.method;
import static scotch.compiler.syntax.value.Values.unboundMethod;
import static scotch.compiler.util.Pair.pair;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Symbol.QualifiedSymbol;
import scotch.compiler.symbol.Symbol.SymbolVisitor;
import scotch.compiler.symbol.Symbol.UnqualifiedSymbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Pair;

public class Identifier extends Value {

    public static Builder builder() {
        return new Builder();
    }
    private final SourceRange sourceRange;
    private final Symbol      symbol;
    private final Type        type;

    Identifier(SourceRange sourceRange, Symbol symbol, Type type) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return state.addDependency(this);
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return this;
    }

    @Override
    public Optional<Pair<Identifier, Operator>> asOperator(Scope scope) {
        return scope.qualify(symbol)
            .map(scope::getOperator)
            .map(operator -> pair(this, operator));
    }

    public Value bind(Scope scope) {
        Type valueType = scope.getValue(symbol);
        return symbol.accept(new SymbolVisitor<Value>() {
            @Override
            public Value visit(QualifiedSymbol symbol) {
                if (scope.isMember(symbol) || valueType.hasContext()) {
                    return unboundMethod(sourceRange, valueRef(symbol), valueType);
                } else {
                    return method(sourceRange, valueRef(symbol), asList(), valueType);
                }
            }

            @Override
            public Value visit(UnqualifiedSymbol symbol) {
                return arg(sourceRange, symbol.getSimpleName(), valueType);
            }
        });
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return this;
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withType(state.generate(type));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return bind(state.scope()).checkTypes(state);
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Identifier) {
            Identifier other = (Identifier) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, type);
    }

    @Override
    public boolean isOperator(Scope scope) {
        return scope.isOperator(symbol);
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        if (state.isOperator(symbol)) {
            return state.qualify(symbol)
                .map(this::withSymbol)
                .orElseGet(() -> {
                    state.symbolNotFound(symbol, sourceRange);
                    return this;
                });
        } else {
            return this;
        }
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        return state.qualify(symbol)
            .map(this::withSymbol)
            .orElseGet(() -> {
                state.symbolNotFound(symbol, sourceRange);
                return this;
            });
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + ")";
    }

    public Identifier withSourceRange(SourceRange sourceRange) {
        return new Identifier(sourceRange, symbol, type);
    }

    public Identifier withSymbol(Symbol symbol) {
        return new Identifier(sourceRange, symbol, type);
    }

    public Identifier withType(Type type) {
        return new Identifier(sourceRange, symbol, type);
    }

    public static class Builder implements SyntaxBuilder<Identifier> {

        private Optional<Symbol>      symbol;
        private Optional<Type>        type;
        private Optional<SourceRange> sourceRange;

        private Builder() {
            symbol = Optional.empty();
            type = Optional.empty();
            sourceRange = Optional.empty();
        }

        @Override
        public Identifier build() {
            return id(
                require(sourceRange, "Source range"),
                require(symbol, "Identifier symbol"),
                require(type, "Identifier type")
            );
        }

        @Override
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public Builder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }

        public Builder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }
}
