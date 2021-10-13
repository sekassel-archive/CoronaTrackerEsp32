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

const fileSelector2 = document.getElementById('file-selector2');
fileSelector2.addEventListener('change', (event) => {
  const file = event.target.files[0];
  //console.log(file);
  md5OfFile(file)
});
async function md5OfFile(file) {
  var fileReader = new FileReader();
  fileReader.onload = async function () {
    var readerResult = fileReader.result;
    console.log(md5(readerResult));
  }
  fileReader.readAsBinaryString(file);
}



const fileSelector = document.getElementById('file-selector');
fileSelector.addEventListener('change', (event) => {
  const file = event.target.files[0];
  //console.log(file);
  flashBootloader(file)
});

var filesFlashed = 0;
const adress1 = Uint8Array.of(0x00, 0x10, 0x00, 0x00);
const adress2 = Uint8Array.of(0x00, 0x80, 0x00, 0x00);
const adress3 = Uint8Array.of(0x00, 0xe0, 0x00, 0x00);
const adress4 = Uint8Array.of(0x00, 0x00, 0x01, 0x00);
const adresses = Array.of(adress1, adress2, adress3, adress4);
async function flashBootloader(file) {
  //      |  ||      ||15872 ||  16  || 1024 ||0x1000|
  //c00002100000000000003e0000100000000004000000100000c0
  //c0 00 02 10 00 00 00 00 00 00 3e 00 00 10 00 00 00 00 04 00 00 00 10 00 00 c0
  //await writeToStream(writer, 0xc0, 0x00, 0x02, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3e, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0xc0);
  //const res = await read(secReader);
  //await read(secReader);

  var fileReader = new FileReader();
  var fileContent = new Uint8Array();
  fileReader.onload = async function () {
    var readerResult = fileReader.result;
    fileContent = new Uint8Array(readerResult)
    if (!("TextDecoder" in window))
      alert("Sorry, this browser does not support TextDecoder...");

    var enc = new TextDecoder("utf-8");
    console.log(fileContent.length)
    const sizeHexString = toHexString(fileContent.length);
    console.log(sizeHexString);

    //console.log(md5(fileContent.buffer));
    //console.log(JSON.stringify(fileContent, null, 2));
    //console.log(fileContent.length);
    //console.log(checkSum);

    //fill filecontent with ff to fit x*1024
    //if != 0
    if (fileContent.length % 1024 != 0) {
      const nOfFF = 1024 - fileContent.length % 1024;
      var arrFF = new Uint8Array(nOfFF);
      for (var i = 0; i < nOfFF; i++) {
        arrFF[i] = 0xff;
      }
      fileContent = concatTypedArrays(fileContent, arrFF);
    }

    console.log(md5(enc.decode(fileContent.buffer)));

    const nOfDataPackets = Math.floor(fileContent.length / 1024);
    console.log(nOfDataPackets);
    const nOfDataPacketsHexString = toHexString(nOfDataPackets);

    await writeToStream(writer, 0xc0, 0x00, 0x02, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, /*|*/parseInt(sizeHexString.substring(6, 8), 16), parseInt(sizeHexString.substring(4, 6), 16), parseInt(sizeHexString.substring(2, 4), 16), parseInt(sizeHexString.substring(0, 2), 16),/*|*/ parseInt(nOfDataPacketsHexString.substring(6, 8), 16), parseInt(nOfDataPacketsHexString.substring(4, 6), 16), parseInt(nOfDataPacketsHexString.substring(2, 4), 16), parseInt(nOfDataPacketsHexString.substring(0, 2), 16),/*|*/ 0x00, 0x04, 0x00, 0x00,/*|*/ adresses[filesFlashed], 0xc0);
    await read(secReader);

    for (var i = 0; i < nOfDataPackets; i++) {
      var subArr = fileContent.subarray(i * 1024, i * 1024 + 1024);
      var checkSum = 0xef;
      for (var j = 0; j < subArr.length; j++) {
        checkSum = checkSum ^ subArr[j]; //TODO: escape checksum
      }
      var indexHexString = toHexString(i);

      console.log(`len: ${subArr.length}`);

      //subArr = escapeArray(subArr);
      console.log(`checksum: ${checkSum}`);

      console.log(`Hex: ${indexHexString}`);

      await writeToStream(writer, 0xc0, 0x00, 0x03, 0x10, 0x04, checkSum /*0xcc*/, 0x00, 0x00, 0x00, /*lengthHexString[0], lengthHexString[1], lengthHexString[2], lengthHexString[3],*/ 0x00, 0x04, 0x00, 0x00, /*TODO: convertToNumber*/ parseInt(indexHexString.substring(6, 8), 16), parseInt(indexHexString.substring(4, 6), 16), parseInt(indexHexString.substring(2, 4), 16), parseInt(indexHexString.substring(0, 2), 16), /*0x00, 0x00, 0x00, 0x00,*/ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, subArr, 0xc0);
      /*for (var j = 0; j < subArr.length; j++) {
        writeToStream(writer, subArr[j]);
      }*/
      //writeToStream(writer, 0xc0);
      //await new Promise(resolve => setTimeout(resolve, 3200));

      const answer = await read(secReader);
      if (answer.data[answer.data.length - 4] > 0) {
        throw new Error(`fail from chip: code: ${answer.data[answer.data.length - 3]}`);
      }
    }
    console.log('sended');

    //get md5 checksum from esp
    //c0001310000000000000100000003e00000000000000000000c0
    await writeToStream(writer, 0xc0, 0x00, 0x13, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, adresses[filesFlashed], parseInt(sizeHexString.substring(6, 8), 16), parseInt(sizeHexString.substring(4, 6), 16), parseInt(sizeHexString.substring(2, 4), 16), parseInt(sizeHexString.substring(0, 2), 16), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xc0);
    const md5SlipFrame = await read(secReader);
    console.log(enc.decode(md5SlipFrame.data.buffer));
    filesFlashed = filesFlashed + 1;
  }
  fileReader.readAsArrayBuffer(file)
  /*fetch("https://api.github.com/repos/drinkbuddy/trackerTest/releases/assets/29689661")
    .then(res => res.blob()).then(blob => {
      fileReader.readAsArrayBuffer(blob);
    });*/
  //writeToStream(writer, 0xc0, 0x00, 0x02, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3e, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0xc0);
}

