require 'buildr/scala'

VERSION_NUMBER = "1.0.0"

COPYRIGHT = "Copyright (C) 2009-2010 Alex Boisvert"

repositories.remote << "http://www.scala-tools.org/repo-snapshots"
repositories.remote << "http://www.ibiblio.org/maven2/"

desc "Stopwatch project"
define "stopwatch" do
  project.version = VERSION_NUMBER
  project.group = "org.alexboisvert"
  manifest["Implementation-Vendor"] = COPYRIGHT

  define "core" do
    package(:jar, :id => 'stopwatch')
  end

  define "web" do
    compile.with projects("core")
    package(:jar)
    task "server" do
      Java.java "stopwatch.web.TestServer",
                :classpath => [ compile.dependencies, compile.target,
                                test.compile.target, resources.target ]
    end
  end

end

task "server" => "stopwatch:web:server"

