FROM eclipse-temurin:21.0.8_9-jre-ubi10-minimal

RUN mkdir -p /opt/ && \
     cd /opt/ && \
     /usr/bin/curl -L https://github.com/async-profiler/async-profiler/releases/download/v4.1/async-profiler-4.1-linux-x64.tar.gz > ap.tar.gz && \
     tar zvxf *.tar.gz && \
     rm *.tar.gz


