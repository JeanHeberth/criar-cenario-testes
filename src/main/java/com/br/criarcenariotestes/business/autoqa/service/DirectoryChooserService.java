package com.br.criarcenariotestes.business.autoqa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.awt.AWTError;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DirectoryChooserService {

    private static final Logger log =
            LoggerFactory.getLogger(DirectoryChooserService.class);

    private static final String DIALOG_TITLE =
            "Selecione a pasta do projeto de automação";

    private static final String MAC_DIRECTORY_PROPERTY =
            "apple.awt.fileDialogForDirectories";

    /**
     * Abre o seletor nativo de diretórios.
     *
     * Retorna Optional.empty() quando:
     * - o ambiente gráfico não estiver disponível;
     * - o usuário cancelar;
     * - nenhuma pasta válida for selecionada;
     * - ocorrer uma falha ao abrir a interface gráfica.
     */
    public Optional<Path> chooseDirectory() {
        logGraphicsEnvironment();

        if (!isGraphicalEnvironmentAvailable()) {
            log.warn(
                    "Seleção gráfica de pasta indisponível. " +
                            "O usuário deverá informar o caminho manualmente."
            );

            return Optional.empty();
        }

        String operatingSystem = System.getProperty(
                "os.name",
                ""
        ).toLowerCase(Locale.ROOT);

        try {
            if (operatingSystem.contains("mac")) {
                return chooseWithMacFileDialog();
            }

            return chooseWithSwingFileChooser();

        } catch (IllegalStateException | AWTError exception) {
            log.error(
                    "Não foi possível abrir o seletor gráfico de pasta. " +
                            "O caminho deverá ser informado manualmente.",
                    exception
            );

            return Optional.empty();
        }
    }

    /**
     * Indica se a JVM possui acesso a um ambiente gráfico.
     */
    public boolean isGraphicalEnvironmentAvailable() {
        try {
            return !GraphicsEnvironment.isHeadless();
        } catch (AWTError exception) {
            log.warn(
                    "Não foi possível identificar o ambiente gráfico da JVM.",
                    exception
            );

            return false;
        }
    }

    /**
     * Seletor utilizado principalmente no Windows e Linux.
     */
    private Optional<Path> chooseWithSwingFileChooser() {
        AtomicReference<File> selectedFile = new AtomicReference<>();
        AtomicReference<Integer> dialogResult =
                new AtomicReference<>(JFileChooser.CANCEL_OPTION);

        Runnable openDialog = () -> {
            JFileChooser chooser = new JFileChooser();

            try {
                chooser.setDialogTitle(DIALOG_TITLE);
                chooser.setFileSelectionMode(
                        JFileChooser.DIRECTORIES_ONLY
                );
                chooser.setMultiSelectionEnabled(false);
                chooser.setAcceptAllFileFilterUsed(false);

                Path initialDirectory = resolveInitialDirectory();

                if (initialDirectory != null) {
                    chooser.setCurrentDirectory(
                            initialDirectory.toFile()
                    );
                }

                int result = chooser.showOpenDialog(null);

                dialogResult.set(result);

                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFile.set(chooser.getSelectedFile());
                }

            } finally {
                chooser.cancelSelection();
            }
        };

        runOnEventDispatchThread(openDialog);

        if (dialogResult.get() != JFileChooser.APPROVE_OPTION) {
            log.info("Seleção de pasta cancelada pelo usuário.");
            return Optional.empty();
        }

        File file = selectedFile.get();

        if (file == null) {
            log.warn(
                    "O seletor foi confirmado, mas nenhuma pasta foi retornada."
            );
            return Optional.empty();
        }

        return validateSelectedDirectory(file.toPath());
    }

    /**
     * Seletor nativo utilizado no macOS.
     */
    private Optional<Path> chooseWithMacFileDialog() {
        AtomicReference<Path> selectedPath = new AtomicReference<>();

        Runnable openDialog = () -> {
            String previousPropertyValue =
                    System.getProperty(MAC_DIRECTORY_PROPERTY);

            Frame frame = null;
            FileDialog dialog = null;

            try {
                System.setProperty(
                        MAC_DIRECTORY_PROPERTY,
                        Boolean.TRUE.toString()
                );

                frame = new Frame();
                frame.setUndecorated(true);

                dialog = new FileDialog(
                        frame,
                        DIALOG_TITLE,
                        FileDialog.LOAD
                );

                dialog.setMultipleMode(false);

                Path initialDirectory = resolveInitialDirectory();

                if (initialDirectory != null) {
                    dialog.setDirectory(
                            initialDirectory.toString()
                    );
                }

                dialog.setVisible(true);

                String directory = dialog.getDirectory();
                String file = dialog.getFile();

                if (directory == null) {
                    log.info(
                            "Seleção de pasta cancelada pelo usuário."
                    );
                    return;
                }

                Path candidate = buildMacSelectedPath(
                        directory,
                        file
                );

                validateSelectedDirectory(candidate)
                        .ifPresent(selectedPath::set);

            } finally {
                if (dialog != null) {
                    dialog.dispose();
                }

                if (frame != null) {
                    frame.dispose();
                }

                restoreMacDirectoryProperty(
                        previousPropertyValue
                );
            }
        };

        runOnEventDispatchThread(openDialog);

        return Optional.ofNullable(selectedPath.get());
    }

    /**
     * Monta o caminho devolvido pelo FileDialog do macOS.
     */
    private Path buildMacSelectedPath(
            String directory,
            String file
    ) {
        Path directoryPath = Path.of(directory)
                .toAbsolutePath()
                .normalize();

        if (file == null || file.isBlank()) {
            return directoryPath;
        }

        Path candidate = directoryPath.resolve(file)
                .toAbsolutePath()
                .normalize();

        /*
         * Dependendo da versão do macOS/Java, o FileDialog pode
         * retornar a própria pasta no campo file ou somente no
         * campo directory.
         */
        if (Files.isDirectory(candidate)) {
            return candidate;
        }

        return directoryPath;
    }

    /**
     * Valida e normaliza o diretório selecionado.
     */
    private Optional<Path> validateSelectedDirectory(
            Path selectedPath
    ) {
        if (selectedPath == null) {
            return Optional.empty();
        }

        Path normalizedPath = selectedPath
                .toAbsolutePath()
                .normalize();

        if (!Files.exists(normalizedPath)) {
            log.warn(
                    "A pasta selecionada não existe: {}",
                    normalizedPath
            );
            return Optional.empty();
        }

        if (!Files.isDirectory(normalizedPath)) {
            log.warn(
                    "O caminho selecionado não é um diretório: {}",
                    normalizedPath
            );
            return Optional.empty();
        }

        if (!Files.isReadable(normalizedPath)) {
            log.warn(
                    "A pasta selecionada não possui permissão de leitura: {}",
                    normalizedPath
            );
            return Optional.empty();
        }

        log.info(
                "Pasta selecionada com sucesso: {}",
                normalizedPath
        );

        return Optional.of(normalizedPath);
    }

    /**
     * Diretório inicial apresentado no seletor.
     */
    private Path resolveInitialDirectory() {
        String userHome = System.getProperty("user.home");

        if (userHome == null || userHome.isBlank()) {
            return null;
        }

        try {
            Path homePath = Path.of(userHome)
                    .toAbsolutePath()
                    .normalize();

            if (Files.isDirectory(homePath)) {
                return homePath;
            }

        } catch (RuntimeException exception) {
            log.debug(
                    "Não foi possível usar a pasta do usuário como diretório inicial.",
                    exception
            );
        }

        return null;
    }

    /**
     * Executa o seletor na Event Dispatch Thread do Swing.
     */
    private void runOnEventDispatchThread(
            Runnable runnable
    ) {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                runnable.run();
                return;
            }

            SwingUtilities.invokeAndWait(runnable);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "A seleção da pasta foi interrompida.",
                    exception
            );

        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() != null
                    ? exception.getCause()
                    : exception;

            throw new IllegalStateException(
                    "Falha ao abrir o seletor de pasta.",
                    cause
            );
        }
    }

    /**
     * Restaura a configuração utilizada pelo seletor do macOS.
     */
    private void restoreMacDirectoryProperty(
            String previousValue
    ) {
        if (previousValue == null) {
            System.clearProperty(MAC_DIRECTORY_PROPERTY);
            return;
        }

        System.setProperty(
                MAC_DIRECTORY_PROPERTY,
                previousValue
        );
    }

    /**
     * Registra informações para diagnóstico do modo headless.
     */
    private void logGraphicsEnvironment() {
        String headlessProperty =
                System.getProperty("java.awt.headless");

        boolean headless;

        try {
            headless = GraphicsEnvironment.isHeadless();
        } catch (AWTError exception) {
            headless = true;
        }

        log.info(
                "Diagnóstico do seletor de pasta: " +
                        "os={}, java.awt.headless={}, graphicsHeadless={}",
                System.getProperty("os.name"),
                headlessProperty,
                headless
        );
    }
}