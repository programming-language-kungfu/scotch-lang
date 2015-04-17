package scotch.compiler.scanner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static scotch.compiler.scanner.Scanner.forString;
import static scotch.compiler.scanner.Token.TokenKind.ID;
import static scotch.compiler.scanner.Token.TokenKind.IN;
import static scotch.compiler.scanner.Token.TokenKind.OPEN_CURLY;
import static scotch.compiler.scanner.Token.TokenKind.CLOSE_CURLY;
import static scotch.compiler.scanner.Token.TokenKind.SEMICOLON;
import static scotch.compiler.util.TestUtil.token;
import static scotch.compiler.util.TestUtil.tokenAt;

import java.net.URI;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class LayoutScannerTest {

    @Rule public final TestName testName = new TestName();

    @Test
    public void sequentialLet_shouldCollapseIntoSingleLet() {
        Scanner scanner = scan(
            "let one = 1",
            "let two = 2",
            "three one two"
        );
        assertThat(tokenAt(scanner, 2), equalTo(token(OPEN_CURLY, "{")));
        assertThat(tokenAt(scanner, 4), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(ID, "two")));
        assertThat(tokenAt(scanner, 3), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(CLOSE_CURLY, "}")));
        assertThat(tokenAt(scanner, 1), equalTo(token(IN, "in")));
        assertThat(tokenAt(scanner, 1), equalTo(token(ID, "three")));
    }

    @Test
    public void shouldAcceptLetWithoutIn_whenScopeOnSeparateLine() {
        Scanner scanner = scan(
            "let x = 1",
            "    y = 2",
            "x + y"
        );
        assertThat(tokenAt(scanner, 2), equalTo(token(OPEN_CURLY, "{")));
        assertThat(tokenAt(scanner, 4), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(ID, "y")));
        assertThat(tokenAt(scanner, 3), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(CLOSE_CURLY, "}")));
        assertThat(tokenAt(scanner, 1), equalTo(token(IN, "in")));
    }

    @Test
    public void shouldGetTokensOnSameLine() {
        Scanner scanner = scan("one two three");
        assertThat(scanner.nextToken(), equalTo(token(ID, "one")));
        assertThat(scanner.nextToken(), equalTo(token(ID, "two")));
        assertThat(scanner.nextToken(), equalTo(token(ID, "three")));
    }

    @Test
    public void shouldIndentAccordingToFirstTokenAfterLet() {
        Scanner scanner = scan(
            "let",
            "  x = 1",
            "  y = 2",
            "x + y"
        );
        assertThat(tokenAt(scanner, 2), equalTo(token(OPEN_CURLY, "{")));
        assertThat(tokenAt(scanner, 4), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(ID, "y")));
        assertThat(tokenAt(scanner, 3), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(CLOSE_CURLY, "}")));
        assertThat(tokenAt(scanner, 1), equalTo(token(IN, "in")));
        assertThat(tokenAt(scanner, 1), equalTo(token(ID, "x")));
    }

    @Test
    public void shouldInsertCurliesAroundLet() {
        Scanner scanner = scan(
            "let x = y",
            "    z = a",
            "in x + z"
        );
        assertThat(tokenAt(scanner, 2), equalTo(token(OPEN_CURLY, "{")));
        assertThat(tokenAt(scanner, 4), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(ID, "z")));
        assertThat(tokenAt(scanner, 3), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(CLOSE_CURLY, "}")));
        assertThat(tokenAt(scanner, 1), equalTo(token(IN, "in")));
    }

    @Test
    public void shouldInsertCurliesAroundLetOnOneLine() {
        Scanner scanner = scan("let z = \\x -> y in z");
        assertThat(tokenAt(scanner, 2), equalTo(token(OPEN_CURLY, "{")));
        assertThat(tokenAt(scanner, 7), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(CLOSE_CURLY, "}")));
        assertThat(tokenAt(scanner, 1), equalTo(token(IN, "in")));
    }

    @Test
    public void shouldInsertSemicolonBetweenLinesWithSameIndent() {
        Scanner scanner = scan(
            "these are",
            "separate"
        );
        assertThat(tokenAt(scanner, 3), equalTo(token(SEMICOLON, ";")));
    }

    @Test
    public void shouldLayoutMatch() {
        Scanner scanner = scan(
            "fib n = match n on",
            "    0 = 0",
            "    1 = 1",
            "    n = fib (n - 1) + fib (n - 2)",
            "main = ..."
        );
        assertThat(tokenAt(scanner, 7), equalTo(token(OPEN_CURLY, "{")));
        assertThat(tokenAt(scanner, 4), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 4), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 16), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(CLOSE_CURLY, "}")));
        assertThat(tokenAt(scanner, 1), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(ID, "main")));
    }

    @Test
    public void shouldLayoutWhere_whenEndingInEof() {
        Scanner scanner = scan(
            "where",
            "    (+) :: a",
            "    (-) :: a",
            "    abs :: a"
        );
        assertThat(tokenAt(scanner, 2), equalTo(token(OPEN_CURLY, "{")));
        assertThat(tokenAt(scanner, 6), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 6), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 4), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(CLOSE_CURLY, "}")));
    }

    @Test
    public void shouldLayoutWhere_whenEndingWithDedent() {
        Scanner scanner = scan(
            "where",
            "    (+) :: a",
            "    (-) :: b",
            "    abs :: c",
            "sub :: d"
        );
        assertThat(tokenAt(scanner, 2), equalTo(token(OPEN_CURLY, "{")));
        assertThat(tokenAt(scanner, 6), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 6), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 4), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(CLOSE_CURLY, "}")));
        assertThat(tokenAt(scanner, 1), equalTo(token(SEMICOLON, ";")));
    }

    @Test
    public void shouldLayoutWhere_withMemberOnSameLine() {
        Scanner scanner = scan(
            "where (+) :: a",
            "      (-) :: b",
            "      abs :: c",
            "sub :: d"
        );
        assertThat(tokenAt(scanner, 2), equalTo(token(OPEN_CURLY, "{")));
        assertThat(tokenAt(scanner, 6), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 6), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 4), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(CLOSE_CURLY, "}")));
        assertThat(tokenAt(scanner, 1), equalTo(token(SEMICOLON, ";")));
    }

    @Test
    public void shouldNotLayoutWhenCurliesProvided() {
        Scanner scanner = scan(
            "   let { x = 1;",
            "y = 2;",
            " } in x + y"
        );
        assertThat(tokenAt(scanner, 2), equalTo(token(OPEN_CURLY, "{")));
        assertThat(tokenAt(scanner, 4), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(ID, "y")));
        assertThat(tokenAt(scanner, 3), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(CLOSE_CURLY, "}")));
        assertThat(tokenAt(scanner, 1), equalTo(token(IN, "in")));
    }

    @Test
    public void shouldRemoveNewLineBetweenLinesWithIndent() {
        Scanner scanner = scan(
            "these are",
            "  together"
        );
        assertThat(tokenAt(scanner, 3), equalTo(token(ID, "together")));
    }

    @Test
    public void shouldRemoveNewLinesBetweenAllIndentedLines() {
        Scanner scanner = scan(
            "these are",
            "  all",
            "  together",
            "this is not"
        );
        assertThat(tokenAt(scanner, 3), equalTo(token(ID, "all")));
        assertThat(scanner.nextToken(), equalTo(token(ID, "together")));
        assertThat(scanner.nextToken(), equalTo(token(SEMICOLON, ";")));
    }

    @Test
    public void test() {
        Scanner scanner = scan(
            "let fib n = match n on",
            "        0 = 0",
            "        1 = 1",
            "        n = fib (n - 1) + fib (n - 2)",
            "fib 20"
        );
        assertThat(tokenAt(scanner, 2), equalTo(token(OPEN_CURLY, "{")));
        assertThat(tokenAt(scanner, 7), equalTo(token(OPEN_CURLY, "{")));
        assertThat(tokenAt(scanner, 4), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 4), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 16), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(CLOSE_CURLY, "}")));
        assertThat(tokenAt(scanner, 1), equalTo(token(SEMICOLON, ";")));
        assertThat(tokenAt(scanner, 1), equalTo(token(CLOSE_CURLY, "}")));
        assertThat(tokenAt(scanner, 1), equalTo(token(IN, "in")));
    }

    private Scanner scan(String... lines) {
        return forString(URI.create("test://" + testName.getMethodName()), lines);
    }
}
