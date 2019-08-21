# imply-takehome
Contained in this repo is the takehome project solution for the Imply.io interview

Completed by: Steven Carpenter

## Usage
The application is built with Maven so that will need to be installed and accessible in the path. To build the project navigate to the root of the working directory and run:
```bash
mvn clean install
```

Once the project is built you can run the program providing the `n` unique path threshold as an argument to the following command (in this case I chose 4):
```bash
java -jar target/imply-takehome-1.0-SNAPSHOT.jar 4 
```

You should see lof output in the console that indicates that the program is running with the final log indicating runtime in seconds.
```
~/P/imply-takehome ❯❯❯ java -jar target/imply-takehome-1.0-SNAPSHOT.jar 4                                                                                                                                                                              master ✚ ✱
2019-Aug-21 17:48:59 PM [main] INFO  ParseLog - Finding userId's that have visited 4.
2019-Aug-21 17:48:59 PM [main] INFO  ParseLog - Creating splits based on the last 3 digits of the userId.
2019-Aug-21 17:49:40 PM [main] INFO  ParseLog - Finding userId's in each split that have visited n or more distinct paths.
2019-Aug-21 17:49:55 PM [main] INFO  ParseLog - Start epoch: 1566431339
2019-Aug-21 17:49:55 PM [main] INFO  ParseLog - Split epoch: 1566431380
2019-Aug-21 17:49:55 PM [main] INFO  ParseLog - End epoch: 1566431395
2019-Aug-21 17:49:55 PM [main] INFO  ParseLog - Total Seconds: 56
```

## Approach taken

I approached this problem from the angle that I would need to keep a certain amount of data in memory in order to satisfy the requirement that the paths were unique. Initially I had overlooked this requirement and merely counted the occurrence of ids which would have skewed the results considerably.
