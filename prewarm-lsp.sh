#!/usr/bin/env bash
set -e

KOTLIN_LS="$HOME/.cache/mimocode/bin/kotlin-ls/kotlin-lsp.sh"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
TIMEOUT=300

echo "Pre-warming kotlin-ls for $PROJECT_DIR ..."

ANDROID_HOME="$HOME/code/Android/sdk" \
ANDROID_SDK_ROOT="$HOME/code/Android/sdk" \
timeout $TIMEOUT \
node -e "
const { spawn } = require('child_process');

const server = spawn('$KOTLIN_LS', ['--stdio'], {
  cwd: '$PROJECT_DIR',
  env: { ...process.env, ANDROID_HOME: '$HOME/code/Android/sdk', ANDROID_SDK_ROOT: '$HOME/code/Android/sdk' },
  stdio: ['pipe', 'pipe', 'pipe']
});

server.stderr.on('data', () => {});

let buf = '';
server.stdout.on('data', d => {
  buf += d.toString();
  if (buf.includes('\"result\"')) {
    const initResp = JSON.stringify({ jsonrpc: '2.0', method: 'initialized', params: {} });
    server.stdin.write('Content-Length: ' + Buffer.byteLength(initResp) + '\\r\\n\\r\\n' + initResp);
    console.log('kotlin-ls initialized. Cache built. Shutting down...');
    setTimeout(() => { server.kill(); process.exit(0); }, 5000);
  }
});

const init = JSON.stringify({
  jsonrpc: '2.0', id: 1, method: 'initialize',
  params: { processId: process.pid, rootUri: 'file://$PROJECT_DIR', capabilities: {} }
});
server.stdin.write('Content-Length: ' + Buffer.byteLength(init) + '\\r\\n\\r\\n' + init);

setTimeout(() => {
  console.log('Timeout after ${TIMEOUT}s');
  server.kill();
  process.exit(1);
}, ${TIMEOUT}000);
" 2>/dev/null
