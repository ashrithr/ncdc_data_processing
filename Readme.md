Processing weather dataset from NCDC
------------------------------------

##Deploying hadoop on AWS using Ankus
Ankus is an open-source big-data deployment and orchestration framework. Ankus supports many big data frameworks, for
this project we used ankus to deploy hadoop on AWS.

Instructions for installing ankus can be found [here](https://github.com/ashrithr/ankus)

Here is the configuration file used to deploy hadoop on aws on `10 m1.large` slave instances (replace AWS_ACCESS_ID and
AWS_SECRET_KEY with your own values from
[aws management console](https://console.aws.amazon.com/iam/home?#security_credential)):

```yaml
install_mode: 'cloud'
cloud_platform: aws
cloud_credentials:
  aws_access_id: 'YOUR_AWS_ACCESS_ID'
  aws_secret_key: 'YOUR_AWS_SECRET_KEY'
  aws_machine_type: 'm1.large'
  aws_region: 'us-east-1'
  aws_key: 'ankus'
cloud_os_type: CentOS
hadoop_deploy:
  ha: 'disabled'
  mapreduce:
    type: mr1
  ecosystem:
    - hive
    - sqoop
    - pig
hbase_deploy: 'disabled'
zookeeper_deploy: 'disabled'
worker_nodes_count: 3
solr_deploy: 'disabled'
cassandra_deploy: disabled
kafka_deploy: 'disabled'
storm_deploy: 'disabled'
volumes: disabled
security: simple
monitoring: disabled
alerting: disabled
admin_email: ''
log_aggregation: disabled
```

Once, the configuration is in place, you can start the deployment process using:

```
${ANKUS_HOME}/bin/ankus deploy --debug
```

##PreProcessing the NCDC Weather dataset
NCDC weather dataset is a collection of daily weather measurements (temperature, wind speed, humidity, pressure, &c.)
from 9000+ weather stations around the world ranging from 1929-2009. This dataset contains a lot of small files divided
into weather stations ranging in several KiloBytes range. Hadoop does not work well with small files rather works better
with small number of larger files, the reason is that hadoop's namenode stores the metadata information of all the files
located in HDFS (Hadoop Distributed File System) so as the number of files increase in HDFS the amount of metadata need
to be stored by namenode in its increases exponentially, hence it's recommended to store small number of large files
over large number of small files.

Hence the script `DataPreProcessor.sh` merges all the small files from one year and puts it into HDFS as single file.

Before, running the script mount the dataset from a snapshot to one of the instance in the cluster, in this case we
will mount the volume to namenode and run the data preprocessor from there. Find and attach snapshot `snap-ac47f4c5` on
namenode.

> NOTE: make sure to check the availability zone of the instance and the volume being created from snapshot, the volume
> should reside in the same availability zone as that of the instance otherwise the instance will not be able to attach
> the volume.

Once, attached from AWS console log into the namenode instance

```
${ANKUS_HOME}/bin/ankus ssh --role namenode
lsblk
mkdir /ncdc
mount -t ext3 /dev/xvdf /ncdc
```

Run the script from namenode to pre-process the dataset and dump to hdfs.

```
chmod +x src/main/resources/DataPreProcessor.sh
src/main/resources/DataPreProcessor.sh
```

> Note: The above script should be run from node that belongs to the hadoop cluster either from NameNode or DataNodes

This will take a while to complete the dump process.

##Build the mapreduce project using maven
This project contains a maven pom file for managing the project dependencies and its life cycle.

To build a jar, run the following command from projects home path:

```
mvn package
```

##Running the MapReduce application to calculate temperature frequencies

```
sudo -u hdfs hadoop jar mapreduce-1.0-SNAPSHOT.jar \
com.ashrith.mapreduce.MaxTempFreqDriver \
/ncdcdata \
int_data \
final_sort_data
```

##Sending the results to S3

Add the following properties to `core-site.xml`, replacing the ACCESS_ID and SECRET_KEY with your values, from the node
on which you are copying data from to s3.

```xml
<property>
  <name>fs.s3.awsAccessKeyId</name>
  <value>YOUR_ACCESS_ID</value>
</property>
<property>
  <name>fs.s3.awsSecretAccessKey</name>
  <value>YOUR_SECRET_KEY</value>
</property>
<property>
  <name>fs.s3n.awsAccessKeyId</name>
  <value>YOUR_ACCESS_ID</value>
</property>
<property>
  <name>fs.s3n.awsSecretAccessKey</name>
  <value>YOUR_SECRET_KEY</value>
</property>
```

Finally copy the results to s3
```
sudo -u hdfs hadoop fs -cp final_sort_data s3n://BUCKET_NAME/output
```

##Stats

* Time to pre-process dataset: `106m19.684s`
* Time to process weather dataset to find out max temp frequency using 10 m1.large instances: `~ 7m`

###MapReduce Job Output

```
[/ncdcdata, int_data, final_sort_data]
13/12/10 05:38:59 INFO input.FileInputFormat: Total input paths to process : 81
13/12/10 05:39:00 INFO mapred.JobClient: Running job: job_201312100321_0001
13/12/10 05:39:01 INFO mapred.JobClient:  map 0% reduce 0%
13/12/10 05:39:18 INFO mapred.JobClient:  map 1% reduce 0%
13/12/10 05:39:21 INFO mapred.JobClient:  map 2% reduce 0%
13/12/10 05:39:22 INFO mapred.JobClient:  map 4% reduce 0%
13/12/10 05:39:23 INFO mapred.JobClient:  map 5% reduce 0%
13/12/10 05:39:25 INFO mapred.JobClient:  map 7% reduce 0%
13/12/10 05:39:30 INFO mapred.JobClient:  map 8% reduce 0%
13/12/10 05:39:34 INFO mapred.JobClient:  map 9% reduce 0%
13/12/10 05:39:38 INFO mapred.JobClient:  map 10% reduce 0%
13/12/10 05:39:39 INFO mapred.JobClient:  map 11% reduce 0%
13/12/10 05:39:40 INFO mapred.JobClient:  map 12% reduce 0%
13/12/10 05:39:41 INFO mapred.JobClient:  map 13% reduce 0%
13/12/10 05:39:43 INFO mapred.JobClient:  map 14% reduce 3%
13/12/10 05:39:44 INFO mapred.JobClient:  map 15% reduce 3%
13/12/10 05:39:48 INFO mapred.JobClient:  map 16% reduce 3%
13/12/10 05:39:53 INFO mapred.JobClient:  map 16% reduce 4%
13/12/10 05:39:54 INFO mapred.JobClient:  map 17% reduce 4%
13/12/10 05:39:55 INFO mapred.JobClient:  map 18% reduce 4%
13/12/10 05:39:56 INFO mapred.JobClient:  map 19% reduce 5%
13/12/10 05:39:57 INFO mapred.JobClient:  map 20% reduce 5%
13/12/10 05:39:58 INFO mapred.JobClient:  map 21% reduce 5%
13/12/10 05:40:00 INFO mapred.JobClient:  map 22% reduce 5%
13/12/10 05:40:03 INFO mapred.JobClient:  map 23% reduce 5%
13/12/10 05:40:06 INFO mapred.JobClient:  map 23% reduce 7%
13/12/10 05:40:09 INFO mapred.JobClient:  map 24% reduce 7%
13/12/10 05:40:12 INFO mapred.JobClient:  map 25% reduce 7%
13/12/10 05:40:13 INFO mapred.JobClient:  map 26% reduce 7%
13/12/10 05:40:15 INFO mapred.JobClient:  map 28% reduce 7%
13/12/10 05:40:16 INFO mapred.JobClient:  map 28% reduce 8%
13/12/10 05:40:17 INFO mapred.JobClient:  map 29% reduce 8%
13/12/10 05:40:18 INFO mapred.JobClient:  map 30% reduce 8%
13/12/10 05:40:19 INFO mapred.JobClient:  map 30% reduce 9%
13/12/10 05:40:20 INFO mapred.JobClient:  map 31% reduce 9%
13/12/10 05:40:28 INFO mapred.JobClient:  map 32% reduce 9%
13/12/10 05:40:30 INFO mapred.JobClient:  map 33% reduce 9%
13/12/10 05:40:31 INFO mapred.JobClient:  map 34% reduce 10%
13/12/10 05:40:32 INFO mapred.JobClient:  map 35% reduce 10%
13/12/10 05:40:33 INFO mapred.JobClient:  map 36% reduce 10%
13/12/10 05:40:34 INFO mapred.JobClient:  map 37% reduce 11%
13/12/10 05:40:36 INFO mapred.JobClient:  map 38% reduce 11%
13/12/10 05:40:40 INFO mapred.JobClient:  map 38% reduce 12%
13/12/10 05:40:43 INFO mapred.JobClient:  map 39% reduce 12%
13/12/10 05:40:45 INFO mapred.JobClient:  map 40% reduce 12%
13/12/10 05:40:46 INFO mapred.JobClient:  map 40% reduce 13%
13/12/10 05:40:47 INFO mapred.JobClient:  map 41% reduce 13%
13/12/10 05:40:48 INFO mapred.JobClient:  map 42% reduce 13%
13/12/10 05:40:49 INFO mapred.JobClient:  map 43% reduce 13%
13/12/10 05:40:51 INFO mapred.JobClient:  map 44% reduce 13%
13/12/10 05:40:52 INFO mapred.JobClient:  map 44% reduce 14%
13/12/10 05:40:53 INFO mapred.JobClient:  map 45% reduce 14%
13/12/10 05:40:55 INFO mapred.JobClient:  map 45% reduce 15%
13/12/10 05:40:57 INFO mapred.JobClient:  map 46% reduce 15%
13/12/10 05:41:01 INFO mapred.JobClient:  map 47% reduce 15%
13/12/10 05:41:03 INFO mapred.JobClient:  map 48% reduce 15%
13/12/10 05:41:04 INFO mapred.JobClient:  map 49% reduce 15%
13/12/10 05:41:05 INFO mapred.JobClient:  map 50% reduce 15%
13/12/10 05:41:07 INFO mapred.JobClient:  map 51% reduce 15%
13/12/10 05:41:08 INFO mapred.JobClient:  map 51% reduce 16%
13/12/10 05:41:09 INFO mapred.JobClient:  map 52% reduce 16%
13/12/10 05:41:11 INFO mapred.JobClient:  map 53% reduce 17%
13/12/10 05:41:18 INFO mapred.JobClient:  map 54% reduce 18%
13/12/10 05:41:21 INFO mapred.JobClient:  map 55% reduce 18%
13/12/10 05:41:22 INFO mapred.JobClient:  map 56% reduce 18%
13/12/10 05:41:23 INFO mapred.JobClient:  map 57% reduce 18%
13/12/10 05:41:24 INFO mapred.JobClient:  map 58% reduce 18%
13/12/10 05:41:25 INFO mapred.JobClient:  map 59% reduce 18%
13/12/10 05:41:27 INFO mapred.JobClient:  map 60% reduce 19%
13/12/10 05:41:30 INFO mapred.JobClient:  map 61% reduce 19%
13/12/10 05:41:36 INFO mapred.JobClient:  map 62% reduce 19%
13/12/10 05:41:37 INFO mapred.JobClient:  map 63% reduce 20%
13/12/10 05:41:38 INFO mapred.JobClient:  map 64% reduce 20%
13/12/10 05:41:39 INFO mapred.JobClient:  map 65% reduce 20%
13/12/10 05:41:41 INFO mapred.JobClient:  map 66% reduce 21%
13/12/10 05:41:43 INFO mapred.JobClient:  map 67% reduce 21%
13/12/10 05:41:46 INFO mapred.JobClient:  map 67% reduce 22%
13/12/10 05:41:47 INFO mapred.JobClient:  map 68% reduce 22%
13/12/10 05:41:51 INFO mapred.JobClient:  map 69% reduce 22%
13/12/10 05:41:52 INFO mapred.JobClient:  map 70% reduce 22%
13/12/10 05:41:53 INFO mapred.JobClient:  map 70% reduce 23%
13/12/10 05:41:54 INFO mapred.JobClient:  map 71% reduce 23%
13/12/10 05:41:56 INFO mapred.JobClient:  map 72% reduce 23%
13/12/10 05:41:57 INFO mapred.JobClient:  map 73% reduce 23%
13/12/10 05:41:59 INFO mapred.JobClient:  map 74% reduce 23%
13/12/10 05:42:01 INFO mapred.JobClient:  map 75% reduce 23%
13/12/10 05:42:03 INFO mapred.JobClient:  map 76% reduce 24%
13/12/10 05:42:06 INFO mapred.JobClient:  map 76% reduce 25%
13/12/10 05:42:09 INFO mapred.JobClient:  map 77% reduce 25%
13/12/10 05:42:10 INFO mapred.JobClient:  map 78% reduce 25%
13/12/10 05:42:12 INFO mapred.JobClient:  map 79% reduce 25%
13/12/10 05:42:13 INFO mapred.JobClient:  map 80% reduce 25%
13/12/10 05:42:14 INFO mapred.JobClient:  map 81% reduce 25%
13/12/10 05:42:15 INFO mapred.JobClient:  map 82% reduce 26%
13/12/10 05:42:17 INFO mapred.JobClient:  map 83% reduce 26%
13/12/10 05:42:18 INFO mapred.JobClient:  map 83% reduce 27%
13/12/10 05:42:21 INFO mapred.JobClient:  map 84% reduce 28%
13/12/10 05:42:25 INFO mapred.JobClient:  map 86% reduce 28%
13/12/10 05:42:26 INFO mapred.JobClient:  map 87% reduce 28%
13/12/10 05:42:27 INFO mapred.JobClient:  map 88% reduce 29%
13/12/10 05:42:28 INFO mapred.JobClient:  map 89% reduce 29%
13/12/10 05:42:29 INFO mapred.JobClient:  map 90% reduce 29%
13/12/10 05:42:31 INFO mapred.JobClient:  map 91% reduce 30%
13/12/10 05:42:33 INFO mapred.JobClient:  map 92% reduce 30%
13/12/10 05:42:36 INFO mapred.JobClient:  map 93% reduce 30%
13/12/10 05:42:37 INFO mapred.JobClient:  map 94% reduce 31%
13/12/10 05:42:38 INFO mapred.JobClient:  map 95% reduce 31%
13/12/10 05:42:39 INFO mapred.JobClient:  map 97% reduce 31%
13/12/10 05:42:40 INFO mapred.JobClient:  map 97% reduce 32%
13/12/10 05:42:42 INFO mapred.JobClient:  map 98% reduce 32%
13/12/10 05:42:43 INFO mapred.JobClient:  map 99% reduce 33%
13/12/10 05:42:46 INFO mapred.JobClient:  map 100% reduce 33%
13/12/10 05:43:14 INFO mapred.JobClient:  map 100% reduce 67%
13/12/10 05:43:17 INFO mapred.JobClient:  map 100% reduce 69%
13/12/10 05:43:20 INFO mapred.JobClient:  map 100% reduce 71%
13/12/10 05:43:24 INFO mapred.JobClient:  map 100% reduce 73%
13/12/10 05:43:26 INFO mapred.JobClient:  map 100% reduce 75%
13/12/10 05:43:30 INFO mapred.JobClient:  map 100% reduce 77%
13/12/10 05:43:33 INFO mapred.JobClient:  map 100% reduce 79%
13/12/10 05:43:35 INFO mapred.JobClient:  map 100% reduce 81%
13/12/10 05:43:38 INFO mapred.JobClient:  map 100% reduce 83%
13/12/10 05:43:42 INFO mapred.JobClient:  map 100% reduce 85%
13/12/10 05:43:44 INFO mapred.JobClient:  map 100% reduce 87%
13/12/10 05:43:48 INFO mapred.JobClient:  map 100% reduce 89%
13/12/10 05:43:50 INFO mapred.JobClient:  map 100% reduce 91%
13/12/10 05:43:54 INFO mapred.JobClient:  map 100% reduce 93%
13/12/10 05:43:57 INFO mapred.JobClient:  map 100% reduce 95%
13/12/10 05:44:00 INFO mapred.JobClient:  map 100% reduce 98%
13/12/10 05:44:03 INFO mapred.JobClient:  map 100% reduce 99%
13/12/10 05:44:05 INFO mapred.JobClient:  map 100% reduce 100%
13/12/10 05:44:08 INFO mapred.JobClient: Job complete: job_201312100321_0001
13/12/10 05:44:09 INFO mapred.JobClient: Counters: 34
13/12/10 05:44:09 INFO mapred.JobClient:   File System Counters
13/12/10 05:44:09 INFO mapred.JobClient:     FILE: Number of bytes read=3580526758
13/12/10 05:44:09 INFO mapred.JobClient:     FILE: Number of bytes written=5186328912
13/12/10 05:44:09 INFO mapred.JobClient:     FILE: Number of read operations=0
13/12/10 05:44:09 INFO mapred.JobClient:     FILE: Number of large read operations=0
13/12/10 05:44:09 INFO mapred.JobClient:     FILE: Number of write operations=0
13/12/10 05:44:09 INFO mapred.JobClient:     HDFS: Number of bytes read=15532110485
13/12/10 05:44:09 INFO mapred.JobClient:     HDFS: Number of bytes written=14605
13/12/10 05:44:09 INFO mapred.JobClient:     HDFS: Number of read operations=550
13/12/10 05:44:09 INFO mapred.JobClient:     HDFS: Number of large read operations=0
13/12/10 05:44:09 INFO mapred.JobClient:     HDFS: Number of write operations=1
13/12/10 05:44:09 INFO mapred.JobClient:   Job Counters
13/12/10 05:44:09 INFO mapred.JobClient:     Launched map tasks=284
13/12/10 05:44:09 INFO mapred.JobClient:     Launched reduce tasks=1
13/12/10 05:44:09 INFO mapred.JobClient:     Data-local map tasks=281
13/12/10 05:44:09 INFO mapred.JobClient:     Rack-local map tasks=3
13/12/10 05:44:09 INFO mapred.JobClient:     Total time spent by all maps in occupied slots (ms)=4221344
13/12/10 05:44:09 INFO mapred.JobClient:     Total time spent by all reduces in occupied slots (ms)=278959
13/12/10 05:44:09 INFO mapred.JobClient:     Total time spent by all maps waiting after reserving slots (ms)=0
13/12/10 05:44:09 INFO mapred.JobClient:     Total time spent by all reduces waiting after reserving slots (ms)=0
13/12/10 05:44:09 INFO mapred.JobClient:   Map-Reduce Framework
13/12/10 05:44:09 INFO mapred.JobClient:     Map input records=111651032
13/12/10 05:44:09 INFO mapred.JobClient:     Map output records=111579990
13/12/10 05:44:09 INFO mapred.JobClient:     Map output bytes=1338959880
13/12/10 05:44:09 INFO mapred.JobClient:     Input split bytes=34125
13/12/10 05:44:09 INFO mapred.JobClient:     Combine input records=0
13/12/10 05:44:09 INFO mapred.JobClient:     Combine output records=0
13/12/10 05:44:09 INFO mapred.JobClient:     Reduce input groups=1367
13/12/10 05:44:09 INFO mapred.JobClient:     Reduce shuffle bytes=1562121498
13/12/10 05:44:09 INFO mapred.JobClient:     Reduce input records=111579990
13/12/10 05:44:09 INFO mapred.JobClient:     Reduce output records=1367
13/12/10 05:44:09 INFO mapred.JobClient:     Spilled Records=367331702
13/12/10 05:44:09 INFO mapred.JobClient:     CPU time spent (ms)=2160180
13/12/10 05:44:09 INFO mapred.JobClient:     Physical memory (bytes) snapshot=74489319424
13/12/10 05:44:09 INFO mapred.JobClient:     Virtual memory (bytes) snapshot=192007471104
13/12/10 05:44:09 INFO mapred.JobClient:     Total committed heap usage (bytes)=71925497856
13/12/10 05:44:09 INFO mapred.JobClient:   org.csu.cptr5950.mapreduce.MaxTempFreqDriver$Temperature
13/12/10 05:44:09 INFO mapred.JobClient:     MISSING=71042
13/12/10 05:44:09 INFO input.FileInputFormat: Total input paths to process : 1
13/12/10 05:44:10 INFO mapred.JobClient: Running job: job_201312100321_0002
13/12/10 05:44:11 INFO mapred.JobClient:  map 0% reduce 0%
13/12/10 05:44:20 INFO mapred.JobClient:  map 100% reduce 0%
13/12/10 05:44:26 INFO mapred.JobClient:  map 100% reduce 100%
13/12/10 05:44:29 INFO mapred.JobClient: Job complete: job_201312100321_0002
13/12/10 05:44:29 INFO mapred.JobClient: Counters: 32
13/12/10 05:44:29 INFO mapred.JobClient:   File System Counters
13/12/10 05:44:29 INFO mapred.JobClient:     FILE: Number of bytes read=24612
13/12/10 05:44:29 INFO mapred.JobClient:     FILE: Number of bytes written=365966
13/12/10 05:44:29 INFO mapred.JobClient:     FILE: Number of read operations=0
13/12/10 05:44:29 INFO mapred.JobClient:     FILE: Number of large read operations=0
13/12/10 05:44:29 INFO mapred.JobClient:     FILE: Number of write operations=0
13/12/10 05:44:29 INFO mapred.JobClient:     HDFS: Number of bytes read=14743
13/12/10 05:44:29 INFO mapred.JobClient:     HDFS: Number of bytes written=14605
13/12/10 05:44:29 INFO mapred.JobClient:     HDFS: Number of read operations=2
13/12/10 05:44:29 INFO mapred.JobClient:     HDFS: Number of large read operations=0
13/12/10 05:44:29 INFO mapred.JobClient:     HDFS: Number of write operations=1
13/12/10 05:44:29 INFO mapred.JobClient:   Job Counters
13/12/10 05:44:29 INFO mapred.JobClient:     Launched map tasks=1
13/12/10 05:44:29 INFO mapred.JobClient:     Launched reduce tasks=1
13/12/10 05:44:29 INFO mapred.JobClient:     Rack-local map tasks=1
13/12/10 05:44:29 INFO mapred.JobClient:     Total time spent by all maps in occupied slots (ms)=12033
13/12/10 05:44:29 INFO mapred.JobClient:     Total time spent by all reduces in occupied slots (ms)=5835
13/12/10 05:44:29 INFO mapred.JobClient:     Total time spent by all maps waiting after reserving slots (ms)=0
13/12/10 05:44:29 INFO mapred.JobClient:     Total time spent by all reduces waiting after reserving slots (ms)=0
13/12/10 05:44:29 INFO mapred.JobClient:   Map-Reduce Framework
13/12/10 05:44:29 INFO mapred.JobClient:     Map input records=1367
13/12/10 05:44:29 INFO mapred.JobClient:     Map output records=1367
13/12/10 05:44:29 INFO mapred.JobClient:     Map output bytes=21872
13/12/10 05:44:29 INFO mapred.JobClient:     Input split bytes=138
13/12/10 05:44:29 INFO mapred.JobClient:     Combine input records=0
13/12/10 05:44:29 INFO mapred.JobClient:     Combine output records=0
13/12/10 05:44:29 INFO mapred.JobClient:     Reduce input groups=1143
13/12/10 05:44:29 INFO mapred.JobClient:     Reduce shuffle bytes=24612
13/12/10 05:44:29 INFO mapred.JobClient:     Reduce input records=1367
13/12/10 05:44:29 INFO mapred.JobClient:     Reduce output records=1367
13/12/10 05:44:29 INFO mapred.JobClient:     Spilled Records=2734
13/12/10 05:44:29 INFO mapred.JobClient:     CPU time spent (ms)=2260
13/12/10 05:44:29 INFO mapred.JobClient:     Physical memory (bytes) snapshot=332648448
13/12/10 05:44:29 INFO mapred.JobClient:     Virtual memory (bytes) snapshot=1405710336
13/12/10 05:44:29 INFO mapred.JobClient:     Total committed heap usage (bytes)=332005376
```
