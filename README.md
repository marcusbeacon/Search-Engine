# Search Engine - 1660 Final Project Submission

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
