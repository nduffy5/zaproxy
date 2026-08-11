FROM --platform=linux/amd64 zaproxy/zap-nightly@sha256:42b0da85583712ed32529c5a624e814c6653058ffb06ce3be4a539fa7aef57d3 # pinned 2026-08-11

USER root

RUN apt-get update && \
	wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
	apt-get install -y ./google-chrome-stable_current_amd64.deb && \
	rm -rf /var/lib/apt/lists/*

USER zap
