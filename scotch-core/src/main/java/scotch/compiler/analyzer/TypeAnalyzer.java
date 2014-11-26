package scotch.compiler.analyzer;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.DefinitionEntry.scopedEntry;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.SyntaxError.typeError;
import static scotch.compiler.syntax.Type.fn;
import static scotch.compiler.syntax.Type.sum;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.DefinitionEntry;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.EqualMatch;
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.PatternMatcher;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.SymbolTable;
import scotch.compiler.syntax.SyntaxError;
import scotch.compiler.syntax.Type;
import scotch.compiler.syntax.TypeGenerator;
import scotch.compiler.syntax.Unification;
import scotch.compiler.syntax.Unification.UnificationVisitor;
import scotch.compiler.syntax.Unification.Unified;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.LiteralValue;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.ValueVisitor;

public class TypeAnalyzer implements
    DefinitionReferenceVisitor<DefinitionReference>,
    DefinitionVisitor<Definition>,
    ValueVisitor<Value>,
    PatternMatchVisitor<PatternMatch> {

    private final SymbolTable                               symbols;
    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final Deque<Scope>                              scopes;
    private final List<SyntaxError>                         errors;
    private       TypeGenerator                             typeGenerator;

    public TypeAnalyzer(SymbolTable symbols) {
        this.symbols = symbols;
        this.definitions = new HashMap<>();
        this.scopes = new ArrayDeque<>();
        this.errors = new ArrayList<>();
        this.typeGenerator = symbols.getTypeGenerator();
    }

    public SymbolTable analyze() {
        symbols.getDefinition(rootRef()).accept(this);
        return symbols
            .copyWith(definitions.values())
            .withSequence(typeGenerator)
            .withErrors(errors)
            .build();
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
        Type type = matchers.getType();
        for (PatternMatcher pattern : patterns) {
            pattern.withType(type.unify(pattern.getType(), currentScope()).accept(new UnificationVisitor<Type>() {
                @Override
                public Type visit(Unified unified) {
                    return unified.getUnifiedType();
                }

                @Override
                public Type visitOtherwise(Unification unification) {
                    errors.add(typeError(unification, pattern.getSourceRange()));
                    return matchers.getType();
                }
            }));
            type = currentScope().generate(type);
        }
        return matchers.withMatchers(patterns).withType(type);
    }

    @Override
    public PatternMatch visit(CaptureMatch match) {
        return match;
    }

    @Override
    public Value visit(Identifier identifier) {
        return identifier.withType(currentScope().getValue(identifier.getSymbol()));
    }

    @Override
    public Value visit(Apply apply) {
        Value function = apply.getFunction().accept(this);
        Value argument = apply.getArgument().accept(this);
        Type resultType = reserveType();
        return function.getType().unify(fn(argument.getType(), resultType), currentScope()).accept(new UnificationVisitor<Value>() {
            @Override
            public Value visit(Unified unified) {
                return apply.withType(currentScope().getTarget(resultType));
            }

            @Override
            public Value visitOtherwise(Unification unification) {
                errors.add(typeError(unification, apply.getSourceRange()));
                return apply.withType(resultType);
            }
        });
    }

    @Override
    public Value visit(LiteralValue literal) {
        Object value = literal.getValue();
        Type type;
        if (value instanceof Double) {
            type = sum("scotch.data.double.Double");
        } else if (value instanceof Integer) {
            type = sum("scotch.data.int.Int");
        } else if (value instanceof String) {
            type = sum("scotch.data.string.String");
        } else if (value instanceof Boolean) {
            type = sum("scotch.data.bool.Bool");
        } else if (value instanceof Character) {
            type = sum("scotch.data.char.Char");
        } else {
            throw new UnsupportedOperationException(); // TODO
        }
        return literal.withType(type);
    }

    @Override
    public PatternMatch visit(EqualMatch match) {
        return match.withValue(match.getValue().accept(this));
    }

    @Override
    public Definition visitOtherwise(Definition definition) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Value visitOtherwise(Value value) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public PatternMatch visitOtherwise(PatternMatch match) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public DefinitionReference visitOtherwise(DefinitionReference reference) {
        return scoped(reference, () -> symbols.getDefinition(reference).accept(this).getReference());
    }

    private Definition collect(Definition definition) {
        definitions.put(definition.getReference(), scopedEntry(definition, getScope(definition.getReference())));
        return definition;
    }

    private Scope currentScope() {
        return scopes.peek();
    }

    private Scope getScope(DefinitionReference reference) {
        return symbols.getScope(reference);
    }

    private List<DefinitionReference> mapDefinitions(List<DefinitionReference> definitions) {
        return definitions.stream()
            .map(reference -> reference.accept(this))
            .collect(toList());
    }

    private Type reserveType() {
        return typeGenerator.reserveType();
    }

    private <T> T scoped(DefinitionReference reference, Supplier<T> supplier) {
        scopes.push(symbols.getScope(reference));
        try {
            return supplier.get();
        } finally {
            scopes.pop();
        }
    }

    private PatternMatcher visitMatcher(PatternMatcher matcher) {
        return scoped(matcher.getReference(), () -> matcher
                .withMatches(matcher.getMatches().stream().map(match -> match.accept(this)).collect(toList()))
                .withBody(matcher.getBody().accept(this))
        );
    }
}
