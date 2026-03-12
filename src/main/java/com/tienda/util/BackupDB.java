package com.tienda.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * Backup automático de la base de datos SQLite.
 * Guarda hasta 7 copias rotativas en AppData/TiendaStock/backups/
 */
public class BackupDB {

    private static final int MAX_BACKUPS = 7;

    public static String realizarBackup() {
        try {
            String appData = System.getenv("APPDATA");
            if (appData == null) appData = System.getProperty("user.home");

            File dbOrigen = new File(appData + File.separator + "TiendaStock" + File.separator + "tienda.db");
            if (!dbOrigen.exists()) return null;

            File carpetaBackup = new File(appData + File.separator + "TiendaStock" + File.separator + "backups");
            if (!carpetaBackup.exists()) carpetaBackup.mkdirs();

            String fecha = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
            File destino = new File(carpetaBackup, "tienda_backup_" + fecha + ".db");

            Files.copy(dbOrigen.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Rotar: eliminar backups más viejos si hay más de MAX_BACKUPS
            File[] backups = carpetaBackup.listFiles((dir, name) -> name.startsWith("tienda_backup_"));
            if (backups != null && backups.length > MAX_BACKUPS) {
                Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
                for (int i = 0; i < backups.length - MAX_BACKUPS; i++)
                    backups[i].delete();
            }

            return destino.getAbsolutePath();
        } catch (IOException e) {
            System.err.println("Error en backup: " + e.getMessage());
            return null;
        }
    }

    public static File getCarpetaBackups() {
        String appData = System.getenv("APPDATA");
        if (appData == null) appData = System.getProperty("user.home");
        return new File(appData + File.separator + "TiendaStock" + File.separator + "backups");
    }
}
