package scotch.compiler.syntax.definition;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.reference.DefinitionReference.rootRef;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import scotch.compiler.analyzer.DependencyAccumulator;
import scotch.compiler.analyzer.NameAccumulator;
import scotch.compiler.analyzer.OperatorAccumulator;
import scotch.compiler.analyzer.PatternAnalyzer;
import scotch.compiler.analyzer.PrecedenceParser;
import scotch.compiler.analyzer.ScopedNameQualifier;
import scotch.compiler.analyzer.TypeChecker;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceLocation;

public class RootDefinition extends Definition {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation            sourceLocation;
    private final List<DefinitionReference> definitions;

    RootDefinition(SourceLocation sourceLocation, List<DefinitionReference> definitions) {
        this.sourceLocation = sourceLocation;
        this.definitions = ImmutableList.copyOf(definitions);
    }

    @Override
    public Definition accumulateDependencies(DependencyAccumulator state) {
        return state.scoped(this, () -> withDefinitions(state.accumulateDependencies(definitions)));
    }

    @Override
    public Definition accumulateNames(NameAccumulator state) {
        return state.scoped(this, () -> withDefinitions(state.accumulateNames(definitions)));
    }

    @Override
    public Definition checkTypes(TypeChecker state) {
        return state.keep(this);
    }

    @Override
    public Definition defineOperators(OperatorAccumulator state) {
        return state.scoped(this, () -> withDefinitions(state.defineDefinitionOperators(definitions)));
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof RootDefinition && Objects.equals(definitions, ((RootDefinition) o).definitions);
    }

    @Override
    public Optional<DefinitionReference> generateIntermediateCode(IntermediateGenerator generator) {
        return generator.scoped(this, () -> generator.defineRoot(definitions.stream()
            .map(generator::generateIntermediateCode)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList())));
    }

    @Override
    public DefinitionReference getReference() {
        return rootRef();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(definitions);
    }

    @Override
    public Optional<Definition> parsePrecedence(PrecedenceParser state) {
        return Optional.of(state.scoped(this, () -> withDefinitions(state.mapOptional(definitions, Definition::parsePrecedence))));
    }

    @Override
    public Definition qualifyNames(ScopedNameQualifier state) {
        return state.scoped(this, () -> withDefinitions(state.qualifyDefinitionNames(definitions)));
    }

    @Override
    public Definition reducePatterns(PatternAnalyzer state) {
        return state.scoped(this, () -> withDefinitions(state.reducePatterns(definitions)));
    }

    @Override
    public String toString() {
        return stringify(this);
    }

    public RootDefinition withDefinitions(List<DefinitionReference> definitions) {
        return new RootDefinition(sourceLocation, definitions);
    }

    public static class Builder implements SyntaxBuilder<RootDefinition> {

        private List<DefinitionReference> definitions;
        private Optional<SourceLocation>  sourceLocation;

        private Builder() {
            definitions = new ArrayList<>();
            sourceLocation = Optional.empty();
        }

        @Override
        public RootDefinition build() {
            return Definitions.root(require(sourceLocation, "Source location"), definitions);
        }

        public Builder withModule(DefinitionReference module) {
            definitions.add(module);
            return this;
        }

        @Override
        public Builder withSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }
    }
}
