# Search Engine - 1660 Final Project Submission

Link to video demo: https://youtu.be/g7XXijibbPw

Requirements completed:
1. First Java Application Implementation and Execution on
Docker
2. Docker to GCP Cluster Communication
3. Inverted Indexing MapReduce Implementation and
Execution on the Cluster (GCP)
4. Successful upload to GCP bucket from GUI

Command to build:  
docker build -t finalproject  

Command to run:  
docker run -it --privileged -e DISPLAY=$DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix -e GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json finalproject mvn package exec:java  

(I did not include my GCP credentials file)

InvertedIndex.java can be found in the InvertedIndex folder  
The main GUI application (SearchEngine.java) can be found in SearchEngine/src/main/java/com/mkb90/app

Maven had to be utilized for this project in order to make connections between GCP and the GUI for uploading, downloading, and submitting jobs. Documentation at https://cloud.google.com/storage/docs/how-to was followed in order to properly write functions for interacting with GCP.

