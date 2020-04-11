package com.mkb90.app;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.api.gax.paging.Page;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.dataproc.Dataproc;
import com.google.api.services.dataproc.DataprocScopes;
import com.google.api.services.dataproc.model.HadoopJob;
import com.google.api.services.dataproc.model.Job;
import com.google.api.services.dataproc.model.JobPlacement;
import com.google.api.services.dataproc.model.SubmitJobRequest;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.common.collect.ImmutableList;

public class SearchEngine {

    static JTextArea fileNamesList = new JTextArea(200, 100);
    static ArrayList<String> filePathList = new ArrayList<String>();
    static ArrayList<String> fileNameArray = new ArrayList<String>();
    private static JFrame frame = new JFrame("Search Engine");
    private static ActionListener listen = new Listener();
    private static String outputData;

    public static void main(String[] args) {
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());

        addMainBox();

        frame.setVisible(true);
    }
    
    public static void addMainBox() {
        Box box = Box.createVerticalBox();
        box.add(addButton("Choose Files", listen));
        box.add(Box.createVerticalStrut(15));
        box.add(fileNamesList);
        box.add(Box.createVerticalStrut(15));
        box.add(addButton("Construct Inverted Indicies", listen));
        
        frame.add(box);
    }
    
    public static void addSearchResults() {
        frame.getContentPane().removeAll();
        
        JLabel label = new JLabel("Inverted Index Results: <Word>   <Total Count>,<DocID>--<DocID Count>");
        Box box = Box.createVerticalBox();
        box.add(label);
        box.add(Box.createVerticalStrut(15));

        fileNamesList = new JTextArea(600, 500);
        fileNamesList.append(outputData);
        fileNamesList.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane (fileNamesList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(600, 500));
        scrollPane.setMinimumSize(new Dimension(600, 500));
        box.add(scrollPane);
        
        frame.add(box);
        frame.getContentPane().validate();
        frame.repaint();
    }
    
    private static JButton addButton(String name, ActionListener listen) {
        JButton button = new JButton(name);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(listen);
        return button;
    }

    private static class Listener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Choose Files")) {
                JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fileChooser.setMultiSelectionEnabled(true);
                int open = fileChooser.showOpenDialog(null);

                if (open == JFileChooser.APPROVE_OPTION) {
                    File[] allFiles = fileChooser.getSelectedFiles();
                    for (File file : allFiles) {
                        fileNamesList.append(fileChooser.getName(file) + "\n");
                        filePathList.add(file.getAbsolutePath());
                        fileNameArray.add(fileChooser.getName(file));
                    }
                }
            }

            if (e.getActionCommand().equals("Construct Inverted Indicies")) {
                for (int i = 0; i < filePathList.size(); i++) {
                    try {
                        uploadObject(fileNameArray.get(i), filePathList.get(i), "DataInput/");
                    } catch (IOException e1) {
                        e1.printStackTrace(); 
                    }
                }

                try {
                    submitJob();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                while (true) {
                    try {
                        downloadObject("Output/", "output.txt");
                        break;
                    } catch (Exception e1) {
                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e2) {
                            e2.printStackTrace();
                        }
                    }
                }

                addSearchResults();
            }
        }
    }
    
    public static void uploadObject(String objectName, String filePath, String folder) throws IOException {
        // The ID of your GCP project
        String projectId = "cloud-computing-final-272116";

        // The ID of your GCS bucket
        String bucketName = "dataproc-staging-us-west1-45037165631-p7rrglgv";

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        BlobId blobId = BlobId.of(bucketName, folder + objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));
    }

    public static void downloadObject(String folder, String outputFile) throws Exception{
        ArrayList<byte[]> mergeData = new ArrayList<byte[]>();
        int arrayLength = 0;

        String projectId = "cloud-computing-final-272116";
        String bucketName = "dataproc-staging-us-west1-45037165631-p7rrglgv";
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
 
        Page<Blob> blobs = storage.list(bucketName, BlobListOption.prefix(folder));
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        Iterator<Blob> iterator = blobs.iterateAll().iterator();
        iterator.next();
        while (iterator.hasNext()) {
            Blob blob = iterator.next();
            if (blob.getName().contains("temp"))
                throw new IOException();
            blob.downloadTo(byteStream);
            mergeData.add(byteStream.toByteArray());
            arrayLength += byteStream.size();
            byteStream.reset();
        }
        byteStream.close();
        doMerge(mergeData, arrayLength, outputFile);
    }
    
    public static void doMerge(ArrayList<byte[]> mergeData, int length, String outputFile) throws IOException {
        byte[] finalMerge = new byte[length];
        
        int destination = 0;
        for (byte[] data: mergeData) {
            System.arraycopy(data, 0, finalMerge, destination, data.length);
            destination += data.length;
        }

        outputData = new String(finalMerge);
    }

    public static void submitJob() throws IOException {
        Dataproc dataproc = new Dataproc.Builder(new NetHttpTransport(), new JacksonFactory(), 
            new HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault().createScoped(DataprocScopes.all())))
            .setApplicationName("SearchEngine/1.0").build();
        dataproc.projects().regions().jobs().submit("cloud-computing-final-272116", "us-west1", new SubmitJobRequest()
            .setJob(new Job().setPlacement(new JobPlacement().setClusterName("hadoop-cluster-eb09"))
            .setHadoopJob(new HadoopJob().setMainClass("InvertedIndex")
                .setJarFileUris(ImmutableList.of("gs://dataproc-staging-us-west1-45037165631-p7rrglgv/invertedindex.jar"))
                .setArgs(ImmutableList.of(
                    "gs://dataproc-staging-us-west1-45037165631-p7rrglgv/DataInput", "gs://dataproc-staging-us-west1-45037165631-p7rrglgv/Output")))))
        .execute();
    }
}
