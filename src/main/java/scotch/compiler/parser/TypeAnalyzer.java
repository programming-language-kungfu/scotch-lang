package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.syntax.DefinitionEntry.scopedEntry;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.DefinitionReference.scopeRef;
import static scotch.compiler.syntax.SyntaxError.typeError;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Unification;
import scotch.compiler.symbol.Unification.UnificationVisitor;
import scotch.compiler.symbol.Unification.Unified;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.DefinitionEntry;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.EqualMatch;
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.PatternMatcher;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.SyntaxError;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.Argument;
import scotch.compiler.syntax.Value.BoolLiteral;
import scotch.compiler.syntax.Value.CharLiteral;
import scotch.compiler.syntax.Value.DoubleLiteral;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.IntLiteral;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.StringLiteral;
import scotch.compiler.syntax.Value.ValueVisitor;

public class TypeAnalyzer implements
    DefinitionReferenceVisitor<DefinitionReference>,
    DefinitionVisitor<Definition>,
    ValueVisitor<Value>,
    PatternMatchVisitor<PatternMatch> {

    private final DefinitionGraph                           graph;
    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final Deque<Scope>                              scopes;
    private final List<SyntaxError>                         errors;
    private       SymbolGenerator                           symbolGenerator;

    public TypeAnalyzer(DefinitionGraph graph) {
        this.graph = graph;
        this.definitions = new HashMap<>();
        this.scopes = new ArrayDeque<>();
        this.errors = new ArrayList<>();
        this.symbolGenerator = graph.getSymbolGenerator();
    }

    public DefinitionGraph analyze() {
        graph.getDefinition(rootRef()).map(definition -> definition.accept(this));
        return graph
            .copyWith(definitions.values())
            .withSequence(symbolGenerator)
            .appendErrors(errors)
            .build();
    }

    @Override
    public Value visit(BoolLiteral literal) {
        return literal;
    }

    @Override
    public Value visit(CharLiteral literal) {
        return literal;
    }

    @Override
    public Value visit(DoubleLiteral literal) {
        return literal;
    }

    @Override
    public Value visit(FunctionValue function) {
        return scoped(scopeRef(function.getSymbol()), () -> {
            List<Type> argumentTypes = function.getArguments().stream()
                .map(Argument::getType)
                .collect(toList());
            argumentTypes.forEach(currentScope()::specialize);
            try {
                return function
                    .withBody(function.getBody().accept(this))
                    .withArguments(function.getArguments().stream()
                        .map(arg -> arg.withType(currentScope().generate(arg.getType())))
                        .collect(toList()));
            } finally {
                argumentTypes.forEach(currentScope()::generalize);
            }
        });
    }

    @Override
    public Value visit(IntLiteral literal) {
        return literal;
    }

    @Override
    public Value visit(StringLiteral literal) {
        return literal;
    }

    @Override
    public Definition visit(ModuleDefinition definition) {
        return collect(definition.withDefinitions(mapDefinitions(definition.getDefinitions())));
    }

    @Override
    public Definition visit(RootDefinition definition) {
        return collect(definition.withDefinitions(mapDefinitions(definition.getDefinitions())));
    }

    @Override
    public Definition visit(ValueDefinition definition) {
        Value body = definition.getBody().accept(this);
        Type type = currentScope().getSignature(definition.getSymbol()).orElseGet(() -> currentScope().getValue(definition.getSymbol()));
        return type.unify(body.getType(), currentScope()).accept(new UnificationVisitor<Definition>() {
            @Override
            public Definition visit(Unified unified) {
                currentScope().redefineValue(definition.getSymbol(), unified.getUnifiedType());
                return collect(definition.withBody(body).withType(unified.getUnifiedType()));
            }

            @Override
            public Definition visitOtherwise(Unification unification) {
                errors.add(typeError(unification, definition.getSourceRange()));
                return collect(definition.withBody(body).withType(type));
            }
        });
    }

    @Override
    public Value visit(PatternMatchers matchers) {
        List<PatternMatcher> patterns = matchers.getMatchers().stream().map(this::visitMatcher).collect(toList());
        AtomicReference<Type> type = new AtomicReference<>(reserveType());
        patterns = patterns.stream()
            .map(pattern -> pattern.getType().unify(type.get(), currentScope()).accept(new UnificationVisitor<PatternMatcher>() {
                @Override
                public PatternMatcher visit(Unified unified) {
                    Type result = currentScope().generate(unified.getUnifiedType());
                    type.set(result);
                    return pattern.withType(result);
                }

                @Override
                public PatternMatcher visitOtherwise(Unification unification) {
                    errors.add(typeError(unification.flip(), pattern.getSourceRange()));
                    return pattern;
                }
            }))
            .collect(toList());
        return matchers.withMatchers(patterns).withType(type.get());
    }

    @Override
    public PatternMatch visit(CaptureMatch match) {
        Scope scope = currentScope();
        return scope.generate(match.getType())
            .unify(scope.getValue(unqualified(match.getArgument())), scope)
            .accept(new UnificationVisitor<PatternMatch>() {
                @Override
                public PatternMatch visit(Unified unified) {
                    return match.withType(unified.getUnifiedType());
                }

                @Override
                public PatternMatch visitOtherwise(Unification unification) {
                    errors.add(typeError(unification, match.getSourceRange()));
                    return match;
                }
            });
    }

    @Override
    public Value visit(Identifier identifier) {
        return currentScope().bind(identifier);
    }

    @Override
    public Value visit(Apply apply) {
        Value function = apply.getFunction().accept(this);
        Value argument = apply.getArgument().accept(this);
        Type resultType = reserveType();
        return function.getType().unify(fn(argument.getType(), resultType), currentScope()).accept(new UnificationVisitor<Value>() {
            @Override
            public Value visit(Unified unified) {
                Value typedFunction = function.withType(currentScope().generate(function.getType()));
                Value typedArgument = argument.withType(currentScope().generate(argument.getType()));
                return apply
                    .withFunction(typedFunction)
                    .withArgument(typedArgument)
                    .withType(currentScope().generate(resultType));
            }

            @Override
            public Value visitOtherwise(Unification unification) {
                errors.add(typeError(unification, apply.getSourceRange()));
                return apply.withType(resultType);
            }
        });
    }

    @Override
    public PatternMatch visit(EqualMatch match) {
        return match.withValue(match.getValue().accept(this));
    }

    @Override
    public DefinitionReference visitOtherwise(DefinitionReference reference) {
        return scoped(reference, () -> graph.getDefinition(reference).get().accept(this).getReference());
    }

    private Definition collect(Definition definition) {
        definitions.put(definition.getReference(), scopedEntry(definition, getScope(definition.getReference())));
        return definition;
    }

    private Scope currentScope() {
        return scopes.peek();
    }

    private Scope getScope(DefinitionReference reference) {
        return graph.getScope(reference);
    }

    private List<DefinitionReference> mapDefinitions(List<DefinitionReference> definitions) {
        return definitions.stream()
            .map(reference -> reference.accept(this))
            .collect(toList());
    }

    private Type reserveType() {
        return symbolGenerator.reserveType();
    }

    private <T> T scoped(DefinitionReference reference, Supplier<T> supplier) {
        scopes.push(graph.getScope(reference));
        try {
            return supplier.get();
        } finally {
            scopes.pop();
        }
    }

    private PatternMatcher visitMatcher(PatternMatcher matcher) {
        return scoped(matcher.getReference(), () -> {
            Value body = matcher.getBody().accept(this);
            List<PatternMatch> matches = matcher.getMatches().stream()
                .map(match -> match.accept(this))
                .collect(toList());
            return matcher
                .withMatches(matches)
                .withBody(body);
        });
    }
}