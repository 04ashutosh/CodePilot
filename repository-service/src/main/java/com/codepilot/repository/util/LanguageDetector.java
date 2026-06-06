package com.codepilot.repository.util;

import java.util.Map;
import java.util.Set;

/**
 * Detects programming language from file extensions and identifies
 * files/directories that should be excluded from indexing.
 *
 * Design: Static utility class (no Spring bean needed) since it holds
 * only pure functions and constant lookup maps.
 */
public final class LanguageDetector {

    private LanguageDetector() {} // Prevent instantiation

    // ===== Extension → Language Mapping =====
    // Covers all major languages. Returns null for unknown extensions.
    private static final Map<String, String> EXTENSION_TO_LANGUAGE = Map.ofEntries(
            // JVM
            Map.entry("java", "Java"),
            Map.entry("kt", "Kotlin"),
            Map.entry("kts", "Kotlin"),
            Map.entry("groovy", "Groovy"),
            Map.entry("scala", "Scala"),

            // Web Frontend
            Map.entry("js", "JavaScript"),
            Map.entry("jsx", "JavaScript"),
            Map.entry("ts", "TypeScript"),
            Map.entry("tsx", "TypeScript"),
            Map.entry("html", "HTML"),
            Map.entry("htm", "HTML"),
            Map.entry("css", "CSS"),
            Map.entry("scss", "SCSS"),
            Map.entry("sass", "SASS"),
            Map.entry("less", "LESS"),
            Map.entry("vue", "Vue"),
            Map.entry("svelte", "Svelte"),

            // Python
            Map.entry("py", "Python"),
            Map.entry("pyi", "Python"),
            Map.entry("pyx", "Python"),

            // Systems
            Map.entry("c", "C"),
            Map.entry("h", "C"),
            Map.entry("cpp", "C++"),
            Map.entry("hpp", "C++"),
            Map.entry("cc", "C++"),
            Map.entry("cxx", "C++"),
            Map.entry("rs", "Rust"),
            Map.entry("go", "Go"),

            // .NET
            Map.entry("cs", "C#"),
            Map.entry("fs", "F#"),
            Map.entry("vb", "Visual Basic"),

            // Mobile
            Map.entry("swift", "Swift"),
            Map.entry("m", "Objective-C"),
            Map.entry("dart", "Dart"),

            // Scripting
            Map.entry("rb", "Ruby"),
            Map.entry("php", "PHP"),
            Map.entry("pl", "Perl"),
            Map.entry("lua", "Lua"),
            Map.entry("r", "R"),
            Map.entry("jl", "Julia"),
            Map.entry("ex", "Elixir"),
            Map.entry("exs", "Elixir"),
            Map.entry("erl", "Erlang"),
            Map.entry("clj", "Clojure"),
            Map.entry("hs", "Haskell"),

            // Shell
            Map.entry("sh", "Shell"),
            Map.entry("bash", "Shell"),
            Map.entry("zsh", "Shell"),
            Map.entry("fish", "Shell"),
            Map.entry("ps1", "PowerShell"),
            Map.entry("bat", "Batch"),
            Map.entry("cmd", "Batch"),

            // Data / Config
            Map.entry("json", "JSON"),
            Map.entry("xml", "XML"),
            Map.entry("yaml", "YAML"),
            Map.entry("yml", "YAML"),
            Map.entry("toml", "TOML"),
            Map.entry("ini", "INI"),
            Map.entry("properties", "Properties"),
            Map.entry("env", "Environment"),

            // Query
            Map.entry("sql", "SQL"),
            Map.entry("graphql", "GraphQL"),
            Map.entry("gql", "GraphQL"),

            // Docs
            Map.entry("md", "Markdown"),
            Map.entry("rst", "reStructuredText"),
            Map.entry("txt", "Text"),
            Map.entry("adoc", "AsciiDoc"),

            // Build / Infra
            Map.entry("gradle", "Gradle"),
            Map.entry("dockerfile", "Dockerfile"),
            Map.entry("tf", "Terraform"),
            Map.entry("hcl", "HCL"),
            Map.entry("proto", "Protocol Buffers")
    );

    // ===== Directories to Always Skip =====
    // These directories contain dependencies, build artifacts, or IDE configs
    // that would pollute the index with irrelevant code.
    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git",
            "node_modules",
            "target",
            "build",
            "dist",
            "out",
            ".idea",
            ".vscode",
            ".settings",
            ".gradle",
            "__pycache__",
            ".pytest_cache",
            ".mypy_cache",
            "venv",
            ".venv",
            "env",
            ".env",
            ".svn",
            ".hg",
            "vendor",
            "bin",
            "obj",
            ".next",
            ".nuxt",
            "coverage",
            ".angular"
    );

    // ===== Binary Extensions to Skip =====
    // Binary files cannot be meaningfully searched or embedded as vectors.
    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            // Images
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "svg", "webp", "tiff",
            // Audio / Video
            "mp3", "mp4", "wav", "avi", "mkv", "flv", "mov", "ogg", "webm",
            // Archives
            "zip", "tar", "gz", "rar", "7z", "bz2", "xz", "jar", "war", "ear",
            // Compiled / Executables
            "exe", "dll", "so", "dylib", "class", "o", "obj", "pyc", "pyo",
            // Documents (binary format)
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            // Fonts
            "ttf", "otf", "woff", "woff2", "eot",
            // Database
            "db", "sqlite", "sqlite3",
            // Other
            "lock", "map"
    );

    /**
     * Detects the programming language from a file extension.
     * @param extension File extension without the dot (e.g., "java", "ts").
     * @return Language name or null if unknown.
     */
    public static String detectLanguage(String extension) {
        if (extension == null) return null;
        return EXTENSION_TO_LANGUAGE.get(extension.toLowerCase());
    }

    /**
     * Extracts the file extension from a filename.
     * @return Extension without dot, or null if no extension.
     */
    public static String getExtension(String filename) {
        if (filename == null) return null;
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) return null;
        return filename.substring(lastDot + 1).toLowerCase();
    }

    /**
     * Checks if a directory name should be skipped during indexing.
     */
    public static boolean isIgnoredDirectory(String dirName) {
        return IGNORED_DIRECTORIES.contains(dirName);
    }

    /**
     * Checks if a file is binary (non-text) based on its extension.
     */
    public static boolean isBinaryFile(String filename) {
        String ext = getExtension(filename);
        return ext != null && BINARY_EXTENSIONS.contains(ext);
    }
}