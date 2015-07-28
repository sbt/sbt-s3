package com.typesafe.sbt

import com.amazonaws.event.{ProgressEventType, ProgressEvent, SyncProgressListener}
import com.amazonaws.services.s3.model.{GeneratePresignedUrlRequest, GetObjectRequest, PutObjectRequest, ObjectMetadata}
import sbt.{File => _, _}
import java.io.File
import java.util.Date
import Keys._
import com.amazonaws._
import auth._, services.s3._
import org.apache.commons.lang.StringUtils.removeEndIgnoreCase

/**
  * S3Plugin is a simple sbt plugin that can manipulate objects on Amazon S3.
  *
  * == Example ==
  * Here is a complete example:
  *
  *  - project/plugin.sbt:
  * {{{addSbtPlugin("com.typesafe.sbt" % "sbt-s3" % "0.8")}}}
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
  * You can also see progress while uploading:
  * {{{
  * $ sbt
  * > set S3.progress in S3.upload := true
  * > s3-upload
  * [==================================================]   100%   zipa.txt
  * [=====================================>            ]    74%   zipb.jar
  * }}}
  *
  *  Please select the nested `S3` object link, below, for additional information on the available tasks.
  */
object S3Plugin extends sbt.Plugin {

  type MetadataMap = Map[String, ObjectMetadata]
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
    *   If running under EC2, the credentials will automatically be provided via IAM.
    *  - ''mappings in S3.upload:'' the list of local files and S3 keys (pathnames), for example:
    *  `Seq((File("f1.txt"),"aaa/bbb/file1.txt"), ...)`
    *  - ''S3.host in S3.upload:'' the bucket name, in one of two forms:
    *   1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *   1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
    *
    * If you set logLevel to "Level.Debug", the list of files will be printed while uploading.
    *
    * Returns: the sequence of uploaded keys (pathnames).
    */
    val upload=TaskKey[Seq[String]]("s3-upload","Uploads files to an S3 bucket.")

  /**
    * The task "s3-download" downloads a set of files from a specificed S3 bucket.
    * Depends on:
    *  - ''credentials in S3.download:'' security credentials used to access the S3 bucket, as follows:
    *   - ''realm:'' "Amazon S3"
    *   - ''host:'' the string specified by S3.host in S3.download, see below
    *   - ''user:'' Access Key ID
    *   - ''password:'' Secret Access Key
    *   If running under EC2, the credentials will automatically be provided via IAM.
    *  - ''mappings in S3.download:'' the list of local files and S3 keys (pathnames), for example:
    *  `Seq((File("f1.txt"),"aaa/bbb/file1.txt"), ...)`
    *  - ''S3.host in S3.download'': the bucket name, in one of two forms:
    *   1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *   1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
    *
    * If you set logLevel to "Level.Debug", the list of files will be printed while downloading.
    *
    * Returns: the sequence of downloaded files.
    */
    val download=TaskKey[Seq[File]]("s3-download","Downloads files from an S3 bucket.")

  /**
    * The task "s3-delete" deletes a set of files from a specificed S3 bucket.
    * Depends on:
    *  - ''credentials in S3.delete:'' security credentials used to access the S3 bucket, as follows:
    *   - ''realm:'' "Amazon S3"
    *   - ''host:'' the string specified by S3.host in S3.delete, see below
    *   - ''user:'' Access Key ID
    *   - ''password:'' Secret Access Key
    *   If running under EC2, the credentials will automatically be provided via IAM.
    *  - ''S3.keys in S3.delete:'' the list of S3 keys (pathnames), for example:
    *  `Seq("aaa/bbb/file1.txt", ...)`
    *  - ''S3.host in S3.delete:'' the bucket name, in one of two forms:
    *   1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *   1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
    *
    * If you set logLevel to "Level.Debug", the list of keys will be printed while the S3 objects are being deleted.
    *
    * Returns: the sequence of deleted keys (pathnames).
    */
    val delete=TaskKey[Seq[String]]("s3-delete","Delete files from an S3 bucket.")

