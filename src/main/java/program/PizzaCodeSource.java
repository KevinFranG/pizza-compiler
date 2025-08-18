package program;

import compiler.lexical.LexicalAnalyzer;
import compiler.lexical.Token;
import compiler.parser.ASTNode;
import compiler.parser.Parser;
import compiler.semantic.InvalidPathException;
import compiler.semantic.SemanticAnalyzer;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Getter
public class PizzaCodeSource {
    private final Path path;
    private final String name;
    private final String root;
    private final BufferedReader buffer;

    private static final Path programResourceFolder = Paths.get("ingredients");
    private static final Path relativeResourceFolder = Paths.get("resources");

    private final boolean showProcess;

    public PizzaCodeSource(@NotNull File file, boolean showProcess) {
        try {
            this.buffer = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(
                "Pizza buffer (.pf) root='%s' could not be found"
                    .formatted(file.getPath()));
        }

        this.path = Paths.get(file.getPath()).toAbsolutePath();
        System.out.println(path);
        this.name = path.getFileName().toString();
        this.root = path.getParent().toString();
        this.showProcess = showProcess;

        checkExtension();
    }

    public PizzaCodeSource(@NotNull BufferedReader buffer, @NotNull Path path, boolean showProcess) {
        this.buffer = buffer;

        this.path = path.toAbsolutePath();
        this.name = path.getFileName().toString();
        this.root = null;
        this.showProcess = showProcess;

        checkExtension();
    }

    public PizzaCodeSource(@NotNull String code) {
        try {
            File temporaryFile = File.createTempFile("aux_program", ".pf");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(temporaryFile))) {
                writer.write(code);
            }

            this.buffer = new BufferedReader(new FileReader(temporaryFile));

            this.path = Paths.get(temporaryFile.getPath());
            this.name = path.getFileName().toString();
            this.root = (path.getParent() == null ? Paths.get(".") : path.getParent()).toString();
            this.showProcess = false;

            checkExtension();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create temporary buffer", e);
        }
    }

    /**
     * This method does the compilation of code in its object.
     * When the method compiles first does the lexical analyzer to get tokens (named tokenization
     * of code).
     * Then does the semantic analysis (named parsing too) when doing this, get the sourceCodePath's
     * AST node (abstract syntax tree) containing the correct struct of this sourceCodePath.
     * Finally, does the semantic analysis, checking the correct use of each variable or instruction.
     *
     * @return a list with the instructions to be executed.
     */
    public SemanticAnalyzer.Intermediate compile() {
        try {
            LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer(this);
            List<Token> tokens = lexicalAnalyzer.analyze();

            if (showProcess) {
                if (!tokens.isEmpty())
                    System.out.printf("\n\n%s\n----TOKENS----%n", path);
                tokens.forEach(System.out::println);
            }

            Parser parser = new Parser(this, tokens);
            ASTNode programNode = parser.parse();


            if (showProcess) {
                System.out.printf("\n\n%s\n----PROGRAM ASTNode----%n", path);
                System.out.println(programNode);
            }

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(programNode);
            var intermediate = semanticAnalyzer.analyze();

            if (showProcess) {
                if (!intermediate.instructions.isEmpty())
                    System.out.printf("\n\n%s\n----SEMANTIC----%n", path);
                intermediate.instructions.forEach(System.out::println);
            }

            return intermediate;
        } catch (IOException e) {
            throw new RuntimeException("File %s could not be access or read"
                .formatted(buffer));
        }
    }

    /**
     * Checks if the path provided is absolute or not, that means, the path could be a root
     * path (for example, c:\\users\\...) or relative to the sourceCodePath (for example, a resource named
     * foto.png that is in a resources folder next to the source path of the code sourceCodePath).
     * Finally, the method adds the root path if the path provided is relative, adding the resource
     * folder, otherwise, returns the absolute path as it is.
     *
     * @param path the path to be validated, this path could be absolute or relative.
     * @return a URI with the correct direction of this resource.
     */
    public @NotNull URI generateURI(@NotNull Path path) {
        if (path.isAbsolute()) return path.toUri();

        Path root = this.path.getParent();
        return root.resolve(path).toUri();
    }

    /**
     * The method checks if the path is a compiler resource reference (this means the resource that
     * the code is trying to access is in the resource of this sourceCodePath - the compiler sourceCodePath - inside
     * the ingredients folder).
     * If it is not a compiler resource, then it could be an absolute or relative path.
     *
     * @return A BufferedImage that contains the canvas read if all went good.
     */
    public @NotNull InputStream getResource(@NotNull Path path) throws FileNotFoundException {
        InputStream input = PizzaCodeSource.class.getClassLoader()
            .getResourceAsStream(path.toString().replace('\\', '/'));

        if (input != null)
            return input;

        File file = path.toFile();
        if (!file.exists())
            throw new FileNotFoundException("No se encontró el recurso " + path);
        return new FileInputStream(file);
    }

    /**
     * The method checks if the path is a compiler resource reference (this means the resource that
     * the code is trying to access is in the resource of this sourceCodePath - the compiler sourceCodePath -).
     * If it is not a compiler resource, then it could be an absolute or relative path.
     *
     * @return A BufferedImage that contains the canvas read if all went good.
     */
    public @NotNull BufferedReader getBuffer(@NotNull Path path) throws FileNotFoundException {
        InputStream is = PizzaCodeSource.class.getClassLoader()
            .getResourceAsStream(path.toString());

        if (is != null)
            return new BufferedReader(new InputStreamReader(is));

        File file = path.toFile();
        if (file.exists())
            return new BufferedReader(new FileReader(file));

        throw new FileNotFoundException("No se encontró el archivo " + path);
    }

    private void checkExtension() {
        String extension = name.substring(name.lastIndexOf(".") + 1);

        if (!extension.equalsIgnoreCase("pf"))
            throw new IllegalArgumentException(
                "The buffer's extension '%s' is not available. Use instead 'pf' extension"
                    .formatted(extension));
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
