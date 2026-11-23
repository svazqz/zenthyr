FROM node:22-bookworm-slim AS node
FROM ubuntu:24.04
ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
    leiningen openjdk-17-jdk-headless xvfb xauth ca-certificates \
    && rm -rf /var/lib/apt/lists/*
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules/npm /usr/local/lib/node_modules/npm
RUN ln -s /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm
ENV LEIN_ROOT=1
WORKDIR /app
COPY project.clj ./
RUN lein deps
COPY src src
COPY test test
CMD ["xvfb-run", "-a", "lein", "-o", "test"]
