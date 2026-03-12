package org.example.service.tex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * @author
 * Victor Weniger
 */

/**
 * TeXPdfCompiler service
 */
public class TeXPdfCompiler {

/**
 * Method
 */
    public CompileResult compile(String texSource) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("mpe-tex-" + Instant.now().toEpochMilli());
            Path texFile = tempDir.resolve("export.tex");
            Files.writeString(texFile, texSource, StandardCharsets.UTF_8);

            Process process = new ProcessBuilder("pdflatex", "-interaction=nonstopmode", "-halt-on-error", "export.tex")
                    .directory(tempDir.toFile())
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                process.destroyForcibly();
                return new CompileResult(false, null, "pdflatex timed out", output);
            }

            if (process.exitValue() != 0) {
                return new CompileResult(false, null, "pdflatex failed", output);
            }

            Path pdf = tempDir.resolve("export.pdf");
            if (!Files.exists(pdf)) {
                return new CompileResult(false, null, "export.pdf not created", output);
            }

            byte[] bytes = Files.readAllBytes(pdf);
            return new CompileResult(true, bytes, "ok", output);
        } catch (IOException e) {
            return new CompileResult(false, null, "pdflatex unavailable or IO error", String.valueOf(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CompileResult(false, null, "pdflatex interrupted", String.valueOf(e.getMessage()));
        } finally {
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                            .sorted((a, b) -> b.compareTo(a))
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                }
                            });
                } catch (IOException ignored) {
                }
            }
        }
    }

/**
 * CompileResult service
 */
    public record CompileResult(boolean success, byte[] pdfBytes, String message, String compilerOutput) {
    }
}
