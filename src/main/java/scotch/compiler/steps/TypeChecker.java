package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.definition.DefinitionEntry.entry;
import static scotch.util.StringUtil.quote;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.TypeClassDescriptor;
import scotch.compiler.symbol.TypeInstanceDescriptor;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.symbol.Unification;
import scotch.compiler.symbol.type.SumType;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.type.VariableType;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.definition.ValueDefinition;
import scotch.compiler.syntax.definition.ValueSignature;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Argument;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

public class TypeChecker implements TypeScope {

    private static final Object mark = new Object();

    public static SyntaxError ambiguousTypeInstance(TypeClassDescriptor typeClass, List<Type> parameters, Set<TypeInstanceDescriptor> typeInstances, SourceRange location) {
        return new AmbiguousTypeInstanceError(typeClass, parameters, typeInstances, location);
    }

    public static SyntaxError typeInstanceNotFound(TypeClassDescriptor typeClass, List<Type> parameters, SourceRange location) {
        return new TypeInstanceNotFoundError(typeClass, parameters, location);
    }

    private final DefinitionGraph                           graph;
    private final Map<DefinitionReference, DefinitionEntry> entries;
    private final Deque<Scope>                              scopes;
    private final Deque<Scope>                              closures;
    private final Deque<Object>                             nestings;
    private final Deque<Map<Type, Argument>>                arguments;
    private final List<SyntaxError>                         errors;

    public TypeChecker(DefinitionGraph graph) {
        this.graph = graph;
        this.entries = new HashMap<>();
        this.scopes = new ArrayDeque<>();
        this.closures = new ArrayDeque<>();
        this.nestings = new ArrayDeque<>();
        this.arguments = new ArrayDeque<>(asList(ImmutableMap.of()));
        this.errors = new ArrayList<>();
    }

    public void addLocal(Symbol symbol) {
        closure().addLocal(symbol.getCanonicalName());
    }

    public Definition bind(ValueDefinition definition) {
        return bindMethods(definition
            .withType(scope().generate(definition.getType()))
            .withBody(definition.getBody().bindTypes(this)));
    }

    public void capture(Symbol symbol) {
        closure().capture(symbol.getCanonicalName());
    }

    public DefinitionGraph checkTypes() {
        map(graph.getSortedReferences(), Definition::checkTypes);
        return graph
            .copyWith(entries.values())
            .appendErrors(errors)
            .build();
    }

    public <T extends Scoped> T enclose(T scoped, Supplier<T> supplier) {
        return scoped(scoped, () -> {
            enterNest();
            try {
                return supplier.get();
            } finally {
                leaveNest();
            }
        });
    }

    public void error(SyntaxError error) {
        errors.add(error);
    }

    @Override
    public Unification bind(VariableType variableType, Type targetType) {
        return scope().bind(variableType, targetType);
    }

    @Override
    public void extendContext(Type type, Set<Symbol> additionalContext) {
        scope().extendContext(type, additionalContext);
    }

    @Override
    public void generalize(Type type) {
        scope().generalize(type);
    }

    @Override
    public Type generate(Type type) {
        return scope().generate(type);
    }

    @Override
    public Set<Symbol> getContext(Type type) {
        return scope().getContext(type);
    }

    public Type getRawValue(ValueReference valueRef) {
        return scope().getRawValue(valueRef);
    }

    @Override
    public Type getTarget(Type type) {
        return scope().getTarget(type);
    }

    @Override
    public void implement(Symbol typeClass, SumType type) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public boolean isBound(VariableType variableType) {
        return scope().isBound(variableType);
    }

    @Override
    public boolean isGeneric(VariableType variableType) {
        return scope().isGeneric(variableType);
    }

    public Optional<Definition> getDefinition(DefinitionReference reference) {
        return graph.getDefinition(reference);
    }

    public Type getType(ValueDefinition definition) {
        return scope()
            .getSignature(definition.getSymbol())
            .orElseGet(() -> scope().getValue(definition.getSymbol()));
    }

    public Definition keep(Definition definition) {
        return scoped(definition, () -> definition);
    }

