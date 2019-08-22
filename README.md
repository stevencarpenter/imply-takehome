# imply-takehome
Contained in this repo is the takehome project solution for the Imply.io interview

Completed by: Steven Carpenter

## Usage
First place the unzipped log file in the root of the working directory with the filename `access.log`.

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

I approached this problem from the angle that I would need to keep a certain amount of data in memory in order to satisfy the requirement that the paths were unique. The goal was to greatly reduce the amount of data worked on at one time by utilizing temp files following a split of the initial log. 

To split the log I iterated through the log file line by line in parallel and pushed the lines in the log into temp files corresponding to the last 3 digits of the userId in each line. In the case of the test file, this resulted in a fairly even split due to the high cardinality of the last three digits of each userId. In a real world excercise this might not be as gaurunteed, especially if the log was from an older application that could have considerably more inactive users. I stuck with this approach to reducing the file size because it made sure that each file would contain all the paths for all the userIds that it contained. Additionally, I maintained a lever to pull if there where billions of unique userIds where I might need to use many more temp files while still never resorting to one temp file per user. This would be a great area for improvement in this project. Even though this method ensures far less than one file per user, it still could result in millions of files if the user base was in the billions.

With the files split, I then iterated over each of the temp files line by line and populated a Map with each userId as the key and a list of unique paths they had visited. The idea here is that each list of paths per userId would never have more than `n-1` entries in the list. This is also an imperfect approach for very large numbers `n` in terms of space complexity, however it is a significant improvement over the same method with one file. 

I took a couple of shortcuts when adding to the cache Map to alleviate as much space in memory as possible. First, I kept a list of satisfied userIds on each temp file iteration and checked against this before going any further. If it was satisfied, I had no reason to add it to my cache and waste memory. Second, I checked `if (n == 1)`. In this case every user should appear exactly once in the final list so there was no need to cache anything and I wrote each userId directly to the success file. This covered one of a few edge cases that might plague this implementation that I will discuss further below. Third, before adding to the cache I tested if the addition of this path would satisfy the `n` requirement and if it would then I instead wrote the userId to the success file, added it to my success list, and removed that key value pair from the Map. Otherwise, I built up entries in the cache until I had exhausted the temp file and it could be flushed for the next iteration.

There are a number of improvements that could be made here and would be necessary if the dataset where absolutely enormous. On the processing side, I had to operate on the stream serially because I ran into a race condition with both the Map and Array of satisfied userIds when running in parallel. I could fix this by building a more robust object around the Map and Array that included a lock.

From the memory side, letting the hash increase unchecked until the end of the temp file is also dangerous. In an earlier iteration I attempted a recursive solution to building up the success file by iterating through the full file in the same manor and dumping to a temp file when I estimated the Map was taking up more memory than I intended. With this method I still had to keep track of the satisfied userIds so it would either result in keeping the entire list of satisfied userIds in memory by the time there was no more possible userIds that could match the criteria (checked by comparing each recursion's file size with the next to check for progress or a file size of 0). I tried to mitigate this by instead checking against the file full of satisfied ids line by line but this, while memory efficient, was very slow.

## Resource needs

I didn't go too deep and profile the application but I did monitor the memory usage by the java process and it seemed reasonable at ~150M of memory usage, though it was CPU hungry. Each run logs the runtime in second to the console. For `n = 4` I consistently saw runtimes of around 54-60 seconds. For large end, memory usage must be higher because each temp file worth of data will fill the Map on each iteration. Processing these was exceptionally fast though because there was no IO writing to the success file. Consistently, most of the time was spent generating the split, likely due to the IO. This could likely be improved by reading splits of the gz file instead of working with the data uncompressed.

## The data

When I poked through the temp files I noticed that they were all ~1,000 records and with 1,000 files and 1,000,000 lines in the log that there was a reasonably high number of unique userIds to cause such a distribution. This was beneficial for the approach that I took because each temp file was roughly the same size so I could estimate memory usage to be roughly consistent through the iterations. There are ~82k unique userIds in the set which means the max result set that would be stored in the List would be ~82k, significantly less than the 1 million lines in the log file, however the extreme corner case would be one path per userId so it is still unsafe to assume that this approach would stand the rigor of such a distribution at scale.

I also assumed that the userId max could get significantly large that the number of digits on the end of the userId to be used for temp files could be increased if needed though I did not make this configurable for this test.

One large assumption that I made is that the data would always be clean like the test data that was provided. In an ideal world everything will come in clean but it doesn't always and this program would benefit from tests that ensured the helper functions handle potential weirdness in their inputs. I tested it thoroughly with manual inputs, but I would not make the same assumption or treat a production system quite the same.

## Thanks!

I appreciate you taking the time to read this and test out my implementation! I'm sure there are much better and worse ways to solve this problem but this is what I came up with over the past couple days. I enjoyed the exercise and I look forward to your feedback.

-Steve

