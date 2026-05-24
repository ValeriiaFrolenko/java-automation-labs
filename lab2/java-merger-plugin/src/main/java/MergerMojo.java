import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Mojo(name = "merge", defaultPhase = LifecyclePhase.PACKAGE)
public class MergerMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/java", readonly = true)
    private File sourceDirectory;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        File outputFile = new File(outputDirectory, "merged.java");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
             Stream<Path> paths = Files.walk(sourceDirectory.toPath())) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            writer.write("// ======= " + path.getFileName() + " =======");
                            writer.newLine();
                            writer.write(Files.readString(path));
                            writer.newLine();
                            writer.newLine();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            getLog().info("Merged file created: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Error merging files", e);
        }
    }
}
