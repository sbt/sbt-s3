# sbt-s3

## Description

*sbt-s3* is a simple sbt plugin that can manipulate objects on Amazon S3.

## Usage

* add to your project/plugin.sbt:
   `resolvers += Resolver.url("sbts3 ivy resolver", url("https://dl.bintray.com/emersonloureiro/sbt-plugins"))(Resolver.ivyStylePatterns)`
   `addSbtPlugin("cf.janga" % "sbts3" % "0.10.2")`
* then add to your build.sbt the line:
   `enablePlugins(S3Plugin)`

You will then be able to use the tasks `s3-upload`, `s3-download`, `s3-delete`, and `s3-generate-links`, defined
in the object `com.typesafe.sbt.S3Keys` as `s3Upload`, `s3Download`, `s3Delete`, and `s3GenerateLinks` respectively.
All these operations will use HTTPS as a transport protocol.

Please check the Scaladoc API of the `S3Plugin` object, and the `S3Keys` object,
to get additional documentation on the available sbt tasks, and their parameters.

## Example 1

Here is a complete example:

project/plugin.sbt:

    resolvers += Resolver.url("sbts3 ivy resolver", url("https://dl.bintray.com/emersonloureiro/sbt-plugins"))(Resolver.ivyStylePatterns)

    addSbtPlugin("cf.janga" % "sbts3" % "0.10.2")

build.sbt:

    enablePlugins(S3Plugin)

    mappings in s3Upload := Seq((new java.io.File("a"),"zipa.txt"),(new java.io.File("b"),"pongo/zipb.jar"))

    s3Host in s3Upload := "s3sbt-test.s3.amazonaws.com"

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
    > set s3Progress in s3Upload := true
    > s3-upload
    [==================================================]   100%   zipa.txt
    [=====================================>            ]    74%   zipb.jar

Unless explicitly provided as described above, credentials will be obtained via (in order):

1. `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` environment variables
2. `aws.accessKeyId` and `aws.secretKey` Java system properties
3. Default aws cli credentials file (`~/.aws/credentials`)
4. IAM instance profile if running under EC2.

## Example 2

As well as simply uploading a file to s3 you can also set some s3 ObjectMetadata.
For example, you may want to gzip a CSS file for quicker download but still have its content type as css,
In which case you need to set the Content-Type and Content-Encoding, a small change to
build.sbt is all that is needed.

build.sbt:

    enablePlugins(S3Plugin)

    mappings in s3Upload := Seq((target.value / "web" / "stage" / "css" / "style-group2.css.gz" ,"css/style-group2.css"))

    def md = {
      import com.amazonaws.services.s3.model.ObjectMetadata
      var omd = new ObjectMetadata()
      omd.setContentEncoding("gzip")
      omd.setContentType("text/css")
      omd
    }

    s3Metadata in s3Upload := Map("css/style-group2.css" -> md)

    s3Host in s3Upload := "s3sbt-test.s3.amazonaws.com"

    credentials += Credentials(Path.userHome / ".s3credentials")


## License

This code is open source software licensed under the <a href="http://www.apache.org/licenses/LICENSE-2.0.html">Apache 2.0 License</a>.
