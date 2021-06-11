window.onload = () => {
  'use strict';

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('../sw.js');
  }

  if (!('serial' in navigator)) {
    document.location.href = '/pages/serialnotactive.html';
  }
}


class LineBreakTransformer {
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

        if (this.startSetted && !this.endSetted) {
          var value2 = value;
          if (this.escaped) {
            if (value == 0xdc) {
              value2 = 0xc0;
            } else if (value == 0xdd) {
              value2 = 0xdb;
            }
            this.escaped = false;
          }

          if (this.direction == -1) {
            this.direction = value2;
            return;
          }
          if (this.command == -1) {
            this.command = value2;
            return;
          }

          if (this.size.length < 1) {
            this.size = Uint8Array.of(value2)
            return;
          }
          if (this.size.length < 2) {
            this.size = Uint8Array.of(this.size[0], value2)
            return;
          }


          if (this.value.length < 1) {
            this.value = Uint8Array.of(value2)
            return;
          }
          if (this.value.length < 2) {
            this.value = Uint8Array.of(this.value[0], value2)
            return;
          }
          if (this.value.length < 3) {
            this.value = Uint8Array.of(this.value[0], this.value[1], value2)
            return;
          }
          if (this.value.length < 4) {
            this.value = Uint8Array.of(this.value[0], this.value[1], this.value[2], value2)
            return;
          }

          if (this.data == null) {
            this.data = new Uint8Array(this.size[0] + 16 * this.size[1])
          }

          this.data[this.dataPosition] = value2;
          this.dataPosition = this.dataPosition + 1;
        }

        break;

    }
  }
}

