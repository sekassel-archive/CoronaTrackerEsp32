window.onload = () => {
  'use strict';

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('../sw.js');
  }

  if (!('serial' in navigator)) {
    document.location.href = '/pages/serialnotactive.html';
  }
}


class MyTransformer {
  constructor() {
    // A container for holding stream data until a new line.
    this.chunks = "";
  }

  start() {
    this.chunks = '';
  }

  transform(chunk, controller) {
    // Append new chunks to existing chunks.
    this.chunks += chunk;
    // For each line breaks in chunks, send the parsed lines out.
    const lines = this.chunks.split("\r\n");
    this.chunks = lines.pop();
    lines.forEach((line) => controller.enqueue(line));
  }

  flush(controller) {
    // When the stream is closed, flush any remaining chunks out.
    controller.enqueue(this.chunks);
  }
}

class SlipFrame {
  constructor() {
    this.escaped = false;
    this.startSetted = false;
    this.endSetted = false;
    this.direction = -1;
    this.command = -1;
    this.size = new Uint8Array();
    this.value = new Uint8Array();
    this.data = null;
    this.dataPosition = 0;
  }

  insert(value) {
    switch (value) {
      case 0xc0:
        if (!this.startSetted) {
          this.startSetted = true;
          return;
        } else {
          this.endSetted = true;
        }
        break;
      case 0xdb:
        this.escaped = true;
        break;
      default:

        if (this.startSetted && !this.endSetted){
          var value2 = value;
          if (this.escaped){
            if(value == 0xdc){
              value2 = 0xc0;
            } else if (value == 0xdd){
              value2 = 0xdb;
            }
            this.escaped = false;
          }

          if (this.direction == -1){
            this.direction = value2;
            return;
          }
          if (this.command == -1){
            this.command = value2;
            return;
          }

          if (this.size.length < 1){
            this.size = Uint8Array.of(value2)
            return;
          }
          if (this.size.length < 2){
            this.size = Uint8Array.of(this.size[0], value2)
            return;
          }

          
          if (this.value.length < 1){
            this.value = Uint8Array.of(value2)
            return;
          }
          if (this.value.length < 2){
            this.value = Uint8Array.of(this.value[0], value2)
            return;
          }
          if (this.value.length < 3){
            this.value = Uint8Array.of(this.value[0], this.value[1], value2)
            return;
          }
          if (this.value.length < 4){
            this.value = Uint8Array.of(this.value[0], this.value[1], this.value[2], value2)
            return;
          }

          if (this.data == null){
            this.data = new Uint8Array(this.size[0] + 16 * this.size[1])
          }
          
          this.data[this.dataPosition] = value2;
          this.dataPosition = this.dataPosition + 1;
        }

        break;

    }
  }
}

class TransformContent {
  constructor() {
    this.slipFrame = new SlipFrame();
  }

  start() {
    this.slipFrame = new SlipFrame();
   } // required.
  async transform(chunk, controller) {
    var arr = new Uint8Array();

    chunk = await chunk;
    switch (typeof chunk) {
      case 'object':
        // just say the stream is done I guess
        if (chunk === null) controller.terminate()
        else if (ArrayBuffer.isView(chunk)) {
          arr = new Uint8Array(chunk.buffer, chunk.byteOffset, chunk.byteLength);
          //console.log(JSON.stringify(arr, null, 2));
        }
        else if (Array.isArray(chunk) && chunk.every(value => typeof value === 'number'))
          arr = new Uint8Array(chunk)
        break
      case 'symbol':
        controller.error("Cannot send a symbol as a chunk part")
        return
      case 'undefined':
        controller.error("Cannot send undefined as a chunk part")
        return
      default:
        controller.enqueue(this.textencoder.encode(String(chunk)))
        return
    }

    //console.log(JSON.stringify(arr, null, 2))
    for (var i = 0; i < arr.length; i++){
      this.slipFrame.insert(arr[i]);
      if (this.slipFrame.endSetted){
        controller.enqueue(this.slipFrame)
        this.slipFrame = new SlipFrame();
      }
    }
  }
  flush() { /* do any destructor work here */ }
}

var writer = null;
var port = null;
var serialActive = 'not active';
if ('serial' in navigator) {
  serialActive = 'active';
}


document.getElementById('syncButton').addEventListener('click', () => {
  click2();
});
async function click2() {
  // writeToStream(outputStream.getWriter(), '\x01');

  writeToStream(writer, 0xc0, 0x00, 0x08, 0x07, 0x07, 0x12, 0x20, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0xc0);
  //b'\x07\x07\x12\x20' + 32 * b'\x55'
}

async function readreg() {
  //c0000a0400000000000ca00160c0
  writeToStream(writer, 0xc0, 0x00, 0x0a, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0c, 0xa0, 0x01, 0x60, 0xc0);
}

document.getElementById('connectButton').addEventListener('click', () => {
  click();
});

let secReader = null;
async function click() {
  port = await navigator.serial.requestPort();
  // - Wait for the port to open.
  await port.open({ baudRate: 115200 });
  let img = document.getElementById('statusPic').setAttribute("src", '../images/upload.gif');

  const decoder = new TextDecoderStream();
  const [stream1, stream2] = port.readable.tee();
  const inputDone = stream2.pipeTo(decoder.writable);
  const inputStream = decoder.readable.pipeThrough(new TransformStream(new MyTransformer()));

  secReader = stream1.pipeThrough(new TransformStream(new TransformContent())).getReader();

  const reader = inputStream.getReader();

  writer = port.writable.getWriter();

  readLoop(reader);
}


document.getElementById('bootloaderButton').addEventListener('click', () => {
  enterBootloader();
});
async function enterBootloader() {

  await port.setSignals({ dataTerminalReady: false });
  await port.setSignals({ requestToSend: true });
  await new Promise(resolve => setTimeout(resolve, 100));
  await port.setSignals({ dataTerminalReady: true });
  await port.setSignals({ requestToSend: false });

  await new Promise(resolve => setTimeout(resolve, 100));

  await port.setSignals({ dataTerminalReady: false });

  await new Promise(resolve => setTimeout(resolve, 3200));

  readLoop2(secReader);
}

async function reset() {

  await port.setSignals({ requestToSend: true });
  await new Promise(resolve => setTimeout(resolve, 100));
  await port.setSignals({ requestToSend: false });
}

function writeToStream(writer, ...lines) {
  const arr = new Uint8Array(40);
  var i = 0;
  lines.forEach((line) => {
    console.log('[SEND]', line);
    //writer.write(line); // + '\n'
    arr[i] = line;
    ++i;
  });
  writer.write(arr);
  //writer.releaseLock();
}

async function readLoop(reader) {
  console.log('readloop begin');
  while (true) {
    try {
      const { value, done } = await reader.read();
      if (value) {
        console.log(value /*JSON.stringify(value, null, 2)*/ + '\n');
      }
      if (done) {
        console.log('[readLoop] DONE', done);
        reader.releaseLock();
        break;
      }
    } catch (e) {
      console.log(e);
      break;
    }

  }
  console.log('readloop end');

}

async function readLoop2(reader) {
  console.log('readloop begin');
  while (true) {
    try {
      const { value, done } = await reader.read();
      if (value) {
        console.log(JSON.stringify(value, null, 2) + '\n');
      }
      if (done) {
        console.log('[readLoop] DONE', done);
        reader.releaseLock();
        break;
      }
    } catch (e) {
      console.log(e);
      break;
    }

  }
  console.log('readloop end');

}