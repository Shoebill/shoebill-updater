package net.gtaun.updater;

import net.gtaun.updater.constant.FileType;
import net.gtaun.updater.constant.OSType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

/**
 * Created by MarvinWindows on 23.12.2014 in project shoebill-updater.
 * Copyright (c) 2014 Marvin Haschker. All rights reserved.
 */
public class Updater {

    private static String OS = System.getProperty("os.name").toLowerCase();
    private OSType currentOS = null;
    private HashMap<File, String> fileHashes;
    private static String UpdateURL = "http://catboy5.bplaced.net/update.php";
    private static List<FileType> missingFileTypes;

    public Updater() {
        fileHashes = new HashMap<File, String>();
        missingFileTypes = new ArrayList<FileType>(Arrays.asList(FileType.values()));
        currentOS = Utilities.parseOS(OS);
    }

    public void update() {
        File sampServerFile = null;
        if(currentOS == null) {
            System.err.println("Unsupported OS - " + OS);
            return;
        }
        else if(currentOS == OSType.Windows) sampServerFile = new File("samp-server.exe");
        else if(currentOS == OSType.Solaris || currentOS == OSType.Mac || currentOS == OSType.Unix) sampServerFile = new File("samp03svr");
        if(sampServerFile == null) {
            System.err.println("Unsupported OS - " + OS);
            return;
        }
        if(!Utilities.checkForDirectory(sampServerFile)) return;
        checkForUpdates(new File("shoebill", "bootstrap"), new File("plugins"));
    }

    private void checkForUpdates(File bootStrap, File pluginDir) {
        addFileHashes(bootStrap);
        addFileHashes(pluginDir);
        checkMissingFiles();
        getUpdateInformation();
    }

    private void checkMissingFiles() {
        Iterator<FileType> missingFilesIterator = missingFileTypes.iterator();
        while(missingFilesIterator.hasNext()) {
            FileType fileType = missingFilesIterator.next();
            switch (fileType) {
                case DependencyManager:
                    fileHashes.put(new File(new File("shoebill", "bootstrap"), "shoebill-dependency-manager.jar"), "");
                    break;

                case Launcher:
                    fileHashes.put(new File(new File("shoebill", "bootstrap"), "shoebill-launcher.jar"), "");
                    break;

                case Plugin:
                    if(currentOS == OSType.Windows) fileHashes.put(new File("plugins", "Shoebill.dll"), "");
                    else fileHashes.put(new File("plugins", "Shoebill.so"), "");
                    break;
            }
            System.out.println(fileType.toString() + " was missing and was added to download queue.");
            missingFilesIterator.remove();
        }
    }

    private void addFileHashes(File folder) {
        File[] files = folder.listFiles();
        assert files != null;
        for(File file : files) {
            if(file.getName().toLowerCase().startsWith("shoebill-dependency-manager") && file.getName().endsWith(".jar")) {
                fileHashes.put(file, Utilities.hashFile(file, "MD5"));
                if(missingFileTypes.contains(FileType.DependencyManager))
                    missingFileTypes.remove(FileType.DependencyManager);
                System.out.println("Added " + file + " for update queue.");
            }
            else if(file.getName().toLowerCase().startsWith("shoebill-launcher") && file.getName().endsWith(".jar")) {
                fileHashes.put(file, Utilities.hashFile(file, "MD5"));
                if(missingFileTypes.contains(FileType.Launcher))
                    missingFileTypes.remove(FileType.Launcher);
                System.out.println("Added " + file + " for update queue.");
            }
            else if(file.getName().toLowerCase().startsWith("shoebill") && ((file.getName().toLowerCase().endsWith(".dll") && OSType.Windows == currentOS) ||
                    (file.getName().toLowerCase().endsWith(".so") && currentOS != OSType.Windows))) {
                fileHashes.put(file, Utilities.hashFile(file, "MD5"));
                if(missingFileTypes.contains(FileType.Plugin))
                    missingFileTypes.remove(FileType.Plugin);
                System.out.println("Added " + file + " for update queue.");
            }
        }
    }

