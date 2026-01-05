FROM ghcr.io/graalvm/native-image-community:21 AS builder

WORKDIR /build

COPY . .

RUN chmod +x gradlew

RUN ./gradlew nativeCompile --no-daemon

FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /build/build/native/nativeCompile/downloader /app/downloader

ENTRYPOINT ["/app/downloader"]