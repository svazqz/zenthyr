FROM node:22-bookworm-slim AS node
FROM ubuntu:24.04
ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
    leiningen openjdk-17-jdk xvfb xauth ca-certificates libgtk-3-0t64 libnss3 \
    libasound2t64 libgbm1 libdrm2 libxss1 libxshmfence1 libgl1 libx11-xcb1 \
    && rm -rf /var/lib/apt/lists/*
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules/npm /usr/local/lib/node_modules/npm
RUN ln -s /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm
ENV LEIN_ROOT=1 CLOVERAGE_VERSION=1.2.2
WORKDIR /app
COPY project.clj ./
RUN lein with-profile +coverage,+native-$(dpkg --print-architecture) deps
COPY src src
COPY test test
RUN lein with-profile +coverage,+native-$(dpkg --print-architecture) cloverage --help
CMD ["sh", "test/run-coverage.sh"]
