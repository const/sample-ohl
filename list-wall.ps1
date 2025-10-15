$profile = $args[0]
$report = $profile.Replace(",", "_")
mkdir -Force ./target/report | out-null
docker run -t -v ${PWD}\\target/report:/report -v ${PWD}\\target\\sample-ohl-0.0.1-SNAPSHOT.jar:/app/app.jar --rm --network host --cap-add SYS_ADMIN --security-opt seccomp=unconfined --name sample-ohl-app const/jre-21-async-profiler java -agentpath:/opt/async-profiler-4.1-linux-x64/lib/libasyncProfiler.so=start,event=wall,include=sample/service/output/runner/StressTestService.measure,file=/report/list-wall-$($report).html -jar /app/app.jar --spring.profiles.active=$profile list-rps
