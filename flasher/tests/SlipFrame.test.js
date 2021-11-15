import { test } from 'uvu';
import * as assert from 'uvu/assert';
import { SlipFrame } from '../js/SlipFrame.js';

test('test desc', () => {
    const slipframe = new SlipFrame();
    slipframe.insert(0xc0);
    assert.equal(slipframe.startSetted, true, 'start setted');
    slipframe.insert(0x00);
    assert.equal(slipframe.direction, 0x00, 'direction');
    slipframe.insert(0x02);
    assert.equal(slipframe.command, 0x02, 'command');
    slipframe.insert(0x02);
    slipframe.insert(0x00);
    assert.equal(slipframe.size[0], 0x02, 'size 0');
    assert.equal(slipframe.size[1], 0x00, 'size 1');
    slipframe.insert(0x00);
    slipframe.insert(0x00);
    slipframe.insert(0x00);
    slipframe.insert(0x00);
    for (var i = 0; i < 4; i++){
        assert.equal(slipframe.value[i], 0x00, 'value');
    }
    slipframe.insert(0xdb);
    slipframe.insert(0xdc);
    slipframe.insert(0xdb);
    slipframe.insert(0xdd);
    try{
        slipframe.insert(0x00);
        assert.equal(1, 2, 'should not reach');
    } catch(e){

    }
    assert.equal(slipframe.data[0], 0xc0, 'data 0');
    assert.equal(slipframe.data[1], 0xdb, 'data 1');
    slipframe.insert(0xc0);
    assert.equal(slipframe.endSetted, true, 'end setted');
});

test.run()