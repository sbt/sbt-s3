package com.typesafe.sbt

import sbt.{File => _, _}
import java.io.File
import Keys._
import com.amazonaws._
import auth._,services.s3._
import model._
import org.apache.commons.lang.StringUtils.removeEndIgnoreCase

/**
  * S3Plugin is a simple sbt plugin that can manipulate objects on Amazon S3.
  *
  * == Example ==
  * Here is a complete example:
  * 
  *  - project/plugin.sbt:
  * {{{addSbtPlugin("com.typesafe.sbt" % "sbt-s3" % "0.1")}}}
  * 
  *  - build.sbt:
  * {{{
  * import S3._
  *
  * s3Settings
  *
  * mappings in upload := Seq((new java.io.File("a"),"zipa.txt"),(new java.io.File("b"),"pongo/zipb.jar"))
  * 
  * host in upload := "s3sbt-test.s3.amazonaws.com"
  * 
  * credentials += Credentials(Path.userHome / ".s3credentials")
  * }}}
  * 
  *  - ~/.s3credentials:
  * {{{
  * realm=Amazon S3
  * host=s3sbt-test.s3.amazonaws.com
  * user=<Access Key ID>
  * password=<Secret Access Key>
  * }}}
  * 
  * Just create two sample files called "a" and "b" in the same directory that contains build.sbt,
  * then try:
  * {{{$ sbt s3-upload}}}
  * 
  *  Please select the nested `S3` object link, below, for additional information on the available tasks.
  */
object S3Plugin extends sbt.Plugin {

  /**
    * This nested object defines the sbt keys made available by the S3Plugin: read here for tasks info.
    */
  object S3 {

  /**
    * The task "s3-upload" uploads a set of files to a specificed S3 bucket.
    * Depends on:
    *  - ''credentials in S3.upload:'' security credentials used to access the S3 bucket, as follows:
    *   - ''realm:'' "Amazon S3"
    *   - ''host:'' the string specified by S3.host in S3.upload, see below
    *   - ''user:'' Access Key ID
    *   - ''password:'' Secret Access Key
    *  - ''mappings in S3.upload:'' the list of local files and S3 keys (pathnames), for example:
    *  `Seq((File("f1.txt"),"aaa/bbb/file1.txt"), ...)`
    *  - ''S3.host in S3.upload:'' the bucket name, in one of two forms:
    *   1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *   1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
    *
    * If you set logLevel to "Level.Debug", the list of files will be printed while uploading.
    */
    val upload=TaskKey[Unit]("s3-upload","Uploads files to an S3 bucket.")

  /**
    * The task "s3-download" downloads a set of files from a specificed S3 bucket.
    * Depends on:
    *  - ''credentials in S3.download:'' security credentials used to access the S3 bucket, as follows:
    *   - ''realm:'' "Amazon S3"
    *   - ''host:'' the string specified by S3.host in S3.download, see below
    *   - ''user:'' Access Key ID
    *   - ''password:'' Secret Access Key
    *  - ''mappings in S3.download:'' the list of local files and S3 keys (pathnames), for example:
    *  `Seq((File("f1.txt"),"aaa/bbb/file1.txt"), ...)`
    *  - ''S3.host in S3.download:'' the bucket name, in one of two forms:
    *   1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *   1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
    *
    * If you set logLevel to "Level.Debug", the list of files will be printed while downloading.
    */
    val download=TaskKey[Unit]("s3-download","Downloads files from an S3 bucket.")

  /**
    * The task "s3-delete" deletes a set of files from a specificed S3 bucket.
    * Depends on:
    *  - ''credentials in S3.delete:'' security credentials used to access the S3 bucket, as follows:
    *   - ''realm:'' "Amazon S3"
    *   - ''host:'' the string specified by S3.host in S3.delete, see below
    *   - ''user:'' Access Key ID
    *   - ''password:'' Secret Access Key
    *  - ''S3.keys in S3.delete:'' the list of S3 keys (pathnames), for example:
    *  `Seq("aaa/bbb/file1.txt", ...)`
    *  - ''S3.host in S3.delete:'' the bucket name, in one of two forms:
    *   1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *   1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
    *
    * If you set logLevel to "Level.Debug", the list of keys will be printed while the S3 objects are being deleted.
    */
    val delete=TaskKey[Unit]("s3-delete","Delete files from an S3 bucket.")

  /**
    * A string representing the S3 bucket name, in one of two forms:
    *  1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *  1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
    */
    val host=SettingKey[String]("s3-host","Host used by the S3 operation, either \"mybucket.s3.amazonaws.com\" or \"mybucket\".")

  /**
    * A list of S3 keys (pathnames) representing objects in a bucket on which a certain operation should be performed.
    */
    val keys=TaskKey[Seq[String]]("s3-keys","List of S3 keys (pathnames) on which to perform a certain operation.")
  }

  import S3._

  type Bucket=String

  private def getClient(creds:Seq[Credentials],host:String) = {
    val Some(cred) = Credentials.forHost(creds, host)
    // username -> Access Key Id ; passwd -> Secret Access Key
    new AmazonS3Client(new BasicAWSCredentials(cred.userName, cred.passwd),
                       new ClientConfiguration().withProtocol(Protocol.HTTPS))
  }
  private def getBucket(host:String) = removeEndIgnoreCase(host,".s3.amazonaws.com")

  private def s3InitTask[A]( thisTask:TaskKey[Unit], itemsKey:TaskKey[Seq[A]],
                             op:(AmazonS3Client,Bucket,A)=>Unit,
                             msg:(Bucket,A)=>String, lastMsg:(Bucket,Seq[A])=>String )  =

    (credentials in thisTask, itemsKey in thisTask, host in thisTask,streams) map { (creds,items,host,streams) =>
      val client = getClient(creds, host)
      val bucket = getBucket(host)
      items foreach { item =>
        streams.log.debug(msg(bucket,item))
        op(client,bucket,item)
      }
      streams.log.info(lastMsg(bucket,items))
    }


  /*
   * Include the line {{{s3Settings}}} in your build.sbt file, in order to import the tasks defined by this S3 plugin.
   */
  val s3Settings = Seq(

    upload <<= s3InitTask[(File,String)](upload, mappings,
                           { case (client,bucket,(file,key)) => client.putObject(bucket,key,file) },
                           { case (bucket,(file,key)) =>        "Uploading "+file.getAbsolutePath()+" as "+key+" into "+bucket },
                           { case (bucket,mapps) =>             "Uploaded "+mapps.length+" files to the S3 bucket \""+bucket+"\"." }
                         ),

    download <<= s3InitTask[(File,String)](download, mappings,
                           { case (client,bucket,(file,key)) => client.getObject(new GetObjectRequest(bucket,key),file) },
                           { case (bucket,(file,key)) =>        "Downloading "+file.getAbsolutePath()+" as "+key+" from "+bucket },
                           { case (bucket,mapps) =>             "Downloaded "+mapps.length+" files from the S3 bucket \""+bucket+"\"." }
                         ),

    delete <<= s3InitTask[String](delete, keys,
                           { case (client,bucket,key) => client.deleteObject(bucket,key) },
                           { case (bucket,key) =>        "Deleting "+key+" from "+bucket },
                           { case (bucket,keys1) =>      "Deleted "+keys1.length+" objects from the S3 bucket \""+bucket+"\"." }
                         ),

    host := "",
    keys := Seq(),
    mappings in download := Seq(),
    mappings in upload := Seq()

  )
}
