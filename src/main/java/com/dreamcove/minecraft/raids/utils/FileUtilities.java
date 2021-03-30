package com.dreamcove.minecraft.raids.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class FileUtilities {
    private FileUtilities() {
        // Hide the constructor
    }

    public static void deleteFile(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                assert files != null;
                for (File value : files) {
                    deleteFile(value);
                }
            }

            Files.delete(Paths.get(file.toURI()));
        }
    }

}