  /**
    * The task "s3-generate-link" creates a link for set of files in a S3 bucket.
    * Depends on:
    *  - ''credentials in S3.generateLink:'' security credentials used to access the S3 bucket, as follows:
    *   - ''realm:'' "Amazon S3"
    *   - ''host:'' the string specified by S3.host in S3.upload, see below
    *   - ''user:'' Access Key ID
    *   - ''password:'' Secret Access Key
    *   If running under EC2, the credentials will automatically be provided via IAM.
    *  - ''keys in S3.upload:'' the list of remote files, for example:
    *  `Seq("aaa/bbb/file1.txt", ...)`
    *  - ''expirationDate in S3.generateLink:'' the expiration date at which point the
    *   pre-signed URL will no longer be accepted by Amazon S3. It must be specified.
    *  - ''S3.host in S3.generateLink'': the bucket name, in one of two forms:
    *   1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *   1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
    *
    * If you set logLevel to "Level.Debug", the list of files will be printed while uploading.
    *
    * Returns: the sequence of generated URLs.
    */
    val generateLink=TaskKey[Seq[URL]]("s3-generate-link","Creates links to a set of files in an S3 bucket.")

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

  /**
    * If you set "progress" to true, a progress indicator will be displayed while the individual files are uploaded or downloaded.
    * Only recommended for interactive use or testing; the default value is false.
    */
    val progress=SettingKey[Boolean]("s3-progress","Set to true to get a progress indicator during S3 uploads/downloads (default false).")

    val metadata=SettingKey[MetadataMap]("s3-metadata","Mapping from S3 keys (pathnames) to the corresponding metadata")

    val expirationDate=SettingKey[java.util.Date]("s3-expiration-date", "Expiration date for the generated link")

