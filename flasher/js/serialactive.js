window.onload = () => {
  'use strict';

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('../sw.js');
  }

  if (!('serial' in navigator)) {
    document.location.href = '/pages/serialnotactive.html';
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

async function sync() {
  
  await writeToStream(writer, 0xc0, 0x00, 0x08, 0x07, 0x07, 0x12, 0x20, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0xc0);
  //b'\x07\x07\x12\x20' + 32 * b'\x55'
}

async function spiAttach() {
  //c0000d0800000000000000000000000000c0
  await writeToStream(writer, 0xc0, 0x00, 0x0d, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xc0);
}

async function spiSetParams() {
  //c0000b1800000000000000000000004000000001000010000000010000ffff0000c0
  await writeToStream(writer, 0xc0, 0x00, 0x0b, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0xff, 0xff, 0x00, 0x00, 0xc0);
}

async function changeBaud() {
  //c0000f0800000000000008070000000000c0
  writeToStream(writer, 0xc0, 0x00, 0x0f, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0xc0);
}

var progress = 0;
var filesFlashed = 0;
const adress1 = Uint8Array.of(0x00, 0x10, 0x00, 0x00);
const adress2 = Uint8Array.of(0x00, 0x80, 0x00, 0x00);
const adress3 = Uint8Array.of(0x00, 0xe0, 0x00, 0x00);
const adress4 = Uint8Array.of(0x00, 0x00, 0x01, 0x00);
const adresses = Array.of(adress1, adress2, adress3, adress4);
async function flashFileFromUrl(url, md5checksum) {
  //      |  ||      ||15872 ||  16  || 1024 ||0x1000|
  //c00002100000000000003e0000100000000004000000100000c0
  //c0 00 02 10 00 00 00 00 00 00 3e 00 00 10 00 00 00 00 04 00 00 00 10 00 00 c0
 

  //What file flashing?
  const filenameParagraph = document.createElement("p"); //TODO: File (1/4)
  const node = document.createTextNode("flashing file: \"" + url.substring(url.lastIndexOf("/") + 1, url.length) + "\"");
  filenameParagraph.appendChild(node);
  const barRoot = document.getElementById("statusBarRoot");
  barRoot.style.textAlign = 'center';
  barRoot.appendChild(filenameParagraph);

  //Add status bar
  const background = document.createElement("div");
  const bar = document.createElement("div");

  background.style.width = "100%";
  background.style.backgroundColor = "grey";

  bar.style.width = "1%";
  bar.style.backgroundColor = "green";
  bar.style.height = "30px";

  background.appendChild(bar);
  barRoot.appendChild(background);

  return new Promise((resolve, reject) => {
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

      const nOfDataPackets = Math.floor(fileContent.length / 1024);
      console.log('Number of Data Packets', nOfDataPackets);
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

        console.log(`checksum: ${checkSum}`);

        console.log(`Hex: ${indexHexString}`);

        await writeToStream(writer, 0xc0, 0x00, 0x03, 0x10, 0x04, checkSum /*0xcc*/, 0x00, 0x00, 0x00, /*lengthHexString[0], lengthHexString[1], lengthHexString[2], lengthHexString[3],*/ 0x00, 0x04, 0x00, 0x00, parseInt(indexHexString.substring(6, 8), 16), parseInt(indexHexString.substring(4, 6), 16), parseInt(indexHexString.substring(2, 4), 16), parseInt(indexHexString.substring(0, 2), 16), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, subArr, 0xc0);

        const answer = await read(secReader);
        if (answer.data[answer.data.length - 4] > 0) {
          reject(new Error(`fail from chip: code: ${answer.data[answer.data.length - 3]}`));
        }
        //Update status bar
        progress = i * 100 / nOfDataPackets;
        bar.style.width = progress + "%";
        bar.innerHTML = '';
        const paragraph = document.createElement("p");
        //paragraph.style = 'text-align: center;';
        paragraph.appendChild(document.createTextNode(progress + "%"));
        bar.appendChild(paragraph);
      }
      progress = 100;
      bar.style.width = progress + "%";
      console.log('sended');
      barRoot.innerHTML = '';
      progress = 1;
      //get md5 checksum from esp
      //c0001310000000000000100000003e00000000000000000000c0
      await writeToStream(writer, 0xc0, 0x00, 0x13, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, adresses[filesFlashed], parseInt(sizeHexString.substring(6, 8), 16), parseInt(sizeHexString.substring(4, 6), 16), parseInt(sizeHexString.substring(2, 4), 16), parseInt(sizeHexString.substring(0, 2), 16), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xc0);
      const md5SlipFrame = await read(secReader);
      const md5checksumToCheck = enc.decode(md5SlipFrame.data.buffer);
      console.log('Checksum from chip: ', md5checksumToCheck);
      console.log('Checksum from file: ', md5checksum);
      if (md5checksumToCheck.localeCompare(md5checksum) == 0) {
        filesFlashed = filesFlashed + 1;
        resolve();
      } else {
        reject(new Error('Checksum Fail'));
      }
    }
    downloadBlobFromUrl(url, fileReader);
  })


}

