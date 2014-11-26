package scotch.compiler.syntax.builder;

import static scotch.compiler.syntax.Import.moduleImport;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.Optional;
import scotch.compiler.syntax.Import;
import scotch.compiler.syntax.Import.ModuleImport;
import scotch.compiler.syntax.SourceRange;

public abstract class ImportBuilder<T extends Import> implements SyntaxBuilder<T> {

    public static ModuleImportBuilder moduleImportBuilder() {
        return new ModuleImportBuilder();
    }

    private ImportBuilder() {
        // intentionally empty
    }

    public abstract T build();

    public abstract ImportBuilder<T> withSourceRange(SourceRange sourceRange);

    public static class ModuleImportBuilder extends ImportBuilder<ModuleImport> {

        private Optional<SourceRange> sourceRange = Optional.empty();
        private Optional<String>      moduleName  = Optional.empty();

        @Override
        public ModuleImport build() {
            return moduleImport(require(moduleName, "Module name"))
                .withSourceRange(require(sourceRange, "Source range"));
        }

        public ModuleImportBuilder withModuleName(String moduleName) {
            this.moduleName = Optional.of(moduleName);
            return this;
        }

        @Override
        public ModuleImportBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }
    }
}
