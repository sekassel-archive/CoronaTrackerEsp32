const SlipFrame = require('../js/SlipFrame');
test('test desc', () => {
    const slipframe = new SlipFrame();
    slipframe.insert(0xc0);
    expect(slipframe.startSetted).toBe(true);
    slipframe.insert(0x00);
    expect(slipframe.direction).toBe(0x00);
    slipframe.insert(0x02);
    expect(slipframe.command).toBe(0x02);
    slipframe.insert(0x02);
    slipframe.insert(0x00);
    expect(slipframe.size[0]).toBe(0x02);
    expect(slipframe.size[1]).toBe(0x00);
    slipframe.insert(0x00);
    slipframe.insert(0x00);
    slipframe.insert(0x00);
    slipframe.insert(0x00);
    for (var i = 0; i < 4; i++){
        expect(slipframe.value[i]).toBe(0x00);
    }
    slipframe.insert(0xdb);
    slipframe.insert(0xdc);
    slipframe.insert(0xdb);
    slipframe.insert(0xdd);
    try{
        slipframe.insert(0x00);
        fail('should not reach');
    } catch(e){

    }
    expect(slipframe.data[0]).toBe(0xc0);
    expect(slipframe.data[1]).toBe(0xdb);
    slipframe.insert(0xc0);
    expect(slipframe.endSetted).toBe(true);
});
