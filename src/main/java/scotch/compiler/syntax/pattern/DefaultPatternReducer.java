package scotch.compiler.syntax.pattern;

import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.value.Values.apply;
import static scotch.compiler.syntax.value.Values.conditional;
import static scotch.compiler.syntax.value.Values.fn;
import static scotch.compiler.syntax.value.Values.id;
import static scotch.symbol.Symbol.symbol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import scotch.compiler.syntax.value.PatternMatcher;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceLocation;
import scotch.symbol.type.VariableType;
import scotch.symbol.util.SymbolGenerator;

public class DefaultPatternReducer implements PatternReducer {

    private final SymbolGenerator     generator;
    private final Deque<PatternState> patterns;

    public DefaultPatternReducer(SymbolGenerator generator) {
        this.generator = generator;
        patterns = new ArrayDeque<>();
    }

    @Override
    public void addAssignment(CaptureMatch capture) {
        patterns.peek().addAssignment(capture);
    }

    @Override
    public void addCondition(Value condition) {
        patterns.peek().addCondition(condition);
    }

    @Override
    public void beginPattern(PatternMatcher matcher) {
        patterns.push(new PatternState(matcher));
    }

    @Override
    public void beginPatternCase(Value body) {
        patterns.peek().beginPatternCase(body);
    }

    @Override
    public void endPattern() {
        patterns.pop();
    }

    @Override
    public void endPatternCase() {
        patterns.peek().endPatternCase();
    }

    @Override
    public Value reducePattern() {
        return patterns.peek().reducePattern();
    }

    private final class CaseState {

        private final List<Value>        conditions;
        private final List<CaptureMatch> assignments;
        private       Value              body;

        public CaseState() {
            this.conditions = new ArrayList<>();
            this.assignments = new ArrayList<>();
        }

        public void addAssignment(CaptureMatch capture) {
            assignments.add(capture);
        }

        public void addCondition(Value condition) {
            conditions.add(condition);
        }

        public void beginPatternCase(Value body) {
            this.body = body;
        }

        public boolean isDefaultCase() {
            return conditions.isEmpty();
        }

        public Value reducePattern() {
            return reduceBody();
        }

        public Value reducePattern(Value result) {
            if (conditions.isEmpty()) {
                return reduceBody();
            } else {
                List<Value> reverseConditions = new ArrayList<>(conditions);
                reverse(conditions);
                Value resultCondition = conditions.get(0);
                for (Value condition : reverseConditions.subList(1, reverseConditions.size())) {
                    resultCondition = apply(
                        apply(id(condition.getSourceLocation(), symbol("scotch.data.bool.(&&)"), reserveType()), resultCondition, reserveType()),
                        condition,
                        reserveType()
                    );
                }
                return conditional(
                    SourceLocation.extent(conditions.stream().map(Value::getSourceLocation).collect(toList())),
                    resultCondition,
                    reduceBody(),
                    result,
                    reserveType()
                );
            }
        }

        private Value reduceBody() {
            Value result = body;
            List<CaptureMatch> reverseAssignments = new ArrayList<>(assignments);
            reverse(reverseAssignments);
            for (CaptureMatch match : reverseAssignments) {
                result = match.reducePattern(generator, result);
            }
            return result;
        }
    }

    private VariableType reserveType() {
        return generator.reserveType();
    }

    private final class PatternState {

        private final PatternMatcher  matcher;
        private final List<CaseState> cases;
        private       CaseState       currentCase;

        public PatternState(PatternMatcher matcher) {
            this.matcher = matcher;
            this.cases = new ArrayList<>();
        }

        public void addAssignment(CaptureMatch capture) {
            currentCase.addAssignment(capture);
        }

        public void addCondition(Value condition) {
            currentCase.addCondition(condition);
        }

        public void beginPatternCase(Value body) {
            currentCase = new CaseState();
            currentCase.beginPatternCase(body);
        }

        public void endPatternCase() {
            cases.add(currentCase);
            currentCase = null;
        }

        public Value reducePattern() {
            Value result = calculateDefaultCase();
            if (cases.size() > 1) {
                List<CaseState> reverseCases = new ArrayList<>(cases);
                reverse(reverseCases);
                for (CaseState patternCase : reverseCases) {
                    result = patternCase.reducePattern(result);
                }
            }
            return fn(
                matcher.getSourceLocation(),
                matcher.getSymbol(),
                matcher.getArguments(),
                result
            );
        }

        private Value calculateDefaultCase() {
            CaseState lastCase = cases.get(cases.size() - 1);
            if (lastCase.isDefaultCase()) {
                return lastCase.reducePattern();
            } else {
                throw new UnsupportedOperationException(); // TODO
            }
        }
    }
}
