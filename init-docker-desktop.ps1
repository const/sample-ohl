wsl.exe -d docker-desktop sh -c "sysctl kernel.perf_event_paranoid=1"
wsl.exe -d docker-desktop sh -c "sysctl kernel.kptr_restrict=0"