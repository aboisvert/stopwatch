require 'buildr/scala'
require 'buildr/groovy'
#require 'buildr/clojure'

repositories.remote << "http://www.scala-tools.org/repo-snapshots"
repositories.remote << "http://mirrors.ibiblio.org/pub/mirrors/maven2/"

Java.load

VERSION_NUMBER = "1.0.1-SNAPSHOT"

COPYRIGHT = "Copyright (C) 2009-2010 Alex Boisvert"

repositories.release_to[:username] ||= "boisvert"
repositories.release_to[:url] ||= "/tmp/maven2"
repositories.release_to[:permissions] ||= 0664

Project.local_task :run

desc "Stopwatch project"
define "stopwatch" do
  project.version = VERSION_NUMBER
  project.group = "org.alexboisvert.stopwatch"
  manifest["Implementation-Vendor"] = COPYRIGHT

  define "core" do
    package(:jar, :id => 'stopwatch')

    run.using :main => ["stopwatch.StopwatchPerformanceSuiteRunner", "-t"],
              :java_args => ["-server"]
  end

  define "web" do
    compile.with projects("core")
    package(:jar)

    run.using :main => "stopwatch.web.SampleServer"
  end

  doc.using :scaladoc
  doc.from projects('core', 'web')
end

task "perf" => "stopwatch:core:perf"
task "sample" => "stopwatch:web:sample"

