import { TextDecoderStream, TextEncoderStream } from "@stardazed/streams-text-encoding";
import {ReadableStream, TransformStream} from "@stardazed/streams";
//import {Transform} from 'stream';
import React from 'react';
import logo from '../logo.svg';
import './SerialIsActive.css'

//const {Transform} = require('stream');
function SerialIsActive() {
  //var outputStream;
  var writer = null;
  var port = null;
  var serialActive = 'not active';
  if ('serial' in navigator) {
    serialActive = 'active';
  }

  async function click2() {
    // writeToStream(outputStream.getWriter(), '\x01');

    writeToStream(writer, 0xc0, 0x00, 0x08, 0x07, 0x07, 0x12, 0x20, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0xc0);
    //writeToStream(writer, '\xc0', '\x00', '\x08','\x07','\x07','\x12','\x20', '\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55', '\xc0'); //, 'echo(false);', 'console.log("yes");'
    //b'\x07\x07\x12\x20' + 32 * b'\x55'
  }

  async function readreg(){
    //c0000a0400000000000ca00160c0
    writeToStream(writer, 0xc0, 0x00, 0x0a, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0c, 0xa0, 0x01, 0x60, 0xc0);
  }


  async function click() {
    port = await navigator.serial.requestPort();
    // - Wait for the port to open.
    await port.open({ baudRate: 115200 });
    //9600
    //460800

    /*const transRead = new ReadableStream({
      start(controller){
        this.chunks = [];
        this.writer = 
        var {value, done} = await reader.read();
        if (done){
          reader.releaseLock();
        } else {

        }
      }
    });*/

    const trans = new TransformStream();/*{readable: new ReadableStream({
      start(controller) {
        reader = this.getReader();
        // Die folgende Funktion behandelt jeden Daten-Chunk
        function push() {
          // "done" ist ein Boolean und "value" ein "Uint8Array"
          return reader.read().then(({ done, value }) => {
            // Gibt es weitere Daten zu laden?
            if (done) {
              // Teile dem Browser mit, dass wir fertig mit dem Senden von Daten sind
              controller.close();
              return;
            }
  
            // Bekomme die Daten und sende diese an den Browser durch den Controller weiter
            controller.enqueue(value);
          }).then(push);
        };
  
        push();
      }
    }),
      writeable: new WritableStream({
        write(chunk, controller){
          controller.enqueue(chunk);
        }
      })}*/

    const decoder = new TextDecoderStream();
    //const devReadable = port.readable;
    const [stream1, stream2] = port.readable.tee();
    //const webReadable = toWebReadableStream(stream1);
    const inputDone = stream2.pipeThrough(trans).pipeTo(decoder.writable);
    const inputStream = decoder.readable;
    //const inputStream = port.readable;


    //pipe
    //let reader = port.readable.pipeThrough(transformStream).getReader();

    let reader = inputStream.getReader();
    //let reader = port.readable.getReader();

    //write
    //const encoder = new TextEncoderStream();
    //const outputDone = encoder.readable.pipeTo(port.writable);
    //const outputStream = encoder.writable;
    //writer = outputStream.getWriter();
    writer = port.writable.getWriter();

    readLoop(reader);

    //writeToStream(outputStream.getWriter(), '\x01'); //, 'echo(false);', 'console.log("yes");'


  }

  async function enterBootloader(){
    
    await port.setSignals({ dataTerminalReady: false });
    await port.setSignals({ requestToSend: true });
    await new Promise(resolve => setTimeout(resolve, 100));
    await port.setSignals({ dataTerminalReady: true });
    await port.setSignals({ requestToSend: false });

    await new Promise(resolve => setTimeout(resolve, 100));

    await port.setSignals({ dataTerminalReady: false });

    await new Promise(resolve => setTimeout(resolve, 3200));
  }

  async function reset(){
    
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

  return (
    <div className="ActiveApp">
      <header className="ActiveApp-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          Serial is {serialActive}
        </p>
        <button onClick={click}>Connect</button>
        <button onClick={enterBootloader}>enter bootloader</button>
        <button onClick={click2}>sync</button>
        <button onClick={reset}>reset</button>
        <button onClick={readreg}>readreg</button>
      </header>
    </div>
  );
}

export default SerialIsActive;