class SlipFrameTransformer {
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
    for (var i = 0; i < arr.length; i++) {
      this.slipFrame.insert(arr[i]);
      if (this.slipFrame.endSetted) {
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
  sync();
});
async function sync() {
  // writeToStream(outputStream.getWriter(), '\x01');

  //var result = null;
  //while (result == null) {
  await writeToStream(writer, 0xc0, 0x00, 0x08, 0x07, 0x07, 0x12, 0x20, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0xc0);
  /*result = await Promise.race([
    (async () => {
      for(var i = 0; i < 8; i++){
        read(secReader);
      }
    })(),
    (async () => {
      await new Promise((res) => setTimeout(res, 2000));
      return null;
    })()
  ]);*/

  //}
  //b'\x07\x07\x12\x20' + 32 * b'\x55'
}


document.getElementById('readregButton').addEventListener('click', () => {
  flashBootloader();
});
async function readreg() {
  //c0000a0400000000000ca00160c0
  writeToStream(writer, 0xc0, 0x00, 0x0a, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0c, 0xa0, 0x01, 0x60, 0xc0);
}


document.getElementById('spiAttachButton').addEventListener('click', () => {
  spiAttach();
});
async function spiAttach() {
  //c0000d0800000000000000000000000000c0
  await writeToStream(writer, 0xc0, 0x00, 0x0d, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xc0);
  //const res = await read(secReader);
  //console.log(res);
}

document.getElementById('spiSetParamsButton').addEventListener('click', () => {
  spiSetParams();
});
async function spiSetParams() {
  //c0000b1800000000000000000000004000000001000010000000010000ffff0000c0
  //c0000b1800000000000000000000004000000001000010000000010000ffff0000c0
  await writeToStream(writer, 0xc0, 0x00, 0x0b, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0xff, 0xff, 0x00, 0x00, 0xc0);
  //await read(secReader);
}

document.getElementById('changeBaudButton').addEventListener('click', () => {
  changeBaud();
});
async function changeBaud() {
  //c0000f0800000000000008070000000000c0
  writeToStream(writer, 0xc0, 0x00, 0x0f, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0xc0);
  //await read(secReader);
}

document.getElementById('writeReg1Button').addEventListener('click', () => {
  writeReg1();
});
async function writeReg1() {
  //c000091000000000002c20006017000000ffffffff00000000c0
  writeToStream(writer, 0xc0, 0x00, 0x09, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2c, 0x20, 0x00, 0x60, 0x17, 0x00, 0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0xc0);
  //await read(secReader);
}

document.getElementById('writeReg2Button').addEventListener('click', () => {
  writeReg2();
});
async function writeReg2() {
  //c000091000000000001c20006000000090ffffffff00000000c0
  writeToStream(writer, 0xc0, 0x00, 0x09, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1c, 0x20, 0x00, 0x60, 0x00, 0x00, 0x00, 0x90, 0xff, 0xff, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0xc0);
  //await read(secReader);
}

document.getElementById('writeReg3Button').addEventListener('click', () => {
  writeReg3();
});
async function writeReg3() {
  //c00009100000000000242000609f000070ffffffff00000000c0
  writeToStream(writer, 0xc0, 0x00, 0x09, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x24, 0x20, 0x00, 0x60, 0x9f, 0x00, 0x00, 0x70, 0xff, 0xff, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0xc0);
  //await read(secReader);
}

document.getElementById('writeReg4Button').addEventListener('click', () => {
  writeReg4();
});
async function writeReg4() {
  //c000091000000000008020006000000000ffffffff00000000c0
  writeToStream(writer, 0xc0, 0x00, 0x09, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x20, 0x00, 0x60, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0xc0);
  //await read(secReader);
}

document.getElementById('writeReg5Button').addEventListener('click', () => {
  writeReg5();
});
async function writeReg5() {
  //c000091000000000000020006000000400ffffffff00000000c0
  writeToStream(writer, 0xc0, 0x00, 0x09, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x00, 0x60, 0x00, 0x00, 0x04, 0x00, 0xff, 0xff, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0xc0);
  //await read(secReader);
}

//After 2 read regs
document.getElementById('writeReg6Button').addEventListener('click', () => {
  writeReg6();
});
async function writeReg6() {
  //c000091000000000001c20006040000080ffffffff00000000c0
  writeToStream(writer, 0xc0, 0x00, 0x09, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1c, 0x20, 0x00, 0x60, 0x40, 0x00, 0x00, 0x80, 0xff, 0xff, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0xc0);
  //await read(secReader);
}

//After 2 read regs
document.getElementById('writeReg7Button').addEventListener('click', () => {
  writeReg7();
});
async function writeReg7() {
  //c000091000000000002420006000000070ffffffff00000000c0
  writeToStream(writer, 0xc0, 0x00, 0x09, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x24, 0x20, 0x00, 0x60, 0x00, 0x00, 0x00, 0x70, 0xff, 0xff, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0xc0);
  //await read(secReader);
}



const fileSelector = document.getElementById('file-selector');
fileSelector.addEventListener('change', (event) => {
  const file = event.target.files[0];
  //console.log(file);
  flashBootloader(file)
});
async function flashBootloader(file) {
  //c00002100000000000003e0000100000000004000000100000c0
  //c0 00 02 10 00 00 00 00 00 00 3e 00 00 10 00 00 00 00 04 00 00 00 10 00 00 c0
  await writeToStream(writer, 0xc0, 0x00, 0x02, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3e, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0xc0);
  //const res = await read(secReader);
  await read(secReader);

  var fileReader = new FileReader();
  var fileContent = new Uint8Array();
  fileReader.onload = async function () {
    fileContent = new Uint8Array(fileReader.result)
    //console.log(JSON.stringify(fileContent, null, 2));
    //console.log(fileContent.length);
    //console.log(checkSum);

    //fill filecontent with ff to fit x*1024
    //if != 0
    const nOfFF = 1024 - fileContent.length % 1024;
    var arrFF = new Uint8Array(nOfFF);
    for (var i = 0; i < nOfFF; i++) {
      arrFF[i] = 0xff;
    }
    fileContent = concatTypedArrays(fileContent, arrFF);

    const nOfDataPackets = Math.floor(fileContent.length / 1024);
    for (var i = 0; i < nOfDataPackets; i++) {
      var subArr = fileContent.subarray(i * 1024, i * 1024 + 1024);
      var checkSum = 0xef;
      for (var j = 0; j < subArr.length; j++) {
        checkSum = checkSum ^ subArr[j];
      }
      var indexHexString = (i).toString(16);
      //fill zeros
      while (indexHexString.length < 8) {
        indexHexString = `0${indexHexString}`;
      }

      console.log(`len: ${subArr.length}`);

      subArr = escapeArray(subArr);
      console.log(`checksum: ${checkSum}`);

      console.log(`Hex: ${indexHexString}`);

      await writeToStream(writer, 0xc0, 0x00, 0x03, 0x10, 0x04, checkSum /*0xcc*/, 0x00, 0x00, 0x00, /*lengthHexString[0], lengthHexString[1], lengthHexString[2], lengthHexString[3],*/ 0x00, 0x04, 0x00, 0x00, /*TODO: convertToNumber*/ parseInt(indexHexString.substring(6, 8), 10), parseInt(indexHexString.substring(4, 6), 10), parseInt(indexHexString.substring(2, 4), 10), parseInt(indexHexString.substring(0, 2), 10), /*0x00, 0x00, 0x00, 0x00,*/ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, subArr, 0xc0);
      /*for (var j = 0; j < subArr.length; j++) {
        writeToStream(writer, subArr[j]);
      }*/
      //writeToStream(writer, 0xc0);
      //await new Promise(resolve => setTimeout(resolve, 3200));

      await read(secReader);
    }
    console.log('sended');
  }
  fileReader.readAsArrayBuffer(file)
  //writeToStream(writer, 0xc0, 0x00, 0x02, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3e, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0xc0);
}
function concatTypedArrays(a, b) { // a, b TypedArray of same type
  var c = new (a.constructor)(a.length + b.length);
  c.set(a, 0);
  c.set(b, a.length);
  return c;
}
function escapeArray(arr) {
  var numToEscape = 0;
  for (var value of arr) {
    if (value == 0xc0 || value == 0xdb) {
      numToEscape++;
    }
  }
  var resultArray = new Uint8Array(arr.length + numToEscape);
  var indexCounter = 0;
  for (var i = 0; i < arr.length; i++) {
    if (arr[i] == 0xc0) {
      resultArray[indexCounter++] = 0xdb;
      resultArray[indexCounter++] = 0xdc;
    } else if (arr[i] == 0xdb) {
      resultArray[indexCounter++] = 0xdb;
      resultArray[indexCounter++] = 0xdd;
    } else {
      resultArray[indexCounter++] = arr[i];
    }
  }
  return resultArray;
}


document.getElementById('connectButton').addEventListener('click', () => {
  connect();
});

let inputStream = null;
let abortController = null;
async function connect() {
  port = await navigator.serial.requestPort();
  // - Wait for the port to open.
  await port.open({ baudRate: 115200 });
  let img = document.getElementById('statusPic').setAttribute("src", '../images/upload.gif');

  /*abortController = new AbortController();

  const decoder = new TextDecoderStream();
  inputStream = port.readable;
  const inputDone = inputStream.pipeTo(decoder.writable, { signal: abortController.signal });
  const inputStreamAsText = decoder.readable.pipeThrough(new TransformStream(new LineBreakTransformer()));

  //secReader = stream1.pipeThrough(new TransformStream(new SlipFrameTransformer())).getReader();

  secReader = inputStreamAsText.getReader();*/

  writer = port.writable.getWriter();

  //readLoop(secReader);

  await new Promise(resolve => setTimeout(resolve, 100));
  await enterBootloader();

  secReader = port.readable.pipeThrough(new TransformStream(new SlipFrameTransformer())).getReader();

  await syncAndRead(secReader);
  await spiAttach();
  await read(secReader);
  await spiSetParams();
  await read(secReader);
}

async function syncAndRead(secReader) {
  await sync();
  try {
    await read(secReader);
  } catch (e) {
    await syncAndRead(secReader);
    return;
  }
  var notFailed = true;
  while (notFailed) {
    try {
      await read(secReader);
    } catch (e) {
      notFailed = false;
    }
  }
}

var secReader = null;
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
}

