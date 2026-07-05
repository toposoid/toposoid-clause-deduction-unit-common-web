FROM toposoid/toposoid-scala-lib:0.7-SNAPSHOT

WORKDIR /app
ARG TARGET_BRANCH
ARG JAVA_OPT_XMX
ENV DEPLOYMENT=local
ENV _JAVA_OPTIONS="-Xms256m -Xmx4g"

#COPY . /app/toposoid-clause-deduction-unit-common-web

RUN git clone https://github.com/toposoid/toposoid-test-utils.git \
&& cd toposoid-test-utils \
&& git pull \
&& git fetch origin ${TARGET_BRANCH} \
&& git checkout ${TARGET_BRANCH} \
&& sbt publishLocal \
&& rm -Rf ./target \
&& cd .. \
&& git clone https://github.com/toposoid/toposoid-clause-deduction-unit-common-web.git \
&& cd toposoid-clause-deduction-unit-common-web \
&& git fetch origin ${TARGET_BRANCH} \
&& git checkout ${TARGET_BRANCH} \
&& sbt playUpdateSecret 1> /dev/null \
&& sbt dist \
&& cd /app/toposoid-clause-deduction-unit-common-web/target/universal \
&& unzip -o toposoid-clause-deduction-unit-common-web-0.7-SNAPSHOT.zip

COPY ./docker-entrypoint.sh /app/
ENTRYPOINT ["/app/docker-entrypoint.sh"]

