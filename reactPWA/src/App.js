import { TextDecoderStream, TextEncoderStream } from "@stardazed/streams-text-encoding";
import React from 'react';
import logo from './logo.svg';
import './App.css';

function App() {
  //var outputStream;
  var writer = null;
  var port = null;
  var serialActive = 'not active';
  if ('serial' in navigator) {
    serialActive = 'active';
  }

  async function click2() {
    // writeToStream(outputStream.getWriter(), '\x01');
    

    writeToStream(writer, '\xc0', '\x00', '\x08','\x07','\x07','\x12','\x20', '\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55','\x55', '\xc0'); //, 'echo(false);', 'console.log("yes");'
    //b'\x07\x07\x12\x20' + 32 * b'\x55'
  }


  async function click() {
    port = await navigator.serial.requestPort();
    // - Wait for the port to open.
    await port.open({ baudRate: 115200 });
    //9600
    //460800

    await port.setSignals({ dataTerminalReady: false });
    await port.setSignals({ requestToSend: true });
    await new Promise(resolve => setTimeout(resolve, 100));
    await port.setSignals({ dataTerminalReady: true });
    await port.setSignals({ requestToSend: false });

    await new Promise(resolve => setTimeout(resolve, 100));

    await port.setSignals({ dataTerminalReady: false });

    await new Promise(resolve => setTimeout(resolve, 3200));

    const decoder = new TextDecoderStream();
    const inputDone = port.readable.pipeTo(decoder.writable);
    const inputStream = decoder.readable;

    let reader = inputStream.getReader();
    //let reader = port.readable.getReader();

    //write
    const encoder = new TextEncoderStream();
    //const outputDone = encoder.readable.pipeTo(port.writable);
    //const outputStream = encoder.writable;
    //writer = outputStream.getWriter();
    writer = port.writable.getWriter();

    readLoop(reader);

    //writeToStream(outputStream.getWriter(), '\x01'); //, 'echo(false);', 'console.log("yes");'


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
          console.log(value + '\n');
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
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          Serial is {serialActive}
        </p>
        <button onClick={click}>Connect</button>
        <button onClick={click2}>enter bootloader</button>
      </header>
    </div>
  );
}

export default App;
