FROM node:22-bookworm-slim

ARG FIREBASE_TOOLS_VERSION=15.20.0

RUN npm install --global "firebase-tools@${FIREBASE_TOOLS_VERSION}" \
    && firebase --version

WORKDIR /workspace

COPY firebase.json .firebaserc start-auth-emulator.cjs ./

EXPOSE 9099

CMD ["node", "start-auth-emulator.cjs"]
