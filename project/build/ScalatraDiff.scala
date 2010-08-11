import sbt._
import java.io.File

class ScalatraDiff(info: ProjectInfo) extends DefaultProject(info) {
  
  val jettyEmbedded = "org.mortbay.jetty" % "jetty-embedded" % "6.1.25"

  val scalatra = "org.scalatra" % "scalatra_2.8.0" % "2.0.0-SNAPSHOT"
  val scalatraFileUploader = 
    "org.scalatra" % "scalatra-fileupload_2.8.0" % "2.0.0-SNAPSHOT"

  //Define extra repos needed for scalatra snapshots

  val fuseSourceSnapshots = "FuseSource Snapshot Repository" at 
    "http://repo.fusesource.com/nexus/content/repositories/snapshots"
  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at 
    "http://scala-tools.org/repo-snapshots"
  val ossSnapshots = "OSS (Sonatype) Snapshot Repo" at
    "https://oss.sonatype.org/content/repositories/snapshots/"
  



  val proguardJar = "net.sf.proguard" % "proguard" % "4.3" % "tools->default"

  override def mainClass = Some("com.benkolera.scalatradiff.DiffApp")

  lazy val outputJar = outputPath / (name + "-" + version + "-standalone.jar")

  val toolsConfig = config("tools")
  def rootProjectDirectory = rootProject.info.projectPath

  val proguardConfigurationPath: Path = outputPath / "proguard.pro"
  lazy val proguard = 
    proguardTask dependsOn(`package`, writeProguardConfiguration)
  
  private lazy val writeProguardConfiguration = 
    writeProguardConfigurationTask dependsOn `package`
  
  //lazy val pack = packTask dependsOn(proguard)

  private def proguardTask = task {
    FileUtilities.clean(outputJar :: Nil, log)
    val proguardClasspathString = 
      Path.makeString(managedClasspath(toolsConfig).get)
    val configFile = proguardConfigurationPath.toString
    val exitValue = Process(
      "java", 
      List(
        "-Xmx256M", 
        "-cp", 
        proguardClasspathString, 
        "proguard.ProGuard", 
        "@" + configFile)
    ) ! log
    
    if(exitValue == 0) 
      None 
    else 
      Some("Proguard failed with nonzero exit code (" + exitValue + ")")
  }
  
  private def writeProguardConfigurationTask =
    task {
      /* the template for the proguard configuration file
      * You might try to remove "-keep class *" and "-keep class *", 
      * but this might break dynamic classloading.
      */
      val outTemplate = """
      |-dontskipnonpubliclibraryclasses
      |-dontskipnonpubliclibraryclassmembers
      |-dontoptimize
      |-dontobfuscate
      |-dontshrink
      |-dontpreverify
      |-dontnote
      |-dontwarn
      |-libraryjars %s
      |%s
      |-outjars %s
      |-ignorewarnings
      |-keep class *
      |-keep class %s { *** main(...); }
      |"""

      val defaultJar = (outputPath / defaultJarName).asFile.getAbsolutePath
      log.debug("proguard configuration using main jar " + defaultJar)
      
      val externalDependencies = Set() ++ (
        mainCompileConditional.analysis.allExternals ++ 
        compileClasspath.get.map { _.asFile }
      ) map { _.getAbsoluteFile } filter { _.getName.endsWith(".jar") }
      
      def quote(s: Any) = '"' + s.toString + '"'
      log.debug(
        "proguard configuration external dependencies: \n\t" + 
        externalDependencies.mkString("\n\t")
      )
      
      // partition jars from the external jar dependencies of this project 
      // by whether they are located in the project directory
      // if they are, they are specified with -injars, otherwise they are 
      // specified with -libraryjars
      val (externalJars, libraryJars) = 
        externalDependencies.toList.partition(
          jar => Path.relativize(
            rootProjectDirectory, jar
          ).isDefined
        )
      
      log.debug(
        "proguard configuration library jars locations: " 
        + libraryJars.mkString(", ")
      )
      
      // exclude properties files and manifests from scala-library jar
      val inJars = (
        quote(defaultJar) :: externalJars.map(
          quote(_) + 
          "(!META-INF/**,!*.txt)")
      ).map("-injars " + _).mkString("\n")
      
      val proguardConfiguration = outTemplate.stripMargin.format(
        libraryJars.map(quote).mkString(File.pathSeparator), 
        inJars, 
        quote(outputJar.absolutePath), 
        mainClass.get
      )
      
      log.debug(
        "Proguard configuration written to " + proguardConfigurationPath
      )
      
      FileUtilities.write(
        proguardConfigurationPath.asFile, 
        proguardConfiguration, 
        log
      )
    }
  
}
