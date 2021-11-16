import { test } from 'uvu';
import * as assert from 'uvu/assert';
import { SlipFrameTransformer } from '../js/SlipFrameTransformer.js';

test('Test SlipFrameTransformer', async () => {
    const transformStream = new TransformStream(new SlipFrameTransformer());
    const writer = transformStream.writable.getWriter();
    const reader = transformStream.readable.getReader();

    await writer.write(0xc0);
    await writer.write(0x00);
    await writer.write(0x03);
    await writer.write(0x02);
    await writer.write(0x00);
    await writer.write(0x00);
    await writer.write(0x00);
    await writer.write(0x00);
    await writer.write(0x00);
    await writer.write(0xdb);
    await writer.write(0xdc);
    await writer.write(0xdb);
    await writer.write(0xdd);
    await writer.write(0xc0);

    const { value, done } = await reader.read();

    assert.equal(value.data[0], 0xc0, 'Data[0]');
});

test.run()