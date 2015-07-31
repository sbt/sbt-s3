# sbt-s3

## Description

*sbt-s3* is a simple sbt plugin that can manipulate objects on Amazon S3.

## Usage

* add to your project/plugin.sbt the line:
   `addSbtPlugin("com.typesafe.sbt" % "sbt-s3" % "0.8")`
* then add to your build.sbt the line:
   `s3Settings`
 
You will then be able to use the tasks s3-upload, s3-download, and s3-delete, defined
in the nested object `com.typesafe.sbt.S3Plugin.S3` as upload, download, and delete, respectively.
All these operations will use HTTPS as a transport protocol.
 
Please check the Scaladoc API of the `S3Plugin` object, and of its nested `S3` object,
to get additional documentation of the available sbt tasks.

## Example 1

Here is a complete example:

project/plugin.sbt:
    
    addSbtPlugin("com.typesafe.sbt" % "sbt-s3" % "0.8")

build.sbt:

    import S3._

    s3Settings

    mappings in upload := Seq((new java.io.File("a"),"zipa.txt"),(new java.io.File("b"),"pongo/zipb.jar"))

    host in upload := "s3sbt-test.s3.amazonaws.com"

    credentials += Credentials(Path.userHome / ".s3credentials")

~/.s3credentials:

    realm=Amazon S3
    host=s3sbt-test.s3.amazonaws.com
    user=<Access Key ID>
    password=<Secret Access Key>

Just create two sample files called "a" and "b" in the same directory that contains build.sbt, then try:

    $ sbt s3-upload
    
You can also see progress while uploading:

    $ sbt
    > set S3.progress in S3.upload := true
    > s3-upload
    [==================================================]   100%   zipa.txt
    [=====================================>            ]    74%   zipb.jar

Unless explicitly provided as described above, credentials will be obtained via (in order):

1. `AWS_ACCESS_KEY_ID` and `AWS_SECRET_KEY` environment variables
2. `aws.accessKeyId` and `aws.secretKey` Java system properties 
3. Default aws cli credentials file (`~/.aws/credentials`)
4. IAM instance profile if running under EC2.

## Example 2

As well as simply uploading a file to s3 you can also set some s3 ObjectMetadata.
For example, you may want to gzip a CSS file for quicker download but still have its content type as css,
In which case you need to set the Content-Type and Content-Encoding, a small change to
build.sbt is all that is needed.

build.sbt:

    import S3._

    s3Settings

    mappings in upload := Seq((target.value / "web" / "stage" / "css" / "style-group2.css.gz" ,"css/style-group2.css"))

    val md = {
      import com.amazonaws.services.s3.model.ObjectMetadata
      var omd = new ObjectMetadata()
      omd.setContentEncoding("gzip")
      omd.setContentType("text/css")
      omd
    }

    metadata in upload := Map("css/style-group2.css" -> md)

    host in upload := "s3sbt-test.s3.amazonaws.com"

    credentials += Credentials(Path.userHome / ".s3credentials")

    metadata in upload