    private void getUpdateInformation() {
        JSONArray jsonArray = new JSONArray();
        for (Map.Entry<File, String> mapEntry : fileHashes.entrySet()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Filename", mapEntry.getKey().getName());
            jsonObject.put("Filetype", Utilities.getFileType(mapEntry.getKey().getName()).toString());
            jsonObject.put("Filehash", mapEntry.getValue());
            jsonArray.add(jsonObject);
        }
        try {
            String response = Utilities.excutePost(UpdateURL, "json=" + jsonArray.toString());
            JSONArray array=(JSONArray) JSONValue.parse(response);
            downloadNewVersions(array);
        } catch (IOException e) {
            System.err.println("The request (" + UpdateURL + ") could not be send. Please make sure that you are connected to the internet.");
            System.err.println("Try again later.");
        }
    }

    private void downloadNewVersions(JSONArray array) {
        if(array.size() == 0) {
            System.out.println("");
            System.out.println("All files are up-to-date!");
            return;
        }
        System.out.println("/***********************************\\");
        System.out.println("  The download will now be started ");
        System.out.println("  Total files to download: " + array.size());
        System.out.println("/***********************************\\");
        System.out.println("");
        int failedDownloads = 0;
        for(Object object : array) {
            JSONObject jsonObject = (JSONObject)object;
            FileType fileType = FileType.valueOf((String) jsonObject.get("Filetype"));
            try {
                URL website = new URL((String) jsonObject.get("Filedownload"));
                File file;
                switch (fileType)
                {
                    case DependencyManager:
                    case Launcher:
                        file = new File(new File("shoebill", "bootstrap"), (String) jsonObject.get("Filename"));
                        break;

                    case Plugin:
                        file = new File("plugins", (String) jsonObject.get("Filename"));
                        break;

                    default:
                        throw new Exception("Invalid file type " + fileType);
                }
                if(file.exists()) {
                    if(!file.delete())
                        throw new Exception("The file " + file + " could not be deleted. Make sure it's not in use!");
                }
                System.out.println("Starting download for " + file + " (" + website + ")");
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(file);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                rbc.close();
                fos.close();
                System.out.println("File " + file + " was successfully downloaded and replaced.");
            } catch (IOException ex) {
                System.err.println("File (" + jsonObject.get("Filename") + " - " + jsonObject.get("Filedownload") + ") could not be downloaded.");
                ex.printStackTrace();
                failedDownloads++;
            } catch (Exception e) {
                e.printStackTrace();
                failedDownloads++;
            }
        }

        downloadsFinished(failedDownloads);
    }

    private void downloadsFinished(int failedDownloads) {

        /*File resourcesFile = new File("shoebill", "resources.yml");
        if(resourcesFile.exists()) {
            List<String> lines = new ArrayList<String>();
            String line;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(resourcesFile));
                while((line = reader.readLine()) != null) lines.add(line);
                reader.close();
                for(int i = 0; i < lines.size(); i++) {
                    if(lines.get(i).startsWith("offlineMode:"))
                        lines.set(i, "offlineMode: false");
                }
                BufferedWriter writer = new BufferedWriter(new FileWriter(resourcesFile));
                for(String ln: lines) {
                    writer.write(ln);
                    writer.newLine();
                }
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
        File onlineModeOnceFile = new File("shoebill", "ONLINE_MODE_ONCE");
        if(!onlineModeOnceFile.exists())
            try {
                onlineModeOnceFile.createNewFile();
            } catch (IOException e) {
                System.err.println(onlineModeOnceFile.toString() + " could not be created. Please make sure you deactivate offlineMode once.");
            }

        System.out.println("");
        System.out.println("All downloads were finished. Failed downloads: " + failedDownloads);
        System.out.println("Please make sure that you use the correct version of the new files in your configuration files.");
    }

}
