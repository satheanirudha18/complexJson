<strong>Getting Started</strong> - 
<ol>
  <li>Download the latest release of the tar file from the Apache Gobblin website.
    Link -  <a href="https://gobblin.apache.org/download/">Download</a>
    In this case, I have downloaded the 0.14.0 version of Gobblin.
  <li>Change your directory to the downloaded repository,
    <i>Command – cd gobblin_0.14.0</i>
  </li>
  <li>You should see a directory named as gobblin-distribution, we have to build the distribution
    <i>Command – ./gradlew :gobblin-distribution:buildDistributionTar</i>
  </li>
</ol>

<p><b>Note:</b> You will then see a gobblin-distribution-unknown.tar.gz in your existing directory or it will definitely exist in build/gobblin-distribution/distributions/. You can also move it to the /opt directory, where all the installations are present.</p>

<strong>Requirements</strong>
<ul>
  <li>Java >= 1.8</li>
  <li>gradle-wrapper.jar version 2.13 or Maven version 3.5.3 (If building the distribution with tests turned on)</li>
  <li>Hadoop (If the destination is hdfs)</li>
</ul>

<strong>Instructions to run your first Gobblin job</strong>
<ol>
  <li>Change your directory to where the distribution is built.</li>
  <li>Create 2 directories namely, logs and jobs.</li>
  <li>Once directories are created, the jobs can be run in two modes, standalone and mapreduce mode.</li>
</ol>

<b>Note:</b>
<ul>
  <li>Logs directory is used to store the logs, where there are 2 files namely, gobblin-current.log and gobblin-gc.log, where gobblin-current.log holds the log of the execution and gobblin-gc.log covers metrics related to garbage collection, e.g., counts and time spent on garbage collection.
  </li>
  <li>
    Jobs directory is used to store the jobs or pull or conf files, which specifies the execution of your ingestion framework/program.
  </li>
</ul>

<strong>Pre-requisites:</strong>
<p>
  Some configurations in the job file are required,
  <ul>
    <li><i>source.filebased.files.to.pull</i> to point to your file present in the resources. If the resources directory is not present, then create one and also create a file for ingestion. e.g - <i>source.filebased.files.to.pull=file:///opt/gobblin/distributions/resources/complexJson.json</i> or <i>http://api.eventful.com/json/events/search?&location=San+Diego&app_key={YOUR_APP_KEY}&keywords=music</i>
    </li>
    <li>
      <i>converter.avroSchema</i> to point to your Avro Schema for the input data.
    </li>
    <li>
      <i>writer.fs.uri</i> to point to the destination type, in case of local system then specify <i>file:///</i> or in case of hadoop then specify <i>hdfs://sanbox.hortonworks.com</i>.
    </li>
    <li>
      There are some additional properties that are applied like <i>events.source.type</i> for specifying the input format of the data extracted. For eg : - <b>xml</b> or <b>json</b>.
    </li>
    <li>
      Below is the example for the configurations of the destination,
      <ul>
        <li>mr.job.root.dir=hdfs://sandbox.hortonworks.com:8020/gobblin/complexJsonDir/working</li>
        <li>state.store.dir=hdfs://sandbox.hortonworks.com:8020/gobblin/complexJsonDir/state-store</li>
        <li>task.data.root.dir=hdfs://sandbox.hortonworks.com:8020/gobblin/complexJsonDir/task-data</li>
        <li>writer.staging.dir=hdfs://sandbox.hortonworks.com:8020/gobblin/complexJsonDir/task-staging</li>
        <li>writer.output.dir=hdfs://sandbox.hortonworks.com:8020/gobblin/complexJsonDir/task-output</li>
        <li>data.publisher.final.dir=hdfs://sandbox.hortonworks.com:8020/gobblin/complexJsonDir</li>
      </ul>
    </li>
  </ul>
</p>

<strong>To run in Standalone mode:</strong>
<p>
  Execute the following command to start the gobbin job: <br>
  <i>bin/gobblin-standalone.sh start
--conf /opt/gobblin/distributions/jobs/complexJson.pull</i>
  <br><br>
  Execute the following command to stop the gobblin job: <br>
  <i>bin/gobblin-standalone.sh stop
--conf /opt/gobblin/distributions/jobs/complexJson.pull</i>
  <br><br>
  Execute the following command to view the status of the job: <br>
  <i>bin/gobblin-standalone.sh status
--conf /opt/gobblin/distributions/jobs/complexJson.pull</i>
</p>

<strong>To run in Mapreduce mode:</strong>
<p>
  Execute the following command to start the gobblin job: <br>
  <i>bin/gobblin-mapreduce.sh start
--conf /opt/gobblin/distributions/jobs/complexJson.pull</i>
  <br><br>
  Execute the following command to the gobblin job: <br>
  <i>bin/gobblin-mapreduce.sh stop
--conf /opt/gobblin/distributions/jobs/complexJson.pull</i>
  <br><br>
  Execute the following command to view the status of the gobblin job: <br>
  <i>bin/gobblin-mapreduce.sh status
--conf /opt/gobblin/distributions/jobs/complexJson.pull</i>
</p>

<p>
  Hope it was helful. Have a great day.
</p>