function downloadBlobFromUrl(url, fileReader) {
  const xhr = new XMLHttpRequest();
  xhr.open('GET', url);
  xhr.responseType = 'blob';
  xhr.setRequestHeader('Accept', 'application/octet-stream');
  xhr.onload = () => {
    fileReader.readAsArrayBuffer(xhr.response);
  }
  xhr.send();
}

function downloadBlobFromUrlAsText(url) {
  return new Promise((resolve, reject) => {
    const fileReader = new FileReader();
    fileReader.onload = () => {
      const res = fileReader.result;
      resolve(res);
    }
    fileReader.onerror = () => {
      reject(new Error('Error in read file on downloadBlobFromUrlAsText'));
    }
    const xhr = new XMLHttpRequest();
    xhr.open('GET', url);
    xhr.responseType = 'blob';
    xhr.setRequestHeader('Accept', 'application/octet-stream');
    xhr.onload = () => {
      fileReader.readAsText(xhr.response);
    }
    xhr.onerror = () => {
      reject(new Error('Error in http request from downloadBlobFromUrlAsText'));
    }
    xhr.send();
  })
}

function concatTypedArrays(a, b) { // a, b TypedArray of same type
  var c = new (a.constructor)(a.length + b.length);
  c.set(a, 0);
  c.set(b, a.length);
  return c;
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
  document.getElementById("statusBarRoot").innerHTML = '';
  port = await navigator.serial.requestPort();
  // - Wait for the port to open.
  await port.open({ baudRate: 115200 });
  let img = document.getElementById('statusPic').setAttribute("src", '../images/upload.gif');


  writer = port.writable.getWriter();

  await new Promise(resolve => setTimeout(resolve, 100));
  await enterBootloader();

  const slipTransformer = new TransformStream(new SlipFrameTransformer());
  readerClosed = port.readable.pipeTo(slipTransformer.writable);
  secReader = slipTransformer.readable.getReader();

  //const baseUrl = 'http://localhost/';
  const baseUrl = 'https://flasher.uniks.de/';

  await syncAndRead(secReader);
  await spiAttach();
  await read(secReader);
  await spiSetParams();
  await read(secReader);
  const hashesJsonText = await downloadBlobFromUrlAsText(baseUrl + 'hashes/hashes.json');
  const hashesJson = JSON.parse(hashesJsonText);
  await flashFileFromUrl(baseUrl + 'firmwares/bootloader_dio_40m.bin', hashesJson['bootloader']);
  await flashFileFromUrl(baseUrl + 'firmwares/partitions.bin', hashesJson['partitions']);
  await flashFileFromUrl(baseUrl + 'firmwares/boot_app0.bin', hashesJson['bootapp']);
  await flashFileFromUrl(baseUrl + 'firmwares/firmware.bin', hashesJson['firmware']);
  filesFlashed = 0;

  await endFlash();
  await reset();

  await secReader.cancel();
  await readerClosed.catch(() => { });
  await writer.close();
  await port.close();

  secReader = null;
  writer = null;
  port = null;

  const sendedParagraph = document.createElement("p");
  const node = document.createTextNode("flashing complete");
  sendedParagraph.appendChild(node);
  const barRoot = document.getElementById("statusBarRoot");
  barRoot.appendChild(sendedParagraph);
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
  console.log('[SEND]', lines);
  await writer.write(Uint8Array.of(lines[0]));
  for (var i = 1; i < lines.length - 1; i++) {
    if (ArrayBuffer.isView(lines[i])) {
      const tmp = new Uint8Array(lines[i]);
      for (const tmpp of tmp) {
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
    } else {
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

  //writer.releaseLock();
}

var readingPromise = null;
async function read(reader) {
  //try {
  let timeoutHandle;
  const timeoutPromise = new Promise((resolve, reject) => {
    timeoutHandle = setTimeout(() => reject('Timed out'), 15000);
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
