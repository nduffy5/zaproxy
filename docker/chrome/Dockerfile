FROM --platform=linux/amd64 zaproxy/zap-nightly@sha256:7b494f83090654d315f79d4f72b3971515c1f77ecf4d7441c84779e2c92dbce0

USER root

RUN apt-get update && \
	wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
	apt-get install -y ./google-chrome-stable_current_amd64.deb && \
	rm -rf /var/lib/apt/lists/*

USER zap
