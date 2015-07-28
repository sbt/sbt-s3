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

## Example

Here is a complete example:

project/plugin.sbt:
    
    addSbtPlugin("com.typesafe.sbt" % "sbt-s3" % "0.8")

build.sbt:

    import S3._

    s3Settings

    mappings in upload := Seq((new java.io.File("a"),"zipa.txt"),(new java.io.File("b"),"pongo/zipb.jar"))

    host in upload := "s3sbt-test.s3.amazonaws.com"

    credentials += Credentials(Path.userHome / ".s3credentials")

If you want to set a region name, add `s3-<region>` to the host. So it should look like `bucket.s3-region.amazonaws.com`.

It's `bucket` + `.s3-` + `region` + `.amazonaws.com`

e.g.)

Bucket: `my-test-bucket`<br>
Region: `ap-southeast-2`<br>
Then the `host` should be<br>
`my-test-bucket.s3-ap-southeast-2.amazonaws.com`

```scala
import S3._

s3Settings

mappings in upload := Seq((new java.io.File("a"),"zipa.txt"),(new java.io.File("b"),"pongo/zipb.jar"))

host in upload := "my-test-bucket.s3-ap-southeast-2.amazonaws.com"

credentials += Credentials(Path.userHome / ".s3credentials")
```

More about the region/s endpoint location: http://www.bucketexplorer.com/documentation/amazon-s3--amazon-s3-buckets-and-regions.html

You can set up the S3 access info in the `.s3credentials` file:

`~/.s3credentials`:

    realm=Amazon S3
    host=s3sbt-test.s3.amazonaws.com
    user=<Access Key ID>
    password=<Secret Access Key>
    
To set it up in the `build.sbt` using environment variables,

`build.sbt`:
```scala
credentials += Credentials(
                 realm = "Amazon S3",
                 host = "s3sbt-test.s3.amazonaws.com",
                 userName = sys.env.getOrElse("AWS_ACCESS_KEY_ID", ""),
                 passwd = sys.env.getOrElse("AWS_SECRET_ACCESS_KEY", "")
               )
```


Just create two sample files called "a" and "b" in the same directory that contains build.sbt, then try:

    $ sbt s3-upload
    
You can also see progress while uploading:

    $ sbt
    > set S3.progress in S3.upload := true
    > s3-upload
    [==================================================]   100%   zipa.txt
    [=====================================>            ]    74%   zipb.jar

If running under EC2, the credentials will be automatically obtained via IAM, unless
explicitly provided as described above.
