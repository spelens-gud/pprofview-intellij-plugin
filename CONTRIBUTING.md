# Contributing Guide

Thank you for your interest in Pprof Plus! We welcome contributions of all kinds.

## How to Contribute

### Reporting Issues

If you find a bug or have a feature suggestion:

1. Search [Issues](https://github.com/spelens-gud/pprofview-intellij-plugin/issues) to see if it already exists
2. If not, create a new issue with:
   - Clear title and description
   - Steps to reproduce (for bugs)
   - Expected vs actual behavior
   - Environment info (IDE version, Go version, OS, etc.)
   - Relevant logs or screenshots

### Submitting Code

1. **Fork the Repository**
   ```bash
   git clone https://github.com/your-username/pprofview-intellij-plugin.git
   cd pprofview-intellij-plugin
   ```

2. **Create a Branch**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/your-bug-fix
   ```

3. **Development**
   - Follow project coding standards (see `.kiro/steering/project-standards.md`)
   - Write clear code comments (in English for internationalization)
   - Ensure all tests pass
   - Add necessary test cases

4. **Commit**
   ```bash
   git add .
   git commit -m "feat: add new feature description"
   # or
   git commit -m "fix: fix issue description"
   ```
   
   Follow [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat:` New feature
   - `fix:` Bug fix
   - `docs:` Documentation update
   - `style:` Code formatting
   - `refactor:` Code refactoring
   - `test:` Test related
   - `chore:` Build/tooling related

5. **Push and Create Pull Request**
   ```bash
   git push origin feature/your-feature-name
   ```
   
   Then create a Pull Request on GitHub:
   - Provide clear PR title and description
   - Link related issues (e.g., `Closes #123`)
   - Wait for code review

## Development Setup

### Prerequisites

- JDK 21+
- IntelliJ IDEA 2025.2+ or GoLand 2025.2+
- Go 1.16+

### Building the Project

```bash
# Build plugin
./gradlew buildPlugin

# Run tests
./gradlew test

# Run IDE (for debugging)
./gradlew runIde

# Code inspection
./gradlew verifyPlugin
```

### Project Structure

```
src/main/kotlin/com/github/spelens/pprofview/
├── actions/         # IDE actions
├── editor/          # Editor extensions
├── model/           # Data models
├── parser/          # pprof file parsing
├── runconfig/       # Run configurations
├── services/        # Service layer
├── startup/         # Startup activities
├── toolWindow/      # Tool windows
├── ui/              # UI components
└── utils/           # Utilities
```

## Code Standards

### Kotlin Code Style

- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Prefer Kotlin features (data class, sealed class, extension functions, etc.)
- Avoid `!!` operator, use safe calls `?.` and Elvis operator `?:`

### Documentation

- All public APIs must have KDoc comments
- Comments in English (for internationalization)
- Add inline comments for complex logic

Example:
```kotlin
/**
 * Parse pprof format performance analysis file
 *
 * @param file The file to parse
 * @return Parsed performance analysis data
 * @throws PprofParseException When file format is incorrect
 */
fun parseProfile(file: VirtualFile): Profile {
    // Implementation
}
```

### Internationalization

- All user-facing strings must be internationalized
- Add entries to both `PprofViewBundle.properties` (English) and `PprofViewBundle_zh_CN.properties` (Chinese)
- Use `PprofViewBundle.message("key")` to retrieve localized strings

Example:
```kotlin
// ❌ Wrong - hardcoded string
val message = "Enable pprof profiling"

// ✅ Correct - internationalized
val message = PprofViewBundle.message("pprof.config.enable")
```

## Testing

- Add unit tests for new features
- Ensure all tests pass: `./gradlew test`
- Place test data in `src/test/testData/` directory

## Documentation

- Update relevant documentation when adding features
- Add usage instructions in README for new features
- Record changes in `[Unreleased]` section of CHANGELOG.md
- Update both English and Chinese versions of documentation

## Release Process

Releases are handled by maintainers:

1. Update version in `gradle.properties`
2. Update `CHANGELOG.md` and `CHANGELOG_EN.md`
3. Create Git tag
4. GitHub Actions automatically builds and publishes to JetBrains Marketplace

## Code of Conduct

- Respect all contributors
- Maintain friendly and professional communication
- Accept constructive criticism
- Focus on what's best for the project

## Getting Help

If you have questions:

- Check the [documentation](README.md)
- Search or create an [Issue](https://github.com/spelens-gud/pprofview-intellij-plugin/issues)
- Refer to [IntelliJ Platform SDK Documentation](https://plugins.jetbrains.com/docs/intellij/)

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).

---

Thank you for contributing! 🎉
