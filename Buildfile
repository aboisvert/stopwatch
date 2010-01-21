require 'buildr/scala'

repositories.remote << "http://www.scala-tools.org/repo-snapshots"
repositories.remote << "http://mirrors.ibiblio.org/pub/mirrors/maven2/"

Java.load

VERSION_NUMBER = "1.0-for-scala-#{Buildr::Scala.version_str}-SNAPSHOT"

COPYRIGHT = "Copyright (C) 2009-2010 Alex Boisvert"

repositories.release_to[:username] ||= "boisvert"
repositories.release_to[:url] ||= "sftp://repo.alexboisvert.org/var/www/repo.alexboisvert.org/maven2"
repositories.release_to[:permissions] ||= 0664

desc "Stopwatch project"
define "stopwatch" do
  project.version = VERSION_NUMBER
  project.group = "org.alexboisvert.stopwatch"
  manifest["Implementation-Vendor"] = COPYRIGHT

  define "core" do
    package(:jar, :id => 'stopwatch')

    task "perf" do
      Java.java "stopwatch.StopwatchPerformanceSuiteRunner",
        :classpath => [ test.compile.dependencies, test.compile.target,
                        test.compile.target, resources.sources ],
        :java_args => ["-server"]
    end
  end

  define "web" do
    compile.with projects("core")
    package(:jar)

    task "sample" do
      if ENV["JREBEL_HOME"]
        java_args = [
          "-noverify",
          "-javaagent:#{ENV['JREBEL_HOME']}/jrebel.jar"
              ]
      end
      Java.java "stopwatch.web.SampleServer",
        :classpath => [ compile.dependencies, compile.target,
                        test.compile.target, resources.sources ],
        :java_args => java_args || []
    end

  end
end

task "perf" => "stopwatch:core:perf"
task "sample" => "stopwatch:web:sample"

