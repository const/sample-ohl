$profile = $args[0]
$report = $args[0].Replace(",", "_")
mkdir -Force ./target/report | out-null
docker run -t -v ${PWD}\\target/report:/report -v ${PWD}\\target\\sample-ohl-0.0.1-SNAPSHOT.jar:/app/app.jar --rm --network host --cap-add SYS_ADMIN --security-opt seccomp=unconfined --name sample-ohl-app const/jre-21-async-profiler java -agentpath:/opt/async-profiler-4.1-linux-x64/lib/libasyncProfiler.so=start,event=cpu,include=sample/service/output/runner/StressTestService.measure,file=/report/single-cpu-$($report).html -jar /app/app.jar --spring.profiles.active=$profile single-rps
