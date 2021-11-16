import { SlipFrameTransformer } from './SlipFrameTransformer.js';
import { enterBootloader, syncAndRead, spiAttach, read, spiSetParams, downloadBlobFromUrlAsText, flashFileFromUrl, endFlash, reset } from './chipCommunication.js';

window.onload = () => {
  'use strict';

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('../sw.js');
  }

  if (!('serial' in navigator)) {
    document.location.href = '/pages/serialnotactive.html';
  }
}

export const DEBUGMODE = false;
const adress1 = Uint8Array.of(0x00, 0x10, 0x00, 0x00);
const adress2 = Uint8Array.of(0x00, 0x80, 0x00, 0x00);
const adress3 = Uint8Array.of(0x00, 0xe0, 0x00, 0x00);
const adress4 = Uint8Array.of(0x00, 0x00, 0x01, 0x00);

document.getElementById('connectButton').addEventListener('click', () => {
  connect();
});

var port = null;
var writer = null;
var secReader = null;
let inputStream = null;
let abortController = null;
var readerClosed = null;
async function connect() {
  document.getElementById("statusBarRoot").innerHTML = '';
  try {
    port = await navigator.serial.requestPort();
    document.getElementById('connectButton').style.display = 'none';
    try {
      // - Wait for the port to open.
      await port.open({ baudRate: 115200 });
      document.getElementById('statusPic').setAttribute("src", '../images/upload.gif');

      writer = port.writable.getWriter();

      await new Promise(resolve => setTimeout(resolve, 100));
      await enterBootloader(port);

      const slipTransformer = new TransformStream(new SlipFrameTransformer());
      readerClosed = port.readable.pipeTo(slipTransformer.writable);
      secReader = slipTransformer.readable.getReader();

      //const baseUrl = 'http://localhost/';
      const baseUrl = 'https://flasher.uniks.de/';

      console.log('synchronize with chip');
      await syncAndRead(secReader, 8, writer);
      console.log('synchronized, prepare flashing with spi');
      await spiAttach(writer);
      await read(secReader, 1500);
      await spiSetParams(writer);
      await read(secReader, 1500);
      const hashesJsonText = await downloadBlobFromUrlAsText(baseUrl + 'hashes/hashes.json');
      const hashesJson = JSON.parse(hashesJsonText);
      await flashFileFromUrl(baseUrl + 'firmwares/bootloader_dio_40m.bin', hashesJson['bootloader'], writer, secReader, adress1);
      await flashFileFromUrl(baseUrl + 'firmwares/partitions.bin', hashesJson['partitions'], writer, secReader, adress2);
      await flashFileFromUrl(baseUrl + 'firmwares/boot_app0.bin', hashesJson['bootapp'], writer, secReader, adress3);
      await flashFileFromUrl(baseUrl + 'firmwares/firmware.bin', hashesJson['firmware'], writer, secReader, adress4);

      await endFlash(writer);
      await reset(port);


      const sendedParagraph = document.createElement("p");
      const node = document.createTextNode("Flashing completed!");
      console.log("flashing completed");
      sendedParagraph.appendChild(node);
      sendedParagraph.style.color = 'green';
      sendedParagraph.style.fontWeight = 'bold';
      const barRoot = document.getElementById("statusBarRoot");
      barRoot.innerHTML = '';
      barRoot.appendChild(sendedParagraph);

      document.getElementById('statusPic').setAttribute("src", '../images/flashComplete.png');
    } catch (e) {
      console.log(e);

      document.getElementById('statusPic').setAttribute("src", '../images/failed.png')

      const failParagraph = document.createElement("p");
      const node = document.createTextNode("An error occured while flashing. Please try again!");
      failParagraph.appendChild(node);
      failParagraph.style.color = 'red';
      const barRoot = document.getElementById("statusBarRoot");
      barRoot.innerHTML = '';
      barRoot.appendChild(failParagraph);

      const failParagraph1 = document.createElement("p");
      const node1 = document.createTextNode("(maybe reload page and reconnect chip)");
      failParagraph1.appendChild(node1);
      failParagraph1.style.color = 'red';
      barRoot.appendChild(failParagraph1);

      const failParagraph2 = document.createElement("p");
      const node2 = document.createTextNode(`${e.name} message: ${e.message}`);
      failParagraph2.appendChild(node2);
      failParagraph2.style.color = 'red';
      failParagraph2.style.borderStyle = 'solid';
      failParagraph2.style.borderColor = 'red';
      failParagraph2.style.fontWeight = 'bold';
      failParagraph2.style.padding = '5px';
      barRoot.appendChild(failParagraph2);
    } finally {
      await secReader.cancel().catch(() => { });
      await readerClosed.catch(() => { });
      await writer.close().catch(() => { });
      await port.close().catch(() => { });

      secReader = null;
      writer = null;
      port = null;
      document.getElementById('connectButton').style.display = 'block';
    }
  } catch { }
}
