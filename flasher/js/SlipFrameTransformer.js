import { SlipFrame } from './SlipFrame.js';

export class SlipFrameTransformer {
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
