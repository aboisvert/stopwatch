require 'buildr/scala'
require 'buildr/groovy'

# temporary until Scala 2.9.1 is released
if Buildr::Scala.version =~ /2.9.1/
  Buildr.settings.build['scala.test'] = "org.scalatest:scalatest_2.9.0:jar:1.6.1"
  Buildr.settings.build['scala.check'] = "org.scala-tools.testing:scalacheck_2.9.0-1:jar:1.9"
end

repositories.remote << "http://www.scala-tools.org/repo-snapshots"
repositories.remote << "http://mirrors.ibiblio.org/pub/mirrors/maven2/"

Java.load

VERSION_NUMBER = "2.0.1"

COPYRIGHT = "Copyright (C) 2009-2010 Alex Boisvert"

repositories.release_to[:username] ||= "boisvert"
repositories.release_to[:url] ||= "/tmp/maven2"
repositories.release_to[:permissions] ||= 0664

Project.local_task :run

desc "Stopwatch project"
define "stopwatch2_#{Buildr::Scala.version}" do
  project.version = VERSION_NUMBER
  project.group = "stopwatch"
  manifest["Implementation-Vendor"] = COPYRIGHT

  define "core" do
    package(:jar, :id => "stopwatch_#{Buildr::Scala.version}")

    run.using :main => ["stopwatch2.StopwatchPerformanceSuiteRunner", "-t"],
              :java_args => ["-server"]
  end

  define "web" do
    compile.with projects("core")
    package(:jar)

    run.using :main => "stopwatch2.web.SampleServer"
  end

  doc.using :scaladoc
  doc.from projects('core', 'web')
end

task "perf" => "stopwatch2_#{Buildr::Scala.version}:core:run"
task "sample" => "stopwatch2_#{Buildr::Scala.version}:web:run"