function testDownload() {
  const xhr = new XMLHttpRequest();
  xhr.open('GET', 'http://127.0.0.1/firmwares/firmware.bin');
  xhr.responseType = 'blob';
  xhr.setRequestHeader('Accept', 'application/octet-stream');
  xhr.onload = () => {
    downloadBlob(xhr.response, 'testfirmware.bin');
  }
  xhr.send();
}
function downloadBlob(blob, name = 'file.txt') {
  // Convert your blob into a Blob URL (a special url that points to an object in the browser's memory)
  const blobUrl = URL.createObjectURL(blob);

  // Create a link element
  const link = document.createElement("a");

  // Set link's href to point to the Blob URL
  link.href = blobUrl;
  link.download = name;

  // Append link to the body
  document.body.appendChild(link);

  // Dispatch click event on the link
  // This is necessary as link.click() does not work on the latest firefox
  link.dispatchEvent(
    new MouseEvent('click', {
      bubbles: true,
      cancelable: true,
      view: window
    })
  );

  // Remove link from body
  document.body.removeChild(link);
}


/* For the example */
const exportButton = document.getElementById('export');
const jsonBlob = new Blob(['{"name": "test"}'])

exportButton.addEventListener('click', () => {
  testDownload();
});

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

function toHexString(num) {
  var numHexString = num.toString(16);
  while (numHexString.length < 8) {
    numHexString = `0${numHexString}`;
  }
  return numHexString;
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

  console.log(md5("test"));
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
  endFlash();
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

async function endFlash() {
  await writeToStream(writer, 0xc0, 0x00, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xc0);
}

async function reset() {

  await port.setSignals({ requestToSend: true });
  await new Promise(resolve => setTimeout(resolve, 100));
  await port.setSignals({ requestToSend: false });
}

async function writeToStream(writer, ...lines) {
  await writer.write(Uint8Array.of(lines[0]));
  for (var i = 1; i < lines.length - 1; i++) {
    /*console.log('vor IS Array');
    if (Array.isArray(lines[i])) {
      await writer.write(lines[i])
      return;
    }
    //console.log('nach IS Array');*/
    if (ArrayBuffer.isView(lines[i])) {
      console.log('vor new Array');
      const tmp = new Uint8Array(lines[i]);
      console.log('nach new Array');
      console.log(tmp);
      for (const tmpp of tmp) {
        console.log(Uint8Array.of(tmpp));
        if (tmpp == 0xc0) {
          await writer.write(Uint8Array.of(0xdb));
          await writer.write(Uint8Array.of(0xdc));
        } else if (tmpp == 0xdb) {
          await writer.write(Uint8Array.of(0xdb));
          await writer.write(Uint8Array.of(0xdd));
        } else {
          await writer.write(Uint8Array.of(tmpp));
        }
      }
      //await writer.write(tmp);
    } else {
      //console.log('nach isview');

      console.log('[SEND]', lines[i]);
      //writer.write(line); // + '\n'
      if (lines[i] == 0xc0) {
        await writer.write(Uint8Array.of(0xdb));
        await writer.write(Uint8Array.of(0xdc));
      } else if (lines[i] == 0xdb) {
        await writer.write(Uint8Array.of(0xdb));
        await writer.write(Uint8Array.of(0xdd));
      } else {
        await writer.write(Uint8Array.of(lines[i]));
      }
    }

  }
  await writer.write(Uint8Array.of(lines[lines.length - 1]));
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
    timeoutHandle = setTimeout(() => reject('Timed out'), 9000);
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