async function reset() {

  await port.setSignals({ requestToSend: true });
  await new Promise(resolve => setTimeout(resolve, 100));
  await port.setSignals({ requestToSend: false });
}

async function writeToStream(writer, ...lines) {
  for (const line of lines) {
    //console.log('vor IS Array');
    if (Array.isArray(line)) {
      await writer.write(line)
      return;
    }
    //console.log('nach IS Array');
    if (ArrayBuffer.isView(line)) {
      console.log('vor new Array');
      const tmp = new Uint8Array(line);
      console.log('nach new Array');
      console.log(tmp);
      for (const tmpp of tmp) {
        console.log(Uint8Array.of(tmpp));
        await writer.write(Uint8Array.of(tmpp));
      }
      //await writer.write(tmp);
    } else {
      //console.log('nach isview');

      console.log('[SEND]', line);
      //writer.write(line); // + '\n'
      await writer.write(Uint8Array.of(line));
    }

  }
  /*lines.forEach(async (line) => {
    
  });*/
  //await writer.write(arr);
  //writer.releaseLock();
}

let readLoopRuns = true;
async function readLoop(reader) {
  console.log('readloop begin');
  while (readLoopRuns) {
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
  /*while (true) {
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
  console.log('readloop end');*/

}
var readingPromise = null;
async function read(reader) {
  //try {
  let timeoutHandle;
  const timeoutPromise = new Promise((resolve, reject) => {
    timeoutHandle = setTimeout(() => reject(), 5000);
  });
  if (readingPromise == null) {
    readingPromise = reader.read();
  }
  const { value, done } = await Promise.race([
    readingPromise,
    timeoutPromise,
  ]).then((result) => {
    clearTimeout(timeoutHandle);
    return result;
  });
  readingPromise = null;
  if (value) {
    console.log(JSON.stringify(value, null, 2) + '\n');
    return value;
  }
  if (done) {
    console.log('[read] DONE', done);
    reader.releaseLock();
    return;
  }
  /*} catch (e) {
    console.log(e);
    return;
  }*/

}

async function promiseWithTimeout(timeoutMs, promise) {
  let timeoutHandle;
  const timeoutPromise = new Promise((resolve, reject) => {
    timeoutHandle = setTimeout(() => reject(), timeoutMs);
  });

  return Promise.race([
    promise(),
    timeoutPromise,
  ]).then((result) => {
    clearTimeout(timeoutHandle);
    return result;
  });
}