    public List<DefinitionReference> map(List<DefinitionReference> references, BiFunction<? super Definition, TypeChecker, ? extends Definition> function) {
        return references.stream()
            .map(this::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(definition -> function.apply(definition, this))
            .map(Definition::getReference)
            .collect(toList());
    }

    public void redefine(ValueDefinition definition) {
        scope().redefineValue(definition.getSymbol(), definition.getType());
    }

    public void redefine(ValueSignature signature) {
        scope().redefineSignature(signature.getSymbol(), signature.getType());
    }

    public VariableType reserveType() {
        return scope().reserveType();
    }

    public Scope scope() {
        return scopes.peek();
    }

    public <T extends Scoped> T scoped(T scoped, Supplier<T> supplier) {
        enterScope(scoped);
        try {
            T result = supplier.get();
            entries.put(result.getReference(), entry(scope(), result.getDefinition()));
            return result;
        } finally {
            leaveScope();
        }
    }

    @Override
    public void specialize(Type type) {
        scope().specialize(type);
    }

    private Definition bindMethods(ValueDefinition definition) {
        throw new UnsupportedOperationException(); // TODO
    }

    private Scope closure() {
        return closures.peek();
    }

    private void enterNest() {
        if (isNested()) {
            closures.push(scope());
        }
        nestings.push(mark);
    }

    private <T extends Scoped> void enterScope(T scoped) {
        Scope scope = graph.tryGetScope(scoped.getReference())
            .orElseGet(() -> Optional.ofNullable(entries.get(scoped.getReference()))
                .map(DefinitionEntry::getScope)
                .orElseThrow(() -> new IllegalArgumentException("No scope found for reference " + scoped.getReference())));
        scopes.push(scope);
    }

    private boolean isNested() {
        return !nestings.isEmpty();
    }

    private void leaveNest() {
        nestings.pop();
        if (isNested()) {
            closures.pop();
        }
    }

    private void leaveScope() {
        scopes.pop();
    }

    public List<Value> bindMethods(List<Value> values) {
        return values.stream()
            .map(value -> value.bindMethods(this))
            .collect(toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends Value> List<T> bindTypes(List<T> values) {
        return values.stream()
            .map(value -> (T) value.bindTypes(this))
            .collect(toList());
    }

    public List<Value> checkTypes(List<Value> values) {
        return values.stream()
            .map(value -> value.checkTypes(this))
            .collect(toList());
    }

    @EqualsAndHashCode
    @ToString
    public static class AmbiguousTypeInstanceError extends SyntaxError {

        private final TypeClassDescriptor         typeClass;
        private final List<Type>                  parameters;
        private final Set<TypeInstanceDescriptor> typeInstances;
        private final SourceRange                 location;

        private AmbiguousTypeInstanceError(TypeClassDescriptor typeClass, List<Type> parameters, Set<TypeInstanceDescriptor> typeInstances, SourceRange location) {
            this.typeClass = typeClass;
            this.parameters = parameters;
            this.typeInstances = typeInstances;
            this.location = location;
        }

        @Override
        public String prettyPrint() {
            return "Ambiguous instance of " + quote(typeClass.getSymbol().getCanonicalName())
                + " for parameters [" + parameters.stream().map(Type::toString).collect(joining(", ")) + "];"
                + " instances found in modules [" + typeInstances.stream().map(TypeInstanceDescriptor::getModuleName).collect(joining(", ")) + "]"
                + " " + location.prettyPrint();
        }
    }

    @EqualsAndHashCode
    @ToString
    public static class TypeInstanceNotFoundError extends SyntaxError {

        private final TypeClassDescriptor typeClass;
        private final List<Type>          parameters;
        private final SourceRange         location;

        private TypeInstanceNotFoundError(TypeClassDescriptor typeClass, List<Type> parameters, SourceRange location) {
            this.typeClass = typeClass;
            this.parameters = ImmutableList.copyOf(parameters);
            this.location = location;
        }

        @Override
        public String prettyPrint() {
            return "Instance of type class " + quote(typeClass.getSymbol().getCanonicalName())
                + " not found for parameters [" + parameters.stream().map(Type::toString).collect(joining(", ")) + "]"
                + " " + location.prettyPrint();
        }
    }
}