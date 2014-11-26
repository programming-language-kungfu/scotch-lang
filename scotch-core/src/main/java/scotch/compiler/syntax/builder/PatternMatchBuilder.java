package scotch.compiler.syntax.builder;

import static scotch.compiler.syntax.PatternMatch.capture;
import static scotch.compiler.syntax.PatternMatch.equal;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.Optional;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.EqualMatch;
import scotch.compiler.syntax.SourceRange;
import scotch.compiler.syntax.Symbol;
import scotch.compiler.syntax.Type;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Identifier;

public abstract class PatternMatchBuilder<T extends PatternMatch> implements SyntaxBuilder<T> {

    public static CaptureMatchBuilder captureMatchBuilder() {
        return new CaptureMatchBuilder();
    }

    public static EqualMatchBuilder equalMatchBuilder() {
        return new EqualMatchBuilder();
    }

    private PatternMatchBuilder() {
        // intentionally empty
    }

    public static class CaptureMatchBuilder extends PatternMatchBuilder<CaptureMatch> {

        private Optional<SourceRange> sourceRange = Optional.empty();
        private Optional<Symbol>      symbol      = Optional.empty();
        private Optional<Type>        type        = Optional.empty();

        private CaptureMatchBuilder() {
            // intentionally empty
        }

        @Override
        public CaptureMatch build() {
            return capture(
                require(symbol, "Capture symbol"),
                require(type, "Capture type")
            ).withSourceRange(require(sourceRange, "Source range"));
        }

        public CaptureMatchBuilder withIdentifier(Identifier identifier) {
            this.symbol = Optional.of(identifier.getSymbol());
            this.type = Optional.of(identifier.getType());
            return this;
        }

        @Override
        public CaptureMatchBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }
    }

    public static class EqualMatchBuilder extends PatternMatchBuilder<EqualMatch> {

        private Optional<Value>       value;
        private Optional<SourceRange> sourceRange;

        private EqualMatchBuilder() {
            // intentionally empty
        }

        @Override
        public EqualMatch build() {
            return equal(require(value, "Capture value")).withSourceRange(require(sourceRange, "Source range"));
        }

        @Override
        public EqualMatchBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public EqualMatchBuilder withValue(Value value) {
            this.value = Optional.of(value);
            return this;
        }
    }
}
