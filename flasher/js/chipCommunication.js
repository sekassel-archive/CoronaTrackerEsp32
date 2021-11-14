import { DEBUGMODE } from './serialactive.js';


async function sync(writer) {

  await writeToStream(writer, 0xc0, 0x00, 0x08, 0x07, 0x07, 0x12, 0x20, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0xc0);
  //b'\x07\x07\x12\x20' + 32 * b'\x55'
}
export async function spiAttach(writer) {
  //c0000d0800000000000000000000000000c0
  await writeToStream(writer, 0xc0, 0x00, 0x0d, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xc0);
}
export async function spiSetParams(writer) {
  //c0000b1800000000000000000000004000000001000010000000010000ffff0000c0
  await writeToStream(writer, 0xc0, 0x00, 0x0b, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0xff, 0xff, 0x00, 0x00, 0xc0);
}

async function changeBaud(writer) {
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
export async function flashFileFromUrl(url, md5checksum, writer, secReader) {
  //      |  ||      ||15872 ||  16  || 1024 ||0x1000|
  //c00002100000000000003e0000100000000004000000100000c0
  //c0 00 02 10 00 00 00 00 00 00 3e 00 00 10 00 00 00 00 04 00 00 00 10 00 00 c0
  //What file flashing?
  const filenameParagraph = document.createElement("p"); //TODO: File (1/4)
  const flashingFileString = "flashing file: \"" + url.substring(url.lastIndexOf("/") + 1, url.length) + "\"";
  const nodeFilename = document.createTextNode(flashingFileString);
  filenameParagraph.appendChild(nodeFilename);
  const barRoot = document.getElementById("statusBarRoot");
  barRoot.style.position = 'relative';
  barRoot.appendChild(filenameParagraph);

  console.log(flashingFileString);

  //Add status bar
  const background = document.createElement("div");
  const bar = document.createElement("div");
  const paragraph = document.createElement("p");
  paragraph.className = 'center';
  background.appendChild(paragraph);
  var node = null;

  background.style.width = "100%";
  background.style.backgroundColor = "grey";

  bar.style.width = "1%";
  bar.style.backgroundColor = "green";
  bar.style.height = "30px";

  background.appendChild(bar);
  barRoot.appendChild(background);

  return new Promise(async (resolve, reject) => {
    var fileReader = new FileReader();
    var fileContent = new Uint8Array();
    fileReader.onload = async function () {
      try {
        var readerResult = fileReader.result;
        fileContent = new Uint8Array(readerResult);
        if (!("TextDecoder" in window))
          alert("Sorry, this browser does not support TextDecoder...");

        var enc = new TextDecoder("utf-8");
        if (DEBUGMODE)
          console.log('File Content length', fileContent.length);
        const sizeHexString = toHexString(fileContent.length);
        if (DEBUGMODE)
          console.log('Size Hex String', sizeHexString);

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
        if (DEBUGMODE)
          console.log('Number of Data Packets', nOfDataPackets);
        const nOfDataPacketsHexString = toHexString(nOfDataPackets);

        await writeToStream(writer, 0xc0, 0x00, 0x02, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, /*|*/ parseInt(sizeHexString.substring(6, 8), 16), parseInt(sizeHexString.substring(4, 6), 16), parseInt(sizeHexString.substring(2, 4), 16), parseInt(sizeHexString.substring(0, 2), 16), /*|*/ parseInt(nOfDataPacketsHexString.substring(6, 8), 16), parseInt(nOfDataPacketsHexString.substring(4, 6), 16), parseInt(nOfDataPacketsHexString.substring(2, 4), 16), parseInt(nOfDataPacketsHexString.substring(0, 2), 16), /*|*/ 0x00, 0x04, 0x00, 0x00, /*|*/ adresses[filesFlashed], 0xc0);
        await read(secReader, 15000);

        for (var i = 0; i < nOfDataPackets; i++) {
          var subArr = fileContent.subarray(i * 1024, i * 1024 + 1024);
          var checkSum = 0xef;
          for (var j = 0; j < subArr.length; j++) {
            checkSum = checkSum ^ subArr[j]; //TODO: escape checksum
          }
          var indexHexString = toHexString(i);

          if (DEBUGMODE)
            console.log(`Subarray length: ${subArr.length}`);

          if (DEBUGMODE)
            console.log(`checksum: ${checkSum}`);

          if (DEBUGMODE)
            console.log(`Index Hex String: ${indexHexString}`);

          await writeToStream(writer, 0xc0, 0x00, 0x03, 0x10, 0x04, checkSum /*0xcc*/, 0x00, 0x00, 0x00, /*lengthHexString[0], lengthHexString[1], lengthHexString[2], lengthHexString[3],*/ 0x00, 0x04, 0x00, 0x00, parseInt(indexHexString.substring(6, 8), 16), parseInt(indexHexString.substring(4, 6), 16), parseInt(indexHexString.substring(2, 4), 16), parseInt(indexHexString.substring(0, 2), 16), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, subArr, 0xc0);

          await read(secReader, 15000);
          //Update status bar
          progress = i * 100 / nOfDataPackets;
          bar.style.width = progress + "%";

          if (paragraph.hasChildNodes()) {
            paragraph.removeChild(node);
          }
          node = document.createTextNode(progress.toFixed(2) + "%");
          //paragraph.style = 'text-align: center;';
          paragraph.appendChild(node);
        }
        progress = 100;
        bar.style.width = progress + "%";
        if (paragraph.hasChildNodes()) {
          paragraph.removeChild(node);
        }
        node = document.createTextNode(progress.toFixed(2) + "%");
        paragraph.appendChild(node);
        console.log(`File ${url.substring(url.lastIndexOf("/") + 1, url.length)} sended`);
        progress = 1;
        //get md5 checksum from esp
        //c0001310000000000000100000003e00000000000000000000c0
        await writeToStream(writer, 0xc0, 0x00, 0x13, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, adresses[filesFlashed], parseInt(sizeHexString.substring(6, 8), 16), parseInt(sizeHexString.substring(4, 6), 16), parseInt(sizeHexString.substring(2, 4), 16), parseInt(sizeHexString.substring(0, 2), 16), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xc0);
        const md5SlipFrame = await read(secReader, 10000);
        const md5checksumToCheck = enc.decode(md5SlipFrame.data.buffer);
        if (DEBUGMODE)
          console.log('Checksum from chip: ', md5checksumToCheck);
        if (DEBUGMODE)
          console.log('Checksum from file: ', md5checksum);
        barRoot.innerHTML = '';
        if (md5checksumToCheck.localeCompare(md5checksum) == 0) {
          filesFlashed = filesFlashed + 1;
          resolve();
        } else {
          reject(new Error('Checksum from chip not equal to checksum from file'));
        }
      } catch (e) {
        reject(e);
      }
    };
    downloadBlobFromUrl(url, fileReader);
  });


}
export function resetFilesFlashed() {
  filesFlashed = 0;
}
function downloadBlobFromUrl(url, fileReader) {
  const xhr = new XMLHttpRequest();
  xhr.open('GET', url);
  xhr.responseType = 'blob';
  xhr.setRequestHeader('Accept', 'application/octet-stream');
  xhr.onload = () => {
    fileReader.readAsArrayBuffer(xhr.response);
  };
  xhr.send();
}
export function downloadBlobFromUrlAsText(url) {
  return new Promise((resolve, reject) => {
    const fileReader = new FileReader();
    fileReader.onload = () => {
      const res = fileReader.result;
      resolve(res);
    };
    fileReader.onerror = () => {
      reject(new Error('Error in read file on downloadBlobFromUrlAsText, url: ' + url));
    };
    const xhr = new XMLHttpRequest();
    xhr.open('GET', url);
    xhr.responseType = 'blob';
    xhr.setRequestHeader('Accept', 'application/octet-stream');
    xhr.onload = () => {
      fileReader.readAsText(xhr.response);
    };
    xhr.onerror = () => {
      reject(new Error('Error in http request from downloadBlobFromUrlAsText, url: ' + url));
    };
    xhr.send();
  });
}
function concatTypedArrays(a, b) {
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
export async function syncAndRead(secReader, ntimes, writer) {
  if (ntimes <= 0)
    throw new Error('Synchronization with Chip failed');
  await sync(writer);
  try {
    await read(secReader, 1500);
  } catch (e) {
    await syncAndRead(secReader, ntimes - 1, writer);
    return;
  }
  //Consume all sync responses from Chip until timeout occures
  var notFailed = true;
  while (notFailed) {
    try {
      await read(secReader, 1500);
    } catch (e) {
      notFailed = false;
    }
  }
}
export async function enterBootloader(port) {
  await port.setSignals({ dataTerminalReady: false });
  await port.setSignals({ requestToSend: true });
  await new Promise(resolve => setTimeout(resolve, 100));
  await port.setSignals({ dataTerminalReady: true });
  await port.setSignals({ requestToSend: false });

  await new Promise(resolve => setTimeout(resolve, 100));

  await port.setSignals({ dataTerminalReady: false });

  await new Promise(resolve => setTimeout(resolve, 3200));
}
export async function endFlash(writer) {
  await writeToStream(writer, 0xc0, 0x00, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xc0);
}
export async function reset(port) {

  await port.setSignals({ requestToSend: true });
  await new Promise(resolve => setTimeout(resolve, 100));
  await port.setSignals({ requestToSend: false });
}

async function writeToStream(writer, ...lines) {
  if (DEBUGMODE)
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
export async function read(reader, timeOut) {
  //try {
  let timeoutHandle;
  const timeoutPromise = new Promise((resolve, reject) => {
    timeoutHandle = setTimeout(() => reject('Timed out'), timeOut);
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
    if (DEBUGMODE)
      console.log(JSON.stringify(value, null, 2) + '\n');
    //throw new Error('Test Errror');
    if (value.data[value.data.length - 4] > 0) {
      throw new Error(`Fail from chip: slip frame fail code: ${value.data[value.data.length - 3]}`);
    }
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
