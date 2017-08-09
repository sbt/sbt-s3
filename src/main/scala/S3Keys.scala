package com.typesafe.sbt

trait S3Keys {
  /**
    * The task "s3-upload" uploads a set of files to a specificed S3 bucket.
    * Depends on:
    *  - ''credentials in S3.upload'': security credentials used to access the S3 bucket, as follows:
    *   - ''realm'': "Amazon S3"
    *   - ''host'': the string specified by S3.host in S3.upload, see below
    *   - ''user'': Access Key ID
    *   - ''password'': Secret Access Key
    *   If running under EC2, the credentials will automatically be provided via IAM.
    *  - ''mappings in S3.upload'': the list of local files and S3 keys (pathnames), for example:
    *  `Seq((File("f1.txt"),"aaa/bbb/file1.txt"), ...)`
    *  - ''S3.host in S3.upload'': the bucket name, in one of these forms:
    *   1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *   1. "mybucket.s3-myregion.amazonaws.com", where "myregion" is the region name, or
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
    *  - ''credentials in S3.download'': security credentials used to access the S3 bucket, as follows:
    *   - ''realm'': "Amazon S3"
    *   - ''host'': the string specified by S3.host in S3.download, see below
    *   - ''user'': Access Key ID
    *   - ''password'': Secret Access Key
    *   If running under EC2, the credentials will automatically be provided via IAM.
    *  - ''mappings in S3.download'': the list of local files and S3 keys (pathnames), for example:
    *  `Seq((File("f1.txt"),"aaa/bbb/file1.txt"), ...)`
    *  - ''S3.host in S3.download'': the bucket name, in one of these forms:
    *   1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *   1. "mybucket.s3-myregion.amazonaws.com", where "myregion" is the region name, or
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
    *  - ''credentials in S3.delete'': security credentials used to access the S3 bucket, as follows:
    *   - ''realm'': "Amazon S3"
    *   - ''host'': the string specified by S3.host in S3.delete, see below
    *   - ''user'': Access Key ID
    *   - ''password'': Secret Access Key
    *   If running under EC2, the credentials will automatically be provided via IAM.
    *  - ''S3.keys in S3.delete'': the list of S3 keys (pathnames), for example:
    *  `Seq("aaa/bbb/file1.txt", ...)`
    *  - ''S3.host in S3.delete'': the bucket name, in one of these forms:
    *   1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *   1. "mybucket.s3-myregion.amazonaws.com", where "myregion" is the region name, or
    *   1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
    *
    * If you set logLevel to "Level.Debug", the list of keys will be printed while the S3 objects are being deleted.
    *
    * Returns: the sequence of deleted keys (pathnames).
    */
    val delete=TaskKey[Seq[String]]("s3-delete","Delete files from an S3 bucket.")

  /**
    * The task "s3-generate-links" creates a link for set of files in a S3 bucket.
    * Depends on:
    *  - ''credentials in S3.generateLinks'': security credentials used to access the S3 bucket, as follows:
    *   - ''realm'': "Amazon S3"
    *   - ''host'': the string specified by S3.host in S3.upload, see below
    *   - ''user'': Access Key ID
    *   - ''password'': Secret Access Key
    *   If running under EC2, the credentials will automatically be provided via IAM.
    *  - ''keys in S3.upload'': the list of remote files, for example:
    *  `Seq("aaa/bbb/file1.txt", ...)`
    *  - ''expirationDate in S3.generateLinks'': the expiration date at which point the
    *   pre-signed URL will no longer be accepted by Amazon S3. It must be specified.
    *  - ''S3.host in S3.generateLinks'': the bucket name, in one of these forms:
    *   1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *   1. "mybucket.s3-myregion.amazonaws.com", where "myregion" is the region name, or
    *   1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
    *
    * If you set logLevel to "Level.Debug", the list of files will be printed while uploading.
    *
    * Returns: the sequence of generated URLs.
    */
    val generateLinks=TaskKey[Seq[URL]]("s3-generate-links","Creates links to a set of files in an S3 bucket.")

  /**
    * A string representing the S3 bucket name, in one of these forms:
    *   1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
    *   1. "mybucket.s3-myregion.amazonaws.com", where "myregion" is the region name, or
    *   1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
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
}

object S3Keys extends S3Keys