    private[S3Plugin] val dummy=SettingKey[Unit]("dummy-internal","Dummy setting")
  }

  import S3._

  type Bucket=String

  private def getClient(creds:Seq[Credentials],host:String) = {
    val credentials = Credentials.forHost(creds, host) match {
      // username -> Access Key Id ; passwd -> Secret Access Key
      case Some(cred) => new BasicAWSCredentials(cred.userName, cred.passwd)
      case None       =>
        val provider = new DefaultAWSCredentialsProviderChain
        try {
          provider.getCredentials()
        } catch {
          case e:com.amazonaws.AmazonClientException =>
            sys.error("Could not find S3 credentials for the host: "+host+", and no IAM credentials available")
        }
    }
    new AmazonS3Client(credentials, new ClientConfiguration().withProtocol(Protocol.HTTPS))
  }
  private def getBucket(host:String) = {
    val dotS3DashIndex = host.lastIndexOf(".s3-")
    if (dotS3DashIndex >= 0) {
      host.take(dotS3DashIndex)
    } else {
      val dotS3DotIndex = host.lastIndexOf(".s3.")
      if (dotS3DotIndex >= 0) host.take(dotS3DotIndex) else host
    }
  }

  private def s3InitTask[Item,Extra,Return](thisTask:TaskKey[Seq[Return]], itemsKey:TaskKey[Seq[Item]],
                                            extra:SettingKey[Extra], // may be unused (a dummy value)
                                            op:(AmazonS3Client,Bucket,Item,Extra,Boolean)=>Return,
                                            msg:(Bucket,Item)=>String, lastMsg:(Bucket,Seq[Item])=>String )  =

    (credentials in thisTask, itemsKey in thisTask, host in thisTask, extra in thisTask, progress in thisTask, streams) map {
      (creds,items,host,extra,progress,streams) =>
        val client = getClient(creds, host)
        val bucket = getBucket(host)
        val ret = items map { item =>
          streams.log.debug(msg(bucket,item))
          op(client,bucket,item,extra,progress)
        }
        streams.log.info(lastMsg(bucket,items))
        ret
    }


  private def progressBar(percent:Int) = {
    val b="=================================================="
    val s="                                                 "
    val p=percent/2
    val z:StringBuilder=new StringBuilder(80)
    z.append("\r[")
    z.append(b.substring(0,p))
    if (p<50) {z.append(">"); z.append(s.substring(p))}
    z.append("]   ")
    if (p<5) z.append(" ")
    if (p<50) z.append(" ")
    z.append(percent)
    z.append("%   ")
    z.mkString
  }

  private def addProgressListener(request: AmazonWebServiceRequest, fileSize: Long, key: String) = {
    request.setGeneralProgressListener(new SyncProgressListener {
      var uploadedBytes = 0L
      val fileName = {
        val area = 30
        val n = new File(key).getName()
        val l = n.length()
        if (l > area - 3)
          "..." + n.substring(l - area + 3)
        else
          n
      }
      override def progressChanged(progressEvent: ProgressEvent): Unit = {
        if (progressEvent.getEventType == ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT ||
            progressEvent.getEventType == ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT) {
          uploadedBytes = uploadedBytes + progressEvent.getBytesTransferred()
        }
        print(progressBar(if (fileSize > 0) ((uploadedBytes * 100) / fileSize).toInt else 100))
        print(fileName)
        if (progressEvent.getEventType == ProgressEventType.TRANSFER_COMPLETED_EVENT)
          println()
      }
    })
  }

  def prettyLastMsg(verb:String, objects:Seq[String], preposition:String, bucket:String) =
    if (objects.length == 1) s"$verb '${objects.head}' $preposition the S3 bucket '$bucket'."
    else                     s"$verb ${objects.length} objects $preposition the S3 bucket '$bucket'."

  /*
   * Include the line {{{s3Settings}}} in your build.sbt file, in order to import the tasks defined by this S3 plugin.
   */
  val s3Settings = Seq(

    upload <<= s3InitTask[(File,String),MetadataMap,String](upload, mappings, metadata,
                           { case (client,bucket,(file,key),metadata,progress) =>
                               val request=new PutObjectRequest(bucket,key,file)
                               if (progress) addProgressListener(request,file.length(),key)
                               client.putObject(metadata.get(key).map(request.withMetadata).getOrElse(request))
                               key
                           },
                           { case (bucket,(file,key)) =>  "Uploading "+file.getAbsolutePath()+" as "+key+" into "+bucket },
                           {      (bucket,mapps) =>       prettyLastMsg("Uploaded", mapps.map(_._2), "to", bucket) }
                         ),

    download <<= s3InitTask[(File,String),Unit,File](download, mappings, dummy,
                           { case (client,bucket,(file,key),_,progress) =>
                               val request=new GetObjectRequest(bucket,key)
                               val objectMetadata=client.getObjectMetadata(bucket,key)
                               if (progress) addProgressListener(request,objectMetadata.getContentLength(),key)
                               client.getObject(request,file)
                               file
                           },
                           { case (bucket,(file,key)) =>  "Downloading "+file.getAbsolutePath()+" as "+key+" from "+bucket },
                           {      (bucket,mapps) =>       prettyLastMsg("Downloaded", mapps.map(_._2), "from", bucket) }
                         ),

    delete <<= s3InitTask[String,Unit,String](delete, keys, dummy,
                           { (client,bucket,key,_,_) => client.deleteObject(bucket,key); key },
                           { (bucket,key) =>          "Deleting "+key+" from "+bucket },
                           { (bucket,keys1) =>        prettyLastMsg("Deleted", keys1, "from", bucket) }
                         ),

    generateLink <<= s3InitTask[String,Date,URL](generateLink, keys, expirationDate,
                           { (client,bucket,key,date,_) =>
                               val request = new GeneratePresignedUrlRequest(bucket, key)
                               request.setMethod(HttpMethod.GET)
                               request.setExpiration(date)
                               val url = client.generatePresignedUrl(request)
                               println(s"$key link: $url")
                               url
                           },
                           { (bucket,key) =>          s"Creating link for $key in $bucket" },
                           { (bucket,keys1) =>        prettyLastMsg("Generated link", keys1, "from", bucket) }
                         ),

    host := "",
    keys := Seq(),
    metadata := Map(),
    mappings in download := Seq(),
    mappings in upload := Seq(),
    progress := false,
    expirationDate := new java.util.Date(),
    dummy := ()
  )
}
